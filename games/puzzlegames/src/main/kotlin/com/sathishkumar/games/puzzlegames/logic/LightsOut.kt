package com.sathishkumar.games.puzzlegames.logic

import kotlin.random.Random

/**
 * Lights Out: tapping a light toggles it and its four neighbours. Turn every light off.
 * Levels are made by tapping a solved (dark) board, so they're always solvable, and the
 * fewest taps needed is worked out exactly by solving the puzzle as equations over GF(2).
 */
class LightsOut(override val level: Int, val size: Int, private val start: BooleanArray) : Puzzle {
    override val type = PuzzleType.LIGHTS
    val lights: BooleanArray = start.copyOf()
    override var moves = 0
        private set
    override val par: Int = solve(start, size)!!.size

    override val isSolved: Boolean get() = lights.none { it }

    fun tap(index: Int): Boolean {
        if (isSolved) return false
        press(lights, size, index)
        moves++
        return true
    }

    override fun hint(): Boolean {
        val plan = solve(lights, size) ?: return false
        return plan.firstOrNull()?.let { tap(it) } ?: false
    }

    override fun restart() {
        start.copyInto(lights)
        moves = 0
    }

    companion object {
        fun generate(level: Int, random: Random = Random(level * 104729L + 3)): LightsOut {
            val size = when {
                level <= 4 -> 3
                level <= 12 -> 4
                else -> 5
            }
            val presses = (2 + level / 2).coerceAtMost(size * size - 2)
            while (true) {
                val board = BooleanArray(size * size)
                (0 until size * size).shuffled(random).take(presses).forEach { press(board, size, it) }
                if (board.any { it } && solve(board, size)!!.size >= minOf(presses, 2)) {
                    return LightsOut(level, size, board)
                }
            }
        }

        fun press(board: BooleanArray, size: Int, i: Int) {
            val r = i / size
            val c = i % size
            board[i] = !board[i]
            if (r > 0) board[i - size] = !board[i - size]
            if (r < size - 1) board[i + size] = !board[i + size]
            if (c > 0) board[i - 1] = !board[i - 1]
            if (c < size - 1) board[i + 1] = !board[i + 1]
        }

        /**
         * The fewest cells to press to turn [board] dark, or null if impossible.
         * Gaussian elimination over GF(2), then the lightest of the solutions it allows.
         */
        fun solve(board: BooleanArray, size: Int): List<Int>? {
            val n = size * size
            // Row i: which presses toggle light i, and whether light i is on.
            val rows = Array(n) { i ->
                BooleanArray(n + 1).also { row ->
                    val b = BooleanArray(n)
                    press(b, size, i) // pressing i affects the same cells that affect i (symmetric)
                    for (j in 0 until n) row[j] = b[j]
                    row[n] = board[i]
                }
            }
            val pivotCol = IntArray(n) { -1 }
            var r = 0
            for (col in 0 until n) {
                val p = (r until n).firstOrNull { rows[it][col] } ?: continue
                val tmp = rows[p]; rows[p] = rows[r]; rows[r] = tmp
                for (k in 0 until n) if (k != r && rows[k][col]) for (j in 0..n) rows[k][j] = rows[k][j] xor rows[r][j]
                pivotCol[r] = col
                r++
            }
            for (k in r until n) if (rows[k][n]) return null
            val free = (0 until n).filter { col -> (0 until r).none { pivotCol[it] == col } }
            var best: BooleanArray? = null
            // Small boards have at most a handful of free variables, so trying them all is cheap.
            for (mask in 0 until (1 shl free.size)) {
                val x = BooleanArray(n)
                free.forEachIndexed { b, col -> x[col] = (mask shr b) and 1 == 1 }
                for (k in r - 1 downTo 0) {
                    val col = pivotCol[k]
                    var v = rows[k][n]
                    for (j in 0 until n) if (j != col && rows[k][j] && x[j]) v = v xor true
                    x[col] = v
                }
                if (best == null || x.count { it } < best.count { it }) best = x
            }
            return best!!.indices.filter { best[it] }
        }
    }
}
