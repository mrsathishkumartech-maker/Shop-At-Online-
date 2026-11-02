package com.sathishkumar.games.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Thin wrapper around Google Mobile Ads shared by all games.
 * Keeps one rewarded and one interstitial ad preloaded and reloads after each show.
 */
class Ads(context: Context, private val units: AdUnits) {
    private val appContext = context.applicationContext
    private var rewarded: RewardedAd? = null
    private var interstitial: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false

    init {
        MobileAds.initialize(appContext) {
            loadRewarded()
            loadInterstitial()
        }
    }

    val isRewardedReady: Boolean get() = rewarded != null

    /**
     * Shows a rewarded ad. [onReward] runs only if the player watched long enough to earn it.
     * [onUnavailable] runs immediately if no ad is loaded (e.g. offline).
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded()
            onUnavailable()
            return
        }
        rewarded = null
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded()
                if (earned) onReward()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadRewarded()
                onUnavailable()
            }
        }
        ad.show(activity) { earned = true }
    }

    /** Shows an interstitial if one is ready; [onDone] always runs afterwards. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            loadInterstitial()
            onDone()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadInterstitial()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadInterstitial()
                onDone()
            }
        }
        ad.show(activity)
    }

    private fun loadRewarded() {
        if (rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(appContext, units.rewarded, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingRewarded = false
                    rewarded = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingRewarded = false
                }
            })
    }

    private fun loadInterstitial() {
        if (interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(appContext, units.interstitial, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingInterstitial = false
                    interstitial = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                }
            })
    }
}
