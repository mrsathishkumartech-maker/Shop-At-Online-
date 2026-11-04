package com.sathishkumar.games.puzzlegames.logic

import kotlin.random.Random

/**
 * Pipe Flow: every tile is a pipe piece. Tap to rotate it a quarter turn clockwise. Connect
 * them so water from the tank in the middle reaches every tile with no open ends.
 * Levels are a random tree of pipes (so a solution exists) with each piece then turned at random.
 */
class PipeFlow(
    override val level: Int,
    val size: Int,
    /** Solved shape of each tile: bit 0 = up, 1 = right, 2 = down, 3 = left. */
    private val solution: IntArray,
    private val startTurns: IntArray,
) : Puzzle {
    override val type = PuzzleType.PIPES
    val source: Int = (size / 2) * size + size / 2
    /** Quarter turns applied to each solved tile (0..3). */
    val turns: IntArray = startTurns.copyOf()
    override var moves = 0
        private set
    override val par: Int = solution.indices.sumOf { turnsToFix(it, startTurns[it]) }

    fun mask(i: Int): Int = rotate(solution[i], turns[i])

    override val isSolved: Boolean get() = connected().size == size * size && noOpenEnds()

    fun tap(i: Int): Boolean {
        if (isSolved) return false
        turns[i] = (turns[i] + 1) % 4
        moves++
        return true
    }

    /** Tiles water reaches from the source through matching ends. */
    fun connected(): Set<Int> {
        val seen = hashSetOf(source)
        val stack = ArrayDeque(listOf(source))
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            for (d in 0 until 4) {
                if (mask(i) and (1 shl d) == 0) continue
                val n = neighbour(i, d) ?: continue
                if (mask(n) and (1 shl ((d + 2) % 4)) != 0 && seen.add(n)) stack += n
            }
        }
        return seen
    }

    private fun noOpenEnds(): Boolean = (0 until size * size).all { i ->
        (0 until 4).all { d ->
            mask(i) and (1 shl d) == 0 ||
                neighbour(i, d)?.let { n -> mask(n) and (1 shl ((d + 2) % 4)) != 0 } == true
        }
    }

    /** Turns one wrong tile into its solved orientation. */
    override fun hint(): Boolean {
        if (isSolved) return false
        val wrong = solution.indices.firstOrNull { turnsToFix(it, turns[it]) > 0 } ?: return false
        repeat(turnsToFix(wrong, turns[wrong])) { tap(wrong) }
        return true
    }

    override fun restart() {
        startTurns.copyInto(turns)
        moves = 0
    }

    /** Clockwise quarter turns from [current] back to a solved orientation (symmetric pieces need fewer). */
    private fun turnsToFix(i: Int, current: Int): Int =
        (0 until 4).first { k -> rotate(solution[i], (current + k) % 4) == solution[i] }

    fun neighbour(i: Int, d: Int): Int? {
        val r = i / size
        val c = i % size
        return when (d) {
            0 -> if (r > 0) i - size else null
            1 -> if (c < size - 1) i + 1 else null
            2 -> if (r < size - 1) i + size else null
            else -> if (c > 0) i - 1 else null
        }
    }

    companion object {
        /** Rotates a 4-bit mask [k] quarter turns clockwise. */
        fun rotate(mask: Int, k: Int): Int {
            var m = mask
            repeat(((k % 4) + 4) % 4) { m = ((m shl 1) or (m shr 3)) and 0xF }
            return m
        }

        fun generate(level: Int, random: Random = Random(level * 15485863L + 7)): PipeFlow {
            val size = when {
                level <= 3 -> 3
                level <= 8 -> 4
                level <= 15 -> 5
                else -> 6
            }
            val n = size * size
            val masks = IntArray(n)
            val probe = PipeFlow(level, size, IntArray(n), IntArray(n))
            // Random depth-first spanning tree from the source.
            val visited = BooleanArray(n)
            val stack = ArrayDeque(listOf(probe.source))
            visited[probe.source] = true
            while (stack.isNotEmpty()) {
                val i = stack.last()
                val options = (0 until 4).filter { d -> probe.neighbour(i, d)?.let { !visited[it] } == true }
                if (options.isEmpty()) { stack.removeLast(); continue }
                val d = options.random(random)
                val j = probe.neighbour(i, d)!!
                masks[i] = masks[i] or (1 shl d)
                masks[j] = masks[j] or (1 shl ((d + 2) % 4))
                visited[j] = true
                stack += j
            }
            while (true) {
                val turns = IntArray(n) { random.nextInt(4) }
                val p = PipeFlow(level, size, masks, turns)
                if (!p.isSolved && p.par >= 2) return p
            }
        }
    }
}
