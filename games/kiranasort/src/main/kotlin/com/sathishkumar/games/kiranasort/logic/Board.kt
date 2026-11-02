package com.sathishkumar.games.kiranasort.logic

/** Things sold in the shop. Three of the same on one shelf clear. */
enum class Item { CHAI, MANGO, LADDOO, BANGLES, SAMOSA, JALEBI, COCONUT, BANANA, CHILLI, LEMON }

const val SLOTS_PER_SHELF = 3

/** A slot on the front row of a shelf. */
data class SlotRef(val shelf: Int, val slot: Int)

data class Move(val from: SlotRef, val to: SlotRef)

/**
 * One shelf: a front row the player can touch, and rows hidden behind it.
 * [layers] first element is the row that comes forward next.
 */
class Shelf(
    val front: Array<Item?> = arrayOfNulls(SLOTS_PER_SHELF),
    val layers: ArrayDeque<Array<Item?>> = ArrayDeque(),
) {
    init {
        require(front.size == SLOTS_PER_SHELF)
        require(layers.all { it.size == SLOTS_PER_SHELF })
    }

    val isFrontEmpty: Boolean get() = front.all { it == null }
    val isEmpty: Boolean get() = isFrontEmpty && layers.isEmpty()
    val emptySlots: Int get() = front.count { it == null }
    val hiddenCount: Int get() = layers.sumOf { row -> row.count { it != null } }

    /** The item if the front row is three of the same, else null. */
    val frontTriple: Item? get() = front[0]?.takeIf { first -> front.all { it == first } }

    fun copy(): Shelf = Shelf(front.copyOf(), ArrayDeque(layers.map { it.copyOf() }))
}

/** What happened as a result of a move. */
data class MoveResult(
    val move: Move,
    /** Items cleared as triples, with the shelf they cleared from. */
    val cleared: List<Pair<Int, Item>>,
)

class Board(shelves: List<Shelf>) {
    private val _shelves = shelves.toMutableList()
    val shelves: List<Shelf> get() = _shelves

    val itemCount: Int get() = _shelves.sumOf { s -> s.front.count { it != null } + s.hiddenCount }
    val isCleared: Boolean get() = _shelves.all { it.isEmpty }

    fun itemAt(ref: SlotRef): Item? = _shelves.getOrNull(ref.shelf)?.front?.getOrNull(ref.slot)

    fun canMove(move: Move): Boolean =
        move.from.shelf != move.to.shelf &&
            itemAt(move.from) != null &&
            move.to.shelf in _shelves.indices &&
            move.to.slot in 0 until SLOTS_PER_SHELF &&
            itemAt(move.to) == null

    /** Moves an item, then clears any triple and brings hidden rows forward. Null if illegal. */
    fun move(move: Move): MoveResult? {
        if (!canMove(move)) return null
        val item = itemAt(move.from)
        _shelves[move.from.shelf].front[move.from.slot] = null
        _shelves[move.to.shelf].front[move.to.slot] = item
        val cleared = settle(move.to.shelf) + settle(move.from.shelf)
        return MoveResult(move, cleared)
    }

    /** First empty front slot on [shelf], if any. */
    fun firstEmptySlot(shelf: Int): SlotRef? =
        _shelves.getOrNull(shelf)?.front?.indexOfFirst { it == null }?.takeIf { it >= 0 }?.let { SlotRef(shelf, it) }

    /** True if any item can be moved to an empty slot on a different shelf. */
    fun hasAnyMove(): Boolean {
        val withItems = _shelves.indices.filter { i -> _shelves[i].front.any { it != null } }
        val withSpace = _shelves.indices.filter { i -> _shelves[i].emptySlots > 0 }
        return withItems.any { a -> withSpace.any { b -> a != b } }
    }

    /** Stuck: items left but no legal move. */
    val isStuck: Boolean get() = !isCleared && !hasAnyMove()

    /** Adds an empty shelf (the "extra shelf" reward). */
    fun addShelf() {
        _shelves += Shelf()
    }

    /** Randomly redistributes all front-row items across all front slots, then settles. */
    fun shuffle(random: kotlin.random.Random): List<Pair<Int, Item>> {
        val items = _shelves.flatMap { s -> s.front.filterNotNull() }.shuffled(random)
        val slots = _shelves.indices.flatMap { s -> (0 until SLOTS_PER_SHELF).map { SlotRef(s, it) } }.shuffled(random)
        _shelves.forEach { it.front.fill(null) }
        items.forEachIndexed { i, item -> _shelves[slots[i].shelf].front[slots[i].slot] = item }
        return _shelves.indices.flatMap { settle(it) }
    }

    /** Clears triples and advances hidden rows until the shelf is stable. */
    private fun settle(index: Int): List<Pair<Int, Item>> {
        val shelf = _shelves[index]
        val cleared = mutableListOf<Pair<Int, Item>>()
        while (true) {
            val triple = shelf.frontTriple
            if (triple != null) {
                shelf.front.fill(null)
                cleared += index to triple
                continue
            }
            if (shelf.isFrontEmpty && shelf.layers.isNotEmpty()) {
                shelf.layers.removeFirst().copyInto(shelf.front)
                continue
            }
            return cleared
        }
    }

    fun copy(): Board = Board(_shelves.map { it.copy() })
}
