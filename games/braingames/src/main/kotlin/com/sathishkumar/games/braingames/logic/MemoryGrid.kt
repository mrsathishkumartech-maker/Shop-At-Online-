package com.sathishkumar.games.braingames.logic

import kotlin.random.Random

enum class MemoryTap { CORRECT, ALREADY, WRONG, ROUND_COMPLETE, GAME_OVER }

/**
 * Memory Grid: some tiles light up, then go dark; tap the ones that were lit. Each cleared
 * round lights one more tile (the grid grows to fit). A wrong tap costs a life and replays the
 * round; three lives in all.
 */
class MemoryGrid(private val random: Random = Random.Default) {
    var round = 1
        private set
    var lives = MAX_LIVES
        private set
    var score = 0
        private set
    var size = 3
        private set
    var pattern: Set<Int> = emptySet()
        private set
    val found = mutableSetOf<Int>()
    var continueUsed = false
        private set

    val tilesToRemember: Int get() = 2 + round
    val isOver: Boolean get() = lives <= 0

    init { newRound() }

    fun tap(index: Int): MemoryTap {
        if (isOver) return MemoryTap.GAME_OVER
        if (index in found) return MemoryTap.ALREADY
        if (index !in pattern) {
            lives--
            return if (isOver) MemoryTap.GAME_OVER else { newRound(); MemoryTap.WRONG }
        }
        found += index
        if (found.size < pattern.size) return MemoryTap.CORRECT
        score += pattern.size
        round++
        newRound()
        return MemoryTap.ROUND_COMPLETE
    }

    /** Rewarded continue after the last life: one more life, once per game. */
    fun useContinue(): Boolean {
        if (!isOver || continueUsed) return false
        continueUsed = true
        lives = 1
        newRound()
        return true
    }

    private fun newRound() {
        val k = tilesToRemember
        size = when {
            k <= 4 -> 3
            k <= 7 -> 4
            k <= 11 -> 5
            else -> 6
        }
        val count = k.coerceAtMost(size * size / 2)
        pattern = (0 until size * size).shuffled(random).take(count).toSet()
        found.clear()
    }

    companion object {
        const val MAX_LIVES = 3
    }
}
