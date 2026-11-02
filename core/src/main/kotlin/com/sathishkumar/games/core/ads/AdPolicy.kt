package com.sathishkumar.games.core.ads

/**
 * When to show interstitials. Kept free of Android code so it can be unit-tested.
 *
 * Default: no interstitials until the player has cleared [firstLevelAfter],
 * then one after every [everyLevels]-th level (11..13 -> after 13, 16, 19, ...).
 */
class AdPolicy(
    private val firstLevelAfter: Int = 10,
    private val everyLevels: Int = 3,
) {
    init {
        require(firstLevelAfter >= 0) { "firstLevelAfter must be >= 0" }
        require(everyLevels >= 1) { "everyLevels must be >= 1" }
    }

    fun shouldShowInterstitial(completedLevel: Int): Boolean =
        completedLevel > firstLevelAfter && (completedLevel - firstLevelAfter) % everyLevels == 0
}
