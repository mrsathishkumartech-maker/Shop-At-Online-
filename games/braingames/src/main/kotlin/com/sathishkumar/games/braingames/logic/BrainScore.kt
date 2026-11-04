package com.sathishkumar.games.braingames.logic

enum class BrainGame(val title: String, val skill: String, val emoji: String) {
    QUICK_MATH("Quick Math", "Speed", "➗"),
    MEMORY_GRID("Memory Grid", "Memory", "🧩"),
    COLOR_CLASH("Color Clash", "Focus", "🎨"),
    NUMBER_RUSH("Number Rush", "Attention", "🔢"),
}

/**
 * Turns each game's raw result into points out of [MAX_PER_GAME], so the four games add up
 * to a Brain Score out of 1000.
 *  - Quick Math and Color Clash: the game's score.
 *  - Memory Grid: tiles remembered.
 *  - Number Rush: finishing time in ms (lower is better).
 */
object BrainScore {
    const val MAX_PER_GAME = 250

    fun points(game: BrainGame, raw: Long): Int {
        val fraction = when (game) {
            BrainGame.QUICK_MATH -> raw / 900.0          // ~30 right answers at mid levels
            BrainGame.MEMORY_GRID -> raw / 60.0          // reaching ~round 9
            BrainGame.COLOR_CLASH -> raw / 900.0         // ~30 in a row
            BrainGame.NUMBER_RUSH -> if (raw <= 0) 0.0 else (60_000.0 - raw) / (60_000.0 - 15_000.0) // 15s = full, 60s = none
        }
        return (fraction.coerceIn(0.0, 1.0) * MAX_PER_GAME).toInt()
    }

    /** True if [newRaw] beats [oldRaw] for [game] (Number Rush: lower time wins; 0 = none yet). */
    fun isBetter(game: BrainGame, newRaw: Long, oldRaw: Long): Boolean = when (game) {
        BrainGame.NUMBER_RUSH -> newRaw > 0 && (oldRaw <= 0 || newRaw < oldRaw)
        else -> newRaw > oldRaw
    }
}

/** Days in a row with at least one game played. Days are counted as epoch days. */
object Streak {
    /** New streak after playing on [today], given the [lastPlayedDay] and current [streak]. */
    fun afterPlaying(lastPlayedDay: Long, streak: Int, today: Long): Int = when (today - lastPlayedDay) {
        0L -> maxOf(streak, 1)
        1L -> streak + 1
        else -> 1
    }

    /** The streak to show on [today]: it's broken if yesterday was missed. */
    fun current(lastPlayedDay: Long, streak: Int, today: Long): Int =
        if (today - lastPlayedDay <= 1) streak else 0
}
