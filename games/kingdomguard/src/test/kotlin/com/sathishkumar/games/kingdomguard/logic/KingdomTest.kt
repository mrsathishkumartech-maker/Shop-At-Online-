package com.sathishkumar.games.kingdomguard.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KingdomTest {
    private val maps = listOf(Maps.VALLEY, Maps.HILL_FORT)

    @Test
    fun everyMapHasARoute_onWalkableTiles_withNoJumps() {
        for (m in maps) {
            val path = assertNotNull(m.findPath()).let { m.findPath()!! }
            assertEquals(m.spawn, path.first())
            assertEquals(m.castle, path.last())
            assertTrue(path.all { m.walkable(it) })
            path.zipWithNext().forEach { (a, b) -> assertEquals(1, Math.abs(a.x - b.x) + Math.abs(a.y - b.y)) }
            println("ROUTE ${m.name}: ${path.size} tiles")
        }
    }

    @Test
    fun breadthFirstSearchFindsTheShortestWay() {
        // An open field: the straight line down is the shortest route.
        val m = TileMap("test", listOf("S..", "...", "C.."))
        assertEquals(listOf(Tile(0, 0), Tile(0, 1), Tile(0, 2)), m.findPath())
        // Wall it off and there's no route at all.
        assertNull(TileMap("blocked", listOf("S..", "###", "C..")).findPath())
    }

    @Test
    fun everyPadCanReachTheRoadWithARecruit() {
        for (m in maps) {
            val route = Route(m.findPath()!!).points
            for (pad in m.pads) {
                val here = Point(pad.x + 0.5f, pad.y + 0.5f)
                val nearest = route.minOf { it.distanceTo(here) }
                assertTrue("${m.name} pad $pad is ${"%.2f".format(nearest)} from the road", nearest <= Rank.RECRUIT.range)
            }
        }
        assertEquals("Maps must have the same pad count", Maps.VALLEY.pads.size, Maps.HILL_FORT.pads.size)
    }

    @Test
    fun routeInterpolatesAlongTheRoad() {
        val r = Route(listOf(Tile(0, 0), Tile(0, 1), Tile(1, 1)))
        assertEquals(2f, r.length, 1e-5f)
        assertEquals(Point(0.5f, 1.0f), r.pointAt(0.5f))
        assertEquals(Point(1.0f, 1.5f), r.pointAt(1.5f))
    }

    @Test
    fun mergingTwoOfARankMakesTheNextRank() {
        val b = Battle()
        val p1 = b.buySoldier()!!
        val p2 = b.buySoldier()!!
        assertTrue(b.drag(p1, p2))
        assertNull(b.pads[p1])
        assertEquals(Rank.MILITIA, b.pads[p2]!!.rank)
        // Different ranks don't merge; dragging to an empty pad moves.
        val p3 = b.buySoldier()!!
        assertFalse(b.drag(p3, p2))
        val empty = b.pads.indexOfFirst { it == null }
        assertTrue(b.drag(p3, empty))
        assertEquals(Rank.RECRUIT, b.pads[empty]!!.rank)
        assertNull(b.pads[p3])
    }

    @Test
    fun championsDontMergeFurther() {
        val b = Battle()
        val a = b.buySoldier()!!
        val c = b.buySoldier()!!
        b.pads[a]!!.rank = Rank.CHAMPION
        b.pads[c]!!.rank = Rank.CHAMPION
        assertFalse(b.drag(a, c))
    }

    @Test
    fun soldiersCostMoreEachTime_andGoldRunsOut() {
        val b = Battle()
        val costs = mutableListOf<Int>()
        while (true) { val c = b.soldierCost; if (b.buySoldier() == null) break; costs += c }
        assertEquals(listOf(15, 18, 21, 24), costs) // 100 gold: 15 + 18 + 21 + 24 = 78
    }

    @Test
    fun soldiersTargetTheEnemyFurthestAlong() {
        val b = Battle()
        val pad = b.buySoldier()!!
        b.startWave()
        b.step(2.2f) // a few goblins are on the road
        val inRange = b.enemies.filter {
            val t = b.map.pads[pad]
            b.route.pointAt(it.distance).distanceTo(Point(t.x + 0.5f, t.y + 0.5f)) <= Rank.RECRUIT.range
        }
        assertEquals(inRange.maxByOrNull { it.distance }, b.targetFor(pad, Rank.RECRUIT))
    }

    @Test
    fun withNoSoldiersTheCastleFalls_andCanBeRevivedOnce() {
        val b = Battle(startWave = 20)
        b.startWave()
        repeat(2000) { b.step(0.05f) }
        assertEquals(Phase.GAME_OVER, b.phase)
        assertTrue(b.revive())
        assertEquals(Battle.MAX_LIVES / 2, b.lives)
        assertFalse(b.revive().also { if (b.phase == Phase.GAME_OVER) Unit })
    }

    @Test
    fun bossEveryTenthWave_andEnemiesGetTougher() {
        assertTrue(Waves.plan(10).any { it.kind == EnemyKind.WARLORD })
        assertFalse(Waves.plan(9).any { it.kind == EnemyKind.WARLORD })
        assertTrue(Waves.hpScale(20) > Waves.hpScale(10))
        assertEquals(Waves.plan(7), Waves.plan(7))
    }

    /** A simple player: buys whenever possible, merges any pair, then spends spare gold at the forge. */
    private fun botReaches(maxWave: Int = 80): Int {
        val b = Battle()
        while (b.phase != Phase.GAME_OVER && b.wave <= maxWave) {
            // Build phase.
            var changed = true
            while (changed) {
                changed = false
                if (b.buySoldier() != null) changed = true
                val byRank = b.pads.withIndex().filter { it.value != null }.groupBy { it.value!!.rank }
                for ((rank, list) in byRank) if (rank.next != null && list.size >= 2) {
                    if (b.drag(list[0].index, list[1].index)) { changed = true; break }
                }
                if (!changed) {
                    if (b.damageCost <= b.speedCost) { if (b.upgradeDamage()) changed = true }
                    else if (b.upgradeSpeed()) changed = true
                }
            }
            val wave = b.wave
            b.startWave()
            while (b.phase == Phase.FIGHT) b.step(0.05f)
            if (b.phase == Phase.GAME_OVER) return wave
        }
        return b.wave
    }

    @Test
    fun aSensiblePlayerSurvivesTheEarlyWavesButNotForever() {
        val reached = botReaches()
        println("BOT fell on wave $reached")
        assertTrue("Too hard: fell on wave $reached", reached >= 12)
        assertTrue("Too easy: survived to wave $reached", reached < 80)
    }
}
