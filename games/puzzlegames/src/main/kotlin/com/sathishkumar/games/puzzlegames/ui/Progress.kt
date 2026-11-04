package com.sathishkumar.games.puzzlegames.ui

import android.content.Context
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.puzzlegames.logic.PuzzleType

/** Current level, best stars per level and total stars for each puzzle type. */
class Progress(context: Context) {
    private val prefs = GamePrefs(context, "puzzlegames")

    fun level(type: PuzzleType): Int = prefs.getInt("level_${type.name}", 1)

    fun setLevel(type: PuzzleType, level: Int) = prefs.putInt("level_${type.name}", level.coerceAtLeast(1))

    fun stars(type: PuzzleType, level: Int): Int = prefs.getInt("stars_${type.name}_$level")

    fun totalStars(type: PuzzleType): Int = prefs.getInt("starsum_${type.name}")

    fun solvedCount(): Int = prefs.getInt("solved")

    /** Records a solved level and moves on to the next. Returns stars gained over the previous best. */
    fun recordSolved(type: PuzzleType, level: Int, stars: Int): Int {
        val old = stars(type, level)
        val gained = (stars - old).coerceAtLeast(0)
        if (gained > 0) {
            prefs.putInt("stars_${type.name}_$level", stars)
            prefs.putInt("starsum_${type.name}", totalStars(type) + gained)
        }
        if (level >= level(type)) setLevel(type, level + 1)
        prefs.putInt("solved", solvedCount() + 1)
        return gained
    }
}
