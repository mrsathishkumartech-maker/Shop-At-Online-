package com.sathishkumar.games.puzzlegames.logic

import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.random.Random

/**
 * Sliding tiles: numbers 1..n²-1 and one gap (0). Tap a tile in the gap's row or column to
 * slide it (and any tiles between) into the gap. Levels are made by sliding the solved board
 * at random, so they can always be solved.
 */
class SlidePuzzle(override val level: Int, val size: Int, private val start: IntArray, override val par: Int?) : Puzzle {
    override val type = PuzzleType.SLIDE
    val tiles: IntArray = start.copyOf()
    override var moves = 0
        private set

    override val isSolved: Boolean get() = isGoal(tiles)

    val gap: Int get() = tiles.indexOf(0)

    /** Slides toward the gap from [index] if it shares a row or column with it. Each tile moved is a move. */
    fun tap(index: Int): Boolean {
        if (isSolved) return false
        val g = gap
        val (gr, gc) = g / size to g % size
        val (r, c) = index / size to index % size
        if (index == g || (r != gr && c != gc)) return false
        val step = when {
            r == gr && c < gc -> -1
            r == gr -> 1
            r < gr -> -size
            else -> size
        }
        var cur = g
        while (cur != index) {
            val next = cur + step
            tiles[cur] = tiles[next]
            tiles[next] = 0
            cur = next
            moves++
        }
        return true
    }

    // Hints follow one plan; it's only recomputed if the player has moved tiles since.
    private val plan = ArrayDeque<Int>()
    private var planFor: List<Int>? = null

    override fun hint(): Boolean {
        if (isSolved) return false
        if (planFor != tiles.toList() || plan.isEmpty()) {
            plan.clear()
            plan.addAll(planFrom(tiles, size) ?: return false)
        }
        val next = plan.removeFirst()
        val ok = tap(next)
        planFor = tiles.toList()
        return ok
    }

    override fun restart() {
        start.copyInto(tiles)
        moves = 0
        plan.clear()
        planFor = null
    }

    private fun isGoal(t: IntArray): Boolean = t.indices.all { i -> t[i] == if (i == t.lastIndex) 0 else i + 1 }

    companion object {
        fun generate(level: Int, random: Random = Random(level * 7919L + 1)): SlidePuzzle {
            val size = if (level <= 8) 3 else 4
            val scramble = if (size == 3) (8 + level * 3).coerceAtMost(40) else (20 + (level - 8) * 4).coerceAtMost(80)
            val tiles = IntArray(size * size) { if (it == size * size - 1) 0 else it + 1 }
            var gap = tiles.lastIndex
            var previous = -1
            repeat(scramble) {
                val options = neighbours(gap, size).filter { it != previous }
                val n = options.random(random)
                tiles[gap] = tiles[n]; tiles[n] = 0
                previous = gap
                gap = n
            }
            if (tiles.indices.all { i -> tiles[i] == if (i == tiles.lastIndex) 0 else i + 1 }) {
                // Never hand out a solved board.
                val n = neighbours(gap, size).first()
                tiles[gap] = tiles[n]; tiles[n] = 0
            }
            // 3×3 boards solve instantly, so par is the true minimum. Searching 4×4 boards could stall
            // the screen, so their par is the scramble length (never below the true minimum).
            val optimal = if (size == 3) solve(tiles, size, nodeLimit = 200_000)?.size else null
            return SlidePuzzle(level, size, tiles, optimal ?: scramble)
        }

        fun neighbours(i: Int, size: Int): List<Int> = buildList {
            val r = i / size
            val c = i % size
            if (r > 0) add(i - size)
            if (r < size - 1) add(i + size)
            if (c > 0) add(i - 1)
            if (c < size - 1) add(i + 1)
        }

        fun manhattan(t: IntArray, size: Int): Int {
            var d = 0
            for (i in t.indices) {
                val v = t[i]
                if (v == 0) continue
                val goal = v - 1
                d += abs(i / size - goal / size) + abs(i % size - goal % size)
            }
            return d
        }

        /**
         * Tiles to tap (each one step) from [start] to solved, by A* on Manhattan distance;
         * null if it needs more than [nodeLimit] expansions. With [weight] 1 the result is a
         * shortest solution; a higher weight finds a longer one much faster.
         */
        fun solve(start: IntArray, size: Int, nodeLimit: Int, weight: Int = 1): List<Int>? {
            data class Node(val t: IntArray, val g: Int, val f: Int, val path: List<Int>)
            val open = PriorityQueue<Node>(compareBy({ it.f }, { -it.g }))
            val seen = HashMap<String, Int>()
            open += Node(start, 0, weight * manhattan(start, size), emptyList())
            var expanded = 0
            while (open.isNotEmpty()) {
                val n = open.poll()
                if (manhattan(n.t, size) == 0) return n.path
                if (++expanded > nodeLimit) return null
                val gap = n.t.indexOf(0)
                for (m in neighbours(gap, size)) {
                    val t = n.t.copyOf()
                    t[gap] = t[m]; t[m] = 0
                    val key = t.joinToString(",")
                    val g = n.g + 1
                    if ((seen[key] ?: Int.MAX_VALUE) <= g) continue
                    seen[key] = g
                    open += Node(t, g, g + weight * manhattan(t, size), n.path + m)
                }
            }
            return null
        }

        /** A full solution: shortest on 3×3; on 4×4 a quick, not necessarily shortest, one. */
        fun planFrom(t: IntArray, size: Int): List<Int>? =
            if (size == 3) solve(t, size, nodeLimit = 200_000)
            else solve(t, size, nodeLimit = 60_000, weight = 3) ?: solve(t, size, nodeLimit = 200_000, weight = 6)

    }
}
