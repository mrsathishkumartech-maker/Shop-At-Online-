package com.sathishkumar.games.arrowescape.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {
    private val generator = LevelGenerator()

    @Test
    fun firstHundredLevelsAreAllSolvable() {
        for (n in 1..100) {
            val level = generator.generate(n)
            assertNotNull("Level $n is not solvable", Solver.solve(level.board))
            assertTrue("Level $n is almost empty", level.board.arrowCount >= 2)
        }
    }

    @Test
    fun levelsAreDeterministic() {
        val a = generator.generate(17).board
        val b = generator.generate(17).board
        assertEquals(a.arrows, b.arrows)
        assertEquals(a.gates, b.gates)
    }

    @Test
    fun earlyLevelsHaveNoGates_laterLevelsUseColoredArrows() {
        assertTrue(generator.generate(1).board.gates.isEmpty())
        val later = (10..30).map { generator.generate(it).board }
        assertTrue(later.all { it.gates.isNotEmpty() })
        assertTrue(later.any { board -> board.arrows.any { it.color != null } })
    }

    @Test
    fun boardsAreNotTrivial() {
        // On a non-trivial level some arrows start blocked.
        val level = generator.generate(20).board
        assertTrue(Solver.freeArrows(level).size < level.arrowCount)
    }

    @Test
    fun coloredArrowsAlwaysPointAtTheirGate() {
        for (n in 1..60) {
            val board = generator.generate(n).board
            for (arrow in board.arrows) {
                assertEquals(board.gates[board.exitSlot(arrow)], arrow.color)
            }
        }
    }

    @Test
    fun deadlockedBoardIsDetected() {
        val board = Board(2, 1, listOf(
            Arrow(0, 0, 0, Direction.RIGHT),
            Arrow(1, 1, 0, Direction.LEFT),
        ))
        assertFalse(Solver.isSolvable(board))
    }
}
