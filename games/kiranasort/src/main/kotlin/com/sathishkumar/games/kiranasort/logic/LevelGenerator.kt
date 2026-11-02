package com.sathishkumar.games.kiranasort.logic

import kotlin.random.Random

data class LevelSpec(
    val shelves: Int,
    val itemTypes: Int,
    val triples: Int,
    val maxLayers: Int,
    val scrambleMoves: Int,
    val timeLimitSeconds: Int,
)

/** A level, with one known solution (used by tests and as proof it can be solved). */
class KiranaLevel(val number: Int, val board: Board, val spec: LevelSpec, val solution: List<Move>)

/**
 * Builds levels backwards from an empty shop, so every level is solvable by construction.
 *
 * Working backwards, each step is the reverse of something a player does:
 *  - un-clear: put three of a kind on a shelf's front row (pushing what was there behind it),
 *    then immediately move one of them away (the reverse of the move that completed the triple);
 *  - un-move: move a front item to an empty slot on another shelf.
 * Playing the recorded steps forwards clears the board, so the level always has a solution.
 */
class LevelGenerator(private val baseSeed: Long = 0x6B6972L) {

    fun specFor(level: Int): LevelSpec {
        require(level >= 1) { "Levels start at 1" }
        val step = level - 1
        val shelves = (4 + step / 3).coerceAtMost(9)
        val itemTypes = (3 + step / 2).coerceAtMost(Item.entries.size)
        val triples = (4 + step).coerceAtMost(shelves * 3)
        val maxLayers = (1 + step / 4).coerceAtMost(4)
        val scramble = 2 + step / 2
        val time = 45 + triples * 6
        return LevelSpec(shelves, itemTypes, triples, maxLayers, scramble, time)
    }

    fun generate(level: Int): KiranaLevel {
        val spec = specFor(level)
        // Retry with a different stream in the rare case the shelves fill up mid-build.
        for (attempt in 0 until 20) {
            val random = Random(baseSeed * 31 + level * 1009L + attempt)
            build(spec, random)?.let { (board, reversed) ->
                return KiranaLevel(level, board, spec, reversed.asReversed().toList())
            }
        }
        error("Could not build level $level")
    }

    private fun build(spec: LevelSpec, random: Random): Pair<Board, List<Move>>? {
        val shelves = List(spec.shelves) { Shelf() }
        val board = Board(shelves)
        val forwardMovesReversed = mutableListOf<Move>()
        val types = Item.entries.shuffled(random).take(spec.itemTypes)
        val tripleTypes = List(spec.triples) { types[it % types.size] }.shuffled(random)

        for (type in tripleTypes) {
            if (!unClear(shelves, type, spec, random, forwardMovesReversed)) return null
            repeat(random.nextInt(spec.scrambleMoves + 1)) {
                unMove(shelves, random, fromShelf = null)?.let { forwardMovesReversed += it }
            }
        }
        repeat(spec.scrambleMoves) {
            unMove(shelves, random, fromShelf = null)?.let { forwardMovesReversed += it }
        }
        return board to forwardMovesReversed
    }

    private fun unClear(
        shelves: List<Shelf>,
        type: Item,
        spec: LevelSpec,
        random: Random,
        out: MutableList<Move>,
    ): Boolean {
        val candidates = shelves.indices.filter { i ->
            val s = shelves[i]
            s.isEmpty || (!s.isFrontEmpty && s.layers.size < spec.maxLayers)
        }.shuffled(random)
        for (i in candidates) {
            val shelf = shelves[i]
            val saved = shelf.copy()
            if (!shelf.isFrontEmpty) shelf.layers.addFirst(shelf.front.copyOf())
            shelf.front.fill(type)
            val move = unMove(shelves, random, fromShelf = i)
            if (move != null) {
                out += move
                return true
            }
            // Undo and try another shelf.
            saved.front.copyInto(shelf.front)
            shelf.layers.clear()
            shelf.layers.addAll(saved.layers)
        }
        return false
    }

    /**
     * Reverse of a player move: takes an item from shelf X (or [fromShelf]) and puts it in an
     * empty slot on shelf Y. Returns the forward move (Y -> X), or null if none is possible.
     */
    private fun unMove(shelves: List<Shelf>, random: Random, fromShelf: Int?): Move? {
        val sources = (fromShelf?.let { listOf(it) } ?: shelves.indices.toList()).flatMap { x ->
            (0 until SLOTS_PER_SHELF).filter { shelves[x].front[it] != null }.map { SlotRef(x, it) }
        }.shuffled(random)
        for (src in sources) {
            val x = shelves[src.shelf]
            val item = x.front[src.slot]!!
            // A front row can't be left empty while rows wait behind it.
            if (x.front.count { it != null } == 1 && x.layers.isNotEmpty()) continue
            val targets = shelves.indices.filter { it != src.shelf }.flatMap { y ->
                (0 until SLOTS_PER_SHELF).filter { shelves[y].front[it] == null }.map { SlotRef(y, it) }
            }.filter { dst ->
                // Never leave a resting triple: the forward game would already have cleared it.
                val row = shelves[dst.shelf].front
                row.count { it == item } < SLOTS_PER_SHELF - 1
            }
            if (targets.isEmpty()) continue
            val dst = targets.random(random)
            x.front[src.slot] = null
            shelves[dst.shelf].front[dst.slot] = item
            return Move(from = dst, to = src)
        }
        return null
    }
}
