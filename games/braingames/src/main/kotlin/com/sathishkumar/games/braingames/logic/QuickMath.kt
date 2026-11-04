package com.sathishkumar.games.braingames.logic

import kotlin.random.Random

data class MathProblem(val text: String, val answer: Int, val options: List<Int>)

/**
 * Quick Math: answer as many sums as you can. Each right answer raises the level (bigger
 * numbers, then ×, then ÷); a wrong answer drops it a step. Higher levels score more.
 */
class QuickMath(private val random: Random = Random.Default) {
    var level = 1
        private set
    var score = 0
        private set
    var correct = 0
        private set
    var wrong = 0
        private set
    var current: MathProblem = next()
        private set

    /** Answers the current problem with [choice]; returns true if right. Moves to a new problem. */
    fun answer(choice: Int): Boolean {
        val right = choice == current.answer
        if (right) {
            correct++
            score += 10 + level * 2
            level = (level + 1).coerceAtMost(MAX_LEVEL)
        } else {
            wrong++
            level = (level - 1).coerceAtLeast(1)
        }
        current = next()
        return right
    }

    private fun next(): MathProblem {
        val max = 5 + level * 4
        val ops = buildList {
            add('+'); add('−')
            if (level >= 4) add('×')
            if (level >= 7) add('÷')
        }
        val (text, answer) = when (ops.random(random)) {
            '+' -> { val a = random.nextInt(1, max); val b = random.nextInt(1, max); "$a + $b" to a + b }
            '−' -> { val a = random.nextInt(2, max + 2); val b = random.nextInt(1, a); "$a − $b" to a - b }
            '×' -> { val m = 3 + level / 2; val a = random.nextInt(2, m + 1); val b = random.nextInt(2, m + 1); "$a × $b" to a * b }
            else -> { val m = 2 + level / 2; val b = random.nextInt(2, m + 1); val q = random.nextInt(2, m + 2); "${b * q} ÷ $b" to q }
        }
        return MathProblem(text, answer, options(answer))
    }

    /** The answer plus three close, distinct, non-negative wrong answers, shuffled. */
    private fun options(answer: Int): List<Int> {
        val set = linkedSetOf(answer)
        val spread = maxOf(3, answer / 5)
        while (set.size < 4) {
            val d = random.nextInt(-spread, spread + 1)
            val v = answer + d
            if (d != 0 && v >= 0) set += v
        }
        return set.shuffled(random)
    }

    companion object {
        const val MAX_LEVEL = 15
        const val SECONDS = 60
    }
}
