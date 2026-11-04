package com.sathishkumar.games.braingames.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.sathishkumar.games.braingames.BuildConfig
import com.sathishkumar.games.braingames.R
import com.sathishkumar.games.braingames.logic.BrainGame
import com.sathishkumar.games.braingames.logic.ColorClash
import com.sathishkumar.games.braingames.logic.InkColor
import com.sathishkumar.games.braingames.logic.QuickMath
import com.sathishkumar.games.core.Haptics
import com.sathishkumar.games.core.ads.AdUnits
import com.sathishkumar.games.core.ads.Ads

/** Quick Math and Color Clash: a prompt, four answers and a countdown. */
class QuizActivity : Activity() {
    private lateinit var game: BrainGame
    private lateinit var ads: Ads
    private var math: QuickMath? = null
    private var clash: ColorClash? = null

    private lateinit var prompt: TextView
    private lateinit var instructions: TextView
    private lateinit var scoreText: TextView
    private lateinit var timeBar: ProgressBar
    private lateinit var buttons: List<Button>

    private val handler = Handler(Looper.getMainLooper())
    private var endsAt = 0L
    private var remainingWhenPaused = -1L
    private var finished = false
    private val totalMs: Long get() = (if (game == BrainGame.QUICK_MATH) QuickMath.SECONDS else ColorClash.SECONDS) * 1000L

    private val ticker = object : Runnable {
        override fun run() {
            val left = endsAt - SystemClock.elapsedRealtime()
            timeBar.progress = (left * 1000 / totalMs).toInt().coerceAtLeast(0)
            if (left <= 0) endGame() else handler.postDelayed(this, 50)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)
        game = BrainGame.valueOf(intent.getStringExtra(HomeActivity.EXTRA_GAME) ?: BrainGame.QUICK_MATH.name)
        ads = Ads(this, AdUnits(BuildConfig.ADMOB_REWARDED, BuildConfig.ADMOB_INTERSTITIAL))
        prompt = findViewById(R.id.prompt)
        instructions = findViewById(R.id.instructions)
        scoreText = findViewById(R.id.score_text)
        timeBar = findViewById(R.id.time_bar)
        buttons = listOf(R.id.answer_0, R.id.answer_1, R.id.answer_2, R.id.answer_3).map { findViewById(it) }
        findViewById<TextView>(R.id.game_title).text = game.title
        buttons.forEachIndexed { i, b -> b.setOnClickListener { onAnswer(i) } }
        start()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        if (!finished) remainingWhenPaused = endsAt - SystemClock.elapsedRealtime()
    }

    override fun onResume() {
        super.onResume()
        if (!finished && remainingWhenPaused >= 0) {
            endsAt = SystemClock.elapsedRealtime() + remainingWhenPaused
            remainingWhenPaused = -1
            handler.post(ticker)
        }
    }

    private fun start() {
        finished = false
        if (game == BrainGame.QUICK_MATH) {
            math = QuickMath()
            instructions.text = getString(R.string.math_instructions)
        } else {
            clash = ColorClash()
            instructions.text = getString(R.string.clash_instructions)
        }
        buttons.forEach { it.isEnabled = true }
        timeBar.max = 1000
        endsAt = SystemClock.elapsedRealtime() + totalMs
        handler.removeCallbacks(ticker)
        handler.post(ticker)
        render()
    }

    private fun render() {
        math?.let { m ->
            prompt.text = m.current.text
            prompt.setTextColor(Color.WHITE)
            m.current.options.forEachIndexed { i, v -> buttons[i].text = v.toString() }
            scoreText.text = getString(R.string.score_n, m.score)
        }
        clash?.let { c ->
            prompt.text = c.current.word.word
            prompt.setTextColor(inkColor(c.current.ink))
            // Answer buttons are plain colour swatches, so only the ink matters.
            InkColor.entries.forEachIndexed { i, ink ->
                buttons[i].text = ""
                buttons[i].contentDescription = ink.word.lowercase()
                buttons[i].backgroundTintList = ColorStateList.valueOf(inkColor(ink))
            }
            scoreText.text = getString(R.string.score_n, c.score)
        }
    }

    private fun onAnswer(i: Int) {
        if (finished) return
        val right = math?.let { m -> m.answer(m.current.options[i]) }
            ?: clash!!.answer(InkColor.entries[i])
        if (right) Haptics.tap(prompt) else Haptics.error(prompt)
        // A quick flash so the player knows how they did.
        scoreText.setTextColor(if (right) TileGridView.GOOD else TileGridView.BAD)
        scoreText.postDelayed({ scoreText.setTextColor(Color.WHITE) }, 250)
        render()
    }

    private fun endGame() {
        if (finished) return
        finished = true
        handler.removeCallbacks(ticker)
        buttons.forEach { it.isEnabled = false }
        val (raw, detail) = math?.let { it.score.toLong() to getString(R.string.math_detail, it.correct, it.wrong) }
            ?: clash!!.let { it.score.toLong() to getString(R.string.clash_detail, it.correct, it.wrong) }
        showResult(ads, game, raw, detail, onPlayAgain = ::start)
    }

    private fun inkColor(ink: InkColor): Int = when (ink) {
        InkColor.RED -> Color.parseColor("#EF476F")
        InkColor.BLUE -> Color.parseColor("#4CC9F0")
        InkColor.GREEN -> Color.parseColor("#06D6A0")
        InkColor.YELLOW -> Color.parseColor("#FFD166")
    }
}
