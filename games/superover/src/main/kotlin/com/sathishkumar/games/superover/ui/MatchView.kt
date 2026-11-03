package com.sathishkumar.games.superover.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sathishkumar.games.superover.logic.BallResult
import com.sathishkumar.games.superover.logic.BallType
import com.sathishkumar.games.superover.logic.Outcome
import com.sathishkumar.games.superover.logic.SuperOver
import com.sathishkumar.games.superover.logic.Timings
import kotlin.math.abs
import kotlin.math.sin

/**
 * The pitch seen from behind the batter. Each ball is bowled automatically; tap anywhere to
 * swing. The timing bar at the bottom shows how close the tap was to the perfect moment.
 */
class MatchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** Called after every ball. Set [hold] inside it to pause before the next ball. */
    var onBall: ((BallResult) -> Unit)? = null

    /** Called once, after the last ball's animation, when the match is decided. */
    var onMatchOver: (() -> Unit)? = null

    /** While true, no new ball is bowled (e.g. a dialog is open). */
    var hold = false
        set(value) {
            field = value
            if (!value) {
                phaseStart = SystemClock.uptimeMillis()
                invalidate()
            }
        }

    private enum class Phase { WAITING, BOWLING, RESULT, DONE }

    private var match: SuperOver? = null
    private var phase = Phase.WAITING
    private var phaseStart = 0L
    private var lastResult: BallResult? = null
    private var overNotified = false

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val path = Path()
    private val rect = RectF()
    private var sky: LinearGradient? = null

    fun bind(match: SuperOver) {
        this.match = match
        phase = Phase.WAITING
        phaseStart = SystemClock.uptimeMillis()
        lastResult = null
        overNotified = false
        hold = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        sky = LinearGradient(0f, 0f, 0f, h * 0.2f, Color.parseColor("#5AA9E6"), Color.parseColor("#BDE0FE"), Shader.TileMode.CLAMP)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        val m = match ?: return true
        if (phase != Phase.BOWLING || hold) return true
        val delivery = m.currentDelivery ?: return true
        val elapsed = SystemClock.uptimeMillis() - phaseStart
        // Swings before the ball is half-way are ignored, so a nervous tap doesn't waste a ball.
        if (elapsed < delivery.travelMs / 2) return true
        finishBall(m, elapsed - delivery.travelMs)
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun finishBall(m: SuperOver, errorMs: Long?) {
        val result = m.play(errorMs)
        lastResult = result
        phase = Phase.RESULT
        phaseStart = SystemClock.uptimeMillis()
        onBall?.invoke(result)
        invalidate()
    }

    /** Re-reads the match after an outside change (the free-hit reward). */
    fun refresh() {
        lastResult = match?.results?.lastOrNull()
        overNotified = false
        if (phase == Phase.DONE) phase = Phase.RESULT
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val m = match ?: return
        val now = SystemClock.uptimeMillis()
        val t = now - phaseStart
        advance(m, t)

        val w = width.toFloat()
        val h = height.toFloat()
        val topY = h * 0.24f
        val batY = h * 0.78f

        // Sky, crowd, field.
        paint.shader = sky
        canvas.drawRect(0f, 0f, w, h * 0.2f, paint)
        paint.shader = null
        paint.color = Color.parseColor("#3D405B")
        canvas.drawRect(0f, h * 0.12f, w, h * 0.2f, paint)
        for (i in 0 until 60) {
            paint.color = CROWD[i % CROWD.size]
            canvas.drawCircle((i * 37 % 100) / 100f * w, h * (0.135f + (i * 13 % 7) / 100f), 2.5f * dp, paint)
        }
        paint.color = Color.parseColor("#3FA34D")
        canvas.drawRect(0f, h * 0.2f, w, h, paint)
        paint.color = Color.parseColor("#4CB85C")
        for (i in 0 until 6) if (i % 2 == 0) canvas.drawRect(0f, h * (0.2f + i * 0.14f), w, h * (0.27f + i * 0.14f), paint)

        // Pitch.
        path.reset()
        path.moveTo(w * 0.44f, topY - h * 0.03f)
        path.lineTo(w * 0.56f, topY - h * 0.03f)
        path.lineTo(w * 0.72f, h * 0.9f)
        path.lineTo(w * 0.28f, h * 0.9f)
        path.close()
        paint.color = Color.parseColor("#D9B779")
        canvas.drawPath(path, paint)
        stroke.color = Color.WHITE
        stroke.strokeWidth = 2 * dp
        canvas.drawLine(w * 0.33f, batY + h * 0.03f, w * 0.67f, batY + h * 0.03f, stroke)
        canvas.drawLine(w * 0.45f, topY + h * 0.01f, w * 0.55f, topY + h * 0.01f, stroke)

        // Stumps at both ends.
        val bowled = lastResult?.outcome == Outcome.BOWLED && phase == Phase.RESULT
        drawStumps(canvas, w / 2, topY, 0.4f, false, 0f)
        drawStumps(canvas, w / 2, batY, 1f, bowled, (t / 300f).coerceAtMost(1f))

        // Bowler.
        val delivery = m.currentDelivery
        drawFigure(canvas, w / 2 + 14 * dp, topY - h * 0.02f, 0.45f, Color.parseColor("#1D3557"))

        // Batter and bat swing.
        val swing = if (phase == Phase.RESULT && lastResult?.errorMs != null) (t / 160f).coerceAtMost(1f) else 0f
        drawBatter(canvas, w / 2 - 24 * dp, batY, swing)

        // The ball.
        if (phase == Phase.BOWLING && delivery != null) {
            val p = (t.toFloat() / delivery.travelMs).coerceAtMost(1.15f)
            drawBall(canvas, delivery.type, p, w, topY, batY)
        } else if (phase == Phase.RESULT) {
            drawShot(canvas, lastResult!!, t, w, h, batY)
        }

        drawScoreboard(canvas, m, w)
        drawTimingBar(canvas, m, t, w, h)

        // Big result label.
        val r = lastResult
        if (phase == Phase.RESULT && r != null) {
            val a = (1f - (t - RESULT_MS * 0.6f) / (RESULT_MS * 0.4f)).coerceIn(0f, 1f)
            text.textSize = (if (r.outcome == Outcome.SIX || r.outcome.isOut) 46 else 34) * dp
            text.color = when {
                r.outcome.isOut -> Color.parseColor("#E63946")
                r.outcome == Outcome.SIX -> Color.parseColor("#FFD60A")
                r.outcome == Outcome.FOUR -> Color.parseColor("#4CC9F0")
                else -> Color.WHITE
            }
            text.alpha = (255 * a).toInt()
            text.setShadowLayer(6 * dp, 0f, 2 * dp, Color.BLACK)
            canvas.drawText(r.outcome.label, w / 2, h * 0.46f, text)
            text.textSize = 16 * dp
            text.color = Color.WHITE
            text.alpha = (255 * a).toInt()
            canvas.drawText(timingLabel(r), w / 2, h * 0.46f + 26 * dp, text)
            text.clearShadowLayer()
            text.alpha = 255
        } else if (phase == Phase.WAITING && !hold && delivery != null) {
            text.textSize = 18 * dp
            text.color = Color.WHITE
            text.setShadowLayer(4 * dp, 0f, 1 * dp, Color.BLACK)
            val label = (if (m.nextIsFreeHit) "FREE HIT · " else "") + "${delivery.type.title} · ${delivery.speedKmph} km/h"
            canvas.drawText(label, w / 2, h * 0.46f, text)
            text.clearShadowLayer()
        }

        if (phase != Phase.DONE) postInvalidateOnAnimation()
    }

    /** Moves between waiting, bowling and showing the result. */
    private fun advance(m: SuperOver, t: Long) {
        when (phase) {
            Phase.WAITING -> if (!hold && t >= WAIT_MS) {
                if (m.currentDelivery != null) {
                    phase = Phase.BOWLING
                    phaseStart = SystemClock.uptimeMillis()
                } else {
                    phase = Phase.DONE
                }
            }
            Phase.BOWLING -> {
                val d = m.currentDelivery ?: return
                // No swing in time: the ball goes past.
                if (t > d.travelMs + Timings.lastChanceMs(d, m.bat) + 60) finishBall(m, null)
            }
            Phase.RESULT -> if (!hold && t >= RESULT_MS) {
                phaseStart = SystemClock.uptimeMillis()
                phase = if (m.currentDelivery != null) Phase.WAITING else Phase.DONE
            }
            Phase.DONE -> Unit
        }
        if (phase == Phase.DONE && !overNotified) {
            overNotified = true
            post { onMatchOver?.invoke() }
        }
    }

    private fun timingLabel(r: BallResult): String {
        val e = r.errorMs ?: return if (r.wasFreeHit) "Free hit · no swing" else "No swing"
        val when_ = if (abs(e) < 15) "spot on" else if (e < 0) "${-e} ms early" else "$e ms late"
        return r.timing.name.lowercase().replaceFirstChar { it.uppercase() } + " · " + when_
    }

    private fun drawBall(canvas: Canvas, type: BallType, p: Float, w: Float, topY: Float, batY: Float) {
        val bounceAt = when (type) {
            BallType.BOUNCER -> 0.5f
            BallType.YORKER -> 0.97f
            BallType.SLOWER -> 0.65f
            else -> 0.72f
        }
        val groundY = topY + (batY - topY) * p
        val drift = if (type == BallType.OUTSWING) sin(p * 2.2f) * w * 0.07f else 0f
        val x = w / 2 + drift
        // Height above the pitch: falls to the bounce point, then rises (high for a bouncer).
        val lift = if (p < bounceAt) (1 - p / bounceAt) * 40 * dp
        else (p - bounceAt) / (1 - bounceAt + 0.001f) * (if (type == BallType.BOUNCER) 70 else 22) * dp
        val size = (3 + 6 * p) * dp
        paint.color = Color.argb(70, 0, 0, 0)
        rect.set(x - size, groundY - size * 0.35f, x + size, groundY + size * 0.35f)
        canvas.drawOval(rect, paint)
        paint.color = Color.parseColor("#C1121F")
        canvas.drawCircle(x, groundY - lift, size, paint)
        stroke.color = Color.WHITE
        stroke.strokeWidth = size * 0.18f
        canvas.drawLine(x - size * 0.6f, groundY - lift, x + size * 0.6f, groundY - lift, stroke)
    }

    private fun drawShot(canvas: Canvas, r: BallResult, t: Long, w: Float, h: Float, batY: Float) {
        val p = (t / 700f).coerceAtMost(1f)
        val sx = w / 2
        val sy = batY - 20 * dp
        val (x, y, size) = when (r.outcome) {
            Outcome.SIX -> Triple(sx + p * w * 0.3f, sy - p * h * 0.75f + p * p * h * 0.1f, (9 - 6 * p) * dp)
            Outcome.FOUR -> Triple(sx - p * w * 0.55f, sy - p * h * 0.25f, (9 - 4 * p) * dp)
            Outcome.TWO, Outcome.ONE -> Triple(sx + p * w * 0.25f, sy - p * h * 0.15f, (9 - 3 * p) * dp)
            Outcome.CAUGHT -> Triple(sx + p * w * 0.1f, sy - sin(p * Math.PI).toFloat() * h * 0.3f, 8 * dp)
            else -> return
        }
        paint.color = Color.parseColor("#C1121F")
        canvas.drawCircle(x, y, size, paint)
    }

    private fun drawStumps(canvas: Canvas, cx: Float, baseY: Float, scale: Float, broken: Boolean, k: Float) {
        val hgt = 34 * dp * scale
        val gap = 6 * dp * scale
        paint.color = Color.parseColor("#F1E3C3")
        for (i in -1..1) {
            canvas.save()
            if (broken) canvas.rotate(i * 25f * k + 8f * k, cx + i * gap, baseY)
            rect.set(cx + i * gap - 1.6f * dp * scale, baseY - hgt, cx + i * gap + 1.6f * dp * scale, baseY)
            canvas.drawRect(rect, paint)
            canvas.restore()
        }
        paint.color = Color.parseColor("#B08968")
        val bailLift = if (broken) k * 22 * dp else 0f
        canvas.drawRect(cx - gap - 2 * dp * scale, baseY - hgt - 2 * dp * scale - bailLift, cx + gap + 2 * dp * scale, baseY - hgt - bailLift, paint)
    }

    private fun drawFigure(canvas: Canvas, x: Float, footY: Float, scale: Float, shirt: Int) {
        paint.color = shirt
        rect.set(x - 9 * dp * scale, footY - 46 * dp * scale, x + 9 * dp * scale, footY - 14 * dp * scale)
        canvas.drawRoundRect(rect, 5 * dp * scale, 5 * dp * scale, paint)
        paint.color = Color.WHITE
        canvas.drawRect(x - 8 * dp * scale, footY - 16 * dp * scale, x + 8 * dp * scale, footY, paint)
        paint.color = Color.parseColor("#8D5524")
        canvas.drawCircle(x, footY - 53 * dp * scale, 7 * dp * scale, paint)
    }

    private fun drawBatter(canvas: Canvas, x: Float, footY: Float, swing: Float) {
        drawFigure(canvas, x, footY, 1.2f, Color.parseColor("#0077B6"))
        // Helmet.
        paint.color = Color.parseColor("#023E8A")
        canvas.drawArc(x - 9 * dp, footY - 73 * dp, x + 9 * dp, footY - 55 * dp, 180f, 180f, true, paint)
        // Bat: rests behind, swings through in front.
        canvas.save()
        canvas.rotate(-50f + swing * 150f, x + 8 * dp, footY - 40 * dp)
        paint.color = Color.parseColor("#E9C46A")
        rect.set(x + 6 * dp, footY - 40 * dp, x + 13 * dp, footY + 8 * dp)
        canvas.drawRoundRect(rect, 3 * dp, 3 * dp, paint)
        paint.color = Color.parseColor("#264653")
        canvas.drawRect(x + 8 * dp, footY - 52 * dp, x + 11 * dp, footY - 40 * dp, paint)
        canvas.restore()
    }

    /** Ball-by-ball circles across the top. */
    private fun drawScoreboard(canvas: Canvas, m: SuperOver, w: Float) {
        val r = 15 * dp
        val gap = 8 * dp
        val total = m.deliveries.size
        val start = w / 2 - (total * 2 * r + (total - 1) * gap) / 2 + r
        val shown = m.results.filter { it.outcome != Outcome.NO_BALL }
        text.textSize = 13 * dp
        for (i in 0 until total) {
            val cx = start + i * (2 * r + gap)
            val cy = 26 * dp
            val res = shown.getOrNull(i)
            paint.color = when {
                res == null -> Color.argb(110, 255, 255, 255)
                res.outcome.isOut -> Color.parseColor("#E63946")
                res.outcome == Outcome.SIX -> Color.parseColor("#FFB703")
                res.outcome == Outcome.FOUR -> Color.parseColor("#219EBC")
                else -> Color.WHITE
            }
            canvas.drawCircle(cx, cy, r, paint)
            text.color = if (res == null || res.outcome.isOut || res.outcome == Outcome.FOUR) Color.WHITE else Color.parseColor("#1D3557")
            val label = when {
                res == null -> ""
                res.outcome.isOut -> "W"
                res.outcome.runs == 0 -> "•"
                else -> res.outcome.runs.toString()
            }
            canvas.drawText(label, cx, cy + text.textSize * 0.36f, text)
        }
    }

    /** Coloured timing zones; a marker sweeps to the centre at the perfect moment. */
    private fun drawTimingBar(canvas: Canvas, m: SuperOver, t: Long, w: Float, h: Float) {
        val d = m.currentDelivery ?: lastResult?.delivery ?: return
        val scale = d.type.windowScale * m.bat.timing
        val range = Timings.EDGE_MS * scale * 1.6
        val left = w * 0.08f
        val right = w * 0.92f
        val cy = h * 0.95f
        val half = (right - left) / 2
        val cx = (left + right) / 2
        rect.set(left, cy - 7 * dp, right, cy + 7 * dp)
        paint.color = Color.argb(150, 0, 0, 0)
        canvas.drawRoundRect(rect, 7 * dp, 7 * dp, paint)
        for ((ms, color) in listOf(
            Timings.EDGE_MS to "#E76F51", Timings.OK_MS to "#F4A261",
            Timings.GOOD_MS to "#E9C46A", Timings.PERFECT_MS to "#2A9D8F",
        )) {
            val zw = (ms * scale / range * half).toFloat()
            paint.color = Color.parseColor(color)
            canvas.drawRect(cx - zw, cy - 5 * dp, cx + zw, cy + 5 * dp, paint)
        }
        val errorNow: Long? = when (phase) {
            Phase.BOWLING -> t - d.travelMs
            Phase.RESULT -> lastResult?.errorMs
            else -> null
        }
        if (errorNow != null && abs(errorNow) <= range) {
            val mx = cx + (errorNow / range * half).toFloat()
            paint.color = Color.WHITE
            rect.set(mx - 2.5f * dp, cy - 12 * dp, mx + 2.5f * dp, cy + 12 * dp)
            canvas.drawRoundRect(rect, 2 * dp, 2 * dp, paint)
        }
    }

    private companion object {
        const val WAIT_MS = 1100L
        const val RESULT_MS = 1500L
        val CROWD = intArrayOf(
            Color.parseColor("#FFB703"), Color.parseColor("#FB8500"), Color.parseColor("#8ECAE6"),
            Color.parseColor("#E63946"), Color.parseColor("#F1FAEE"),
        )
    }
}
