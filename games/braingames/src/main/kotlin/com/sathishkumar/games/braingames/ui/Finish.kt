package com.sathishkumar.games.braingames.ui

import android.app.Activity
import android.app.AlertDialog
import com.sathishkumar.games.braingames.R
import com.sathishkumar.games.braingames.logic.BrainGame
import com.sathishkumar.games.braingames.logic.BrainScore
import com.sathishkumar.games.core.ads.AdPolicy
import com.sathishkumar.games.core.ads.Ads

/** An interstitial after every 3rd finished game. */
private val adPolicy = AdPolicy(firstLevelAfter = 0, everyLevels = 3)

/** Records a finished game, then shows the result with Play again / Home. */
fun Activity.showResult(
    ads: Ads,
    game: BrainGame,
    raw: Long,
    detail: String,
    onPlayAgain: () -> Unit,
) {
    if (isFinishing) return
    val store = ScoreStore(this)
    val newBest = store.record(game, raw)
    val points = BrainScore.points(game, raw)
    val message = buildString {
        append(detail).append("\n\n")
        append(getString(R.string.result_points, points, BrainScore.MAX_PER_GAME))
        append("\n").append(getString(R.string.result_best, describe(game, store.best(game))))
        if (newBest) append("\n\n").append(getString(R.string.new_best))
        append("\n").append(getString(R.string.brain_score_today, store.brainScoreToday()))
    }
    val afterAd = { action: () -> Unit ->
        if (adPolicy.shouldShowInterstitial(store.gamesPlayed())) ads.showInterstitial(this, action) else action()
    }
    AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        .setTitle(getString(R.string.result_title, game.title, describe(game, raw)))
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton(R.string.play_again) { _, _ -> afterAd(onPlayAgain) }
        .setNegativeButton(R.string.home) { _, _ -> afterAd { finish() } }
        .show()
}
