package com.sathishkumar.games.puzzlegames.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sathishkumar.games.puzzlegames.logic.LightsOut
import com.sathishkumar.games.puzzlegames.logic.MiniSudoku
import com.sathishkumar.games.puzzlegames.logic.PipeFlow
import com.sathishkumar.games.puzzlegames.logic.Puzzle
import com.sathishkumar.games.puzzlegames.logic.SlidePuzzle

/** Draws any of the four puzzles as a square grid and reports which cell was tapped. */
class PuzzleBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onTap: ((Int) -> Unit)? = null
    /** Sudoku's selected cell. */
    var selected: Int? = null
        set(value) { field = value; invalidate() }

    private var puzzle: Puzzle? = null
    private var flashCell = -1
    private var flashStart = 0L

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val rect = RectF()

    fun bind(p: Puzzle) {
        puzzle = p
        selected = null
        invalidate()
    }

    /** Briefly marks [cell] red (a wrong Sudoku number). */
    fun flash(cell: Int) {
        flashCell = cell
        flashStart = SystemClock.uptimeMillis()
        invalidate()
    }

    private val size: Int
        get() = when (val p = puzzle) {
            is SlidePuzzle -> p.size
            is LightsOut -> p.size
            is PipeFlow -> p.size
            is MiniSudoku -> MiniSudoku.N
            else -> 1
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val s = if (h == 0) w else minOf(w, h)
        setMeasuredDimension(s, s)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
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
        val p = puzzle ?: return
        val cell = width / size.toFloat()
        when (p) {
            is SlidePuzzle -> drawSlide(canvas, p, cell)
            is LightsOut -> drawLights(canvas, p, cell)
            is PipeFlow -> drawPipes(canvas, p, cell)
            is MiniSudoku -> drawSudoku(canvas, p, cell)
        }
    }

    private fun cellRect(i: Int, cell: Float, inset: Float) {
        val x = i % size
        val y = i / size
        rect.set(x * cell + inset, y * cell + inset, (x + 1) * cell - inset, (y + 1) * cell - inset)
    }

    private fun drawSlide(canvas: Canvas, p: SlidePuzzle, cell: Float) {
        text.textSize = cell * 0.42f
        for (i in p.tiles.indices) {
            val v = p.tiles[i]
            if (v == 0) continue
            cellRect(i, cell, 4 * dp)
            paint.color = if (v == i + 1) Color.parseColor("#2A9D8F") else Color.parseColor("#3D4A7A")
            canvas.drawRoundRect(rect, 12 * dp, 12 * dp, paint)
            text.color = Color.WHITE
            canvas.drawText(v.toString(), rect.centerX(), rect.centerY() + text.textSize * 0.36f, text)
        }
    }

    private fun drawLights(canvas: Canvas, p: LightsOut, cell: Float) {
        for (i in p.lights.indices) {
            cellRect(i, cell, 5 * dp)
            if (p.lights[i]) {
                paint.color = Color.argb(70, 255, 209, 102)
                canvas.drawRoundRect(rect.left - 3 * dp, rect.top - 3 * dp, rect.right + 3 * dp, rect.bottom + 3 * dp, 16 * dp, 16 * dp, paint)
                paint.color = Color.parseColor("#FFD166")
            } else {
                paint.color = Color.parseColor("#26304F")
            }
            canvas.drawRoundRect(rect, 14 * dp, 14 * dp, paint)
        }
    }

    private fun drawPipes(canvas: Canvas, p: PipeFlow, cell: Float) {
        val wet = p.connected()
        val solved = p.isSolved
        for (i in 0 until p.size * p.size) {
            cellRect(i, cell, 2 * dp)
            paint.color = Color.parseColor("#1F2742")
            canvas.drawRoundRect(rect, 8 * dp, 8 * dp, paint)
            val cx = rect.centerX()
            val cy = rect.centerY()
            val half = cell / 2
            stroke.strokeWidth = cell * 0.22f
            stroke.color = when {
                solved -> Color.parseColor("#06D6A0")
                i in wet -> Color.parseColor("#4CC9F0")
                else -> Color.parseColor("#6C7A99")
            }
            val m = p.mask(i)
            if (m and 1 != 0) canvas.drawLine(cx, cy, cx, cy - half, stroke)
            if (m and 2 != 0) canvas.drawLine(cx, cy, cx + half, cy, stroke)
            if (m and 4 != 0) canvas.drawLine(cx, cy, cx, cy + half, stroke)
            if (m and 8 != 0) canvas.drawLine(cx, cy, cx - half, cy, stroke)
            paint.color = stroke.color
            canvas.drawCircle(cx, cy, cell * 0.13f, paint)
            if (i == p.source) {
                paint.color = Color.parseColor("#1D3557")
                canvas.drawCircle(cx, cy, cell * 0.3f, paint)
                paint.color = Color.parseColor("#4CC9F0")
                canvas.drawCircle(cx, cy, cell * 0.2f, paint)
            }
        }
    }

    private fun drawSudoku(canvas: Canvas, p: MiniSudoku, cell: Float) {
        val n = MiniSudoku.N
        val sel = selected
        val selValue = sel?.let { p.cells[it] } ?: 0
        val now = SystemClock.uptimeMillis()
        text.textSize = cell * 0.5f
        for (i in 0 until n * n) {
            cellRect(i, cell, 1.5f * dp)
            val r = i / n; val c = i % n
            val related = sel != null && (sel / n == r || sel % n == c || (sel / n / 2 == r / 2 && sel % n / 3 == c / 3))
            paint.color = when {
                i == flashCell && now - flashStart < 350 -> Color.parseColor("#EF476F")
                i == sel -> Color.parseColor("#4E5D94")
                selValue != 0 && p.cells[i] == selValue -> Color.parseColor("#3A4777")
                related -> Color.parseColor("#26304F")
                else -> Color.parseColor("#1F2742")
            }
            canvas.drawRect(rect, paint)
            val v = p.cells[i]
            if (v != 0) {
                text.color = if (p.isGiven(i)) Color.WHITE else Color.parseColor("#FFD166")
                text.isFakeBoldText = p.isGiven(i)
                canvas.drawText(v.toString(), rect.centerX(), rect.centerY() + text.textSize * 0.36f, text)
            }
        }
        text.isFakeBoldText = true
        // Thick lines between the 2×3 boxes.
        stroke.color = Color.parseColor("#A9B4D0")
        stroke.strokeWidth = 3 * dp
        for (k in 0..3) canvas.drawLine(0f, k * 2 * cell, width.toFloat(), k * 2 * cell, stroke)
        for (k in 0..2) canvas.drawLine(k * 3 * cell, 0f, k * 3 * cell, height.toFloat(), stroke)
        if (flashCell >= 0 && now - flashStart < 350) postInvalidateOnAnimation()
    }
}
