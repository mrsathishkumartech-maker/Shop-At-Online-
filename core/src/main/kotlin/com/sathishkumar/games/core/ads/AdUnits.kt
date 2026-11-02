package com.sathishkumar.games.core.ads

/** Ad unit IDs for one game. */
data class AdUnits(
    val rewarded: String,
    val interstitial: String,
) {
    companion object {
        /** Google's public test units: safe to use in development, never earn money. */
        val TEST = AdUnits(
            rewarded = "ca-app-pub-3940256099942544/5224354917",
            interstitial = "ca-app-pub-3940256099942544/1033173712",
        )
    }
}
