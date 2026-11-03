package com.sathishkumar.games.superover.logic

import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.random.Random

enum class BallType(
    val title: String,
    /** True if missing this ball means you're bowled. */
    val onStumps: Boolean,
    /** Multiplies the timing windows: below 1 is harder to time. */
    val windowScale: Double,
) {
    STRAIGHT("Straight", onStumps = true, windowScale = 1.0),
    OUTSWING("Outswinger", onStumps = false, windowScale = 0.95),
    SLOWER("Slower ball", onStumps = true, windowScale = 0.9),
    YORKER("Yorker", onStumps = true, windowScale = 0.75),
    BOUNCER("Bouncer", onStumps = false, windowScale = 0.85),
}

data class Delivery(val type: BallType, val speedKmph: Int) {
    /** Milliseconds from release until the ball reaches the bat: the ideal moment to tap. */
    val travelMs: Long get() = (900.0 * 130 / speedKmph).roundToLong()
}

/** A bat: [timing] widens every timing window, [power] is the chance a good shot clears the rope. */
enum class Bat(val title: String, val price: Int, val timing: Double, val power: Double) {
    GULLY_WILLOW("Gully willow", 0, 1.0, 0.0),
    TAPE_BALL_KING("Tape-ball king", 150, 1.1, 0.15),
    KASHMIR_WILLOW("Kashmir willow", 400, 1.2, 0.25),
    ENGLISH_WILLOW("English willow", 900, 1.3, 0.35),
    GOLDEN_BAT("Golden bat", 2000, 1.4, 0.5),
}

enum class Timing { PERFECT, GOOD, OK, EDGE, MISS }

enum class Outcome(val runs: Int, val isOut: Boolean, val label: String) {
    SIX(6, false, "SIX!"),
    FOUR(4, false, "FOUR!"),
    TWO(2, false, "2 runs"),
    ONE(1, false, "1 run"),
    DOT(0, false, "Dot ball"),
    BOWLED(0, true, "BOWLED!"),
    CAUGHT(0, true, "CAUGHT!"),
    NO_BALL(1, false, "NO BALL! Free hit next"),
}

data class BallResult(
    val delivery: Delivery,
    /** Tap time minus ideal time in ms (negative = early); null if the player didn't swing. */
    val errorMs: Long?,
    val timing: Timing,
    val outcome: Outcome,
    val wasFreeHit: Boolean,
)

object Timings {
    const val PERFECT_MS = 30.0
    const val GOOD_MS = 65.0
    const val OK_MS = 110.0
    const val EDGE_MS = 170.0

    fun judge(errorMs: Long?, delivery: Delivery, bat: Bat): Timing {
        if (errorMs == null) return Timing.MISS
        val scale = delivery.type.windowScale * bat.timing
        val e = abs(errorMs).toDouble()
        return when {
            e <= PERFECT_MS * scale -> Timing.PERFECT
            e <= GOOD_MS * scale -> Timing.GOOD
            e <= OK_MS * scale -> Timing.OK
            e <= EDGE_MS * scale -> Timing.EDGE
            else -> Timing.MISS
        }
    }

    /** Taps later than this after the ideal moment can't connect any more. */
    fun lastChanceMs(delivery: Delivery, bat: Bat): Long =
        (EDGE_MS * delivery.type.windowScale * bat.timing).roundToLong()
}

enum class MatchStatus { IN_PROGRESS, WON, LOST }

/**
 * One super over: chase [target] in [deliveries] (6 balls) with [maxWickets] wickets.
 * [random] decides the close calls (edges, power), so a seeded match replays the same way.
 */
class SuperOver(
    val target: Int,
    val deliveries: List<Delivery>,
    val bat: Bat,
    private val random: Random,
    val maxWickets: Int = 2,
) {
    var runs = 0
        private set
    var wickets = 0
        private set
    var ballIndex = 0
        private set
    val results = mutableListOf<BallResult>()
    var nextIsFreeHit = false
        private set
    var freeHitUsed = false
        private set

    val ballsLeft: Int get() = deliveries.size - ballIndex
    val runsNeeded: Int get() = (target - runs).coerceAtLeast(0)
    val currentDelivery: Delivery? get() = deliveries.getOrNull(ballIndex).takeIf { status == MatchStatus.IN_PROGRESS }

    val status: MatchStatus
        get() = when {
            runs >= target -> MatchStatus.WON
            wickets >= maxWickets || ballIndex >= deliveries.size -> MatchStatus.LOST
            else -> MatchStatus.IN_PROGRESS
        }

    /** Plays the current ball with a tap [errorMs] ms off the ideal moment (null = no swing). */
    fun play(errorMs: Long?): BallResult {
        val delivery = checkNotNull(currentDelivery) { "Match is over" }
        val freeHit = nextIsFreeHit
        nextIsFreeHit = false
        val timing = Timings.judge(errorMs, delivery, bat)
        var outcome = when (timing) {
            Timing.PERFECT -> Outcome.SIX
            Timing.GOOD -> if (random.nextDouble() < bat.power) Outcome.SIX else Outcome.FOUR
            Timing.OK -> if (random.nextBoolean()) Outcome.TWO else Outcome.ONE
            Timing.EDGE -> when {
                random.nextDouble() < 0.4 -> Outcome.CAUGHT
                random.nextBoolean() -> Outcome.ONE
                else -> Outcome.DOT
            }
            Timing.MISS -> if (delivery.type.onStumps) Outcome.BOWLED else Outcome.DOT
        }
        // You can't be out off a free hit.
        if (freeHit && outcome.isOut) outcome = Outcome.DOT
        runs += outcome.runs
        if (outcome.isOut) wickets++
        ballIndex++
        val result = BallResult(delivery, errorMs, timing, outcome, freeHit)
        results += result
        return result
    }

    /** True if the last ball was a dismissal and the once-per-match free hit is still available. */
    val canUseFreeHit: Boolean get() = !freeHitUsed && results.lastOrNull()?.outcome?.isOut == true

    /**
     * Rewarded second chance: the last dismissal becomes a no-ball. The wicket is cancelled,
     * one run is added, the same ball is bowled again, and it's a free hit.
     */
    fun useFreeHit(): Boolean {
        if (!canUseFreeHit) return false
        val last = results.removeAt(results.lastIndex)
        wickets--
        ballIndex--
        runs += Outcome.NO_BALL.runs
        results += last.copy(outcome = Outcome.NO_BALL)
        freeHitUsed = true
        nextIsFreeHit = true
        return true
    }
}
