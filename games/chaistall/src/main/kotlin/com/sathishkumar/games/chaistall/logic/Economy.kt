package com.sathishkumar.games.chaistall.logic

import kotlin.math.pow

/** Something the stall sells. Income is per second at level 1, before any multiplier. */
enum class Product(val title: String, val emoji: String, val unlockCost: Double, val baseIncome: Double) {
    CHAI("Chai", "☕", 0.0, 1.0),
    SAMOSA("Samosa", "🥟", 60.0, 4.0),
    VADA("Vada", "🍩", 700.0, 25.0),
    BUN_MASKA("Bun maska", "🍞", 8_000.0, 150.0),
}

/**
 * Where the business runs. The roadside stall is where every player starts; each branch
 * multiplies everything it sells and costs more to grow.
 */
enum class Location(
    val title: String,
    val unlockCost: Double,
    val multiplier: Double,
    val helperBaseCost: Double,
) {
    ROADSIDE("Roadside stall", 0.0, 1.0, 100.0),
    CHENNAI("Chennai", 3_000_000.0, 5.0, 20_000.0),
    MUMBAI("Mumbai", 500_000_000.0, 25.0, 1_000_000.0),
    DELHI("Delhi", 50_000_000_000.0, 125.0, 50_000_000.0),
}

/** All the numbers that balance the game, in one place. */
object Economy {
    /** Each upgrade costs this much more than the last. */
    const val COST_GROWTH = 1.15

    /** Every this many levels, a product's income doubles. */
    const val MILESTONE_LEVELS = 25

    const val MAX_HELPERS = 3

    /** Each helper adds this share of a location's income. */
    const val HELPER_BONUS = 0.5

    /** Helpers keep earning while the app is closed, at this share of the normal rate... */
    const val OFFLINE_RATE = 0.5

    /** ...for at most this long. */
    const val OFFLINE_CAP_MS = 8 * 60 * 60 * 1000L

    const val BOOST_MS = 4 * 60 * 60 * 1000L
    const val BOOST_MAX_MS = 8 * 60 * 60 * 1000L
    const val BOOST_MULTIPLIER = 2.0

    /** Rewarded "free upgrade" can be used this often. */
    const val FREE_UPGRADE_COOLDOWN_MS = 5 * 60 * 1000L

    /**
     * Cost to take [product] at [location] from [level] to level + 1 (level 0 = unlock).
     * Branch costs grow faster than branch income (power 1.5), so each city is a real step up.
     */
    fun upgradeCost(location: Location, product: Product, level: Int): Double {
        val base = if (product == Product.CHAI) 10.0 else product.unlockCost
        return base * location.multiplier.pow(1.5) * COST_GROWTH.pow(level)
    }

    fun helperCost(location: Location, hired: Int): Double = location.helperBaseCost * 5.0.pow(hired)

    /** Income per second of one product at [level], before helpers and boosts. */
    fun productIncome(location: Location, product: Product, level: Int): Double {
        if (level <= 0) return 0.0
        val milestones = level / MILESTONE_LEVELS
        return product.baseIncome * level * 2.0.pow(milestones) * location.multiplier
    }
}
