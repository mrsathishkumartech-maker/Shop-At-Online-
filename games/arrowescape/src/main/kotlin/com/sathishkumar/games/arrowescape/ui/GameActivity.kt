package com.sathishkumar.games.arrowescape.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.arrowescape.BuildConfig
import com.sathishkumar.games.arrowescape.R
import com.sathishkumar.games.arrowescape.logic.GameSession
import com.sathishkumar.games.arrowescape.logic.LevelGenerator
import com.sathishkumar.games.arrowescape.logic.TapResult
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdPolicy
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads

class GameActivity : Activity() {
    private lateinit var prefs: GamePrefs
    private lateinit var ads: Ads
    private val adPolicy = AdPolicy(firstLevelAfter = 10, everyLevels = 3)
    private val generator = LevelGenerator()
    private lateinit var session: GameSession

    private lateinit var board: BoardView
    private lateinit var levelText: TextView
    private lateinit var livesText: TextView
    private lateinit var undoButton: Button

    private var dialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        prefs = GamePrefs(this, GAME_ID)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))

        board = findViewById(R.id.board)
        levelText = findViewById(R.id.level_text)
        livesText = findViewById(R.id.lives_text)
        undoButton = findViewById(R.id.undo_button)

        board.onTap = ::onBoardTap
        findViewById<Button>(R.id.hint_button).setOnClickListener { onHint() }
        undoButton.setOnClickListener { onUndo() }
        findViewById<Button>(R.id.skip_button).setOnClickListener { onSkip() }
        findViewById<Button>(R.id.restart_button).setOnClickListener { restartLevel() }
        findViewById<Button>(R.id.book_button).setOnClickListener {
            startActivity(Intent(this, CollectionActivity::class.java))
        }

        loadLevel(prefs.currentLevel)
    }

    private fun loadLevel(number: Int) {
        prefs.currentLevel = number
        session = GameSession(generator.generate(number))
        dialogShown = false
        board.bind(session)
        updateHud()
    }

    private fun updateHud() {
        levelText.text = getString(R.string.level_n, session.level.number)
        livesText.text = "♥".repeat(session.lives) + "♡".repeat(session.maxLives - session.lives)
        undoButton.isEnabled = session.canUndo
    }

    private fun onBoardTap(result: TapResult) {
        when (result) {
            is TapResult.Escaped -> Haptics.tap(board)
            is TapResult.Blocked -> Haptics.error(board)
            TapResult.Nothing -> Unit
        }
        updateHud()
        when {
            session.isWon -> board.postDelayed(::showWin, WIN_DIALOG_DELAY_MS)
            session.isLost -> board.postDelayed(::showLost, LOST_DIALOG_DELAY_MS)
        }
    }

    // --- Rewarded actions -------------------------------------------------------------

    private fun withRewardedAd(action: () -> Unit) {
        ads.showRewarded(this, onReward = action, onUnavailable = {
            Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show()
            // Don't leave the player stuck on a lost board with no dialog.
            if (session.isLost) showLost()
        })
    }

    private fun onHint() {
        if (session.isOver) return
        withRewardedAd { board.showHint(session.hint()) }
    }

    private fun onUndo() {
        if (!session.canUndo) return
        withRewardedAd {
            session.undo()
            board.refresh()
            updateHud()
        }
    }

    private fun onSkip() {
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.skip_title)
            .setMessage(R.string.skip_message)
            .setPositiveButton(R.string.watch_ad) { _, _ ->
                // Skipped levels don't earn stars or a rangoli for the book.
                withRewardedAd { loadLevel(session.level.number + 1) }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun restartLevel() {
        session.restart()
        dialogShown = false
        board.refresh()
        updateHud()
    }

    // --- End of level ------------------------------------------------------------------

    private fun showWin() {
        if (dialogShown || isFinishing || !session.isWon) return
        dialogShown = true
        val level = session.level
        val stars = session.stars
        prefs.recordStars(level.number, stars)

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_win, null)
        view.findViewById<RangoliView>(R.id.rangoli).setSeed(level.rangoliSeed)
        view.findViewById<TextView>(R.id.stars).text = "★".repeat(stars) + "☆".repeat(3 - stars)

        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(getString(R.string.level_cleared, level.number))
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(R.string.next_level) { _, _ -> goToNextLevel(level.number) }
            .show()
    }

    private fun goToNextLevel(completed: Int) {
        val next = { loadLevel(completed + 1) }
        if (adPolicy.shouldShowInterstitial(completed)) ads.showInterstitial(this, next) else next()
    }

    private fun showLost() {
        if (dialogShown || isFinishing || !session.isLost) return
        dialogShown = true
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.out_of_lives)
            .setMessage(R.string.out_of_lives_message)
            .setCancelable(false)
            .setPositiveButton(R.string.undo_mistake) { _, _ ->
                dialogShown = false
                onUndo()
            }
            .setNegativeButton(R.string.retry) { _, _ -> restartLevel() }
            .show()
    }

    private companion object {
        const val GAME_ID = "arrowescape"
        const val WIN_DIALOG_DELAY_MS = 1100L
        const val LOST_DIALOG_DELAY_MS = 400L
    }
}
