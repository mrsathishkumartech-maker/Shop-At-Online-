package com.sathishkumar.games.arrowescape.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    private fun session(): GameSession {
        // (0,0) points right into (1,0), which points up and is free.
        val board = Board(2, 1, listOf(
            Arrow(0, 0, 0, Direction.RIGHT),
            Arrow(1, 1, 0, Direction.UP),
        ))
        return GameSession(Level(1, board, 1L))
    }

    @Test
    fun blockedTapCostsALife_andUndoRefundsIt() {
        val s = session()
        assertTrue(s.tap(0, 0) is TapResult.Blocked)
        assertEquals(2, s.lives)
        assertTrue(s.undo())
        assertEquals(3, s.lives)
    }

    @Test
    fun clearingTheBoardWins_withThreeStarsWhenNoMistakes() {
        val s = session()
        assertTrue(s.tap(1, 0) is TapResult.Escaped)
        assertTrue(s.tap(0, 0) is TapResult.Escaped)
        assertTrue(s.isWon)
        assertEquals(3, s.stars)
        assertEquals(setOf(1 to 0, 0 to 0), s.clearedCells)
    }

    @Test
    fun undoPutsAnEscapedArrowBack() {
        val s = session()
        s.tap(1, 0)
        assertTrue(s.undo())
        assertEquals(2, s.board.arrowCount)
        assertTrue(s.clearedCells.isEmpty())
    }

    @Test
    fun runningOutOfLivesLoses_andHintAlwaysPointsAtAFreeArrow() {
        val s = session()
        repeat(3) { s.tap(0, 0) }
        assertTrue(s.isLost)
        assertEquals(TapResult.Nothing, s.tap(1, 0))
        s.undo()
        assertFalse(s.isLost)
        assertEquals(s.board.arrowAt(1, 0), s.hint())
    }

    @Test
    fun hintFollowedRepeatedlySolvesARealLevel() {
        val s = GameSession(LevelGenerator().generate(25))
        while (!s.isWon) {
            val arrow = s.hint()!!
            assertTrue(s.tap(arrow.x, arrow.y) is TapResult.Escaped)
        }
        assertEquals(3, s.stars)
    }
}
