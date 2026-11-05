package com.sathishkumar.games.wordgames.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sathishkumar.games.wordgames.logic.WordSearch
import kotlin.math.abs
import kotlin.math.sin

/** The letter grid. Swipe in a straight line to select; found words stay highlighted. */
class WordSearchView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    /** Called with the start and end cell of a swipe. */
    var onSelect: ((Int, Int) -> Unit)? = null

    private var search: WordSearch? = null
    private var dragStart = -1
    private var dragEnd = -1
    private var hintCell = -1
    private var hintStart = 0L

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true; color = Color.WHITE }

    fun bind(s: WordSearch) { search = s; hintCell = -1; invalidate() }

    fun showHint(cell: Int) { hintCell = cell; hintStart = SystemClock.uptimeMillis(); invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val s = if (h == 0) w else minOf(w, h)
        setMeasuredDimension(s, s)
    }

    private fun cellAt(x: Float, y: Float): Int {
        val s = search ?: return -1
        val cell = width / s.size.toFloat()
        val c = (x / cell).toInt().coerceIn(0, s.size - 1)
        val r = (y / cell).toInt().coerceIn(0, s.size - 1)
        return r * s.size + c
    }

    /** Snaps the end cell onto the nearest straight line (row, column or diagonal). */
    private fun snap(start: Int, raw: Int): Int {
        val n = search?.size ?: return raw
        val (r0, c0) = start / n to start % n
        val (r1, c1) = raw / n to raw % n
        val dr = r1 - r0
        val dc = c1 - c0
        return when {
            abs(dr) > 2 * abs(dc) -> r1 * n + c0
            abs(dc) > 2 * abs(dr) -> r0 * n + c1
            else -> {
                val k = minOf(abs(dr), abs(dc))
                (r0 + Integer.signum(dr) * k) * n + c0 + Integer.signum(dc) * k
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { dragStart = cellAt(event.x, event.y); dragEnd = dragStart; parent?.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_MOVE -> if (dragStart >= 0) dragEnd = snap(dragStart, cellAt(event.x, event.y))
            MotionEvent.ACTION_UP -> {
                if (dragStart >= 0) { onSelect?.invoke(dragStart, dragEnd); performClick() }
                dragStart = -1; dragEnd = -1
            }
            MotionEvent.ACTION_CANCEL -> { dragStart = -1; dragEnd = -1 }
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val s = search ?: return
        val cell = width / s.size.toFloat()
        fun cx(i: Int) = (i % s.size + 0.5f) * cell
        fun cy(i: Int) = (i / s.size + 0.5f) * cell

        paint.color = Color.parseColor("#1F2742")
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), 14 * dp, 14 * dp, paint)

        // Found words as coloured bars.
        paint.strokeWidth = cell * 0.72f
        s.found.forEachIndexed { k, w ->
            val cells = s.cellsOf(w)
            paint.color = FOUND[k % FOUND.size]
            canvas.drawLine(cx(cells.first()), cy(cells.first()), cx(cells.last()), cy(cells.last()), paint)
        }
        // Current swipe.
        if (dragStart >= 0) {
            paint.color = Color.argb(140, 255, 209, 102)
            canvas.drawLine(cx(dragStart), cy(dragStart), cx(dragEnd), cy(dragEnd), paint)
        }
        // Hint pulse.
        if (hintCell >= 0 && SystemClock.uptimeMillis() - hintStart < 2500) {
            val t = (SystemClock.uptimeMillis() - hintStart) / 250.0
            paint.color = Color.argb((120 + 100 * sin(t)).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(cx(hintCell), cy(hintCell), cell * 0.42f, paint)
            postInvalidateOnAnimation()
        }

        text.textSize = cell * 0.5f
        for (i in s.grid.indices) {
            canvas.drawText(s.grid[i].uppercaseChar().toString(), cx(i), cy(i) + text.textSize * 0.36f, text)
        }
    }

    private companion object {
        val FOUND = intArrayOf(
            Color.parseColor("#B04CC9F0"), Color.parseColor("#B006D6A0"), Color.parseColor("#B0EF476F"),
            Color.parseColor("#B0FFB703"), Color.parseColor("#B09B5DE5"), Color.parseColor("#B0F15BB5"),
        )
    }
}
