package com.sathishkumar.games.wordgames.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.sathishkumar.games.wordgames.R
import com.sathishkumar.games.wordgames.logic.WordGame

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        // Warm up the dictionary in the background so the first game opens instantly.
        Thread { Words.bank(applicationContext) }.start()
        findViewById<TextView>(R.id.credits).setOnClickListener {
            val notice = assets.open("scowl-notice.txt").bufferedReader().use { it.readText() }
            android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(R.string.credits_title)
                .setMessage(getString(R.string.credits_intro) + "\n\n" + notice)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        val progress = Progress(this)
        val list = findViewById<LinearLayout>(R.id.game_list)
        list.removeAllViews()
        for (game in WordGame.entries) {
            val card = LayoutInflater.from(this).inflate(R.layout.item_game, list, false)
            card.findViewById<TextView>(R.id.game_emoji).text = game.emoji
            card.findViewById<TextView>(R.id.game_title).text = game.title
            card.findViewById<TextView>(R.id.game_detail).text = game.blurb
            card.findViewById<TextView>(R.id.game_progress).text = if (game == WordGame.ANAGRAM) {
                getString(R.string.best_n, progress.best(game))
            } else {
                getString(R.string.level_stars, progress.level(game), progress.totalStars(game))
            }
            card.setOnClickListener {
                startActivity(Intent(this, WordActivity::class.java).putExtra(WordActivity.EXTRA_GAME, game.name))
            }
            list.addView(card)
        }
    }
}
