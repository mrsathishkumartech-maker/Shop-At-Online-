package com.sathishkumar.games.kingdomguard.logic

import kotlin.math.pow

enum class Phase { BUILD, FIGHT, GAME_OVER }

/** Something the screen may want to animate. Cleared by [Battle.drainEvents]. */
sealed interface BattleEvent {
    data class Shot(val pad: Int, val target: Point, val rank: Rank) : BattleEvent
    data class Kill(val at: Point, val gold: Int) : BattleEvent
    data class Leak(val lives: Int) : BattleEvent
    data class WaveCleared(val wave: Int, val bonus: Int) : BattleEvent
}

/**
 * One run of Kingdom Guard. Between waves ([Phase.BUILD]) the player buys and merges
 * soldiers and upgrades the forge; [startWave] sends the next wave, and [step] moves
 * time forward during the fight.
 */
class Battle(startWave: Int = 1) {
    var wave = startWave
        private set
    var map: TileMap = Maps.forWave(wave)
        private set
    var route: Route = Route(checkNotNull(map.findPath()) { "${map.name} has no route" })
        private set

    var gold = 100
        private set
    var lives = MAX_LIVES
        private set
    var phase = Phase.BUILD
        private set
    var soldiersBought = 0
        private set
    var damageLevel = 0
        private set
    var speedLevel = 0
        private set
    var reviveUsed = false
        private set

    /** Soldier on each pad (null = empty), indexed like [TileMap.pads]. */
    val pads: Array<Soldier?> = arrayOfNulls(map.pads.size)
    val enemies = mutableListOf<Enemy>()

    private var pending = ArrayDeque<Spawn>()
    private var clock = 0f
    private var nextId = 1
    private val events = mutableListOf<BattleEvent>()

    // --- Building -------------------------------------------------------------------

    val soldierCost: Int get() = 15 + 3 * soldiersBought
    val damageCost: Int get() = (50 * 1.6.pow(damageLevel)).toInt()
    val speedCost: Int get() = (60 * 1.6.pow(speedLevel)).toInt()
    val damageMultiplier: Float get() = 1f + 0.15f * damageLevel
    val cooldownMultiplier: Float get() = 0.92f.pow(speedLevel)

    /** Buys a recruit onto the first empty pad. Allowed during fights too. */
    fun buySoldier(): Int? {
        if (phase == Phase.GAME_OVER || gold < soldierCost) return null
        val pad = pads.indexOfFirst { it == null }.takeIf { it >= 0 } ?: return null
        gold -= soldierCost
        soldiersBought++
        pads[pad] = Soldier(nextId++, Rank.RECRUIT)
        return pad
    }

    /**
     * Drags the soldier on [from] onto [to]: merges two of the same rank into the next rank,
     * or moves it if [to] is empty. Returns true if anything changed.
     */
    fun drag(from: Int, to: Int): Boolean {
        if (phase == Phase.GAME_OVER || from == to) return false
        val a = pads.getOrNull(from) ?: return false
        if (to !in pads.indices) return false
        val b = pads[to]
        if (b == null) {
            pads[to] = a
            pads[from] = null
            return true
        }
        val next = a.rank.next ?: return false
        if (a.rank != b.rank) return false
        b.rank = next
        b.cooldownLeft = 0f
        pads[from] = null
        return true
    }

    fun upgradeDamage(): Boolean {
        if (phase == Phase.GAME_OVER || gold < damageCost) return false
        gold -= damageCost
        damageLevel++
        return true
    }

    fun upgradeSpeed(): Boolean {
        if (phase == Phase.GAME_OVER || gold < speedCost) return false
        gold -= speedCost
        speedLevel++
        return true
    }

    // --- Fighting -------------------------------------------------------------------

    fun startWave() {
        if (phase != Phase.BUILD) return
        pending = ArrayDeque(Waves.plan(wave))
        clock = 0f
        phase = Phase.FIGHT
    }

    /** Advances the fight by [dt] seconds. */
    fun step(dt: Float) {
        if (phase != Phase.FIGHT || dt <= 0f) return
        clock += dt
        val scale = Waves.hpScale(wave)
        while (pending.isNotEmpty() && pending.first().atSecond <= clock) {
            val s = pending.removeFirst()
            enemies += Enemy(nextId++, s.kind, s.kind.baseHp * scale)
        }

        // Enemies walk; any that reach the castle cost lives.
        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.distance += e.kind.speed * dt
            if (e.distance >= route.length) {
                it.remove()
                lives -= e.kind.livesCost
                events += BattleEvent.Leak(e.kind.livesCost)
            }
        }
        if (lives <= 0) {
            lives = 0
            phase = Phase.GAME_OVER
            return
        }

        // Soldiers attack the enemy in range that is furthest along the route.
        for ((pad, soldier) in pads.withIndex()) {
            soldier ?: continue
            soldier.cooldownLeft -= dt
            if (soldier.cooldownLeft > 0f) continue
            val target = targetFor(pad, soldier.rank) ?: continue
            val at = route.pointAt(target.distance)
            target.hp -= soldier.rank.damage * damageMultiplier
            soldier.cooldownLeft = soldier.rank.cooldown * cooldownMultiplier
            events += BattleEvent.Shot(pad, at, soldier.rank)
            if (!target.alive) {
                val reward = target.kind.gold
                gold += reward
                events += BattleEvent.Kill(at, reward)
            }
        }
        enemies.removeAll { !it.alive }

        if (pending.isEmpty() && enemies.isEmpty()) endWave()
    }

    /** The enemy furthest along the route within range of [pad], if any. */
    fun targetFor(pad: Int, rank: Rank): Enemy? {
        val tile = map.pads[pad]
        val here = Point(tile.x + 0.5f, tile.y + 0.5f)
        return enemies
            .filter { it.alive && route.pointAt(it.distance).distanceTo(here) <= rank.range }
            .maxByOrNull { it.distance }
    }

    private fun endWave() {
        val bonus = 20 + 5 * wave
        gold += bonus
        events += BattleEvent.WaveCleared(wave, bonus)
        wave++
        phase = Phase.BUILD
        val nextMap = Maps.forWave(wave)
        if (nextMap != map) {
            // New battlefield: soldiers march over and take the pads in the same order.
            map = nextMap
            route = Route(checkNotNull(map.findPath()))
        }
    }

    /** Rewarded second chance after the castle falls: half the lives back, same wave. */
    fun revive(): Boolean {
        if (phase != Phase.GAME_OVER || reviveUsed) return false
        reviveUsed = true
        lives = MAX_LIVES / 2
        phase = Phase.FIGHT
        return true
    }

    fun drainEvents(): List<BattleEvent> = events.toList().also { events.clear() }

    companion object {
        const val MAX_LIVES = 20
    }
}
