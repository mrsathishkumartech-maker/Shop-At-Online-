package com.sathishkumar.games.braingames.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/** A square grid of tappable tiles with optional labels; used by Memory Grid and Number Rush. */
class TileGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onTap: ((Int) -> Unit)? = null
    var enabledForInput = true

    var size = 3
        private set
    private var colors = IntArray(9) { IDLE }
    private var labels: Array<String?> = arrayOfNulls(9)

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        color = Color.WHITE
    }
    private val rect = RectF()

    fun setGrid(size: Int, colors: IntArray, labels: Array<String?> = arrayOfNulls(size * size)) {
        this.size = size
        this.colors = colors
        this.labels = labels
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val s = if (h == 0) w else min(w, h)
        setMeasuredDimension(s, s)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enabledForInput) return true
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val cell = width / size.toFloat()
            val x = (event.x / cell).toInt()
            val y = (event.y / cell).toInt()
            if (x in 0 until size && y in 0 until size) {
                onTap?.invoke(y * size + x)
                performClick()
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val cell = width / size.toFloat()
        val gap = 4 * dp
        text.textSize = cell * 0.4f
        for (i in 0 until size * size) {
            val x = i % size
            val y = i / size
            rect.set(x * cell + gap, y * cell + gap, (x + 1) * cell - gap, (y + 1) * cell - gap)
            paint.color = colors.getOrElse(i) { IDLE }
            canvas.drawRoundRect(rect, 10 * dp, 10 * dp, paint)
            labels.getOrNull(i)?.let { canvas.drawText(it, rect.centerX(), rect.centerY() + text.textSize * 0.36f, text) }
        }
    }

    companion object {
        val IDLE = Color.parseColor("#2E3A59")
        val LIT = Color.parseColor("#FFD166")
        val GOOD = Color.parseColor("#06D6A0")
        val BAD = Color.parseColor("#EF476F")
        val DONE = Color.parseColor("#1B2238")
    }
}
