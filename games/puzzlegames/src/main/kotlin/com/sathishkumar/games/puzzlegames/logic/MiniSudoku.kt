package com.sathishkumar.games.puzzlegames.logic

import kotlin.random.Random

/**
 * 6×6 Sudoku: every row, column and 2×3 box holds 1 to 6 once. Wrong entries are rejected
 * and count as mistakes. Every puzzle is checked to have exactly one solution.
 */
class MiniSudoku(override val level: Int, val solution: IntArray, private val givens: IntArray) : Puzzle {
    override val type = PuzzleType.SUDOKU
    /** 0 = empty. */
    val cells: IntArray = givens.copyOf()
    override var moves = 0 // mistakes
        private set
    override val par: Int? = null
    val clues: Int = givens.count { it != 0 }

    fun isGiven(i: Int) = givens[i] != 0

    override val isSolved: Boolean get() = cells.contentEquals(solution)

    /** Places [value] in [i]. Returns true if right; a wrong value isn't placed and counts as a mistake. */
    fun place(i: Int, value: Int): Boolean {
        if (isGiven(i) || cells[i] != 0 || isSolved) return false
        if (solution[i] == value) { cells[i] = value; return true }
        moves++
        return false
    }

    /** Fills [preferred] if it's empty, otherwise the first empty cell. */
    fun hint(preferred: Int?): Int? {
        val i = preferred?.takeIf { cells[it] == 0 } ?: cells.indexOfFirst { it == 0 }.takeIf { it >= 0 } ?: return null
        cells[i] = solution[i]
        return i
    }

    override fun hint(): Boolean = hint(null) != null

    override fun restart() {
        givens.copyInto(cells)
        moves = 0
    }

    companion object {
        const val N = 6

        fun generate(level: Int, random: Random = Random(level * 2654435761L + 11)): MiniSudoku {
            val solution = fullGrid(random)
            val targetClues = (22 - level).coerceAtLeast(10)
            val puzzle = solution.copyOf()
            for (i in (0 until N * N).shuffled(random)) {
                if (puzzle.count { it != 0 } <= targetClues) break
                val keep = puzzle[i]
                puzzle[i] = 0
                if (countSolutions(puzzle, 2) != 1) puzzle[i] = keep
            }
            return MiniSudoku(level, solution, puzzle)
        }

        /** A random valid grid: a base pattern with rows, columns and digits shuffled within the rules. */
        private fun fullGrid(random: Random): IntArray {
            val base = IntArray(N * N) { i -> val r = i / N; val c = i % N; (r * 3 + r / 2 + c) % N + 1 }
            val bands = listOf(0, 1, 2).shuffled(random)          // 3 bands of 2 rows
            val rows = bands.flatMap { b -> listOf(0, 1).shuffled(random).map { b * 2 + it } }
            val stacks = listOf(0, 1).shuffled(random)            // 2 stacks of 3 columns
            val cols = stacks.flatMap { s -> listOf(0, 1, 2).shuffled(random).map { s * 3 + it } }
            val digits = (1..N).shuffled(random)
            return IntArray(N * N) { i -> digits[base[rows[i / N] * N + cols[i % N]] - 1] }
        }

        fun isValid(grid: IntArray): Boolean {
            fun ok(cells: List<Int>) = cells.map { grid[it] }.filter { it != 0 }.let { it.size == it.toSet().size }
            for (r in 0 until N) if (!ok((0 until N).map { r * N + it })) return false
            for (c in 0 until N) if (!ok((0 until N).map { it * N + c })) return false
            for (b in 0 until N) {
                val r0 = (b / 2) * 2
                val c0 = (b % 2) * 3
                if (!ok((0 until 2).flatMap { dr -> (0 until 3).map { dc -> (r0 + dr) * N + c0 + dc } })) return false
            }
            return true
        }

        /** Counts solutions of [grid], stopping at [limit]. */
        fun countSolutions(grid: IntArray, limit: Int): Int {
            val g = grid.copyOf()
            var count = 0
            fun canPlace(i: Int, v: Int): Boolean {
                val r = i / N; val c = i % N
                for (k in 0 until N) if (g[r * N + k] == v || g[k * N + c] == v) return false
                val r0 = (r / 2) * 2; val c0 = (c / 3) * 3
                for (dr in 0 until 2) for (dc in 0 until 3) if (g[(r0 + dr) * N + c0 + dc] == v) return false
                return true
            }
            fun search() {
                if (count >= limit) return
                val i = g.indexOfFirst { it == 0 }
                if (i < 0) { count++; return }
                for (v in 1..N) if (canPlace(i, v)) { g[i] = v; search(); g[i] = 0 }
            }
            search()
            return count
        }
    }
}
