package com.sathishkumar.games.superover.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToLong
import kotlin.random.Random

class CricketTest {
    private fun match(target: Int, vararg types: BallType, bat: Bat = Bat.GULLY_WILLOW) =
        SuperOver(target, types.map { Delivery(it, 130) }, bat, Random(1))

    @Test
    fun timingWindows() {
        val d = Delivery(BallType.STRAIGHT, 130)
        assertEquals(Timing.PERFECT, Timings.judge(0, d, Bat.GULLY_WILLOW))
        assertEquals(Timing.PERFECT, Timings.judge(-30, d, Bat.GULLY_WILLOW))
        assertEquals(Timing.GOOD, Timings.judge(60, d, Bat.GULLY_WILLOW))
        assertEquals(Timing.OK, Timings.judge(-100, d, Bat.GULLY_WILLOW))
        assertEquals(Timing.EDGE, Timings.judge(160, d, Bat.GULLY_WILLOW))
        assertEquals(Timing.MISS, Timings.judge(250, d, Bat.GULLY_WILLOW))
        assertEquals(Timing.MISS, Timings.judge(null, d, Bat.GULLY_WILLOW))
        // A better bat widens the window; a yorker narrows it.
        assertEquals(Timing.PERFECT, Timings.judge(40, d, Bat.GOLDEN_BAT))
        assertEquals(Timing.GOOD, Timings.judge(25, Delivery(BallType.YORKER, 130), Bat.GULLY_WILLOW))
    }

    @Test
    fun fasterBallsArriveSooner() {
        assertEquals(900, Delivery(BallType.STRAIGHT, 130).travelMs)
        assertTrue(Delivery(BallType.STRAIGHT, 150).travelMs < Delivery(BallType.STRAIGHT, 110).travelMs)
    }

    @Test
    fun perfectTimingHitsSixes_andWinsAssoonAsTargetIsReached() {
        val m = match(12, *Array(6) { BallType.STRAIGHT })
        assertEquals(Outcome.SIX, m.play(0).outcome)
        assertEquals(MatchStatus.IN_PROGRESS, m.status)
        m.play(0)
        assertEquals(MatchStatus.WON, m.status)
        assertEquals(4, m.ballsLeft)
    }

    @Test
    fun missingABallOnTheStumpsIsBowled_offStumpIsADot() {
        val m = match(10, BallType.STRAIGHT, BallType.OUTSWING, BallType.STRAIGHT, BallType.STRAIGHT)
        assertEquals(Outcome.BOWLED, m.play(null).outcome)
        assertEquals(Outcome.DOT, m.play(null).outcome)
        assertEquals(Outcome.BOWLED, m.play(null).outcome)
        assertEquals(MatchStatus.LOST, m.status) // two wickets
    }

    @Test
    fun freeHitTurnsTheWicketIntoANoBall_andTheReplayCantBeOut() {
        val m = match(10, BallType.STRAIGHT, BallType.STRAIGHT, BallType.STRAIGHT)
        m.play(null)
        assertTrue(m.canUseFreeHit)
        assertTrue(m.useFreeHit())
        assertEquals(0, m.wickets)
        assertEquals(1, m.runs)
        assertEquals(0, m.ballIndex) // same ball again
        assertEquals(Outcome.NO_BALL, m.results.single().outcome)
        val replay = m.play(null)
        assertTrue(replay.wasFreeHit)
        assertEquals(Outcome.DOT, replay.outcome)
        m.play(null)
        assertFalse("Only one free hit per match", m.useFreeHit())
    }

    @Test
    fun freeHitCanRescueTheLastWicket() {
        val m = match(10, BallType.STRAIGHT, BallType.STRAIGHT, BallType.STRAIGHT)
        m.play(null); m.play(null)
        assertEquals(MatchStatus.LOST, m.status)
        assertTrue(m.useFreeHit())
        assertEquals(MatchStatus.IN_PROGRESS, m.status)
    }

    @Test
    fun careerProgressesThroughTournaments_andPaysPrizes() {
        val c = Career()
        var guard = 0
        while (c.tournament != Tournament.CITY_LEAGUE && guard++ < 10) {
            val m = c.newMatch(Random(guard))
            while (m.status == MatchStatus.IN_PROGRESS) m.play(0)
            c.finish(m)
        }
        assertEquals(Tournament.CITY_LEAGUE, c.tournament)
        assertEquals(0, c.matchIndex)
        assertEquals(3, c.matchesPlayed)
        // 3 wins: runs + 20 each, plus the Gully Cup prize.
        assertTrue(c.coins >= 8 + 10 + 12 + 60 + Tournament.GULLY_CUP.winnerCoins)
    }

    @Test
    fun losingReplaysTheSameMatch() {
        val c = Career()
        val m = c.newMatch()
        while (m.status == MatchStatus.IN_PROGRESS) m.play(null)
        c.finish(m)
        assertEquals(0, c.matchIndex)
        assertEquals(c.deliveries(), Career().deliveries())
    }

    @Test
    fun batsCostCoins_andSaveLoadRoundTrips() {
        val c = Career(coins = 500)
        assertFalse(c.buy(Bat.ENGLISH_WILLOW))
        assertTrue(c.buy(Bat.KASHMIR_WILLOW))
        assertEquals(100, c.coins)
        assertEquals(Bat.KASHMIR_WILLOW, c.equipped)
        assertTrue(c.equip(Bat.GULLY_WILLOW))
        assertEquals(c.encode(), Career.decode(c.encode()).encode())
        assertEquals(Career().encode(), Career.decode("rubbish").encode())
    }

    /** Win rate for a player whose taps are off by a normal error with [sdMs] spread. */
    private fun winRate(t: Tournament, sdMs: Double, bat: Bat, trials: Int = 400): Double {
        val r = Random(42)
        var wins = 0
        repeat(trials) { i ->
            t.targets.indices.forEach { mi ->
                val c = Career(tournament = t, matchIndex = mi, equipped = bat, ownedBats = mutableSetOf(bat, Bat.GULLY_WILLOW))
                val m = c.newMatch(Random(i * 100L + mi))
                while (m.status == MatchStatus.IN_PROGRESS) {
                    val g = r.nextDouble() + r.nextDouble() + r.nextDouble() - 1.5 // ~N(0, 0.5)
                    m.play((g * 2 * sdMs).roundToLong())
                }
                if (m.status == MatchStatus.WON) wins++
            }
        }
        return wins.toDouble() / (trials * t.targets.size)
    }

    @Test
    fun difficultyRisesThroughTheTournaments() {
        val rows = Tournament.entries.map { t ->
            t to listOf(
                winRate(t, 60.0, Bat.GULLY_WILLOW),
                winRate(t, 60.0, Bat.ENGLISH_WILLOW),
                winRate(t, 100.0, Bat.GULLY_WILLOW),
            )
        }
        rows.forEach { (t, r) -> println("WINRATE ${t.title}: sd60 basic=%.2f sd60 english=%.2f sd100 basic=%.2f".format(r[0], r[1], r[2])) }
        val good = rows.map { it.second[0] }
        // A decent player should usually win the Gully Cup and find each tournament harder.
        assertTrue("Gully Cup too hard: ${good[0]}", good[0] >= 0.6)
        assertTrue("Tournaments should get harder: $good", good.zipWithNext().all { (a, b) -> b <= a + 0.02 })
        // Better bats help.
        rows.forEach { (t, r) -> assertTrue("${t.title}: better bat should help", r[1] >= r[0]) }
    }
}
