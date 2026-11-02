package com.sathishkumar.games.kiranasort.logic

import kotlin.random.Random

/** One playthrough of a level: board, timer and coins. */
class KiranaSession(val level: KiranaLevel, private val random: Random = Random.Default) {
    var board: Board = level.board.copy()
        private set
    val timeLimitMs: Long = level.spec.timeLimitSeconds * 1000L
    var remainingMs: Long = timeLimitMs
        private set
    var moves: Int = 0
        private set
    var triplesCleared: Int = 0
        private set

    val isWon: Boolean get() = board.isCleared
    val isTimeUp: Boolean get() = remainingMs <= 0 && !isWon
    val isStuck: Boolean get() = !isWon && !isTimeUp && board.isStuck
    val isOver: Boolean get() = isWon || isTimeUp

    /** Advances the clock. Returns true if this tick ran the timer out. */
    fun tick(elapsedMs: Long): Boolean {
        if (isOver || elapsedMs <= 0) return false
        remainingMs = (remainingMs - elapsedMs).coerceAtLeast(0)
        return remainingMs == 0L
    }

    fun move(move: Move): MoveResult? {
        if (isOver) return null
        val result = board.move(move) ?: return null
        moves++
        triplesCleared += result.cleared.size
        return result
    }

    fun addTime(ms: Long) {
        if (isWon) return
        remainingMs += ms
    }

    fun addShelf() = board.addShelf()

    fun shuffle(): List<Pair<Int, Item>> {
        val cleared = board.shuffle(random)
        triplesCleared += cleared.size
        return cleared
    }

    /** Base coins for a win, before the shop's multiplier: 10 + 1 per triple + 1 per 5s left. */
    fun baseCoins(): Int = if (!isWon) 0 else 10 + triplesCleared + (remainingMs / 5000).toInt()

    fun restart() {
        board = level.board.copy()
        remainingMs = timeLimitMs
        moves = 0
        triplesCleared = 0
    }
}
