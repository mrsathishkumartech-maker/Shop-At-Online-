package com.sathishkumar.games.wordgames.logic

/**
 * The dictionary. [valid] is every word a player may enter; [common] (a subset) is what the
 * games choose answers and puzzles from, so players are never asked for an obscure word.
 */
class WordBank(valid: Collection<String>, common: Collection<String>) {
    val valid: Set<String> = valid.toHashSet()
    val common: List<String> = common.filter { it in this.valid }.sorted()
    private val commonByLength = this.common.groupBy { it.length }
    private val validByLength = this.valid.groupBy { it.length }

    fun isWord(w: String) = w.lowercase() in valid
    fun isCommon(w: String) = w in commonSet
    private val commonSet = this.common.toHashSet()

    fun common(length: Int): List<String> = commonByLength[length].orEmpty()
    fun valid(length: Int): List<String> = validByLength[length].orEmpty()

    companion object {
        fun parse(validText: String, commonText: String) = WordBank(lines(validText), lines(commonText))
        private fun lines(t: String) = t.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
    }
}

/** Letter counts, for "can this word be made from these letters?". */
fun letterCounts(w: String): IntArray = IntArray(26).also { c -> w.forEach { c[it - 'a']++ } }

fun canMake(word: String, from: IntArray): Boolean {
    val c = letterCounts(word)
    return (0 until 26).all { c[it] <= from[it] }
}
