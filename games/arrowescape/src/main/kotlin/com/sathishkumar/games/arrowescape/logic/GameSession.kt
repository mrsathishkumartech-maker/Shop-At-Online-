package com.sathishkumar.games.arrowescape.logic

sealed interface TapResult {
    /** Tapped an empty cell, or the game is already over. */
    data object Nothing : TapResult
    data class Escaped(val arrow: Arrow, val distance: Int) : TapResult
    data class Blocked(val arrow: Arrow, val block: Block) : TapResult
}

/** One playthrough of a level: board, lives and an undo history. */
class GameSession(val level: Level, val maxLives: Int = 3) {
    private sealed interface Move {
        data class Escape(val arrow: Arrow) : Move
        data class Bump(val arrow: Arrow) : Move
    }

    var board: Board = level.board.copy()
        private set
    var lives: Int = maxLives
        private set
    var moves: Int = 0
        private set

    /** Cells whose arrow has escaped; the rangoli shows through these. */
    val clearedCells: Set<Pair<Int, Int>> get() = _cleared
    private val _cleared = LinkedHashSet<Pair<Int, Int>>()
    private val history = ArrayDeque<Move>()

    val isWon: Boolean get() = board.isCleared
    val isLost: Boolean get() = lives <= 0 && !isWon
    val isOver: Boolean get() = isWon || isLost
    val canUndo: Boolean get() = history.isNotEmpty() && !isWon

    /** 3 stars for no mistakes, 2 for one, 1 otherwise. */
    val stars: Int get() = when (maxLives - lives) {
        0 -> 3
        1 -> 2
        else -> 1
    }

    fun tap(x: Int, y: Int): TapResult {
        if (isOver) return TapResult.Nothing
        val arrow = board.arrowAt(x, y) ?: return TapResult.Nothing
        moves++
        val block = board.blockOf(arrow)
        return if (block != null) {
            lives--
            history.addLast(Move.Bump(arrow))
            TapResult.Blocked(arrow, block)
        } else {
            val distance = board.distanceToEdge(arrow.x, arrow.y, arrow.direction)
            board.remove(arrow)
            _cleared += arrow.x to arrow.y
            history.addLast(Move.Escape(arrow))
            TapResult.Escaped(arrow, distance)
        }
    }

    /** Takes back the last move: puts an escaped arrow back, or refunds a lost life. */
    fun undo(): Boolean {
        if (!canUndo) return false
        when (val move = history.removeLast()) {
            is Move.Escape -> {
                board.add(move.arrow)
                _cleared -= move.arrow.x to move.arrow.y
            }
            is Move.Bump -> lives++
        }
        moves--
        return true
    }

    /** An arrow that can escape right now (null when the board is clear). */
    fun hint(): Arrow? = Solver.freeArrows(board).firstOrNull()

    fun restart() {
        board = level.board.copy()
        lives = maxLives
        moves = 0
        _cleared.clear()
        history.clear()
    }
}
