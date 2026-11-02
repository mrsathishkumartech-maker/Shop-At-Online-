package com.sathishkumar.games.arrowescape.logic

import kotlin.math.roundToInt
import kotlin.random.Random

data class Level(val number: Int, val board: Board, val rangoliSeed: Long)

data class LevelSpec(
    val width: Int,
    val height: Int,
    val density: Double,
    val gateCount: Int,
    val colorCount: Int,
)

/**
 * Builds levels by placing arrows one at a time, each with a clear path to an exit it is
 * allowed to use at the moment it is placed. Removing the arrows in reverse placement
 * order always works, so every generated level is solvable by construction.
 *
 * Levels are deterministic: the same level number always gives the same board.
 */
class LevelGenerator(private val baseSeed: Long = 0x5A7415L) {

    fun specFor(level: Int): LevelSpec {
        require(level >= 1) { "Levels start at 1" }
        val step = level - 1
        val width = (4 + step / 4).coerceAtMost(8)
        val height = (width + step / 6).coerceAtMost(11)
        val density = (0.6 + step * 0.015).coerceAtMost(0.9)
        val gateCount = if (level < 4) 0 else (1 + (level - 4) / 3).coerceAtMost((width + height) / 2)
        val colorCount = if (gateCount == 0) 0 else (1 + (level - 4) / 6).coerceAtMost(GateColor.entries.size)
        return LevelSpec(width, height, density, gateCount, colorCount)
    }

    fun generate(level: Int): Level {
        val spec = specFor(level)
        val random = Random(baseSeed * 31 + level)
        val gates = placeGates(spec, random)
        val board = Board(spec.width, spec.height, gates = gates)
        val target = (spec.width * spec.height * spec.density).roundToInt().coerceAtLeast(2)
        val colors = gates.values.toSet()

        var placedThisPass = true
        while (board.arrowCount < target && placedThisPass) {
            placedThisPass = false
            val empty = cellsOf(spec).filter { (x, y) -> board.arrowAt(x, y) == null }.shuffled(random)
            for ((x, y) in empty) {
                if (board.arrowCount >= target) break
                val options = Direction.entries.filter { dir ->
                    board.firstArrowInPath(x, y, dir) == null
                }
                if (options.isEmpty()) continue
                // An arrow takes the color of the slot it exits through (plain if no gate).
                // Prefer colored exits so the gates actually get used.
                val colored = options.filter { board.gates[board.exitSlot(x, y, it)] != null }
                val dir = if (colored.isNotEmpty() && colors.isNotEmpty() && random.nextDouble() < 0.6) {
                    colored.random(random)
                } else {
                    options.random(random)
                }
                val color = board.gates[board.exitSlot(x, y, dir)]
                board.add(Arrow(id = y * spec.width + x, x = x, y = y, direction = dir, color = color))
                placedThisPass = true
            }
        }
        return Level(level, board, rangoliSeedFor(level))
    }

    /** The rangoli shown for [level]; cheap, so the collection book doesn't regenerate boards. */
    fun rangoliSeedFor(level: Int): Long = baseSeed * 7919 + level

    private fun placeGates(spec: LevelSpec, random: Random): Map<ExitSlot, GateColor> {
        if (spec.gateCount == 0) return emptyMap()
        val slots = Direction.entries.flatMap { side ->
            val count = if (side.isHorizontal) spec.height else spec.width
            (0 until count).map { ExitSlot(side, it) }
        }.shuffled(random)
        val palette = GateColor.entries.take(spec.colorCount)
        return slots.take(spec.gateCount)
            .mapIndexed { i, slot -> slot to palette[i % palette.size] }
            .toMap()
    }

    private fun cellsOf(spec: LevelSpec): List<Pair<Int, Int>> =
        (0 until spec.height).flatMap { y -> (0 until spec.width).map { x -> x to y } }
}
