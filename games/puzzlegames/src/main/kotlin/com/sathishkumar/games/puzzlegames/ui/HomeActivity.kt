package com.sathishkumar.games.puzzlegames.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.sathishkumar.games.puzzlegames.R
import com.sathishkumar.games.puzzlegames.logic.PuzzleType

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
    }

    override fun onResume() {
        super.onResume()
        val progress = Progress(this)
        findViewById<TextView>(R.id.total_stars).text =
            getString(R.string.total_stars, PuzzleType.entries.sumOf { progress.totalStars(it) })
        val list = findViewById<LinearLayout>(R.id.puzzle_list)
        list.removeAllViews()
        for (type in PuzzleType.entries) {
            val card = LayoutInflater.from(this).inflate(R.layout.item_puzzle, list, false)
            card.findViewById<TextView>(R.id.puzzle_emoji).text = type.emoji
            card.findViewById<TextView>(R.id.puzzle_title).text = type.title
            card.findViewById<TextView>(R.id.puzzle_detail).text = type.blurb
            card.findViewById<TextView>(R.id.puzzle_level).text =
                getString(R.string.level_stars, progress.level(type), progress.totalStars(type))
            card.setOnClickListener {
                startActivity(Intent(this, PuzzleActivity::class.java).putExtra(PuzzleActivity.EXTRA_TYPE, type.name))
            }
            list.addView(card)
        }
    }
}
