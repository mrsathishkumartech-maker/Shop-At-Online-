package com.sathishkumar.games.arrowescape.logic

/**
 * Removing an arrow can only ever open paths, never close them, so greedily removing
 * any free arrow is optimal: a board is solvable exactly when greedy removal clears it.
 */
object Solver {
    fun freeArrows(board: Board): List<Arrow> = board.arrows.filter(board::canEscape)

    /** A valid removal order, or null if the board can never be cleared. */
    fun solve(board: Board): List<Arrow>? {
        val work = board.copy()
        val order = ArrayList<Arrow>(work.arrowCount)
        while (!work.isCleared) {
            val free = freeArrows(work)
            if (free.isEmpty()) return null
            free.forEach { work.remove(it); order += it }
        }
        return order
    }

    fun isSolvable(board: Board): Boolean = solve(board) != null
}
