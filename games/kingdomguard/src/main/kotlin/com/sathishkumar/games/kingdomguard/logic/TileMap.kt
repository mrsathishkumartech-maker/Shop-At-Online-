package com.sathishkumar.games.kingdomguard.logic

import kotlin.math.hypot

data class Tile(val x: Int, val y: Int)

/** A point in tile units: (0.5, 0.5) is the centre of the top-left tile. */
data class Point(val x: Float, val y: Float) {
    fun distanceTo(o: Point): Float = hypot(x - o.x, y - o.y)
}

/**
 * A battlefield drawn as text, one string per row:
 *  `.` open ground enemies can walk on, `#` rock, `P` a pad a soldier can stand on,
 *  `S` where enemies spawn, `C` the castle they're trying to reach.
 *
 * Enemies don't follow a hand-drawn route: [findPath] works it out with a breadth-first
 * search, so changing the map changes the route automatically.
 */
class TileMap(val name: String, rows: List<String>) {
    val height = rows.size
    val width = rows.first().length
    private val cells: List<String> = rows

    init {
        require(rows.all { it.length == width }) { "$name: rows must be the same length" }
    }

    val spawn: Tile = find('S')
    val castle: Tile = find('C')
    /** Soldier pads, in reading order. */
    val pads: List<Tile> = (0 until height).flatMap { y -> (0 until width).filter { x -> cells[y][x] == 'P' }.map { Tile(it, y) } }

    fun charAt(t: Tile): Char = if (inside(t)) cells[t.y][t.x] else '#'
    fun inside(t: Tile) = t.x in 0 until width && t.y in 0 until height
    fun walkable(t: Tile) = charAt(t) in WALKABLE

    /** Shortest walkable route from spawn to castle (4-way moves), or null if there is none. */
    fun findPath(): List<Tile>? {
        val previous = HashMap<Tile, Tile>()
        val queue = ArrayDeque(listOf(spawn))
        val seen = hashSetOf(spawn)
        while (queue.isNotEmpty()) {
            val t = queue.removeFirst()
            if (t == castle) {
                val route = mutableListOf(t)
                var cur = t
                while (cur != spawn) { cur = previous.getValue(cur); route += cur }
                return route.asReversed()
            }
            for ((dx, dy) in STEPS) {
                val n = Tile(t.x + dx, t.y + dy)
                if (walkable(n) && seen.add(n)) {
                    previous[n] = t
                    queue.addLast(n)
                }
            }
        }
        return null
    }

    private fun find(c: Char): Tile {
        for (y in 0 until height) for (x in 0 until width) if (cells[y][x] == c) return Tile(x, y)
        error("$name has no '$c'")
    }

    private companion object {
        const val WALKABLE = ".SC"
        val STEPS = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
    }
}

/** The route as a polyline through tile centres, measured in tiles walked. */
class Route(tiles: List<Tile>) {
    val points: List<Point> = tiles.map { Point(it.x + 0.5f, it.y + 0.5f) }
    private val cumulative: FloatArray = FloatArray(points.size).also { c ->
        for (i in 1 until points.size) c[i] = c[i - 1] + points[i - 1].distanceTo(points[i])
    }
    val length: Float get() = cumulative.last()

    /** Where an enemy is after walking [distance] tiles. */
    fun pointAt(distance: Float): Point {
        if (distance <= 0f) return points.first()
        if (distance >= length) return points.last()
        var i = cumulative.indexOfFirst { it > distance }.coerceAtLeast(1)
        val a = points[i - 1]
        val b = points[i]
        val t = (distance - cumulative[i - 1]) / (cumulative[i] - cumulative[i - 1])
        return Point(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
    }
}

object Maps {
    val VALLEY = TileMap("Green Valley", listOf(
        "S........",
        "########.",
        ".P.P.P.P.",
        ".........",
        ".########",
        ".P.P.P.P.",
        ".........",
        "########.",
        ".P.P.P.P.",
        ".........",
        ".########",
        ".........",
        "....C....",
    ))

    /** A switchback road down a hillside; every pad sits right beside the road. */
    val HILL_FORT = TileMap("Hill Fort", listOf(
        "####S####",
        "###P.P###",
        "##...P###",
        "#P.######",
        "##.....P#",
        "###P##.##",
        "#P.....##",
        "##.##P###",
        "##.....P#",
        "###P##.##",
        "####...P#",
        "###P.####",
        "####C####",
    ))

    /** Waves 1-10 in the valley, 11-20 at the fort, and so on. */
    fun forWave(wave: Int): TileMap = if (((wave - 1) / 10) % 2 == 0) VALLEY else HILL_FORT
}
