package com.sathishkumar.games.braingames.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BrainTest {
    @Test
    fun mathProblemsAreRight_andOptionsAreDistinct() {
        val g = QuickMath(Random(1))
        repeat(500) {
            val p = g.current
            val (a, op, b) = p.text.split(" ")
            val expected = when (op) {
                "+" -> a.toInt() + b.toInt()
                "−" -> a.toInt() - b.toInt()
                "×" -> a.toInt() * b.toInt()
                "÷" -> { assertEquals("${p.text} isn't whole", 0, a.toInt() % b.toInt()); a.toInt() / b.toInt() }
                else -> error(op)
            }
            assertEquals(p.text, expected, p.answer)
            assertTrue(p.answer >= 0)
            assertEquals(4, p.options.toSet().size)
            assertTrue(p.answer in p.options)
            assertTrue(p.options.all { it >= 0 })
            g.answer(p.answer)
        }
        assertEquals(QuickMath.MAX_LEVEL, g.level)
    }

    @Test
    fun mathLevelRisesAndFalls() {
        val g = QuickMath(Random(2))
        g.answer(g.current.answer); g.answer(g.current.answer)
        assertEquals(3, g.level)
        g.answer(-1)
        assertEquals(2, g.level)
        assertEquals(1, g.wrong)
        assertTrue(g.score > 0)
    }

    @Test
    fun harderMathAppearsAtHigherLevels() {
        val g = QuickMath(Random(3))
        val seen = mutableSetOf<String>()
        repeat(200) { seen += g.current.text.split(" ")[1]; g.answer(g.current.answer) }
        assertTrue(seen.containsAll(listOf("+", "−", "×", "÷")))
    }

    @Test
    fun memoryRoundsGrow_andWrongTapsCostLives() {
        val g = MemoryGrid(Random(4))
        assertEquals(3, g.size)
        assertEquals(3, g.pattern.size)
        // Clear 4 rounds by tapping the pattern.
        repeat(4) {
            val tiles = g.pattern.toList()
            tiles.dropLast(1).forEach { assertEquals(MemoryTap.CORRECT, g.tap(it)) }
            assertEquals(MemoryTap.ROUND_COMPLETE, g.tap(tiles.last()))
        }
        assertEquals(5, g.round)
        assertEquals(7, g.pattern.size)
        assertEquals(4, g.size)
        assertEquals(3 + 4 + 5 + 6, g.score)
        // Tapping the same tile twice is harmless.
        val first = g.pattern.first()
        g.tap(first)
        assertEquals(MemoryTap.ALREADY, g.tap(first))
        // Wrong taps.
        fun wrongTile() = (0 until g.size * g.size).first { it !in g.pattern }
        assertEquals(MemoryTap.WRONG, g.tap(wrongTile()))
        assertEquals(MemoryTap.WRONG, g.tap(wrongTile()))
        assertEquals(MemoryTap.GAME_OVER, g.tap(wrongTile()))
        assertTrue(g.isOver)
        assertTrue(g.useContinue())
        assertEquals(1, g.lives)
        assertFalse(g.isOver)
        repeat(1) { g.tap(wrongTile()) }
        assertFalse("Only one continue", g.useContinue())
    }

    @Test
    fun patternNeverFillsMoreThanHalfTheGrid() {
        val g = MemoryGrid(Random(5))
        repeat(30) {
            assertTrue(g.pattern.size <= g.size * g.size / 2)
            g.pattern.forEach { g.tap(it) }
        }
        assertEquals(6, g.size)
    }

    @Test
    fun colorClashWantsTheInkNotTheWord() {
        val g = ColorClash(Random(6))
        var clashes = 0
        repeat(1000) {
            val c = g.current
            if (c.word != c.ink) clashes++
            if (c.word != c.ink) assertFalse(g.answer(c.word)) else assertTrue(g.answer(c.ink))
        }
        assertTrue("About 75% of cards should clash: $clashes", clashes in 700..800)
        val c = g.current
        assertTrue(g.answer(c.ink))
        assertEquals(1, g.streak)
    }

    @Test
    fun numberRushInOrder_withPenalties() {
        val g = NumberRush(Random(7))
        assertEquals((1..25).toSet(), g.grid.toSet())
        val wrongCell = g.grid.indexOf(5)
        assertFalse(g.tap(wrongCell))
        for (n in 1..25) assertTrue(g.tap(g.grid.indexOf(n)))
        assertTrue(g.isDone)
        assertEquals(1, g.mistakes)
        assertEquals(22_000, g.finalTimeMs(20_000))
    }

    @Test
    fun brainScoreIsOutOfAThousand() {
        assertEquals(250, BrainScore.points(BrainGame.QUICK_MATH, 5_000))
        assertEquals(0, BrainScore.points(BrainGame.QUICK_MATH, 0))
        assertEquals(250, BrainScore.points(BrainGame.NUMBER_RUSH, 12_000))
        assertEquals(125, BrainScore.points(BrainGame.NUMBER_RUSH, 37_500))
        assertEquals(0, BrainScore.points(BrainGame.NUMBER_RUSH, 90_000))
        assertEquals(0, BrainScore.points(BrainGame.NUMBER_RUSH, 0))
        assertTrue(BrainScore.isBetter(BrainGame.NUMBER_RUSH, 20_000, 25_000))
        assertTrue(BrainScore.isBetter(BrainGame.NUMBER_RUSH, 20_000, 0))
        assertFalse(BrainScore.isBetter(BrainGame.NUMBER_RUSH, 30_000, 25_000))
        assertTrue(BrainScore.isBetter(BrainGame.MEMORY_GRID, 12, 9))
    }

    @Test
    fun streakCountsDaysInARow() {
        assertEquals(1, Streak.afterPlaying(lastPlayedDay = 0, streak = 0, today = 100))
        assertEquals(4, Streak.afterPlaying(99, 3, 100))
        assertEquals(3, Streak.afterPlaying(100, 3, 100))
        assertEquals(1, Streak.afterPlaying(97, 3, 100))
        assertEquals(3, Streak.current(99, 3, 100))
        assertEquals(0, Streak.current(98, 3, 100))
    }
}
