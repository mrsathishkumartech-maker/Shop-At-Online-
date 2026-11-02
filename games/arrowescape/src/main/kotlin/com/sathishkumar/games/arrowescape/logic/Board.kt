package com.sathishkumar.games.arrowescape.logic

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1), RIGHT(1, 0), DOWN(0, 1), LEFT(-1, 0);

    val isHorizontal: Boolean get() = dy == 0
}

/** Gate colors. A colored arrow can only leave through a gate of the same color. */
enum class GateColor { RED, BLUE, GREEN, YELLOW }

/**
 * One arrow on the board. [color] is null for a plain arrow, which can leave through
 * any border slot that has no gate.
 */
data class Arrow(
    val id: Int,
    val x: Int,
    val y: Int,
    val direction: Direction,
    val color: GateColor? = null,
)

/**
 * A slot on the board's border that an arrow leaves through.
 * [side] is the direction the arrow travels to reach it (RIGHT = the right edge);
 * [index] is the row for LEFT/RIGHT and the column for UP/DOWN.
 */
data class ExitSlot(val side: Direction, val index: Int)

/** Why an arrow cannot slide out right now. */
sealed interface Block {
    /** Another arrow is in the way. */
    data class ByArrow(val arrow: Arrow) : Block

    /** The path is clear but the border slot is a gate of a different color, or a gate a plain arrow cannot use. */
    data class ByGate(val slot: ExitSlot, val gate: GateColor?) : Block
}

class Board(
    val width: Int,
    val height: Int,
    arrows: Collection<Arrow> = emptyList(),
    val gates: Map<ExitSlot, GateColor> = emptyMap(),
) {
    private val cells = arrayOfNulls<Arrow>(width * height)

    init {
        require(width > 0 && height > 0) { "Board must be at least 1x1" }
        for ((slot, _) in gates) {
            val limit = if (slot.side.isHorizontal) height else width
            require(slot.index in 0 until limit) { "Gate $slot is off the board" }
        }
        arrows.forEach(::add)
    }

    val arrows: List<Arrow> get() = cells.filterNotNull()
    val arrowCount: Int get() = cells.count { it != null }
    val isCleared: Boolean get() = arrowCount == 0

    fun inside(x: Int, y: Int): Boolean = x in 0 until width && y in 0 until height

    fun arrowAt(x: Int, y: Int): Arrow? = if (inside(x, y)) cells[y * width + x] else null

    fun add(arrow: Arrow) {
        require(inside(arrow.x, arrow.y)) { "Arrow $arrow is off the board" }
        val i = arrow.y * width + arrow.x
        require(cells[i] == null) { "Cell (${arrow.x}, ${arrow.y}) is already taken" }
        cells[i] = arrow
    }

    fun remove(arrow: Arrow) {
        val i = arrow.y * width + arrow.x
        check(cells[i] == arrow) { "Arrow $arrow is not on the board" }
        cells[i] = null
    }

    /** The border slot an arrow at (x, y) moving in [direction] would leave through. */
    fun exitSlot(x: Int, y: Int, direction: Direction): ExitSlot =
        ExitSlot(direction, if (direction.isHorizontal) y else x)

    fun exitSlot(arrow: Arrow): ExitSlot = exitSlot(arrow.x, arrow.y, arrow.direction)

    /** First arrow between (x, y) and the edge in [direction], not counting (x, y) itself. */
    fun firstArrowInPath(x: Int, y: Int, direction: Direction): Arrow? {
        var cx = x + direction.dx
        var cy = y + direction.dy
        while (inside(cx, cy)) {
            arrowAt(cx, cy)?.let { return it }
            cx += direction.dx
            cy += direction.dy
        }
        return null
    }

    fun blockOf(arrow: Arrow): Block? {
        firstArrowInPath(arrow.x, arrow.y, arrow.direction)?.let { return Block.ByArrow(it) }
        val slot = exitSlot(arrow)
        val gate = gates[slot]
        return if (gate == arrow.color) null else Block.ByGate(slot, gate)
    }

    fun canEscape(arrow: Arrow): Boolean = blockOf(arrow) == null

    /** Cells from the arrow to just past the border (used for the slide animation). */
    fun distanceToEdge(x: Int, y: Int, direction: Direction): Int = when (direction) {
        Direction.RIGHT -> width - x
        Direction.LEFT -> x + 1
        Direction.DOWN -> height - y
        Direction.UP -> y + 1
    }

    fun copy(): Board = Board(width, height, arrows, gates)
}
