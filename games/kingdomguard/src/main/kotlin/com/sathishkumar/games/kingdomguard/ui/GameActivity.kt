package com.sathishkumar.games.kingdomguard.ui

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.core.GamePrefs
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdPolicy
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads
import com.sathishkumar.games.kingdomguard.BuildConfig
import com.sathishkumar.games.kingdomguard.R
import com.sathishkumar.games.kingdomguard.logic.Battle
import com.sathishkumar.games.kingdomguard.logic.BattleEvent
import com.sathishkumar.games.kingdomguard.logic.Phase

class GameActivity : Activity() {
    private lateinit var prefs: GamePrefs
    private lateinit var ads: Ads
    // Interstitial after a run ends, from the 3rd run on.
    private val adPolicy = AdPolicy(firstLevelAfter = 2, everyLevels = 1)
    private lateinit var battle: Battle

    private lateinit var view: BattleView
    private lateinit var waveText: TextView
    private lateinit var livesText: TextView
    private lateinit var goldText: TextView
    private lateinit var mapText: TextView
    private lateinit var recruitButton: Button
    private lateinit var damageButton: Button
    private lateinit var speedButton: Button
    private lateinit var waveButton: Button

    private var gameOverShown = false
    private var runCounted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        prefs = GamePrefs(this, GAME_ID)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))

        view = findViewById(R.id.battle_view)
        waveText = findViewById(R.id.wave_text)
        livesText = findViewById(R.id.lives_text)
        goldText = findViewById(R.id.gold_text)
        mapText = findViewById(R.id.map_text)
        recruitButton = findViewById(R.id.recruit_button)
        damageButton = findViewById(R.id.damage_button)
        speedButton = findViewById(R.id.speed_button)
        waveButton = findViewById(R.id.wave_button)

        view.onChanged = ::render
        view.onEvent = ::onBattleEvent
        recruitButton.setOnClickListener {
            if (battle.buySoldier() != null) { Haptics.tap(recruitButton); view.invalidate(); render() }
        }
        damageButton.setOnClickListener { if (battle.upgradeDamage()) { Haptics.tap(damageButton); render() } }
        speedButton.setOnClickListener { if (battle.upgradeSpeed()) { Haptics.tap(speedButton); render() } }
        waveButton.setOnClickListener {
            when (battle.phase) {
                Phase.BUILD -> { battle.startWave(); view.invalidate() }
                Phase.FIGHT -> view.speed = if (view.speed == 1f) 2f else 1f
                Phase.GAME_OVER -> Unit
            }
            render()
        }
        newRun()
    }

    override fun onPause() {
        super.onPause()
        view.paused = true
    }

    override fun onResume() {
        super.onResume()
        view.paused = false
        view.invalidate()
    }

    private fun newRun() {
        battle = Battle()
        gameOverShown = false
        runCounted = false
        view.speed = 1f
        view.bind(battle)
        render()
    }

    private fun onBattleEvent(e: BattleEvent) {
        when (e) {
            is BattleEvent.Leak -> Haptics.error(view)
            is BattleEvent.WaveCleared -> {
                Toast.makeText(this, getString(R.string.wave_cleared, e.wave, e.bonus), Toast.LENGTH_SHORT).show()
                prefs.putInt(KEY_BEST, maxOf(prefs.getInt(KEY_BEST), e.wave))
            }
            else -> Unit
        }
        if (battle.phase == Phase.GAME_OVER && !gameOverShown) {
            gameOverShown = true
            view.post(::showGameOver)
        }
    }

    private fun render() {
        val b = battle
        waveText.text = getString(R.string.wave_n, b.wave)
        livesText.text = getString(R.string.lives_n, b.lives)
        goldText.text = getString(R.string.gold_n, b.gold)
        mapText.text = getString(R.string.map_best, b.map.name, prefs.getInt(KEY_BEST))
        val open = b.phase != Phase.GAME_OVER
        recruitButton.text = getString(R.string.recruit, b.soldierCost)
        recruitButton.isEnabled = open && b.gold >= b.soldierCost && b.pads.any { it == null }
        damageButton.text = getString(R.string.forge_damage, b.damageLevel + 1, b.damageCost)
        damageButton.isEnabled = open && b.gold >= b.damageCost
        speedButton.text = getString(R.string.forge_speed, b.speedLevel + 1, b.speedCost)
        speedButton.isEnabled = open && b.gold >= b.speedCost
        waveButton.text = when (b.phase) {
            Phase.BUILD -> getString(R.string.start_wave, b.wave)
            Phase.FIGHT -> getString(if (view.speed == 1f) R.string.speed_up else R.string.slow_down)
            Phase.GAME_OVER -> getString(R.string.castle_fallen)
        }
        waveButton.isEnabled = open
    }

    private fun showGameOver() {
        if (isFinishing) return
        render()
        // Count each run once, even if the dialog reopens after a failed revive.
        if (!runCounted) {
            runCounted = true
            prefs.putInt(KEY_RUNS, prefs.getInt(KEY_RUNS) + 1)
        }
        val runs = prefs.getInt(KEY_RUNS)
        val builder = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(R.string.game_over_title)
            .setMessage(getString(R.string.game_over_message, battle.wave, prefs.getInt(KEY_BEST)))
            .setCancelable(false)
            .setNegativeButton(R.string.new_run) { _, _ ->
                if (adPolicy.shouldShowInterstitial(runs)) ads.showInterstitial(this) { newRun() } else newRun()
            }
        if (!battle.reviveUsed) {
            builder.setPositiveButton(R.string.revive) { _, _ ->
                ads.showRewarded(this,
                    onReward = {
                        battle.revive()
                        gameOverShown = false
                        view.invalidate()
                        render()
                    },
                    onUnavailable = {
                        Toast.makeText(this, R.string.ad_unavailable, Toast.LENGTH_SHORT).show()
                        showGameOver()
                    })
            }
        }
        builder.show()
    }

    private companion object {
        const val GAME_ID = "kingdomguard"
        const val KEY_BEST = "best_wave"
        const val KEY_RUNS = "runs"
    }
}
