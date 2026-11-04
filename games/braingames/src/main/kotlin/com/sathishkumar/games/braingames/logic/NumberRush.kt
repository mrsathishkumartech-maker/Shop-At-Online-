package com.sathishkumar.games.braingames.logic

import kotlin.random.Random

/**
 * Number Rush: the numbers 1 to 25 are scattered on a 5×5 grid; tap them in order as fast as
 * you can. Each wrong tap adds a 2-second penalty.
 */
class NumberRush(private val random: Random = Random.Default, val size: Int = 5) {
    val count = size * size
    /** grid[i] is the number shown in cell i. */
    val grid: List<Int> = (1..count).shuffled(random)
    var next = 1
        private set
    var mistakes = 0
        private set

    val isDone: Boolean get() = next > count

    /** Returns true if [index] held the next number. */
    fun tap(index: Int): Boolean {
        if (isDone) return false
        return if (grid[index] == next) { next++; true } else { mistakes++; false }
    }

    /** Total time with penalties, in ms. */
    fun finalTimeMs(elapsedMs: Long): Long = elapsedMs + mistakes * PENALTY_MS

    companion object {
        const val PENALTY_MS = 2_000L
    }
}
