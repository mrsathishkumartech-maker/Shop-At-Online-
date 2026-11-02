package com.sathishkumar.games.kiranasort.ui

import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.kiranasort.logic.ShopProgress
import com.sathishkumar.games.kiranasort.logic.ShopStage

/** Loads and saves [ShopProgress] (coins and shop stage). */
class ShopStore(private val prefs: GamePrefs) {
    val progress: ShopProgress = ShopProgress(
        coins = prefs.getInt(KEY_COINS),
        stage = ShopStage.entries.getOrElse(prefs.getInt(KEY_STAGE)) { ShopStage.STREET_STALL },
    )

    /** Awards coins for a level and saves; returns the coins actually added. */
    fun earn(baseCoins: Int): Int = progress.earn(baseCoins).also { save() }

    fun upgrade(): Boolean = progress.upgrade().also { if (it) save() }

    private fun save() {
        prefs.putInt(KEY_COINS, progress.coins)
        prefs.putInt(KEY_STAGE, progress.stage.ordinal)
    }

    private companion object {
        const val KEY_COINS = "coins"
        const val KEY_STAGE = "shop_stage"
    }
}
