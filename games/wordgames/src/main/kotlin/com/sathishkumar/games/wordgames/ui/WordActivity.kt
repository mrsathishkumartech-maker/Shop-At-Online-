package com.sathishkumar.games.wordgames.ui

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdPolicy
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads
import com.sathishkumar.games.wordgames.BuildConfig
import com.sathishkumar.games.wordgames.R
import com.sathishkumar.games.wordgames.logic.AnagramRush
import com.sathishkumar.games.wordgames.logic.Stars
import com.sathishkumar.games.wordgames.logic.WordBank
import com.sathishkumar.games.wordgames.logic.WordBuilder
import com.sathishkumar.games.wordgames.logic.WordGame
import com.sathishkumar.games.wordgames.logic.WordLadder
import com.sathishkumar.games.wordgames.logic.WordSearch

/** Runs any of the four word games. */
class WordActivity : Activity() {
    private lateinit var game: WordGame
    private lateinit var progress: Progress
    private lateinit var ads: Ads
    // An interstitial after every 3rd finished level or round.
    private val adPolicy = AdPolicy(firstLevelAfter = 0, everyLevels = 3)
    private var bank: WordBank? = null

    private var anagram: AnagramRush? = null
    private var builder: WordBuilder? = null
    private var ladder: WordLadder? = null
    private var search: WordSearch? = null

    private lateinit var title: TextView
    private lateinit var status: TextView
    private lateinit var timeBar: ProgressBar
    private lateinit var info: TextView
    private lateinit var entry: TextView
    private lateinit var pad: LetterPadView
    private lateinit var grid: WordSearchView
    private lateinit var backButton: Button
    private lateinit var middleButton: Button
    private lateinit var enterButton: Button
    private lateinit var hintButton: Button

    /** Typed letters, as pad key indexes (tile mode) or plain letters (keyboard mode). */
    private val typed = mutableListOf<Int>()
    private val typedText = StringBuilder()
    private var finished = false

    private val handler = Handler(Looper.getMainLooper())
    private var endsAt = 0L
    private var pausedLeft = -1L
    private val ticker = object : Runnable {
        override fun run() {
            val left = endsAt - SystemClock.elapsedRealtime()
            timeBar.progress = (left * 1000 / (AnagramRush.SECONDS * 1000L)).toInt().coerceAtLeast(0)
            if (left <= 0) endAnagram() else handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_word)
        game = WordGame.valueOf(intent.getStringExtra(EXTRA_GAME) ?: WordGame.ANAGRAM.name)
        progress = Progress(this)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))
        title = findViewById(R.id.title_text)
        status = findViewById(R.id.status_text)
        timeBar = findViewById(R.id.time_bar)
        info = findViewById(R.id.info_text)
        entry = findViewById(R.id.entry_text)
        pad = findViewById(R.id.pad)
        grid = findViewById(R.id.search_grid)
        backButton = findViewById(R.id.back_button)
        middleButton = findViewById(R.id.middle_button)
        enterButton = findViewById(R.id.enter_button)
        hintButton = findViewById(R.id.hint_button)

        val isSearch = game == WordGame.SEARCH
        grid.visibility = if (isSearch) View.VISIBLE else View.GONE
        pad.visibility = if (isSearch) View.GONE else View.VISIBLE
        entry.visibility = if (isSearch) View.GONE else View.VISIBLE
        backButton.visibility = if (isSearch) View.GONE else View.VISIBLE
        enterButton.visibility = if (isSearch || game == WordGame.ANAGRAM) View.GONE else View.VISIBLE
        middleButton.visibility = if (isSearch) View.GONE else View.VISIBLE
        timeBar.visibility = if (game == WordGame.ANAGRAM) View.VISIBLE else View.GONE
        middleButton.text = getString(when (game) {
            WordGame.ANAGRAM -> R.string.skip
            WordGame.LADDER -> R.string.undo
            else -> R.string.shuffle
        })

        pad.onKey = ::onKey
        grid.onSelect = ::onSearchSelect
        backButton.setOnClickListener { backspace() }
        middleButton.setOnClickListener { onMiddle() }
        enterButton.setOnClickListener { submit() }
        hintButton.setOnClickListener { onHint() }

        start()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        if (game == WordGame.ANAGRAM && !finished && endsAt > 0) pausedLeft = endsAt - SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        if (game == WordGame.ANAGRAM && !finished && pausedLeft >= 0) {
            endsAt = SystemClock.elapsedRealtime() + pausedLeft
            pausedLeft = -1
            handler.post(ticker)
        }
    }

    /** Loads the dictionary and builds the round or level off the main thread. */
    private fun start() {
        finished = false
        entry.text = getString(R.string.loading)
        setControlsEnabled(false)
        val level = progress.level(game)
        Thread {
            val b = Words.bank(applicationContext)
            val made: Any = when (game) {
                WordGame.ANAGRAM -> AnagramRush(b)
                WordGame.BUILDER -> WordBuilder.generate(b, level)
                WordGame.LADDER -> WordLadder.generate(b, level)
                WordGame.SEARCH -> WordSearch.generate(b, level)
            }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                bank = b
                anagram = made as? AnagramRush
                builder = made as? WordBuilder
                ladder = made as? WordLadder
                search = made as? WordSearch
                onReady()
            }
        }.start()
    }

    private fun onReady() {
        setControlsEnabled(true)
        clearTyped()
        when (game) {
            WordGame.ANAGRAM -> {
                setTilePad(anagram!!.letters)
                timeBar.max = 1000
                endsAt = SystemClock.elapsedRealtime() + AnagramRush.SECONDS * 1000L
                handler.removeCallbacks(ticker)
                handler.post(ticker)
            }
            WordGame.BUILDER -> setTilePad(builder!!.letters.shuffled())
            WordGame.LADDER -> pad.setKeys("qwertyuiopasdfghjklzxcvbnm".toList(), listOf(10, 9, 7))
            WordGame.SEARCH -> grid.bind(search!!)
        }
        render()
    }

    private fun setTilePad(letters: List<Char>) = pad.setKeys(letters)

    private fun setControlsEnabled(on: Boolean) {
        listOf(backButton, middleButton, enterButton, hintButton).forEach { it.isEnabled = on }
    }

    // --- Typing ------------------------------------------------------------------------

    private val tileMode get() = game == WordGame.ANAGRAM || game == WordGame.BUILDER

    private fun onKey(i: Int) {
        if (finished) return
        val limit = ladder?.current?.length ?: anagram?.answer?.length ?: Int.MAX_VALUE
        if (typedText.length >= limit) return
        typed += i
        typedText.append(pad.key(i))
        if (tileMode) pad.markUsed(i, true)
        Haptics.tap(pad)
        renderEntry()
        if (game == WordGame.ANAGRAM && typedText.length == anagram!!.answer.length) submit()
    }

    private fun backspace() {
        if (typed.isEmpty()) return
        val i = typed.removeAt(typed.lastIndex)
        typedText.deleteCharAt(typedText.lastIndex)
        if (tileMode) pad.markUsed(i, false)
        renderEntry()
    }

    private fun clearTyped() {
        typed.clear()
        typedText.clear()
        if (tileMode) pad.resetUsed()
        renderEntry()
    }

    private fun submit() {
        val word = typedText.toString()
        when (game) {
            WordGame.ANAGRAM -> {
                val a = anagram!!
                if (a.submit(word)) {
                    Haptics.tap(pad)
                    setTilePad(a.letters)
                } else {
                    Haptics.error(pad)
                    toast(getString(R.string.not_it))
                }
                clearTyped()
            }
            WordGame.BUILDER -> {
                val b = builder!!
                val msg = when (b.submit(word)) {
                    WordBuilder.Result.FOUND -> null
                    WordBuilder.Result.ALREADY -> R.string.already_found
                    WordBuilder.Result.NOT_A_WORD -> R.string.not_in_list
                    WordBuilder.Result.TOO_SHORT -> R.string.too_short
                    WordBuilder.Result.BAD_LETTERS -> R.string.not_in_list
                }
                if (msg == null) Haptics.tap(pad) else { Haptics.error(pad); toast(getString(msg)) }
                clearTyped()
                if (b.isSolved) levelDone(Stars.fromHints(b.hints))
            }
            WordGame.LADDER -> {
                val l = ladder!!
                val msg = when (l.submit(word)) {
                    WordLadder.Result.OK -> null
                    WordLadder.Result.NOT_ONE_CHANGE -> R.string.one_letter
                    WordLadder.Result.NOT_A_WORD -> R.string.not_a_word
                    WordLadder.Result.ALREADY_USED -> R.string.already_used
                }
                if (msg == null) Haptics.tap(pad) else { Haptics.error(pad); toast(getString(msg)) }
                clearTyped()
                if (l.isSolved) levelDone(l.stars())
            }
            WordGame.SEARCH -> Unit
        }
        render()
    }

    private fun onMiddle() {
        when (game) {
            WordGame.ANAGRAM -> { anagram!!.skip(); setTilePad(anagram!!.letters); clearTyped() }
            WordGame.BUILDER -> { setTilePad(builder!!.letters.shuffled()); clearTyped() }
            WordGame.LADDER -> { ladder!!.undo(); clearTyped() }
            WordGame.SEARCH -> Unit
        }
        render()
    }

    private fun onSearchSelect(a: Int, b: Int) {
        val s = search ?: return
        if (finished) return
        if (s.select(a, b) != null) {
            Haptics.tap(grid)
            render()
            if (s.isSolved) levelDone(Stars.fromHints(s.hints))
        }
    }

    // --- Hints -------------------------------------------------------------------------

    private fun onHint() {
        if (finished) return
        ads.showRewarded(this,
            onReward = {
                when (game) {
                    WordGame.ANAGRAM -> if (!anagram!!.hint()) toast(getString(R.string.no_hint))
                    WordGame.BUILDER -> builder!!.hint()?.let { toast(getString(R.string.hint_word, it.uppercase())) }
                    WordGame.LADDER -> { clearTyped(); ladder!!.hint() }
                    WordGame.SEARCH -> search!!.hint()?.let { grid.showHint(it) }
                }
                render()
                builder?.let { if (it.isSolved) levelDone(Stars.fromHints(it.hints)) }
                ladder?.let { if (it.isSolved) levelDone(it.stars()) }
            },
            onUnavailable = { toast(getString(R.string.ad_unavailable)) })
    }

    // --- Drawing -------------------------------------------------------------------------

    private fun renderEntry() {
        val a = anagram
        entry.text = when {
            a != null && a.revealed > 0 && typedText.isEmpty() ->
                a.answer.take(a.revealed).uppercase() + "…"
            typedText.isEmpty() -> getString(R.string.tap_letters)
            else -> typedText.toString().uppercase()
        }
        entry.setTextColor(if (typedText.isEmpty()) Color.parseColor("#A9B4D0") else Color.WHITE)
    }

    private fun render() {
        renderEntry()
        when (game) {
            WordGame.ANAGRAM -> {
                val a = anagram!!
                title.text = game.title
                status.text = getString(R.string.anagram_status, a.score, a.solved)
                info.text = if (a.revealed > 0) getString(R.string.starts_with, a.answer.take(a.revealed).uppercase())
                else getString(R.string.anagram_info, a.answer.length)
            }
            WordGame.BUILDER -> {
                val b = builder!!
                title.text = getString(R.string.level_title, game.title, b.level)
                status.text = getString(R.string.builder_status, b.found.size, b.goal, b.targets.size)
                info.text = b.found.sortedWith(compareBy({ -it.length }, { it })).joinToString("  ") { it.uppercase() }
                    .ifEmpty { getString(R.string.builder_info) }
            }
            WordGame.LADDER -> {
                val l = ladder!!
                title.text = getString(R.string.level_title, game.title, l.level)
                status.text = getString(R.string.ladder_status, l.target.uppercase(), l.steps, l.par)
                info.text = l.chain.joinToString("  →  ") { it.uppercase() } +
                    if (l.isSolved) "" else "  →  …  →  ${l.target.uppercase()}"
            }
            WordGame.SEARCH -> {
                val s = search!!
                title.text = getString(R.string.level_title, game.title, s.level)
                status.text = getString(R.string.search_status, s.found.size, s.words.size)
                val sb = SpannableStringBuilder()
                for (w in s.words) {
                    val start = sb.length
                    sb.append(w.uppercase())
                    if (w in s.found) {
                        sb.setSpan(StrikethroughSpan(), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        sb.setSpan(ForegroundColorSpan(Color.parseColor("#6C7A99")), start, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    sb.append("   ")
                }
                info.text = sb
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    // --- Finishing ---------------------------------------------------------------------

    private fun levelDone(stars: Int) {
        if (finished) return
        finished = true
        setControlsEnabled(false)
        val level = builder?.level ?: ladder?.level ?: search!!.level
        val gained = progress.recordLevel(game, level, stars)
        val message = buildString {
            append("★".repeat(stars)).append("☆".repeat(3 - stars))
            ladder?.let { append("\n\n").append(getString(R.string.ladder_done, it.steps, it.par)) }
            builder?.let { append("\n\n").append(getString(R.string.builder_done, it.found.size, it.targets.size)) }
            if (gained > 0) append("\n").append(getString(R.string.stars_gained, gained))
        }
        handler.postDelayed({
            if (isFinishing) return@postDelayed
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(getString(R.string.level_done, level))
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.next_level) { _, _ -> afterInterstitial(::start) }
                .setNegativeButton(R.string.home) { _, _ -> finish() }
                .show()
        }, 400)
    }

    private fun endAnagram() {
        if (finished) return
        finished = true
        handler.removeCallbacks(ticker)
        setControlsEnabled(false)
        val a = anagram!!
        val newBest = progress.recordScore(game, a.score)
        val message = getString(R.string.anagram_done, a.solved, a.answer.uppercase()) +
            "\n" + getString(R.string.best_n, progress.best(game)) +
            if (newBest) "\n\n" + getString(R.string.new_best) else ""
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle(getString(R.string.anagram_score, a.score))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(R.string.play_again) { _, _ -> afterInterstitial(::start) }
            .setNegativeButton(R.string.home) { _, _ -> finish() }
            .show()
    }

    private fun afterInterstitial(action: () -> Unit) {
        if (adPolicy.shouldShowInterstitial(progress.completed())) ads.showInterstitial(this, action) else action()
    }

    companion object {
        const val EXTRA_GAME = "game"
    }
}
