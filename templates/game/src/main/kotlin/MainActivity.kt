package com.sathishkumar.games.__GAME_ID__

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import com.sathishkumar.games.core.GamePrefs

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = GamePrefs(this, "__GAME_ID__")
        setContentView(TextView(this).apply {
            text = getString(R.string.app_name) + "\nLevel " + prefs.currentLevel
            textSize = 28f
            gravity = Gravity.CENTER
        })
    }
}
