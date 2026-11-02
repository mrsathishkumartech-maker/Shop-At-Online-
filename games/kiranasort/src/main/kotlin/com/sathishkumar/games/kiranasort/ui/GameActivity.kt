package com.sathishkumar.games.kiranasort.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads
import com.sathishkumar.games.kiranasort.BuildConfig
import com.sathishkumar.games.kiranasort.R
import com.sathishkumar.games.kiranasort.logic.KiranaSession
import com.sathishkumar.games.kiranasort.logic.LevelGenerator
import com.sathishkumar.games.kiranasort.logic.MoveResult

class GameActivity : Activity() {
    private lateinit var prefs: GamePrefs
    private lateinit var ads: Ads
    private lateinit var shop: ShopStore
    private val generator = LevelGenerator()
    private lateinit var session: KiranaSession

    private lateinit var shelves: ShelfView
    private lateinit var levelText: TextView
    private lateinit var timerText: TextView
    private lateinit var coinsText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var lastTick = 0L
    private var resumed = false
    private var dialogOpen = false

    private val ticker = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            if (session.tick(now - lastTick)) onTimeUp()
            lastTick = now
            updateHud()
            if (timerRunning()) handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        prefs = GamePrefs(this, GAME_ID)
        shop = ShopStore(prefs)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))

        shelves = findViewById(R.id.shelves)
        levelText = findViewById(R.id.level_text)
        timerText = findViewById(R.id.timer_text)
        coinsText = findViewById(R.id.coins_text)

        shelves.onMove = ::onMoved
        shelves.onRejected = { Haptics.error(shelves) }
        findViewById<Button>(R.id.extra_shelf_button).setOnClickListener { offerExtraShelf() }
        findViewById<Button>(R.id.shuffle_button).setOnClickListener { offerShuffle() }
        findViewById<Button>(R.id.extra_time_button).setOnClickListener { offerExtraTime() }
        findViewById<Button>(R.id.restart_button).setOnClickListener { restartLevel() }
        findViewById<Button>(R.id.shop_button).setOnClickListener {
            startActivity(Intent(this, ShopActivity::class.java))
        }

        loadLevel(prefs.currentLevel)
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        // Reload: coins and stage may have changed in the shop screen.
        shop = ShopStore(prefs)
        updateHud()
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        resumed = false
        handler.removeCallbacks(ticker)
    }

    private fun timerRunning() = resumed && !dialogOpen && !session.isOver

    private fun startTimer() {
        handler.removeCallbacks(ticker)
        if (!timerRunning()) return
        lastTick = SystemClock.elapsedRealtime()
        handler.postDelayed(ticker, TICK_MS)
    }

    private fun loadLevel(number: Int) {
        prefs.currentLevel = number
        session = KiranaSession(generator.generate(number))
        dialogOpen = false
        shelves.bind(session)
        updateHud()
        startTimer()
    }

    private fun updateHud() {
        levelText.text = getString(R.string.level_n, session.level.number)
        val seconds = ((session.remainingMs + 999) / 1000).toInt()
        timerText.text = getString(R.string.timer, seconds / 60, seconds % 60)
        timerText.setTextColor(getColor(if (seconds <= 10) R.color.warning else R.color.text))
        coinsText.text = getString(R.string.coins_n, shop.progress.coins)
    }

    private fun onMoved(result: MoveResult) {
        if (result.cleared.isNotEmpty()) Haptics.tap(shelves)
        updateHud()
        when {
            session.isWon -> shelves.postDelayed(::showWin, 500)
            session.isStuck -> showStuck()
        }
    }

    // --- Rewarded helpers --------------------------------------------------------------

    private fun withRewardedAd(action: () -> Unit) {
        dialogOpen = true
        handler.removeCallbacks(ticker)
        ads.showRewarded(
            this,
            onReward = {
                dialogOpen = false
                action()
                startTimer()
            },
            onUnavailable = {
                dialogOpen = false
                Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show()
                when {
                    session.isTimeUp -> onTimeUp()
                    session.isStuck -> showStuck()
                    else -> startTimer()
                }
            },
        )
    }

    private fun offerExtraShelf() {
        if (session.isOver) return
        withRewardedAd {
            session.addShelf()
            shelves.refresh()
        }
    }

    private fun offerShuffle() {
        if (session.isOver) return
        withRewardedAd {
            val cleared = session.shuffle()
            shelves.refresh(cleared.map { it.first })
            updateHud()
            when {
                session.isWon -> showWin()
                session.isStuck -> showStuck()
            }
        }
    }

    private fun offerExtraTime() {
        if (session.isWon) return
        withRewardedAd {
            session.addTime(EXTRA_TIME_MS)
            updateHud()
        }
    }

    private fun restartLevel() {
        session.restart()
        dialogOpen = false
        shelves.refresh()
        updateHud()
        startTimer()
    }

    // --- End states -------------------------------------------------------------------

    private fun showWin() {
        if (dialogOpen || isFinishing || !session.isWon) return
        dialogOpen = true
        handler.removeCallbacks(ticker)
        val level = session.level.number
        val earned = shop.earn(session.baseCoins())
        prefs.recordStars(level, 1)
        updateHud()
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(getString(R.string.level_cleared, level))
            .setMessage(getString(R.string.coins_earned, earned, shop.progress.stage.title))
            .setCancelable(false)
            .setPositiveButton(R.string.next_level) { _, _ -> loadLevel(level + 1) }
            .setNeutralButton(R.string.shop) { _, _ ->
                loadLevel(level + 1)
                startActivity(Intent(this, ShopActivity::class.java))
            }
            .show()
    }

    private fun onTimeUp() {
        if (dialogOpen || isFinishing) return
        dialogOpen = true
        handler.removeCallbacks(ticker)
        updateHud()
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.time_up)
            .setMessage(R.string.time_up_message)
            .setCancelable(false)
            .setPositiveButton(R.string.extra_time_ad) { _, _ ->
                dialogOpen = false
                offerExtraTime()
            }
            .setNegativeButton(R.string.retry) { _, _ -> restartLevel() }
            .show()
    }

    private fun showStuck() {
        if (dialogOpen || isFinishing) return
        dialogOpen = true
        handler.removeCallbacks(ticker)
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.stuck)
            .setMessage(R.string.stuck_message)
            .setCancelable(false)
            .setPositiveButton(R.string.extra_shelf_ad) { _, _ ->
                dialogOpen = false
                offerExtraShelf()
            }
            .setNeutralButton(R.string.shuffle_ad) { _, _ ->
                dialogOpen = false
                offerShuffle()
            }
            .setNegativeButton(R.string.retry) { _, _ -> restartLevel() }
            .show()
    }

    private companion object {
        const val GAME_ID = "kiranasort"
        const val TICK_MS = 100L
        const val EXTRA_TIME_MS = 30_000L
    }
}
