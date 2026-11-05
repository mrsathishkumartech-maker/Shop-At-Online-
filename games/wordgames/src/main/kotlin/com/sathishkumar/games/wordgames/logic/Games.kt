package com.sathishkumar.games.wordgames.logic

import kotlin.random.Random

enum class WordGame(val title: String, val emoji: String, val blurb: String) {
    ANAGRAM("Anagram Rush", "🔀", "Unscramble as many words as you can in 60 s"),
    BUILDER("Word Builder", "🧱", "Make words from the letters"),
    LADDER("Word Ladder", "🪜", "Change one letter at a time"),
    SEARCH("Word Search", "🔍", "Find the hidden words"),
}

/** Stars for a solved level: 3 with no hints, 2 with one, 1 with more. Ladder also needs par. */
object Stars {
    fun fromHints(hints: Int) = when (hints) { 0 -> 3; 1 -> 2; else -> 1 }
}

// --- Anagram Rush -----------------------------------------------------------------------

/** Unscramble common words against the clock. Words get longer as you solve them. */
class AnagramRush(private val bank: WordBank, private val random: Random = Random.Default) {
    var score = 0
        private set
    var solved = 0
        private set
    var answer: String = ""
        private set
    /** The letters shown, scrambled. */
    var letters: List<Char> = emptyList()
        private set
    /** How many leading letters a hint has revealed. */
    var revealed = 0
        private set

    init { next() }

    val wordLength: Int get() = (4 + solved / 3).coerceAtMost(8)

    /** True if [guess] uses all the letters and is a real word (any valid anagram counts). */
    fun submit(guess: String): Boolean {
        val g = guess.lowercase()
        if (g.length != answer.length || !g.toList().sorted().equals(answer.toList().sorted()) || !bank.isWord(g)) return false
        score += g.length * 10 - revealed * 5
        solved++
        next()
        return true
    }

    fun skip() = next()

    /** Reveals one more letter of the answer. False once all but the last are shown. */
    fun hint(): Boolean {
        if (revealed >= answer.length - 1) return false
        revealed++
        return true
    }

    private fun next() {
        // Fall back to any 4+ letter word if there are none of the length wanted.
        val pool = bank.common(wordLength).ifEmpty { bank.common.filter { it.length >= 4 } }
        answer = pool.random(random)
        var mix: List<Char>
        do { mix = answer.toList().shuffled(random) } while (String(mix.toCharArray()) == answer && answer.toSet().size > 1)
        letters = mix
        revealed = 0
    }

    companion object { const val SECONDS = 60 }
}

// --- Word Builder ----------------------------------------------------------------------

/** Find words made from a seed word's letters. Find [goal] of them to clear the level. */
class WordBuilder(val level: Int, val seed: String, val targets: Set<String>) {
    val letters: List<Char> = seed.toList()
    val found = linkedSetOf<String>()
    var hints = 0
        private set

    /** About 40% of the words, between 5 and 20. */
    val goal: Int = (targets.size * 2 / 5).coerceIn(minOf(5, targets.size), 20)
    val isSolved: Boolean get() = found.size >= goal

    enum class Result { FOUND, ALREADY, NOT_A_WORD, TOO_SHORT, BAD_LETTERS }

    fun submit(guess: String): Result {
        val g = guess.lowercase()
        return when {
            g.length < 3 -> Result.TOO_SHORT
            !canMake(g, letterCounts(seed)) -> Result.BAD_LETTERS
            g in found -> Result.ALREADY
            g !in targets -> Result.NOT_A_WORD
            else -> { found += g; Result.FOUND }
        }
    }

    /** Gives away the shortest word not yet found. */
    fun hint(): String? {
        val w = targets.filter { it !in found }.minWithOrNull(compareBy({ it.length }, { it })) ?: return null
        found += w
        hints++
        return w
    }

    companion object {
        fun generate(bank: WordBank, level: Int): WordBuilder {
            val random = Random(level * 7919L + 17)
            val length = if (level <= 10) 6 else 7
            val seeds = bank.common(length).shuffled(random)
            for (seed in seeds) {
                val counts = letterCounts(seed)
                val targets = (3..length).flatMap { bank.valid(it) }.filter { canMake(it, counts) }.toSet()
                if (targets.size in 10..60) return WordBuilder(level, seed, targets)
            }
            error("No seed for level $level")
        }
    }
}

// --- Word Ladder -----------------------------------------------------------------------

/** Turn [start] into [target] one letter at a time; every step must be a real word. */
class WordLadder(val level: Int, private val bank: WordBank, val start: String, val target: String, val par: Int) {
    val chain = mutableListOf(start)
    var hints = 0
        private set
    val current: String get() = chain.last()
    val steps: Int get() = chain.size - 1
    val isSolved: Boolean get() = current == target

    enum class Result { OK, NOT_ONE_CHANGE, NOT_A_WORD, ALREADY_USED }

    fun submit(guess: String): Result {
        val g = guess.lowercase()
        return when {
            g.length != current.length || diff(g, current) != 1 -> Result.NOT_ONE_CHANGE
            !bank.isWord(g) -> Result.NOT_A_WORD
            g in chain -> Result.ALREADY_USED
            else -> { chain += g; Result.OK }
        }
    }

    fun undo(): Boolean = if (chain.size > 1) { chain.removeAt(chain.lastIndex); true } else false

    /** Adds the next word on a shortest route from the current word, preferring common words. */
    fun hint(): String? {
        if (isSolved) return null
        val dist = distancesTo(bank, target)
        val here = dist[current] ?: return null
        val next = neighbours(bank, current).filter { dist[it] == here - 1 && it !in chain }
            .sortedWith(compareBy({ !bank.isCommon(it) }, { it }))
            .firstOrNull() ?: return null
        chain += next
        hints++
        return next
    }

    fun stars(): Int = when {
        hints == 0 && steps <= par -> 3
        hints <= 1 && steps <= par + 2 -> 2
        else -> 1
    }

    companion object {
        fun diff(a: String, b: String) = a.indices.count { a[it] != b[it] }

        /** Valid words one letter away from [w]. */
        fun neighbours(bank: WordBank, w: String): List<String> {
            val out = mutableListOf<String>()
            val chars = w.toCharArray()
            for (i in chars.indices) {
                val orig = chars[i]
                for (c in 'a'..'z') {
                    if (c == orig) continue
                    chars[i] = c
                    val s = String(chars)
                    if (bank.isWord(s)) out += s
                }
                chars[i] = orig
            }
            return out
        }

        /** Breadth-first distances (in steps) from every reachable word to [target]. */
        fun distancesTo(bank: WordBank, target: String): Map<String, Int> {
            val dist = hashMapOf(target to 0)
            val queue = ArrayDeque(listOf(target))
            while (queue.isNotEmpty()) {
                val w = queue.removeFirst()
                for (n in neighbours(bank, w)) if (n !in dist) { dist[n] = dist.getValue(w) + 1; queue += n }
            }
            return dist
        }

        fun generate(bank: WordBank, level: Int): WordLadder {
            val random = Random(level * 104729L + 5)
            val length = when { level <= 5 -> 3; level <= 15 -> 4; else -> 5 }
            val wanted = (3 + level / 5).coerceAtMost(6)
            for (start in bank.common(length).shuffled(random)) {
                val dist = distancesTo(bank, start)
                // A common target exactly `wanted` steps away by the shortest route.
                val target = bank.common(length).filter { dist[it] == wanted }.randomOrNull(random) ?: continue
                return WordLadder(level, bank, start, target, wanted)
            }
            error("No ladder for level $level")
        }
    }
}

// --- Word Search -----------------------------------------------------------------------

data class Placement(val word: String, val row: Int, val col: Int, val dr: Int, val dc: Int) {
    fun cells(size: Int): List<Int> = word.indices.map { (row + dr * it) * size + col + dc * it }
}

/** Words hidden in a letter grid, in more directions as levels rise. Swipe to select. */
class WordSearch(val level: Int, val size: Int, val grid: CharArray, val placements: List<Placement>) {
    val found = linkedSetOf<String>()
    var hints = 0
        private set
    val words: List<String> get() = placements.map { it.word }
    val isSolved: Boolean get() = found.size == placements.size

    /** Checks a straight selection from cell [a] to cell [b]; returns the word if it's one of the hidden ones. */
    fun select(a: Int, b: Int): String? {
        val (r0, c0) = a / size to a % size
        val (r1, c1) = b / size to b % size
        val dr = Integer.signum(r1 - r0)
        val dc = Integer.signum(c1 - c0)
        val len = maxOf(kotlin.math.abs(r1 - r0), kotlin.math.abs(c1 - c0)) + 1
        if (!(r0 == r1 || c0 == c1 || kotlin.math.abs(r1 - r0) == kotlin.math.abs(c1 - c0))) return null
        val cells = (0 until len).map { (r0 + dr * it) * size + c0 + dc * it }
        val text = String(cells.map { grid[it] }.toCharArray())
        val hit = placements.firstOrNull { p ->
            p.word !in found && (p.cells(size) == cells || p.cells(size) == cells.reversed())
        } ?: placements.firstOrNull { p -> p.word !in found && (p.word == text || p.word == text.reversed()) }
        return hit?.word?.also { found += it }
    }

    /** First cell of a word not yet found. */
    fun hint(): Int? {
        val p = placements.firstOrNull { it.word !in found } ?: return null
        hints++
        return p.row * size + p.col
    }

    fun cellsOf(word: String): List<Int> = placements.first { it.word == word }.cells(size)

    companion object {
        private val EASY = listOf(0 to 1, 1 to 0)
        private val MEDIUM = EASY + listOf(1 to 1)
        private val ALL = listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1)

        fun generate(bank: WordBank, level: Int): WordSearch {
            val random = Random(level * 15485863L + 9)
            val size = when { level <= 5 -> 8; level <= 12 -> 9; else -> 10 }
            val count = (5 + level / 3).coerceAtMost(10)
            val dirs = when { level <= 3 -> EASY; level <= 7 -> MEDIUM; else -> ALL }
            while (true) {
                val grid = CharArray(size * size) { ' ' }
                val placed = mutableListOf<Placement>()
                val candidates = (4..minOf(7, size)).flatMap { bank.common(it) }.shuffled(random)
                for (w in candidates) {
                    if (placed.size == count) break
                    if (placed.any { it.word.contains(w) || w.contains(it.word) }) continue
                    for (attempt in 0 until 40) {
                        val (dr, dc) = dirs.random(random)
                        val r = random.nextInt(size)
                        val c = random.nextInt(size)
                        val p = Placement(w, r, c, dr, dc)
                        val endR = r + dr * (w.length - 1)
                        val endC = c + dc * (w.length - 1)
                        if (endR !in 0 until size || endC !in 0 until size) continue
                        val cells = p.cells(size)
                        if (cells.withIndex().all { (i, cell) -> grid[cell] == ' ' || grid[cell] == w[i] }) {
                            cells.forEachIndexed { i, cell -> grid[cell] = w[i] }
                            placed += p
                            break
                        }
                    }
                }
                if (placed.size < count) continue
                for (i in grid.indices) if (grid[i] == ' ') grid[i] = 'a' + random.nextInt(26)
                return WordSearch(level, size, grid, placed)
            }
        }
    }
}
