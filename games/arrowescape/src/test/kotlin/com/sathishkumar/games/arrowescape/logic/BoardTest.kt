package com.sathishkumar.games.arrowescape.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {
    @Test
    fun arrowBlockedByAnotherArrow() {
        val blocker = Arrow(1, 2, 0, Direction.UP)
        val board = Board(3, 1, listOf(Arrow(0, 0, 0, Direction.RIGHT), blocker))
        assertEquals(Block.ByArrow(blocker), board.blockOf(board.arrowAt(0, 0)!!))
    }

    @Test
    fun coloredArrowNeedsMatchingGate() {
        val slot = ExitSlot(Direction.RIGHT, 0)
        val red = Arrow(0, 0, 0, Direction.RIGHT, GateColor.RED)
        assertNull(Board(2, 1, listOf(red), mapOf(slot to GateColor.RED)).blockOf(red))
        assertEquals(
            Block.ByGate(slot, GateColor.BLUE),
            Board(2, 1, listOf(red), mapOf(slot to GateColor.BLUE)).blockOf(red),
        )
        assertEquals(Block.ByGate(slot, null), Board(2, 1, listOf(red)).blockOf(red))
    }

    @Test
    fun plainArrowCannotUseAColoredGate() {
        val slot = ExitSlot(Direction.UP, 0)
        val plain = Arrow(0, 0, 0, Direction.UP)
        val board = Board(1, 1, listOf(plain), mapOf(slot to GateColor.GREEN))
        assertTrue(board.blockOf(plain) is Block.ByGate)
    }
}
