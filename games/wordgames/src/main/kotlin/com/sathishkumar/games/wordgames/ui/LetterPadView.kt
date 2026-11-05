package com.sathishkumar.games.wordgames.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * Letter keys in rows. In tile mode each key can be used once (it greys out); in keyboard
 * mode keys can be pressed any number of times.
 */
class LetterPadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onKey: ((Int) -> Unit)? = null

    private var keys: List<Char> = emptyList()
    private var rows: List<Int> = emptyList()
    var used: BooleanArray = BooleanArray(0)
        private set

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val rects = mutableListOf<RectF>()

    /** [rowSizes] splits [keys] into rows, e.g. 10, 9, 7 for a keyboard. */
    fun setKeys(keys: List<Char>, rowSizes: List<Int> = listOf(keys.size)) {
        this.keys = keys
        this.rows = rowSizes
        used = BooleanArray(keys.size)
        requestLayout()
        invalidate()
    }

    fun markUsed(i: Int, value: Boolean) { used[i] = value; invalidate() }
    fun resetUsed() { used.fill(false); invalidate() }
    fun key(i: Int): Char = keys[i]

    private val keyHeight get() = 54 * dp

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (rows.size * (keyHeight + 6 * dp)).toInt())
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        rects.clear()
        val maxInRow = rows.maxOrNull() ?: 1
        val keyW = minOf(width / maxInRow.toFloat(), 64 * dp)
        var y = 0f
        for (count in rows) {
            val rowW = keyW * count
            var x = (width - rowW) / 2
            repeat(count) {
                rects += RectF(x + 2 * dp, y + 2 * dp, x + keyW - 2 * dp, y + keyHeight)
                x += keyW
            }
            y += keyHeight + 6 * dp
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val i = rects.indexOfFirst { it.contains(event.x, event.y) }
            if (i >= 0 && i < keys.size && !used[i]) {
                onKey?.invoke(i)
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
        text.textSize = minOf(keyHeight * 0.45f, (rects.firstOrNull()?.width() ?: 0f) * 0.6f)
        for ((i, r) in rects.withIndex()) {
            if (i >= keys.size) break
            paint.color = if (used[i]) Color.parseColor("#232B45") else Color.parseColor("#3D4A7A")
            canvas.drawRoundRect(r, 10 * dp, 10 * dp, paint)
            text.color = if (used[i]) Color.parseColor("#56607E") else Color.WHITE
            canvas.drawText(keys[i].uppercaseChar().toString(), r.centerX(), r.centerY() + text.textSize * 0.36f, text)
        }
    }
}
