package com.sathishkumar.games.braingames.ui

import com.sathishkumar.games.braingames.logic.BrainGame

/** How a raw result reads to the player. */
fun describe(game: BrainGame, raw: Long): String = when {
    raw <= 0 -> "–"
    game == BrainGame.NUMBER_RUSH -> "%.1f s".format(raw / 1000.0)
    game == BrainGame.MEMORY_GRID -> "$raw tiles"
    else -> "$raw pts"
}
