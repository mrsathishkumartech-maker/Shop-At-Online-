package com.sathishkumar.games.kiranasort.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KiranaLogicTest {
    private val generator = LevelGenerator()

    private fun shelf(vararg front: Item?, layers: List<List<Item?>> = emptyList()) =
        Shelf(arrayOf(*front), ArrayDeque(layers.map { it.toTypedArray() }))

    @Test
    fun everyLevelIsSolvableByItsRecordedSolution() {
        for (n in 1..150) {
            val level = generator.generate(n)
            val board = level.board.copy()
            assertEquals("Level $n should contain exactly its triples",
                level.spec.triples * 3, board.itemCount)
            for (move in level.solution) {
                assertNotNull("Level $n: illegal move $move", board.move(move))
            }
            assertTrue("Level $n not cleared by its solution", board.isCleared)
        }
    }

    @Test
    fun startingBoardsHaveNoReadyTriplesAndAtLeastOneMove() {
        for (n in 1..150) {
            val board = generator.generate(n).board
            assertTrue(board.shelves.none { it.frontTriple != null })
            assertTrue(board.shelves.none { it.isFrontEmpty && it.layers.isNotEmpty() })
            assertTrue("Level $n starts stuck", board.hasAnyMove())
        }
    }

    @Test
    fun laterLevelsHideItemsBehindTheFrontRow() {
        val board = generator.generate(30).board
        assertTrue(board.shelves.any { it.hiddenCount > 0 })
    }

    @Test
    fun levelsAreDeterministic() {
        val a = generator.generate(12).board
        val b = generator.generate(12).board
        assertEquals(a.shelves.map { it.front.toList() }, b.shelves.map { it.front.toList() })
    }

    @Test
    fun matchingThreeClearsAndBringsTheNextRowForward() {
        val board = Board(listOf(
            shelf(Item.MANGO, Item.MANGO, null, layers = listOf(listOf(Item.CHAI, null, Item.LEMON))),
            shelf(Item.MANGO, null, null),
        ))
        val result = board.move(Move(SlotRef(1, 0), SlotRef(0, 2)))!!
        assertEquals(listOf(0 to Item.MANGO), result.cleared)
        assertEquals(listOf(Item.CHAI, null, Item.LEMON), board.shelves[0].front.toList())
        assertTrue(board.shelves[0].layers.isEmpty())
    }

    @Test
    fun illegalMovesAreRejected() {
        val board = Board(listOf(shelf(Item.CHAI, Item.MANGO, Item.LEMON), shelf(Item.CHAI, null, null)))
        assertNull(board.move(Move(SlotRef(0, 0), SlotRef(1, 0)))) // target slot full
        assertNull(board.move(Move(SlotRef(1, 1), SlotRef(0, 0)))) // no item to move
        assertNull(board.move(Move(SlotRef(1, 0), SlotRef(1, 1)))) // same shelf
    }

    @Test
    fun fullShelvesAreStuck_andAnExtraShelfUnsticksThem() {
        val board = Board(listOf(
            shelf(Item.CHAI, Item.MANGO, Item.LEMON),
            shelf(Item.MANGO, Item.CHAI, Item.LEMON),
        ))
        assertTrue(board.isStuck)
        board.addShelf()
        assertFalse(board.isStuck)
    }

    @Test
    fun shuffleKeepsEveryItem() {
        val board = generator.generate(20).board
        val before = board.itemCount
        val cleared = board.shuffle(Random(1))
        assertEquals(before, board.itemCount + cleared.size * 3)
    }

    @Test
    fun sessionTimerAndCoins() {
        val level = generator.generate(1)
        val session = KiranaSession(level)
        assertFalse(session.tick(1000))
        session.addTime(5000)
        assertEquals(level.spec.timeLimitSeconds * 1000L + 4000, session.remainingMs)
        level.solution.forEach { session.move(it) }
        assertTrue(session.isWon)
        assertEquals(level.spec.triples, session.triplesCleared)
        assertTrue(session.baseCoins() >= 10 + level.spec.triples)
        assertTrue(session.tick(Long.MAX_VALUE).not()) // no ticking once won
    }

    @Test
    fun timeRunningOutEndsTheLevel() {
        val session = KiranaSession(generator.generate(3))
        assertTrue(session.tick(session.timeLimitMs))
        assertTrue(session.isTimeUp)
        assertNull(session.move(session.level.solution.first()))
        session.addTime(30_000)
        assertFalse(session.isTimeUp)
    }

    @Test
    fun shopUpgradesFromStallToSupermarket() {
        val shop = ShopProgress(coins = 0, stage = ShopStage.STREET_STALL)
        assertFalse(shop.canUpgrade)
        assertEquals(100, shop.earn(100))
        assertFalse(shop.canUpgrade)
        shop.earn(50)
        assertTrue(shop.upgrade())
        assertEquals(ShopStage.KIRANA_SHOP, shop.stage)
        assertEquals(0, shop.coins)
        assertEquals(125, shop.reward(100))
        repeat(3) { shop.earn(10_000); shop.upgrade() }
        assertEquals(ShopStage.SUPERMARKET, shop.stage)
        assertFalse(shop.upgrade())
    }
}
