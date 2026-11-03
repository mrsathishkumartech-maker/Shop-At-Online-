package com.sathishkumar.games.chaistall.logic

import kotlin.math.min

class LocationState(
    var unlocked: Boolean = false,
    var helpers: Int = 0,
    val levels: IntArray = IntArray(Product.entries.size),
) {
    fun level(product: Product): Int = levels[product.ordinal]
}

/**
 * Everything about one player's business. Times are wall-clock milliseconds
 * (System.currentTimeMillis on Android) so offline earnings survive the app being closed.
 */
class TycoonState(
    var cash: Double = 0.0,
    var lifetimeEarned: Double = 0.0,
    val locations: List<LocationState> = Location.entries.map { LocationState() },
    var boostUntilMs: Long = 0,
    var freeUpgradeReadyAtMs: Long = 0,
    /** When the player was last in the game; 0 = never (no offline earnings yet). */
    var lastSeenMs: Long = 0,
) {
    fun at(location: Location): LocationState = locations[location.ordinal]

    fun isBoosted(nowMs: Long): Boolean = nowMs < boostUntilMs

    /** Income per second for one location, including helpers but not the ad boost. */
    fun locationIncome(location: Location): Double {
        val state = at(location)
        if (!state.unlocked) return 0.0
        val products = Product.entries.sumOf { Economy.productIncome(location, it, state.level(it)) }
        return products * (1 + Economy.HELPER_BONUS * state.helpers)
    }

    fun incomePerSecond(nowMs: Long): Double {
        val base = Location.entries.sumOf { locationIncome(it) }
        return if (isBoosted(nowMs)) base * Economy.BOOST_MULTIPLIER else base
    }

    /** Earns [elapsedMs] worth of income. */
    fun tick(elapsedMs: Long, nowMs: Long) {
        if (elapsedMs <= 0) return
        earn(incomePerSecond(nowMs) * elapsedMs / 1000.0)
    }

    /** Tapping the stall serves a customer: one second of income, and at least ₹1. */
    fun serve(nowMs: Long): Double {
        val amount = maxOf(1.0, incomePerSecond(nowMs))
        earn(amount)
        return amount
    }

    fun earn(amount: Double) {
        cash += amount
        lifetimeEarned += amount
    }

    /**
     * What helpers earned while the player was away. Only locations with at least one helper
     * keep running; earnings are capped at [Economy.OFFLINE_CAP_MS]. Does not add the money.
     */
    fun offlineEarnings(nowMs: Long): Double {
        if (lastSeenMs <= 0 || nowMs <= lastSeenMs) return 0.0
        val away = min(nowMs - lastSeenMs, Economy.OFFLINE_CAP_MS)
        val staffed = Location.entries.filter { at(it).helpers > 0 }.sumOf { locationIncome(it) }
        return staffed * Economy.OFFLINE_RATE * away / 1000.0
    }

    // --- Spending ----------------------------------------------------------------------

    fun upgradeCost(location: Location, product: Product): Double =
        Economy.upgradeCost(location, product, at(location).level(product))

    fun canUpgrade(location: Location, product: Product): Boolean =
        at(location).unlocked && cash >= upgradeCost(location, product)

    fun upgrade(location: Location, product: Product): Boolean {
        if (!canUpgrade(location, product)) return false
        cash -= upgradeCost(location, product)
        at(location).levels[product.ordinal]++
        return true
    }

    fun isFreeUpgradeReady(nowMs: Long): Boolean = nowMs >= freeUpgradeReadyAtMs

    /**
     * The one product a rewarded ad can upgrade for free right now: the cheapest upgrade the
     * player can't yet afford, among products already on the menu. New products are never
     * free, so an ad can't skip the early game.
     */
    fun freeUpgradeTarget(location: Location, nowMs: Long): Product? {
        if (!at(location).unlocked || !isFreeUpgradeReady(nowMs)) return null
        return Product.entries
            .filter { at(location).level(it) > 0 && !canUpgrade(location, it) }
            .minByOrNull { upgradeCost(location, it) }
    }

    /** Rewarded-ad upgrade of [freeUpgradeTarget]: no cost, then a cooldown. */
    fun freeUpgrade(location: Location, product: Product, nowMs: Long): Boolean {
        if (freeUpgradeTarget(location, nowMs) != product) return false
        at(location).levels[product.ordinal]++
        freeUpgradeReadyAtMs = nowMs + Economy.FREE_UPGRADE_COOLDOWN_MS
        return true
    }

    fun helperCost(location: Location): Double? =
        at(location).helpers.takeIf { it < Economy.MAX_HELPERS }?.let { Economy.helperCost(location, it) }

    fun hireHelper(location: Location): Boolean {
        val cost = helperCost(location) ?: return false
        if (!at(location).unlocked || cash < cost) return false
        cash -= cost
        at(location).helpers++
        return true
    }

    fun canOpen(location: Location): Boolean = !at(location).unlocked && cash >= location.unlockCost

    /** Opens a branch, which starts with chai at level 1. */
    fun open(location: Location): Boolean {
        if (!canOpen(location)) return false
        cash -= location.unlockCost
        at(location).unlocked = true
        at(location).levels[Product.CHAI.ordinal] = 1
        return true
    }

    /** Rewarded 2x profits: adds [Economy.BOOST_MS], up to [Economy.BOOST_MAX_MS] ahead. */
    fun addBoost(nowMs: Long) {
        val start = maxOf(nowMs, boostUntilMs)
        boostUntilMs = min(start + Economy.BOOST_MS, nowMs + Economy.BOOST_MAX_MS)
    }

    // --- Saving ------------------------------------------------------------------------

    /** Compact text form for saving: `key=value` lines. */
    fun encode(): String = buildString {
        appendLine("v=1")
        appendLine("cash=$cash")
        appendLine("lifetime=$lifetimeEarned")
        appendLine("boostUntil=$boostUntilMs")
        appendLine("freeUpgradeAt=$freeUpgradeReadyAtMs")
        appendLine("lastSeen=$lastSeenMs")
        Location.entries.forEach { loc ->
            val s = at(loc)
            appendLine("loc.${loc.name}=${s.unlocked},${s.helpers},${s.levels.joinToString(",")}")
        }
    }

    companion object {
        /** A brand-new player: the roadside stall open with chai at level 1. */
        fun newGame(): TycoonState = TycoonState().apply {
            at(Location.ROADSIDE).unlocked = true
            at(Location.ROADSIDE).levels[Product.CHAI.ordinal] = 1
        }

        /** Reads [encode]'s output; anything missing or broken falls back to defaults. */
        fun decode(text: String?): TycoonState {
            if (text.isNullOrBlank()) return newGame()
            val map = text.lines().mapNotNull { line ->
                val i = line.indexOf('=')
                if (i <= 0) null else line.substring(0, i) to line.substring(i + 1)
            }.toMap()
            val state = TycoonState(
                cash = map["cash"]?.toDoubleOrNull() ?: 0.0,
                lifetimeEarned = map["lifetime"]?.toDoubleOrNull() ?: 0.0,
                boostUntilMs = map["boostUntil"]?.toLongOrNull() ?: 0,
                freeUpgradeReadyAtMs = map["freeUpgradeAt"]?.toLongOrNull() ?: 0,
                lastSeenMs = map["lastSeen"]?.toLongOrNull() ?: 0,
            )
            Location.entries.forEach { loc ->
                val parts = map["loc.${loc.name}"]?.split(",") ?: return@forEach
                val s = state.at(loc)
                s.unlocked = parts.getOrNull(0)?.toBooleanStrictOrNull() ?: false
                s.helpers = (parts.getOrNull(1)?.toIntOrNull() ?: 0).coerceIn(0, Economy.MAX_HELPERS)
                Product.entries.forEach { p ->
                    s.levels[p.ordinal] = (parts.getOrNull(2 + p.ordinal)?.toIntOrNull() ?: 0).coerceAtLeast(0)
                }
            }
            if (!state.at(Location.ROADSIDE).unlocked) {
                state.at(Location.ROADSIDE).unlocked = true
                state.at(Location.ROADSIDE).levels[Product.CHAI.ordinal] =
                    maxOf(1, state.at(Location.ROADSIDE).level(Product.CHAI))
            }
            return state
        }
    }
}
