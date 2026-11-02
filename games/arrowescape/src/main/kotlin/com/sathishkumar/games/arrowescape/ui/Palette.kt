package com.sathishkumar.games.arrowescape.ui

import android.graphics.Color
import com.sathishkumar.games.arrowescape.logic.GateColor
import com.sathishkumar.games.arrowescape.logic.RangoliDesigner

object Palette {
    val BACKGROUND = Color.parseColor("#1B1530")
    val PANEL = Color.parseColor("#251E40")
    val CELL_DOT = Color.parseColor("#3B3260")
    val PLAIN_ARROW = Color.parseColor("#4B5D8C")
    val GLYPH = Color.WHITE
    val ERROR = Color.parseColor("#FF5A5F")
    val HINT = Color.parseColor("#FFE082")

    fun gate(color: GateColor): Int = when (color) {
        GateColor.RED -> Color.parseColor("#E5484D")
        GateColor.BLUE -> Color.parseColor("#3E8EF7")
        GateColor.GREEN -> Color.parseColor("#30A46C")
        GateColor.YELLOW -> Color.parseColor("#F5B700")
    }

    /** Festive rangoli palettes: each is [RangoliDesigner.COLORS_PER_PALETTE] colors. */
    private val RANGOLI = listOf(
        listOf("#E63946", "#F4A261", "#FFD166", "#2A9D8F", "#F1FAEE"),
        listOf("#7B2CBF", "#E0AAFF", "#FF6D00", "#FFB703", "#FFFFFF"),
        listOf("#D00000", "#FFBA08", "#3F88C5", "#136F63", "#FDF0D5"),
        listOf("#FF006E", "#FB5607", "#FFBE0B", "#8338EC", "#FFFFFF"),
        listOf("#006D77", "#83C5BE", "#FFDDD2", "#E29578", "#EDF6F9"),
        listOf("#9B2226", "#EE9B00", "#94D2BD", "#0A9396", "#E9D8A6"),
    ).map { colors -> colors.map(Color::parseColor) }

    init {
        check(RANGOLI.size == RangoliDesigner.PALETTE_COUNT)
        check(RANGOLI.all { it.size == RangoliDesigner.COLORS_PER_PALETTE })
    }

    fun rangoli(palette: Int, color: Int): Int = RANGOLI[palette][color]
}
