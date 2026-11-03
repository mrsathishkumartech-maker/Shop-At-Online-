package com.sathishkumar.games.kingdomguard.logic

/** Soldier ranks. Merging two of one rank makes one of the next. */
enum class Rank(val title: String, val damage: Float, val range: Float, val cooldown: Float) {
    RECRUIT("Recruit", 8f, 2.2f, 0.8f),
    MILITIA("Militia", 18f, 2.3f, 0.8f),
    SPEARMAN("Spearman", 40f, 2.4f, 0.75f),
    ARCHER("Archer", 80f, 3.2f, 0.7f),
    KNIGHT("Knight", 170f, 2.6f, 0.65f),
    CAPTAIN("Captain", 360f, 2.9f, 0.6f),
    PALADIN("Paladin", 760f, 3.1f, 0.55f),
    CHAMPION("Champion", 1600f, 3.4f, 0.5f);

    val next: Rank? get() = entries.getOrNull(ordinal + 1)
}

/** Enemy kinds: [speed] in tiles per second, [livesCost] is castle damage if one gets through. */
enum class EnemyKind(val title: String, val baseHp: Float, val speed: Float, val gold: Int, val livesCost: Int) {
    GOBLIN("Goblin", 30f, 2.2f, 3, 1),
    ORC("Orc", 60f, 1.6f, 5, 1),
    TROLL("Troll", 160f, 1.0f, 10, 2),
    WARLORD("Warlord", 1200f, 0.8f, 60, 10),
}

class Soldier(val id: Int, var rank: Rank) {
    /** Seconds until this soldier can attack again. */
    var cooldownLeft = 0f
}

class Enemy(val id: Int, val kind: EnemyKind, val maxHp: Float) {
    var hp = maxHp
    /** Tiles walked along the route. */
    var distance = 0f
    val alive: Boolean get() = hp > 0
}

data class Spawn(val kind: EnemyKind, val atSecond: Float)

object Waves {
    /** Every wave enemies have this much more health than the last. */
    const val HP_GROWTH = 1.13f

    fun hpScale(wave: Int): Float = Math.pow(HP_GROWTH.toDouble(), (wave - 1).toDouble()).toFloat()

    /** Who comes in [wave], and when (seconds after the wave starts). */
    fun plan(wave: Int): List<Spawn> {
        val kinds = mutableListOf<EnemyKind>()
        repeat(4 + wave) { kinds += EnemyKind.GOBLIN }
        repeat(wave / 2) { kinds += EnemyKind.ORC }
        repeat(wave / 4) { kinds += EnemyKind.TROLL }
        // Mix them so tougher enemies arrive among the goblins, the same way every time.
        val mixed = kinds.shuffled(kotlin.random.Random(wave))
        val spawns = mixed.mapIndexed { i, k -> Spawn(k, i * 0.7f) }.toMutableList()
        if (wave % 10 == 0) spawns += Spawn(EnemyKind.WARLORD, mixed.size * 0.7f + 1f)
        return spawns
    }
}
