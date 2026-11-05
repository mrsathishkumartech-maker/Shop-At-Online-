package com.sathishkumar.games.wordgames.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class WordTest {
    companion object {
        private fun asset(name: String): String =
            listOf("src/main/assets/$name", "games/wordgames/src/main/assets/$name",
                System.getProperty("wordAssets", "") + "/$name")
                .map(::File).first { it.isFile }.readText()

        val bank: WordBank by lazy { WordBank.parse(asset("words.txt"), asset("common.txt")) }
    }

    @Test
    fun dictionaryLooksRight() {
        assertTrue(bank.valid.size > 20_000)
        assertTrue(bank.common.size > 5_000)
        assertTrue(bank.isWord("pencil") && bank.isWord("cat"))
        assertFalse(bank.isWord("xqzt"))
        assertTrue(bank.common.all { it in bank.valid })
        assertFalse("blocked words never become answers", "shit" in bank.common)
    }

    @Test
    fun anagramAcceptsAnyRealArrangement() {
        val g = AnagramRush(bank, Random(1))
        val a = g.answer
        assertTrue(g.letters.sorted() == a.toList().sorted())
        assertFalse(g.submit("zzzz"))
        assertTrue(g.submit(a))
        assertEquals(a.length * 10, g.score)
        // Any real word using all the letters counts, e.g. "listen" for "silent".
        val h = AnagramRush(WordBank(listOf("silent", "listen"), listOf("silent")), Random(2))
        assertTrue(h.submit("listen"))
    }

    @Test
    fun anagramWordsGetLonger() {
        val g = AnagramRush(bank, Random(3))
        repeat(9) { g.submit(g.answer) }
        assertEquals(7, g.wordLength)
        assertEquals(7, g.answer.length)
    }

    @Test
    fun builderTargetsAreRealAndMadeFromTheSeed() {
        for (level in listOf(1, 10, 11, 30)) {
            val b = WordBuilder.generate(bank, level)
            val counts = letterCounts(b.seed)
            assertTrue(b.targets.all { bank.isWord(it) && canMake(it, counts) && it.length >= 3 })
            assertTrue(b.seed in b.targets)
            assertTrue(b.goal in 5..20)
            println("BUILDER level $level: ${b.seed} -> ${b.targets.size} words, goal ${b.goal}")
        }
    }

    @Test
    fun builderRulesAndHints() {
        val b = WordBuilder.generate(bank, 2)
        assertEquals(WordBuilder.Result.TOO_SHORT, b.submit("ab"))
        assertEquals(WordBuilder.Result.FOUND, b.submit(b.seed))
        assertEquals(WordBuilder.Result.ALREADY, b.submit(b.seed))
        assertEquals(WordBuilder.Result.BAD_LETTERS, b.submit("zzz"))
        while (!b.isSolved) assertNotNull(b.hint())
        assertTrue(b.hints > 0)
    }

    @Test
    fun ladderParIsTheShortestRoute_andHintsFollowIt() {
        for (level in listOf(1, 6, 16, 25)) {
            val l = WordLadder.generate(bank, level)
            assertEquals(l.start.length, l.target.length)
            assertEquals(l.par, WordLadder.distancesTo(bank, l.target)[l.start])
            while (!l.isSolved) assertNotNull(l.hint())
            assertEquals(l.par, l.steps)
            println("LADDER level $level: ${l.chain.joinToString(" → ")}")
        }
    }

    @Test
    fun ladderRules() {
        val l = WordLadder(1, bank, "cat", "dog", 3)
        assertEquals(WordLadder.Result.NOT_ONE_CHANGE, l.submit("dog"))
        assertEquals(WordLadder.Result.NOT_A_WORD, l.submit("cqt"))
        assertEquals(WordLadder.Result.OK, l.submit("cot"))
        assertEquals(WordLadder.Result.ALREADY_USED, l.submit("cat"))
        assertEquals(WordLadder.Result.OK, l.submit("dot"))
        assertEquals(WordLadder.Result.OK, l.submit("dog"))
        assertTrue(l.isSolved)
        assertEquals(3, l.stars())
    }

    @Test
    fun wordSearchHidesEveryWordInAStraightLine() {
        for (level in listOf(1, 5, 9, 20)) {
            val s = WordSearch.generate(bank, level)
            for (p in s.placements) {
                val text = String(p.cells(s.size).map { s.grid[it] }.toCharArray())
                assertEquals(p.word, text)
            }
            // Select every word end to end; half of them backwards.
            s.placements.forEachIndexed { i, p ->
                val cells = p.cells(s.size)
                val word = if (i % 2 == 0) s.select(cells.first(), cells.last()) else s.select(cells.last(), cells.first())
                assertEquals(p.word, word)
            }
            assertTrue(s.isSolved)
            assertTrue(s.grid.all { it in 'a'..'z' })
        }
    }

    @Test
    fun levelsAreDeterministic() {
        assertEquals(WordBuilder.generate(bank, 4).seed, WordBuilder.generate(bank, 4).seed)
        assertEquals(WordLadder.generate(bank, 4).target, WordLadder.generate(bank, 4).target)
        assertEquals(WordSearch.generate(bank, 4).grid.toList(), WordSearch.generate(bank, 4).grid.toList())
    }
}
