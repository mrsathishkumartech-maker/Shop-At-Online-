package com.sathishkumar.games.wordgames.ui

import android.content.Context
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.wordgames.logic.WordBank
import com.sathishkumar.games.wordgames.logic.WordGame

/** The dictionary, loaded once from assets. */
object Words {
    @Volatile private var bank: WordBank? = null

    fun bank(context: Context): WordBank = bank ?: synchronized(this) {
        bank ?: WordBank.parse(read(context, "words.txt"), read(context, "common.txt")).also { bank = it }
    }

    private fun read(context: Context, name: String) =
        context.assets.open(name).bufferedReader().use { it.readText() }
}

/** Level, stars and best scores per game. */
class Progress(context: Context) {
    private val prefs = GamePrefs(context, "wordgames")

    fun level(game: WordGame): Int = prefs.getInt("level_${game.name}", 1)
    fun totalStars(game: WordGame): Int = prefs.getInt("starsum_${game.name}")
    fun best(game: WordGame): Int = prefs.getInt("best_${game.name}")
    fun completed(): Int = prefs.getInt("completed")

    /** Records a solved level and moves to the next. Returns stars gained over the previous best. */
    fun recordLevel(game: WordGame, level: Int, stars: Int): Int {
        val old = prefs.getInt("stars_${game.name}_$level")
        val gained = (stars - old).coerceAtLeast(0)
        if (gained > 0) {
            prefs.putInt("stars_${game.name}_$level", stars)
            prefs.putInt("starsum_${game.name}", totalStars(game) + gained)
        }
        if (level >= level(game)) prefs.putInt("level_${game.name}", level + 1)
        prefs.putInt("completed", completed() + 1)
        return gained
    }

    /** Records a timed round's score. Returns true for a new best. */
    fun recordScore(game: WordGame, score: Int): Boolean {
        prefs.putInt("completed", completed() + 1)
        if (score <= best(game)) return false
        prefs.putInt("best_${game.name}", score)
        return true
    }
}
