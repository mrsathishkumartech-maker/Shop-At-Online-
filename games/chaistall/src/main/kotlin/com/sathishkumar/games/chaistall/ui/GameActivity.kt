package com.sathishkumar.games.chaistall.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.chaistall.BuildConfig
import com.sathishkumar.games.chaistall.R
import com.sathishkumar.games.chaistall.logic.Economy
import com.sathishkumar.games.chaistall.logic.Location
import com.sathishkumar.games.chaistall.logic.Money
import com.sathishkumar.games.chaistall.logic.Product
import com.sathishkumar.games.chaistall.logic.TycoonState
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads

class GameActivity : Activity() {
    private lateinit var prefs: GamePrefs
    private lateinit var ads: Ads
    private lateinit var state: TycoonState
    private var selected = Location.ROADSIDE

    private lateinit var cashText: TextView
    private lateinit var incomeText: TextView
    private lateinit var boostButton: Button
    private lateinit var stall: StallView
    private lateinit var tabs: LinearLayout
    private lateinit var rows: LinearLayout

    private class ProductRow(val product: Product, val detail: TextView, val buy: Button, val free: Button)
    private val productRows = mutableListOf<ProductRow>()
    private var helperDetail: TextView? = null
    private var helperButton: Button? = null
    private var openButton: Button? = null

    private val handler = Handler(Looper.getMainLooper())
    private var lastTick = 0L
    private var ticks = 0

    private val ticker = object : Runnable {
        override fun run() {
            val elapsed = SystemClock.elapsedRealtime()
            state.tick(elapsed - lastTick, System.currentTimeMillis())
            lastTick = elapsed
            if (++ticks % 2 == 0) render()
            if (ticks % 100 == 0) save()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        prefs = GamePrefs(this, GAME_ID)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))
        // Loaded once: the in-memory state stays the source of truth while the app is alive.
        state = TycoonState.decode(prefs.getString(KEY_STATE))

        cashText = findViewById(R.id.cash_text)
        incomeText = findViewById(R.id.income_text)
        boostButton = findViewById(R.id.boost_button)
        stall = findViewById(R.id.stall)
        tabs = findViewById(R.id.location_tabs)
        rows = findViewById(R.id.rows)

        stall.onServe = {
            val earned = state.serve(System.currentTimeMillis())
            Haptics.tap(stall)
            render()
            "+" + Money.format(earned)
        }
        boostButton.setOnClickListener {
            withRewardedAd {
                state.addBoost(System.currentTimeMillis())
                save()
                render()
            }
        }
        buildTabs()
        selectLocation(Location.ROADSIDE)
    }

    override fun onResume() {
        super.onResume()
        val now = System.currentTimeMillis()
        val away = if (state.lastSeenMs > 0) now - state.lastSeenMs else 0
        val offline = state.offlineEarnings(now)
        state.lastSeenMs = now
        if (offline > 0) {
            // Credit it straight away so it's never lost; the dialog only offers the ad bonus.
            state.earn(offline)
            // Short breaks (like watching an ad) don't need a dialog.
            if (away >= OFFLINE_DIALOG_MIN_AWAY_MS) showOfflineDialog(offline, away)
        }
        save()
        lastTick = SystemClock.elapsedRealtime()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        state.tick(SystemClock.elapsedRealtime() - lastTick, System.currentTimeMillis())
        lastTick = SystemClock.elapsedRealtime()
        save()
    }

    private fun save() {
        state.lastSeenMs = System.currentTimeMillis()
        prefs.putString(KEY_STATE, state.encode())
    }

    private fun withRewardedAd(action: () -> Unit) {
        ads.showRewarded(this, onReward = action, onUnavailable = {
            Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show()
        })
    }

    // --- Building the screen -----------------------------------------------------------

    private fun buildTabs() {
        tabs.removeAllViews()
        for (loc in Location.entries) {
            val b = LayoutInflater.from(this).inflate(R.layout.item_tab, tabs, false) as Button
            b.tag = loc
            b.setOnClickListener { selectLocation(loc) }
            tabs.addView(b)
        }
    }

    private fun selectLocation(location: Location) {
        selected = location
        rows.removeAllViews()
        productRows.clear()
        helperDetail = null
        helperButton = null
        openButton = null
        val inflater = LayoutInflater.from(this)

        if (!state.at(location).unlocked) {
            val v = inflater.inflate(R.layout.item_locked, rows, false)
            v.findViewById<TextView>(R.id.locked_text).text =
                getString(R.string.open_branch_text, location.title, location.multiplier.toInt())
            openButton = v.findViewById<Button>(R.id.open_button).apply {
                setOnClickListener {
                    if (state.open(location)) {
                        Haptics.tap(this)
                        save()
                        selectLocation(location)
                    }
                }
            }
            rows.addView(v)
        } else {
            for (product in Product.entries) {
                val v = inflater.inflate(R.layout.item_product, rows, false)
                v.findViewById<TextView>(R.id.emoji).text = product.emoji
                v.findViewById<TextView>(R.id.name).text = product.title
                val row = ProductRow(product, v.findViewById(R.id.detail), v.findViewById(R.id.buy), v.findViewById(R.id.free))
                row.buy.setOnClickListener {
                    if (state.upgrade(selected, product)) {
                        Haptics.tap(row.buy)
                        render()
                    }
                }
                row.free.setOnClickListener {
                    withRewardedAd {
                        if (state.freeUpgrade(selected, product, System.currentTimeMillis())) {
                            save()
                            render()
                        }
                    }
                }
                productRows += row
                rows.addView(v)
            }
            val hv = inflater.inflate(R.layout.item_helper, rows, false)
            helperDetail = hv.findViewById(R.id.helper_detail)
            helperButton = hv.findViewById<Button>(R.id.hire_button).apply {
                setOnClickListener {
                    if (state.hireHelper(selected)) {
                        Haptics.tap(this)
                        save()
                        render()
                    }
                }
            }
            rows.addView(hv)
        }
        render()
    }

    private fun render() {
        val now = System.currentTimeMillis()
        cashText.text = Money.format(state.cash)
        incomeText.text = Money.perSecond(state.incomePerSecond(now))
        boostButton.text = if (state.isBoosted(now)) {
            getString(R.string.boost_active, duration(state.boostUntilMs - now))
        } else {
            getString(R.string.boost)
        }
        boostButton.isEnabled = state.boostUntilMs - now < Economy.BOOST_MAX_MS - Economy.BOOST_MS

        for (i in 0 until tabs.childCount) {
            val b = tabs.getChildAt(i) as Button
            val loc = b.tag as Location
            b.isSelected = loc == selected
            b.text = if (state.at(loc).unlocked) loc.title else getString(R.string.locked_tab, loc.title)
        }

        val loc = selected
        val here = state.at(loc)
        stall.show(loc, Product.entries.filter { here.level(it) > 0 }, here.helpers)

        openButton?.let {
            it.text = getString(R.string.open_for, Money.format(loc.unlockCost))
            it.isEnabled = state.canOpen(loc)
        }
        val freeTarget = state.freeUpgradeTarget(loc, now)
        for (row in productRows) {
            val level = here.level(row.product)
            val income = Economy.productIncome(loc, row.product, level)
            row.detail.text = if (level == 0) {
                getString(R.string.not_sold_yet)
            } else {
                getString(R.string.product_detail, level, Money.perSecond(income))
            }
            val cost = state.upgradeCost(loc, row.product)
            row.buy.text = getString(if (level == 0) R.string.unlock_for else R.string.upgrade_for, Money.format(cost))
            row.buy.isEnabled = state.canUpgrade(loc, row.product)
            row.free.visibility = if (row.product == freeTarget) View.VISIBLE else View.GONE
        }
        helperDetail?.text = getString(R.string.helper_detail, here.helpers, Economy.MAX_HELPERS)
        helperButton?.let { b ->
            val cost = state.helperCost(loc)
            if (cost == null) {
                b.text = getString(R.string.helpers_full)
                b.isEnabled = false
            } else {
                b.text = getString(R.string.hire_for, Money.format(cost))
                b.isEnabled = state.cash >= cost
            }
        }
    }

    private fun showOfflineDialog(amount: Double, awayMs: Long) {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.welcome_back)
            .setMessage(getString(R.string.offline_message, duration(minOf(awayMs, Economy.OFFLINE_CAP_MS)), Money.format(amount)))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.collect_double, Money.format(amount * 2))) { _, _ ->
                // The base amount is already in the till; the ad adds the same again.
                withRewardedAd { state.earn(amount); save(); render() }
            }
            .setNegativeButton(getString(R.string.collect, Money.format(amount)), null)
            .show()
    }

    private fun duration(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private companion object {
        const val GAME_ID = "chaistall"
        const val KEY_STATE = "state"
        const val TICK_MS = 100L
        const val OFFLINE_DIALOG_MIN_AWAY_MS = 60_000L
    }
}
