package com.sathishkumar.games.braingames.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.braingames.BuildConfig
import com.sathishkumar.games.braingames.R
import com.sathishkumar.games.braingames.logic.BrainGame
import com.sathishkumar.games.braingames.logic.MemoryGrid
import com.sathishkumar.games.braingames.logic.MemoryTap
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads

/** Memory Grid: watch the lit tiles, then tap them from memory. */
class MemoryActivity : Activity() {
    private lateinit var ads: Ads
    private lateinit var game: MemoryGrid
    private lateinit var grid: TileGridView
    private lateinit var status: TextView
    private lateinit var livesText: TextView
    private var token = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))
        grid = findViewById(R.id.grid)
        status = findViewById(R.id.status_text)
        livesText = findViewById(R.id.side_text)
        findViewById<TextView>(R.id.game_title).text = BrainGame.MEMORY_GRID.title
        findViewById<TextView>(R.id.instructions).text = getString(R.string.memory_instructions)
        grid.onTap = ::onTap
        start()
    }

    private fun start() {
        game = MemoryGrid()
        showPattern()
    }

    /** Lights the pattern, then hides it after a moment (longer for bigger patterns). */
    private fun showPattern() {
        val myToken = ++token
        grid.enabledForInput = false
        status.text = getString(R.string.memory_watch, game.round, game.pattern.size)
        updateLives()
        draw(showPattern = true)
        grid.postDelayed({
            if (myToken != token) return@postDelayed
            grid.enabledForInput = true
            status.text = getString(R.string.memory_tap, game.found.size, game.pattern.size)
            draw(showPattern = false)
        }, 900L + 250L * game.pattern.size)
    }

    private fun draw(showPattern: Boolean, wrongTile: Int? = null) {
        val n = game.size * game.size
        val colors = IntArray(n) { i ->
            when {
                i == wrongTile -> TileGridView.BAD
                showPattern && i in game.pattern -> TileGridView.LIT
                i in game.found -> TileGridView.GOOD
                else -> TileGridView.IDLE
            }
        }
        grid.setGrid(game.size, colors)
    }

    private fun updateLives() {
        livesText.text = "♥".repeat(game.lives) + "♡".repeat((MemoryGrid.MAX_LIVES - game.lives).coerceAtLeast(0))
    }

    private fun onTap(i: Int) {
        val shownSize = game.size
        when (game.tap(i)) {
            MemoryTap.CORRECT -> {
                Haptics.tap(grid)
                draw(showPattern = false)
                status.text = getString(R.string.memory_tap, game.found.size, game.pattern.size)
            }
            MemoryTap.ALREADY -> Unit
            MemoryTap.ROUND_COMPLETE -> {
                Haptics.tap(grid)
                grid.enabledForInput = false
                status.text = getString(R.string.memory_cleared)
                grid.postDelayed(::showPattern, 600)
            }
            MemoryTap.WRONG -> {
                Haptics.error(grid)
                grid.enabledForInput = false
                updateLives()
                // Show the miss on the grid that was tapped, then replay with a new pattern.
                if (shownSize == game.size) draw(showPattern = false, wrongTile = i)
                status.text = getString(R.string.memory_wrong)
                grid.postDelayed(::showPattern, 900)
            }
            MemoryTap.GAME_OVER -> {
                Haptics.error(grid)
                grid.enabledForInput = false
                updateLives()
                draw(showPattern = true, wrongTile = i)
                grid.postDelayed(::gameOver, 700)
            }
        }
    }

    private fun gameOver() {
        if (isFinishing) return
        if (!game.continueUsed) {
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.memory_out_title)
                .setMessage(getString(R.string.memory_out_message, game.score))
                .setCancelable(false)
                .setPositiveButton(R.string.continue_ad) { _, _ ->
                    ads.showRewarded(this,
                        onReward = { game.useContinue(); showPattern() },
                        onUnavailable = {
                            Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show()
                            finishGame()
                        })
                }
                .setNegativeButton(R.string.see_result) { _, _ -> finishGame() }
                .show()
        } else {
            finishGame()
        }
    }

    private fun finishGame() {
        showResult(ads, BrainGame.MEMORY_GRID, game.score.toLong(),
            getString(R.string.memory_detail, game.round - 1, game.score), onPlayAgain = ::start)
    }
}
