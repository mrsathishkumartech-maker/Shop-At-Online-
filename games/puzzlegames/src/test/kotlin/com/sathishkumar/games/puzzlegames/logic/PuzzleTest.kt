package com.sathishkumar.games.puzzlegames.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleTest {
    @Test
    fun slideLevelsAreSolvable_andHintsSolveThem() {
        for (level in listOf(1, 5, 8, 9, 12, 20, 30)) {
            val p = SlidePuzzle.generate(level)
            assertFalse(p.isSolved)
            var guard = 0
            while (!p.isSolved && guard++ < 2000) assertTrue(p.hint())
            assertTrue("Slide level $level not solved by hints", p.isSolved)
        }
    }

    @Test
    fun slideParIsOptimalOn3x3() {
        val p = SlidePuzzle.generate(6)
        val path = SlidePuzzle.solve(p.tiles, 3, 500_000)!!
        assertEquals(path.size, p.par)
        path.forEach { assertTrue(p.tap(it)) }
        assertTrue(p.isSolved)
        assertEquals(3, Stars.rate(p))
    }

    @Test
    fun slideTapMovesAWholeRow() {
        // 1 2 3 / 4 5 6 / _ 7 8 : tapping 8 slides 7 and 8 left (2 moves) and solves it.
        val p = SlidePuzzle(1, 3, intArrayOf(1, 2, 3, 4, 5, 6, 0, 7, 8), 2)
        assertFalse("not in the gap's row or column", p.tap(1))
        assertTrue(p.tap(8))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7, 8, 0), p.tiles.toList())
        assertEquals(2, p.moves)
        assertTrue(p.isSolved)
    }

    @Test
    fun lightsOutSolverFindsTheFewestTaps() {
        for (level in 1..20) {
            val p = LightsOut.generate(level)
            val plan = LightsOut.solve(p.lights, p.size)!!
            assertEquals(p.par, plan.size)
            plan.forEach { p.tap(it) }
            assertTrue("Lights level $level", p.isSolved)
            assertEquals(3, Stars.rate(p))
        }
        // Pressing the centre of a dark 3x3 lights a plus; one press undoes it.
        val b = BooleanArray(9); LightsOut.press(b, 3, 4)
        assertEquals(listOf(4), LightsOut.solve(b, 3))
    }

    @Test
    fun lightsOutHintsAlwaysFinish() {
        val p = LightsOut.generate(18)
        p.tap(0); p.tap(7) // wander off the plan
        var guard = 0
        while (!p.isSolved && guard++ < 50) assertTrue(p.hint())
        assertTrue(p.isSolved)
    }

    @Test
    fun pipeLevelsAreSolvable_andRotationsWork() {
        assertEquals(0b0010, PipeFlow.rotate(0b0001, 1)) // up -> right
        assertEquals(0b0001, PipeFlow.rotate(0b1000, 1)) // left -> up
        assertEquals(0b0101, PipeFlow.rotate(0b1010, 1)) // straight across -> straight up/down
        for (level in listOf(1, 4, 9, 16, 25)) {
            val p = PipeFlow.generate(level)
            assertFalse(p.isSolved)
            var guard = 0
            while (!p.isSolved && guard++ < 100) assertTrue(p.hint())
            assertTrue("Pipes level $level", p.isSolved)
            assertEquals("Solving by hints uses exactly par turns", p.par, p.moves)
        }
    }

    @Test
    fun sudokuPuzzlesHaveExactlyOneSolution() {
        for (level in listOf(1, 5, 10, 20)) {
            val s = MiniSudoku.generate(level)
            assertTrue(MiniSudoku.isValid(s.solution))
            assertTrue(s.solution.all { it in 1..6 })
            assertEquals(1, MiniSudoku.countSolutions(s.cells, 2))
            println("SUDOKU level $level: ${s.clues} clues")
        }
        assertTrue(MiniSudoku.generate(20).clues < MiniSudoku.generate(1).clues)
    }

    @Test
    fun sudokuRejectsWrongNumbers_andRatesMistakes() {
        val s = MiniSudoku.generate(3)
        val empty = s.cells.indexOfFirst { it == 0 }
        val wrong = (1..6).first { it != s.solution[empty] }
        assertFalse(s.place(empty, wrong))
        assertEquals(0, s.cells[empty])
        assertEquals(1, s.moves)
        assertTrue(s.place(empty, s.solution[empty]))
        while (!s.isSolved) assertNotNull(s.hint(null))
        assertEquals(2, Stars.rate(s))
    }

    @Test
    fun levelsAreDeterministic() {
        assertEquals(SlidePuzzle.generate(7).tiles.toList(), SlidePuzzle.generate(7).tiles.toList())
        assertEquals(LightsOut.generate(7).lights.toList(), LightsOut.generate(7).lights.toList())
        assertEquals(MiniSudoku.generate(7).cells.toList(), MiniSudoku.generate(7).cells.toList())
    }
}
