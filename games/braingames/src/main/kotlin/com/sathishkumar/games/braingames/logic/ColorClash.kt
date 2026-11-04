package com.sathishkumar.games.braingames.logic

import kotlin.random.Random

enum class InkColor(val word: String) { RED("RED"), BLUE("BLUE"), GREEN("GREEN"), YELLOW("YELLOW") }

data class ClashCard(val word: InkColor, val ink: InkColor)

/**
 * Color Clash: a colour word is printed in a different ink. Tap the INK colour, not the word.
 * Most cards clash (word ≠ ink) so reading the word gets you wrong. Streaks score extra.
 */
class ColorClash(private val random: Random = Random.Default) {
    var score = 0
        private set
    var correct = 0
        private set
    var wrong = 0
        private set
    var streak = 0
        private set
    var current: ClashCard = next()
        private set

    fun answer(color: InkColor): Boolean {
        val right = color == current.ink
        if (right) {
            correct++
            streak++
            score += 10 + minOf(streak, 10) * 2
        } else {
            wrong++
            streak = 0
        }
        current = next()
        return right
    }

    private fun next(): ClashCard {
        val word = InkColor.entries.random(random)
        val ink = if (random.nextDouble() < CLASH_RATE) InkColor.entries.filter { it != word }.random(random) else word
        return ClashCard(word, ink)
    }

    companion object {
        const val CLASH_RATE = 0.75
        const val SECONDS = 45
    }
}
