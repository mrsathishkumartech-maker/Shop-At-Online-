package com.sathishkumar.games.braingames.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.sathishkumar.games.braingames.R
import com.sathishkumar.games.braingames.logic.BrainGame
import com.sathishkumar.games.braingames.logic.BrainScore

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

    override fun onResume() {
        super.onResume()
        val store = ScoreStore(this)
        findViewById<ScoreRingView>(R.id.score_ring).score = store.brainScoreToday()
        val streak = store.streak()
        findViewById<TextView>(R.id.streak_text).text =
            if (streak > 0) getString(R.string.streak_n, streak) else getString(R.string.streak_start)

        val list = findViewById<LinearLayout>(R.id.game_list)
        list.removeAllViews()
        for (game in BrainGame.entries) {
            val card = LayoutInflater.from(this).inflate(R.layout.item_game, list, false)
            card.findViewById<TextView>(R.id.game_emoji).text = game.emoji
            card.findViewById<TextView>(R.id.game_title).text = game.title
            card.findViewById<TextView>(R.id.game_detail).text =
                getString(R.string.game_detail, game.skill, describe(game, store.best(game)))
            card.findViewById<TextView>(R.id.game_points).text =
                getString(R.string.points_of, store.todayPoints(game), BrainScore.MAX_PER_GAME)
            card.setOnClickListener { startActivity(intentFor(game)) }
            list.addView(card)
        }
    }

    private fun intentFor(game: BrainGame): Intent = when (game) {
        BrainGame.QUICK_MATH, BrainGame.COLOR_CLASH -> Intent(this, QuizActivity::class.java)
        BrainGame.MEMORY_GRID -> Intent(this, MemoryActivity::class.java)
        BrainGame.NUMBER_RUSH -> Intent(this, RushActivity::class.java)
    }.putExtra(EXTRA_GAME, game.name)

    companion object {
        const val EXTRA_GAME = "game"
    }
}
