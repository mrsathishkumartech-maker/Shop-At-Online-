package com.sathishkumar.games.superover.logic

import kotlin.random.Random

/** A tournament: one target per match, and the kind of bowling you'll face. */
enum class Tournament(
    val title: String,
    val targets: List<Int>,
    val speeds: IntRange,
    val ballTypes: List<BallType>,
    val winnerCoins: Int,
) {
    GULLY_CUP("Gully Cup", listOf(8, 10, 12), 95..115,
        listOf(BallType.STRAIGHT, BallType.STRAIGHT, BallType.OUTSWING, BallType.SLOWER), 100),
    CITY_LEAGUE("City League", listOf(11, 13, 14, 16), 110..130,
        listOf(BallType.STRAIGHT, BallType.OUTSWING, BallType.SLOWER, BallType.YORKER), 200),
    NATIONAL_TROPHY("National Trophy", listOf(14, 16, 17, 19), 120..140,
        listOf(BallType.STRAIGHT, BallType.OUTSWING, BallType.SLOWER, BallType.YORKER, BallType.BOUNCER), 400),
    WORLD_CUP("World Super Over Cup", listOf(17, 18, 20, 21, 23), 130..152,
        listOf(BallType.STRAIGHT, BallType.OUTSWING, BallType.SLOWER, BallType.YORKER, BallType.YORKER, BallType.BOUNCER), 800);

    val next: Tournament? get() = entries.getOrNull(ordinal + 1)
}

/** The player's progress through the tournaments, plus coins and bats. */
class Career(
    var tournament: Tournament = Tournament.GULLY_CUP,
    var matchIndex: Int = 0,
    /** World Cups won; each repeat of the World Cup adds this many runs to every target. */
    var titles: Int = 0,
    var coins: Int = 0,
    var matchesPlayed: Int = 0,
    val ownedBats: MutableSet<Bat> = mutableSetOf(Bat.GULLY_WILLOW),
    var equipped: Bat = Bat.GULLY_WILLOW,
) {
    val target: Int get() = tournament.targets[matchIndex] + if (tournament == Tournament.WORLD_CUP) titles * 2 else 0

    /** The six balls for the current match. Same match, same bowling, every time. */
    fun deliveries(): List<Delivery> {
        val random = Random(tournament.ordinal * 1000L + matchIndex * 31L + titles * 7L + 5)
        return List(6) {
            val type = tournament.ballTypes.random(random)
            val base = random.nextInt(tournament.speeds.first, tournament.speeds.last + 1)
            val speed = when (type) {
                BallType.SLOWER -> (base * 0.8).toInt()
                BallType.BOUNCER, BallType.YORKER -> base + 5
                else -> base
            }
            Delivery(type, speed)
        }
    }

    fun newMatch(random: Random = Random.Default): SuperOver = SuperOver(target, deliveries(), equipped, random)

    /** Coins for a finished match: a coin per run, +20 for a win. */
    fun matchCoins(match: SuperOver): Int = match.runs + if (match.status == MatchStatus.WON) 20 else 0

    /**
     * Records a finished match. A win moves to the next match (and the next tournament after
     * the final); a loss means replaying the same match. Returns coins earned, including any
     * tournament prize.
     */
    fun finish(match: SuperOver): Int {
        require(match.status != MatchStatus.IN_PROGRESS) { "Match isn't over" }
        matchesPlayed++
        var earned = matchCoins(match)
        if (match.status == MatchStatus.WON) {
            matchIndex++
            if (matchIndex >= tournament.targets.size) {
                earned += tournament.winnerCoins
                matchIndex = 0
                val next = tournament.next
                if (next != null) tournament = next else titles++
            }
        }
        coins += earned
        return earned
    }

    fun buy(bat: Bat): Boolean {
        if (bat in ownedBats || coins < bat.price) return false
        coins -= bat.price
        ownedBats += bat
        equipped = bat
        return true
    }

    fun equip(bat: Bat): Boolean {
        if (bat !in ownedBats) return false
        equipped = bat
        return true
    }

    fun encode(): String = listOf(
        tournament.name, matchIndex, titles, coins, matchesPlayed,
        ownedBats.joinToString("|") { it.name }, equipped.name,
    ).joinToString(";")

    companion object {
        fun decode(text: String?): Career {
            val p = text?.split(";") ?: return Career()
            if (p.size < 7) return Career()
            val tournament = Tournament.entries.firstOrNull { it.name == p[0] } ?: return Career()
            val bats = p[5].split("|").mapNotNull { n -> Bat.entries.firstOrNull { it.name == n } }.toMutableSet()
            bats += Bat.GULLY_WILLOW
            val equipped = Bat.entries.firstOrNull { it.name == p[6] }?.takeIf { it in bats } ?: Bat.GULLY_WILLOW
            return Career(
                tournament = tournament,
                matchIndex = (p[1].toIntOrNull() ?: 0).coerceIn(0, tournament.targets.lastIndex),
                titles = (p[2].toIntOrNull() ?: 0).coerceAtLeast(0),
                coins = (p[3].toIntOrNull() ?: 0).coerceAtLeast(0),
                matchesPlayed = (p[4].toIntOrNull() ?: 0).coerceAtLeast(0),
                ownedBats = bats,
                equipped = equipped,
            )
        }
    }
}
