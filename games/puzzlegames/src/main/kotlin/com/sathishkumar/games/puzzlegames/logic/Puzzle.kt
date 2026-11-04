package com.sathishkumar.games.puzzlegames.logic

enum class PuzzleType(val title: String, val emoji: String, val blurb: String) {
    SLIDE("Slide Puzzle", "🔢", "Slide the tiles back into order"),
    LIGHTS("Lights Out", "💡", "Turn every light off"),
    PIPES("Pipe Flow", "🚰", "Rotate pipes so water reaches every tile"),
    SUDOKU("Mini Sudoku", "🔳", "Fill the 6×6 grid, 1 to 6"),
}

/** What every puzzle has in common, so one screen can host any of them. */
interface Puzzle {
    val type: PuzzleType
    val level: Int
    /** Moves made (Sudoku: mistakes). */
    val moves: Int
    /** A good score to aim for; null if the puzzle has none. */
    val par: Int?
    val isSolved: Boolean
    /** Applies one helpful step. Returns false if no hint is available. */
    fun hint(): Boolean
    fun restart()
}

object Stars {
    /** 3 stars at or under par, 2 within 50% over, else 1. Sudoku rates mistakes instead. */
    fun rate(p: Puzzle): Int {
        if (p.type == PuzzleType.SUDOKU) return when (p.moves) { 0 -> 3; 1, 2 -> 2; else -> 1 }
        val par = p.par ?: return 3
        return when {
            p.moves <= par -> 3
            p.moves <= par * 3 / 2 -> 2
            else -> 1
        }
    }
}

fun newPuzzle(type: PuzzleType, level: Int): Puzzle = when (type) {
    PuzzleType.SLIDE -> SlidePuzzle.generate(level)
    PuzzleType.LIGHTS -> LightsOut.generate(level)
    PuzzleType.PIPES -> PipeFlow.generate(level)
    PuzzleType.SUDOKU -> MiniSudoku.generate(level)
}
