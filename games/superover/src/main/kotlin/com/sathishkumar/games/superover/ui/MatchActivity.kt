package com.sathishkumar.games.superover.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdPolicy
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads
import com.sathishkumar.games.superover.BuildConfig
import com.sathishkumar.games.superover.R
import com.sathishkumar.games.superover.logic.BallResult
import com.sathishkumar.games.superover.logic.Career
import com.sathishkumar.games.superover.logic.MatchStatus
import com.sathishkumar.games.superover.logic.Outcome
import com.sathishkumar.games.superover.logic.SuperOver
import com.sathishkumar.games.superover.logic.Tournament

class MatchActivity : Activity() {
    private lateinit var store: CareerStore
    private lateinit var career: Career
    private lateinit var match: SuperOver
    private lateinit var ads: Ads
    // An interstitial between matches, from the 3rd match the player finishes.
    private val adPolicy = AdPolicy(firstLevelAfter = 2, everyLevels = 1)

    private lateinit var view: MatchView
    private lateinit var title: TextView
    private lateinit var equation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)
        store = CareerStore(this)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))
        view = findViewById(R.id.match_view)
        title = findViewById(R.id.match_title)
        equation = findViewById(R.id.equation)
        view.onBall = ::onBall
        view.onMatchOver = ::onMatchOver
        startMatch()
    }

    private fun startMatch() {
        career = store.load()
        match = career.newMatch()
        title.text = getString(R.string.match_title, career.tournament.title, career.matchIndex + 1, career.tournament.targets.size)
        view.bind(match)
        updateEquation()
    }

    private fun updateEquation() {
        equation.text = when (match.status) {
            MatchStatus.WON -> getString(R.string.won_short)
            MatchStatus.LOST -> getString(R.string.lost_short)
            MatchStatus.IN_PROGRESS -> {
                val wickets = match.maxWickets - match.wickets
                getString(if (wickets == 1) R.string.need_from_last_wicket else R.string.need_from, match.runsNeeded, match.ballsLeft, wickets)
            }
        }
    }

    private fun onBall(result: BallResult) {
        updateEquation()
        when {
            result.outcome.isOut -> Haptics.error(view)
            result.outcome == Outcome.SIX || result.outcome == Outcome.FOUR -> Haptics.tap(view)
        }
        if (result.outcome.isOut && match.canUseFreeHit) {
            view.hold = true
            // Let the wicket animation play before asking.
            view.postDelayed(::offerFreeHit, 900)
        }
    }

    private fun offerFreeHit() {
        if (isFinishing) return
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.free_hit_title)
            .setMessage(R.string.free_hit_message)
            .setCancelable(false)
            .setPositiveButton(R.string.free_hit_ad) { _, _ ->
                ads.showRewarded(this,
                    onReward = {
                        match.useFreeHit()
                        view.refresh()
                        updateEquation()
                        view.hold = false
                    },
                    onUnavailable = {
                        Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show()
                        view.hold = false
                    })
            }
            .setNegativeButton(R.string.no_thanks) { _, _ -> view.hold = false }
            .show()
    }

    private fun onMatchOver() {
        if (isFinishing) return
        val won = match.status == MatchStatus.WON
        val before = career.tournament
        val earned = career.finish(match)
        store.save(career)
        val champion = won && career.matchIndex == 0 && (career.tournament != before || before == Tournament.WORLD_CUP)

        val message = buildString {
            append(getString(if (won) R.string.won_message else R.string.lost_message, match.runs, match.target))
            append("\n\n")
            append(getString(R.string.coins_earned, earned))
            if (champion) append("\n\n").append(getString(R.string.champion, before.title, before.winnerCoins))
        }
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(if (won) R.string.won_title else R.string.lost_title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(if (won) R.string.next_match else R.string.try_again) { _, _ -> nextMatch() }
            .setNegativeButton(R.string.home) { _, _ -> finish() }
            .show()
    }

    private fun nextMatch() {
        if (adPolicy.shouldShowInterstitial(career.matchesPlayed)) {
            ads.showInterstitial(this) { startMatch() }
        } else {
            startMatch()
        }
    }
}
