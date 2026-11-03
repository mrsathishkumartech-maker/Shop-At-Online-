package com.sathishkumar.games.superover.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.superover.R
import com.sathishkumar.games.superover.logic.Bat
import kotlin.math.roundToInt

/** Buy and equip bats with coins earned from runs. */
class BatsActivity : Activity() {
    private lateinit var store: CareerStore
    private lateinit var list: LinearLayout
    private lateinit var coins: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bats)
        store = CareerStore(this)
        list = findViewById(R.id.bat_list)
        coins = findViewById(R.id.coins_text)
        findViewById<Button>(R.id.back_button).setOnClickListener { finish() }
        render()
    }

    private fun render() {
        val career = store.load()
        coins.text = getString(R.string.coins_n, career.coins)
        list.removeAllViews()
        for (bat in Bat.entries) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_bat, list, false)
            row.findViewById<TextView>(R.id.bat_name).text = bat.title
            row.findViewById<TextView>(R.id.bat_stats).text = getString(
                R.string.bat_stats, ((bat.timing - 1) * 100).roundToInt(), (bat.power * 100).roundToInt(),
            )
            val button = row.findViewById<Button>(R.id.bat_button)
            when {
                career.equipped == bat -> {
                    button.text = getString(R.string.equipped)
                    button.isEnabled = false
                }
                bat in career.ownedBats -> {
                    button.text = getString(R.string.equip)
                    button.setOnClickListener { career.equip(bat); store.save(career); render() }
                }
                else -> {
                    button.text = getString(R.string.buy_for, bat.price)
                    button.isEnabled = career.coins >= bat.price
                    button.setOnClickListener {
                        if (career.buy(bat)) {
                            Haptics.tap(button)
                            store.save(career)
                            render()
                        }
                    }
                }
            }
            list.addView(row)
        }
    }
}
