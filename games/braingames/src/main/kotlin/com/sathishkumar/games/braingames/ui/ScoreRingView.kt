package com.sathishkumar.games.braingames.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/** Today's Brain Score as a ring out of 1000. */
class ScoreRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var score = 0
        set(value) { field = value; contentDescription = "Brain Score $value of 1000"; invalidate() }

    private val dp = resources.displayMetrics.density
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE }
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        val r = minOf(width, height) / 2f - 10 * dp
        val cx = width / 2f
        val cy = height / 2f
        rect.set(cx - r, cy - r, cx + r, cy + r)
        ring.strokeWidth = 12 * dp
        ring.color = Color.parseColor("#2E3A59")
        canvas.drawArc(rect, 0f, 360f, false, ring)
        ring.color = Color.parseColor("#FFD166")
        canvas.drawArc(rect, -90f, 360f * score.coerceIn(0, 1000) / 1000f, false, ring)
        text.isFakeBoldText = true
        text.textSize = r * 0.55f
        canvas.drawText(score.toString(), cx, cy + text.textSize * 0.25f, text)
        text.isFakeBoldText = false
        text.textSize = r * 0.16f
        text.color = Color.parseColor("#A9B4D0")
        canvas.drawText("/ 1000 today", cx, cy + r * 0.45f, text)
        text.color = Color.WHITE
    }
}
