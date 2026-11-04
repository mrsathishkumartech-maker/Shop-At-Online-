package com.sathishkumar.games.puzzlegames.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdPolicy
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads
import com.sathishkumar.games.puzzlegames.BuildConfig
import com.sathishkumar.games.puzzlegames.R
import com.sathishkumar.games.puzzlegames.logic.LightsOut
import com.sathishkumar.games.puzzlegames.logic.MiniSudoku
import com.sathishkumar.games.puzzlegames.logic.PipeFlow
import com.sathishkumar.games.puzzlegames.logic.Puzzle
import com.sathishkumar.games.puzzlegames.logic.PuzzleType
import com.sathishkumar.games.puzzlegames.logic.SlidePuzzle
import com.sathishkumar.games.puzzlegames.logic.Stars
import com.sathishkumar.games.puzzlegames.logic.newPuzzle

/** Plays any puzzle type, one level at a time. */
class PuzzleActivity : Activity() {
    private lateinit var type: PuzzleType
    private lateinit var progress: Progress
    private lateinit var ads: Ads
    // An interstitial after every 3rd solved puzzle.
    private val adPolicy = AdPolicy(firstLevelAfter = 0, everyLevels = 3)
    private lateinit var puzzle: Puzzle

    private lateinit var board: PuzzleBoardView
    private lateinit var title: TextView
    private lateinit var movesText: TextView
    private lateinit var numberPad: LinearLayout
    private var solvedShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)
        type = PuzzleType.valueOf(intent.getStringExtra(EXTRA_TYPE) ?: PuzzleType.SLIDE.name)
        progress = Progress(this)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))
        board = findViewById(R.id.board)
        title = findViewById(R.id.puzzle_title)
        movesText = findViewById(R.id.moves_text)
        numberPad = findViewById(R.id.number_pad)
        findViewById<TextView>(R.id.instructions).text = type.blurb

        board.onTap = ::onCellTap
        findViewById<Button>(R.id.hint_button).setOnClickListener { onHint() }
        findViewById<Button>(R.id.restart_button).setOnClickListener {
            puzzle.restart()
            board.selected = null
            board.invalidate()
            render()
        }
        numberPad.visibility = if (type == PuzzleType.SUDOKU) View.VISIBLE else View.GONE
        for (v in 1..MiniSudoku.N) {
            val b = layoutInflater.inflate(R.layout.item_number, numberPad, false) as Button
            b.text = v.toString()
            b.setOnClickListener { onNumber(v) }
            numberPad.addView(b)
        }
        loadLevel(progress.level(type))
    }

    private fun loadLevel(level: Int) {
        puzzle = newPuzzle(type, level)
        solvedShown = false
        board.bind(puzzle)
        render()
    }

    private fun render() {
        title.text = getString(R.string.puzzle_level, type.title, puzzle.level)
        movesText.text = when {
            type == PuzzleType.SUDOKU -> getString(R.string.mistakes_n, puzzle.moves)
            puzzle.par != null -> getString(R.string.moves_par, puzzle.moves, puzzle.par!!)
            else -> getString(R.string.moves_n, puzzle.moves)
        }
    }

    private fun onCellTap(i: Int) {
        if (puzzle.isSolved) return
        val changed = when (val p = puzzle) {
            is SlidePuzzle -> p.tap(i)
            is LightsOut -> p.tap(i)
            is PipeFlow -> p.tap(i)
            is MiniSudoku -> { board.selected = i; false }
            else -> false
        }
        if (changed) Haptics.tap(board)
        afterChange()
    }

    private fun onNumber(v: Int) {
        val s = puzzle as? MiniSudoku ?: return
        val cell = board.selected ?: return
        if (s.isGiven(cell) || s.cells[cell] != 0) return
        if (s.place(cell, v)) Haptics.tap(board) else { Haptics.error(board); board.flash(cell) }
        afterChange()
    }

    private fun onHint() {
        if (puzzle.isSolved) return
        ads.showRewarded(this,
            onReward = {
                val p = puzzle
                if (p is MiniSudoku) p.hint(board.selected) else p.hint()
                afterChange()
            },
            onUnavailable = { Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show() })
    }

    private fun afterChange() {
        board.invalidate()
        render()
        if (puzzle.isSolved && !solvedShown) {
            solvedShown = true
            board.postDelayed(::showSolved, 450)
        }
    }

    private fun showSolved() {
        if (isFinishing) return
        val stars = Stars.rate(puzzle)
        val gained = progress.recordSolved(type, puzzle.level, stars)
        val message = buildString {
            append("★".repeat(stars)).append("☆".repeat(3 - stars)).append("\n\n")
            append(
                when {
                    type == PuzzleType.SUDOKU -> getString(R.string.solved_mistakes, puzzle.moves)
                    puzzle.par != null -> getString(R.string.solved_moves, puzzle.moves, puzzle.par!!)
                    else -> getString(R.string.moves_n, puzzle.moves)
                },
            )
            if (gained > 0) append("\n").append(getString(R.string.stars_gained, gained))
        }
        val next = puzzle.level + 1
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(getString(R.string.solved_title, puzzle.level))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.next_level) { _, _ ->
                val go = { loadLevel(next) }
                if (adPolicy.shouldShowInterstitial(progress.solvedCount())) ads.showInterstitial(this, go) else go()
            }
            .setNegativeButton(R.string.home) { _, _ -> finish() }
            .show()
    }

    companion object {
        const val EXTRA_TYPE = "type"
    }
}
