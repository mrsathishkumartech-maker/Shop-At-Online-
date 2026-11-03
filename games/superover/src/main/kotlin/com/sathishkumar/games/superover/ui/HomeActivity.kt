package com.sathishkumar.games.superover.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.sathishkumar.games.superover.R

/** Where the career stands: tournament, next match and target, bat and coins. */
class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        findViewById<Button>(R.id.play_button).setOnClickListener {
            startActivity(Intent(this, MatchActivity::class.java))
        }
        findViewById<Button>(R.id.bats_button).setOnClickListener {
            startActivity(Intent(this, BatsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val c = CareerStore(this).load()
        findViewById<TextView>(R.id.tournament_text).text = c.tournament.title
        findViewById<TextView>(R.id.match_text).text =
            getString(R.string.match_of, c.matchIndex + 1, c.tournament.targets.size)
        findViewById<TextView>(R.id.target_text).text = getString(R.string.target_off_six, c.target)
        findViewById<TextView>(R.id.bat_text).text = getString(R.string.bat_line, c.equipped.title)
        findViewById<TextView>(R.id.coins_text).text = getString(R.string.coins_n, c.coins)
        findViewById<TextView>(R.id.titles_text).text =
            if (c.titles > 0) getString(R.string.titles_n, c.titles) else ""
    }
}
