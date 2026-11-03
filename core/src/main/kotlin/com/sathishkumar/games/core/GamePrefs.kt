package com.sathishkumar.games.core

import android.content.Context

/**
 * Per-game saved progress. Each game passes its own [gameId], so games installed
 * from this repo never share or overwrite each other's data.
 */
class GamePrefs(context: Context, gameId: String) {
    private val prefs = context.applicationContext
        .getSharedPreferences("game_$gameId", Context.MODE_PRIVATE)

    /** The level the player is currently on (1-based). */
    var currentLevel: Int
        get() = prefs.getInt(KEY_CURRENT_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_LEVEL, value.coerceAtLeast(1)).apply()

    /** Best star rating (0 = never cleared) for [level]. */
    fun stars(level: Int): Int = prefs.getInt("$KEY_STARS$level", 0)

    /** Keeps the best rating seen for [level]; returns true if this improved it. */
    fun recordStars(level: Int, stars: Int): Boolean {
        if (stars <= stars(level)) return false
        prefs.edit().putInt("$KEY_STARS$level", stars).apply()
        return true
    }

    /** Levels the player actually cleared (skipped levels are not included), ascending. */
    fun clearedLevels(): List<Int> = prefs.all.keys
        .filter { it.startsWith(KEY_STARS) }
        .mapNotNull { it.removePrefix(KEY_STARS).toIntOrNull() }
        .filter { stars(it) > 0 }
        .sorted()

    /** Any other whole number a game wants to keep, such as coins. */
    fun getInt(key: String, default: Int = 0): Int = prefs.getInt("int_$key", default)

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt("int_$key", value).apply()
    }

    /** Free-form text, e.g. a game's whole saved state. */
    fun getString(key: String): String? = prefs.getString("str_$key", null)

    fun putString(key: String, value: String) {
        prefs.edit().putString("str_$key", value).apply()
    }

    private companion object {
        const val KEY_CURRENT_LEVEL = "current_level"
        const val KEY_STARS = "stars_"
    }
}
