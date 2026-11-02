package com.sathishkumar.games.kiranasort.logic

import kotlin.math.roundToInt

/** The player's shop, from a roadside stall to a supermarket. Better shops earn more per level. */
enum class ShopStage(val title: String, val upgradeCost: Int, val coinMultiplier: Double) {
    STREET_STALL("Street stall", 0, 1.0),
    KIRANA_SHOP("Kirana shop", 150, 1.25),
    GENERAL_STORE("General store", 400, 1.5),
    MINI_MART("Mini mart", 1000, 1.75),
    SUPERMARKET("Supermarket", 2500, 2.0);

    val next: ShopStage? get() = entries.getOrNull(ordinal + 1)
}

class ShopProgress(coins: Int, stage: ShopStage) {
    var coins: Int = coins
        private set
    var stage: ShopStage = stage
        private set

    val canUpgrade: Boolean get() = stage.next?.let { coins >= it.upgradeCost } ?: false

    /** Coins actually awarded for a level worth [baseCoins] at the current stage. */
    fun reward(baseCoins: Int): Int = (baseCoins * stage.coinMultiplier).roundToInt()

    fun earn(baseCoins: Int): Int = reward(baseCoins).also { coins += it }

    fun upgrade(): Boolean {
        val next = stage.next ?: return false
        if (coins < next.upgradeCost) return false
        coins -= next.upgradeCost
        stage = next
        return true
    }
}
