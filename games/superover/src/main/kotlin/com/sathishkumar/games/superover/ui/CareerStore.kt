package com.sathishkumar.games.superover.ui

import android.content.Context
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.superover.logic.Career

/** Loads and saves the player's [Career]. */
class CareerStore(context: Context) {
    private val prefs = GamePrefs(context, "superover")

    fun load(): Career = Career.decode(prefs.getString(KEY))

    fun save(career: Career) = prefs.putString(KEY, career.encode())

    private companion object {
        const val KEY = "career"
    }
}
