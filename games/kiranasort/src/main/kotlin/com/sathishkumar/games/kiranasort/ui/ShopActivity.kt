package com.sathishkumar.games.kiranasort.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.kiranasort.R
import kotlin.math.roundToInt

/** Spend coins to grow the shop from a street stall to a supermarket. */
class ShopActivity : Activity() {
    private lateinit var store: ShopStore
    private lateinit var shopView: ShopView
    private lateinit var title: TextView
    private lateinit var coins: TextView
    private lateinit var bonus: TextView
    private lateinit var upgrade: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop)
        store = ShopStore(GamePrefs(this, "kiranasort"))
        shopView = findViewById(R.id.shop_view)
        title = findViewById(R.id.shop_title)
        coins = findViewById(R.id.shop_coins)
        bonus = findViewById(R.id.shop_bonus)
        upgrade = findViewById(R.id.upgrade_button)
        findViewById<Button>(R.id.back_button).setOnClickListener { finish() }
        upgrade.setOnClickListener {
            if (store.upgrade()) {
                Haptics.tap(upgrade)
                render()
            }
        }
        render()
    }

    private fun render() {
        val p = store.progress
        shopView.stage = p.stage
        title.text = p.stage.title
        coins.text = getString(R.string.coins_n, p.coins)
        bonus.text = getString(R.string.coin_bonus, ((p.stage.coinMultiplier - 1) * 100).roundToInt())
        val next = p.stage.next
        if (next == null) {
            upgrade.text = getString(R.string.shop_maxed)
            upgrade.isEnabled = false
        } else {
            upgrade.text = getString(R.string.upgrade_to, next.title, next.upgradeCost)
            upgrade.isEnabled = p.canUpgrade
        }
    }
}
