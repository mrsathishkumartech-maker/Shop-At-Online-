package com.sathishkumar.games.braingames.ui

import android.content.Context
import com.sathishkumar.games.braingames.logic.BrainGame
import com.sathishkumar.games.braingames.logic.BrainScore
import com.sathishkumar.games.braingames.logic.Streak
import com.sathishkumar.games.core.GamePrefs
import java.time.LocalDate

/** Best results, today's results, the daily streak and games played. */
class ScoreStore(context: Context) {
    private val prefs = GamePrefs(context, "braingames")
    private fun today(): Long = LocalDate.now().toEpochDay()

    /** Raw results are stored as Int: Number Rush in ms fits easily. 0 means "not played". */
    fun best(game: BrainGame): Long = prefs.getInt("best_${game.name}").toLong()

    fun todayRaw(game: BrainGame): Long =
        if (prefs.getInt("day_${game.name}").toLong() == today()) prefs.getInt("today_${game.name}").toLong() else 0

    fun todayPoints(game: BrainGame): Int = BrainScore.points(game, todayRaw(game))

    fun brainScoreToday(): Int = BrainGame.entries.sumOf { todayPoints(it) }

    fun streak(): Int = Streak.current(prefs.getInt("last_day").toLong(), prefs.getInt("streak"), today())

    fun gamesPlayed(): Int = prefs.getInt("games_played")

    /** Records a finished game. Returns true if it's a new personal best. */
    fun record(game: BrainGame, raw: Long): Boolean {
        val today = today()
        val newBest = BrainScore.isBetter(game, raw, best(game))
        if (newBest) prefs.putInt("best_${game.name}", raw.toInt())
        if (BrainScore.isBetter(game, raw, todayRaw(game))) {
            prefs.putInt("today_${game.name}", raw.toInt())
            prefs.putInt("day_${game.name}", today.toInt())
        }
        prefs.putInt("streak", Streak.afterPlaying(prefs.getInt("last_day").toLong(), prefs.getInt("streak"), today))
        prefs.putInt("last_day", today.toInt())
        prefs.putInt("games_played", gamesPlayed() + 1)
        return newBest
    }
}
