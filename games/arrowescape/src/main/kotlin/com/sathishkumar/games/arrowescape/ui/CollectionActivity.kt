package com.sathishkumar.games.arrowescape.ui

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.TextView
import com.sathishkumar.games.arrowescape.R
import com.sathishkumar.games.arrowescape.logic.LevelGenerator
import com.sathishkumar.games.core.GamePrefs

/** The rangoli book: one rangoli for every level the player has cleared. */
class CollectionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection)
        val prefs = GamePrefs(this, "arrowescape")
        val levels = prefs.clearedLevels()
        val generator = LevelGenerator()

        findViewById<TextView>(R.id.collection_count).text =
            resources.getQuantityString(R.plurals.rangolis_collected, levels.size, levels.size)
        findViewById<View>(R.id.empty_text).visibility = if (levels.isEmpty()) View.VISIBLE else View.GONE
        findViewById<View>(R.id.back_button).setOnClickListener { finish() }

        findViewById<GridView>(R.id.grid).adapter = object : BaseAdapter() {
            override fun getCount() = levels.size
            override fun getItem(position: Int) = levels[position]
            override fun getItemId(position: Int) = levels[position].toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView
                    ?: LayoutInflater.from(parent.context).inflate(R.layout.item_rangoli, parent, false)
                val level = levels[position]
                val stars = prefs.stars(level)
                // Seed only depends on the level number, so this is the same rangoli the player revealed.
                view.findViewById<RangoliView>(R.id.rangoli).setSeed(generator.rangoliSeedFor(level))
                view.findViewById<TextView>(R.id.label).text =
                    getString(R.string.level_n, level) + "  " + "★".repeat(stars)
                return view
            }
        }
    }
}
