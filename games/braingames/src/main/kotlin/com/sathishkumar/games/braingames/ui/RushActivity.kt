package com.sathishkumar.games.braingames.ui

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.TextView
import com.sathishkumar.games.braingames.BuildConfig
import com.sathishkumar.games.braingames.R
import com.sathishkumar.games.braingames.logic.BrainGame
import com.sathishkumar.games.braingames.logic.NumberRush
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads

/** Number Rush: tap 1 to 25 in order, against the clock. */
class RushActivity : Activity() {
    private lateinit var ads: Ads
    private lateinit var game: NumberRush
    private lateinit var grid: TileGridView
    private lateinit var status: TextView
    private lateinit var clock: TextView
    /** 0 until the first correct tap: the clock starts when the player does. */
    private var startedAt = 0L
    private var pausedElapsed = -1L
    private var done = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            clock.text = "%.1f s".format(game.finalTimeMs(elapsed()) / 1000.0)
            if (!done) handler.postDelayed(this, 100)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))
        grid = findViewById(R.id.grid)
        status = findViewById(R.id.status_text)
        clock = findViewById(R.id.side_text)
        findViewById<TextView>(R.id.game_title).text = BrainGame.NUMBER_RUSH.title
        findViewById<TextView>(R.id.instructions).text = getString(R.string.rush_instructions)
        grid.onTap = ::onTap
        start()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        if (!done && startedAt > 0) pausedElapsed = elapsed()
    }

    override fun onResume() {
        super.onResume()
        if (!done && startedAt > 0 && pausedElapsed >= 0) {
            startedAt = SystemClock.elapsedRealtime() - pausedElapsed
            pausedElapsed = -1
            handler.post(ticker)
        }
    }

    private fun elapsed() = if (startedAt == 0L) 0L else SystemClock.elapsedRealtime() - startedAt

    private fun start() {
        game = NumberRush()
        done = false
        startedAt = 0L
        pausedElapsed = -1
        grid.enabledForInput = true
        draw()
        handler.removeCallbacks(ticker)
        clock.text = "0.0 s"
    }

    private fun draw(wrong: Int? = null) {
        val colors = IntArray(game.count) { i ->
            when {
                i == wrong -> TileGridView.BAD
                game.grid[i] < game.next -> TileGridView.DONE
                else -> TileGridView.IDLE
            }
        }
        val labels = Array<String?>(game.count) { i -> game.grid[i].toString() }
        grid.setGrid(game.size, colors, labels)
        status.text = if (game.isDone) getString(R.string.rush_done) else getString(R.string.rush_next, game.next)
    }

    private fun onTap(i: Int) {
        if (game.tap(i)) {
            if (startedAt == 0L) {
                startedAt = SystemClock.elapsedRealtime()
                handler.post(ticker)
            }
            Haptics.tap(grid)
            draw()
            if (game.isDone) finishGame()
        } else {
            Haptics.error(grid)
            draw(wrong = i)
            grid.postDelayed({ if (!done) draw() }, 250)
        }
    }

    private fun finishGame() {
        done = true
        grid.enabledForInput = false
        val total = game.finalTimeMs(elapsed())
        handler.removeCallbacks(ticker)
        clock.text = "%.1f s".format(total / 1000.0)
        showResult(ads, BrainGame.NUMBER_RUSH, total,
            getString(R.string.rush_detail, total / 1000.0, game.mistakes), onPlayAgain = ::start)
    }
}
