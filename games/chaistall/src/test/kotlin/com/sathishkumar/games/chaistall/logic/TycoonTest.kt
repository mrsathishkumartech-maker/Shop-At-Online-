package com.sathishkumar.games.chaistall.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TycoonTest {
    private val hour = 60 * 60 * 1000L

    @Test
    fun newGameSellsChaiAtTheRoadside() {
        val s = TycoonState.newGame()
        assertEquals(1.0, s.incomePerSecond(0), 1e-9)
        assertFalse(s.at(Location.CHENNAI).unlocked)
    }

    @Test
    fun upgradesCostMoreEachTime_andMilestonesDoubleIncome() {
        val c1 = Economy.upgradeCost(Location.ROADSIDE, Product.CHAI, 1)
        val c2 = Economy.upgradeCost(Location.ROADSIDE, Product.CHAI, 2)
        assertEquals(Economy.COST_GROWTH, c2 / c1, 1e-9)
        val at24 = Economy.productIncome(Location.ROADSIDE, Product.CHAI, 24)
        val at25 = Economy.productIncome(Location.ROADSIDE, Product.CHAI, 25)
        assertEquals(25.0 / 24.0 * 2, at25 / at24, 1e-9)
    }

    @Test
    fun buyingNeedsCash() {
        val s = TycoonState.newGame()
        assertFalse(s.upgrade(Location.ROADSIDE, Product.SAMOSA))
        s.earn(60.0)
        assertTrue(s.upgrade(Location.ROADSIDE, Product.SAMOSA))
        assertEquals(0.0, s.cash, 1e-9)
        assertEquals(5.0, s.incomePerSecond(0), 1e-9)
    }

    @Test
    fun helpersAddIncome_andOnlyStaffedStallsEarnOffline() {
        val s = TycoonState.newGame()
        s.lastSeenMs = 1
        assertEquals(0.0, s.offlineEarnings(hour), 1e-9)
        s.earn(100.0)
        assertTrue(s.hireHelper(Location.ROADSIDE))
        assertEquals(1.5, s.incomePerSecond(0), 1e-9)
        // 1 hour away at 50% of 1.5/s.
        assertEquals(1.5 * 0.5 * 3600, s.offlineEarnings(1 + hour), 1e-6)
        // Capped at 8 hours.
        assertEquals(s.offlineEarnings(1 + 8 * hour), s.offlineEarnings(1 + 48 * hour), 1e-6)
    }

    @Test
    fun helpersAreCappedAtThree() {
        val s = TycoonState.newGame()
        s.earn(1e9)
        repeat(3) { assertTrue(s.hireHelper(Location.ROADSIDE)) }
        assertFalse(s.hireHelper(Location.ROADSIDE))
        assertEquals(null, s.helperCost(Location.ROADSIDE))
    }

    @Test
    fun boostDoublesIncome_andStacksUpToEightHours() {
        val s = TycoonState.newGame()
        s.addBoost(0)
        assertEquals(2.0, s.incomePerSecond(0), 1e-9)
        assertEquals(1.0, s.incomePerSecond(4 * hour), 1e-9)
        s.addBoost(0); s.addBoost(0)
        assertEquals(8 * hour, s.boostUntilMs)
    }

    @Test
    fun freeUpgradeHasACooldown() {
        val s = TycoonState.newGame()
        assertTrue(s.freeUpgrade(Location.ROADSIDE, Product.VADA, 0))
        assertFalse(s.freeUpgrade(Location.ROADSIDE, Product.VADA, 1000))
        assertTrue(s.freeUpgrade(Location.ROADSIDE, Product.VADA, Economy.FREE_UPGRADE_COOLDOWN_MS))
        assertEquals(2, s.at(Location.ROADSIDE).level(Product.VADA))
    }

    @Test
    fun saveAndLoadRoundTrip() {
        val s = TycoonState.newGame()
        s.earn(1e8)
        assertTrue(s.open(Location.CHENNAI))
        s.upgrade(Location.CHENNAI, Product.SAMOSA)
        s.hireHelper(Location.ROADSIDE)
        s.addBoost(123)
        s.lastSeenMs = 456
        val t = TycoonState.decode(s.encode())
        assertEquals(s.encode(), t.encode())
        assertTrue(t.at(Location.CHENNAI).unlocked)
    }

    @Test
    fun brokenSaveFallsBackToANewGame() {
        assertEquals(TycoonState.newGame().encode(), TycoonState.decode("garbage").encode())
        assertEquals(TycoonState.newGame().encode(), TycoonState.decode(null).encode())
    }

    @Test
    fun indianMoneyFormat() {
        assertEquals("₹0", Money.format(0.0))
        assertEquals("₹999", Money.format(999.9))
        assertEquals("₹12,345", Money.format(12_345.0))
        assertEquals("₹99,999", Money.format(99_999.0))
        assertEquals("₹1.5 L", Money.format(150_000.0))
        assertEquals("₹25 L", Money.format(2_500_000.0))
        assertEquals("₹3.4 Cr", Money.format(34_000_000.0))
        assertEquals("₹1,23,456 Cr", Money.format(1_234_560_000_000.0))
    }

    /** A simple player who never taps, always buys the cheapest thing, and saves up for the next branch. */
    @Test
    fun branchesOpenAtAReasonablePace() {
        val s = TycoonState.newGame()
        val opened = mutableMapOf<Location, Long>()
        var now = 0L
        val step = 5000L
        while (now < 30 * 24 * hour && opened.size < 3) {
            s.tick(step, now)
            now += step
            while (true) {
                val next = Location.entries.firstOrNull { !s.at(it).unlocked }
                if (next != null && s.open(next)) { opened[next] = now; continue }
                // Cheapest product upgrade or helper anywhere open.
                val options = Location.entries.filter { s.at(it).unlocked }.flatMap { loc ->
                    Product.entries.map { p -> Triple(s.upgradeCost(loc, p), loc, p) } +
                        listOfNotNull(s.helperCost(loc)?.let { Triple(it, loc, null) })
                }
                val (cost, loc, product) = options.minBy { it.first }
                // Save up for the next branch once it's within reach of an hour's income.
                if (next != null && next.unlockCost - s.cash < s.incomePerSecond(now) * 3600 && cost > s.cash * 0.1) break
                if (cost > s.cash) break
                if (product == null) s.hireHelper(loc) else s.upgrade(loc, product)
            }
        }
        val hours = opened.mapValues { it.value / hour.toDouble() }
        println("Branches opened after (hours of earning): $hours")
        assertEquals("All branches should open within 30 days", 3, opened.size)
        // Chennai: a first session. Mumbai: within a day. Delhi: a multi-day goal.
        assertTrue("Chennai too early/late: ${hours[Location.CHENNAI]}", hours.getValue(Location.CHENNAI) in 0.15..1.0)
        assertTrue("Mumbai too early/late: ${hours[Location.MUMBAI]}", hours.getValue(Location.MUMBAI) in 1.0..12.0)
        assertTrue("Delhi too early/late: ${hours[Location.DELHI]}", hours.getValue(Location.DELHI) in 24.0..14 * 24.0)
    }
}
