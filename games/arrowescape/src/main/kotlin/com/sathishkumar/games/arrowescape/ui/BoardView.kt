package com.sathishkumar.games.arrowescape.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sathishkumar.games.arrowescape.logic.Arrow
import com.sathishkumar.games.arrowescape.logic.Block
import com.sathishkumar.games.arrowescape.logic.Direction
import com.sathishkumar.games.arrowescape.logic.ExitSlot
import com.sathishkumar.games.arrowescape.logic.GameSession
import com.sathishkumar.games.arrowescape.logic.GateColor
import com.sathishkumar.games.arrowescape.logic.RangoliDesigner
import com.sathishkumar.games.arrowescape.logic.TapResult
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Draws the board: coloured gates on the border, arrow tiles, and the level's rangoli
 * showing through every cell whose arrow has escaped. Taps go to the [GameSession].
 */
class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onTap: ((TapResult) -> Unit)? = null

    private var session: GameSession? = null

    // Layout, recomputed in onSizeChanged / bind.
    private var cell = 0f
    private var originX = 0f
    private var originY = 0f

    private val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Palette.GLYPH
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val plainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val glyph = Path()
    private val rect = RectF()
    private val src = Rect()
    private val dst = RectF()

    private var rangoli: Bitmap? = null
    private val rangoliPainter = RangoliPainter()

    // Animations.
    private class Flight(val arrow: Arrow, val cells: Int, val start: Long)
    private class Bump(val arrow: Arrow, val block: Block, val start: Long)

    private val flights = mutableListOf<Flight>()
    private var bump: Bump? = null
    private var hintArrow: Arrow? = null
    private var hintStart = 0L
    private var winStart = 0L

    fun bind(session: GameSession) {
        this.session = session
        flights.clear()
        bump = null
        hintArrow = null
        winStart = 0L
        layoutBoard()
        invalidate()
    }

    /** Call after the session changes outside a tap (undo, restart). */
    fun refresh() {
        flights.clear()
        bump = null
        hintArrow = null
        winStart = if (session?.isWon == true) SystemClock.uptimeMillis() else 0L
        invalidate()
    }

    fun showHint(arrow: Arrow?) {
        hintArrow = arrow
        hintStart = SystemClock.uptimeMillis()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        layoutBoard()
    }

    private fun layoutBoard() {
        val board = session?.board ?: return
        if (width == 0 || height == 0) return
        // Leave room around the board for the gates.
        cell = min(width / (board.width + 1f), height / (board.height + 1f))
        originX = (width - cell * board.width) / 2f
        originY = (height - cell * board.height) / 2f
        buildRangoli()
    }

    private fun buildRangoli() {
        val s = session ?: return
        rangoli?.recycle()
        val w = (cell * s.board.width).toInt().coerceAtLeast(1)
        val h = (cell * s.board.height).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Palette.PANEL)
        val design = RangoliDesigner.design(s.level.rangoliSeed)
        rangoliPainter.draw(canvas, w / 2f, h / 2f, min(w, h) / 2f * 0.96f, design)
        rangoli = bitmap
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val s = session ?: return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
        if (event.actionMasked != MotionEvent.ACTION_UP || cell == 0f) return false
        val x = floor((event.x - originX) / cell).toInt()
        val y = floor((event.y - originY) / cell).toInt()
        if (!s.board.inside(x, y)) return true
        val result = s.tap(x, y)
        val now = SystemClock.uptimeMillis()
        when (result) {
            is TapResult.Escaped -> {
                flights += Flight(result.arrow, result.distance, now)
                if (hintArrow == result.arrow) hintArrow = null
                if (s.isWon) winStart = now + FLIGHT_BASE_MS
            }
            is TapResult.Blocked -> bump = Bump(result.arrow, result.block, now)
            TapResult.Nothing -> return true
        }
        performClick()
        invalidate()
        onTap?.invoke(result)
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val s = session ?: return
        if (cell == 0f) layoutBoard()
        if (cell == 0f) return
        if (rangoli == null) buildRangoli()
        val board = s.board
        val now = SystemClock.uptimeMillis()
        var animating = false

        // Panel.
        tilePaint.color = Palette.PANEL
        rect.set(originX, originY, originX + cell * board.width, originY + cell * board.height)
        canvas.drawRoundRect(rect, cell * 0.2f, cell * 0.2f, tilePaint)

        // Rangoli through cleared cells, then fully revealed once the board is clear.
        val bitmap = rangoli
        if (bitmap != null) {
            for ((x, y) in s.clearedCells) {
                src.set((x * cell).toInt(), (y * cell).toInt(), ((x + 1) * cell).toInt(), ((y + 1) * cell).toInt())
                dst.set(originX + x * cell, originY + y * cell, originX + (x + 1) * cell, originY + (y + 1) * cell)
                canvas.drawBitmap(bitmap, src, dst, bitmapPaint)
            }
            if (winStart != 0L && now >= winStart) {
                val t = ((now - winStart) / WIN_REVEAL_MS.toFloat()).coerceIn(0f, 1f)
                bitmapPaint.alpha = (255 * t).toInt()
                canvas.drawBitmap(bitmap, originX, originY, bitmapPaint)
                bitmapPaint.alpha = 255
                if (t < 1f) animating = true
            } else if (winStart != 0L) {
                animating = true
            }
        }

        // Empty-cell dots.
        plainPaint.color = Palette.CELL_DOT
        if (!s.isWon) {
            for (y in 0 until board.height) for (x in 0 until board.width) {
                if (board.arrowAt(x, y) == null && (x to y) !in s.clearedCells) {
                    canvas.drawCircle(originX + (x + 0.5f) * cell, originY + (y + 0.5f) * cell, cell * 0.06f, plainPaint)
                }
            }
        }

        drawGates(canvas, board.gates)

        // Resting arrows.
        val activeBump = bump?.takeIf { now - it.start < BUMP_MS }
        if (activeBump == null) bump = null else animating = true
        for (arrow in board.arrows) {
            var dx = 0f
            var dy = 0f
            var error = false
            if (activeBump != null && activeBump.arrow == arrow) {
                val t = (now - activeBump.start) / BUMP_MS.toFloat()
                val push = sin(t * PI).toFloat() * cell * 0.22f
                dx = arrow.direction.dx * push
                dy = arrow.direction.dy * push
                error = true
            }
            drawArrow(canvas, arrow, originX + arrow.x * cell + dx, originY + arrow.y * cell + dy, 255, error)
        }

        // Blocker / hint outlines.
        if (activeBump != null) {
            val blocker = (activeBump.block as? Block.ByArrow)?.arrow
            if (blocker != null) outlineCell(canvas, blocker.x, blocker.y, Palette.ERROR)
        }
        val hint = hintArrow
        if (hint != null && board.arrowAt(hint.x, hint.y) == hint) {
            val pulse = 0.5f + 0.5f * sin((now - hintStart) / 180.0).toFloat()
            outlinePaint.alpha = (120 + 135 * pulse).toInt()
            outlineCell(canvas, hint.x, hint.y, Palette.HINT)
            outlinePaint.alpha = 255
            animating = true
        }

        // Escaping arrows slide off the board and fade out.
        val iterator = flights.iterator()
        while (iterator.hasNext()) {
            val f = iterator.next()
            val duration = FLIGHT_BASE_MS + FLIGHT_PER_CELL_MS * f.cells
            val t = ((now - f.start) / duration.toFloat()).coerceIn(0f, 1f)
            if (t >= 1f) {
                iterator.remove()
                continue
            }
            val travel = t * t * (f.cells + 0.5f) * cell
            val alpha = (255 * (1f - t * t)).toInt()
            drawArrow(
                canvas, f.arrow,
                originX + f.arrow.x * cell + f.arrow.direction.dx * travel,
                originY + f.arrow.y * cell + f.arrow.direction.dy * travel,
                alpha, false,
            )
            animating = true
        }

        if (animating) postInvalidateOnAnimation()
    }

    private fun drawGates(canvas: Canvas, gates: Map<ExitSlot, GateColor>) {
        val board = session?.board ?: return
        val gap = cell * 0.1f
        val thick = cell * 0.16f
        val inset = cell * 0.12f
        val bumpedGate = (bump?.block as? Block.ByGate)?.slot
        for ((slot, color) in gates) {
            when (slot.side) {
                Direction.RIGHT -> {
                    val left = originX + board.width * cell + gap
                    rect.set(left, originY + slot.index * cell + inset, left + thick, originY + (slot.index + 1) * cell - inset)
                }
                Direction.LEFT -> {
                    val right = originX - gap
                    rect.set(right - thick, originY + slot.index * cell + inset, right, originY + (slot.index + 1) * cell - inset)
                }
                Direction.DOWN -> {
                    val top = originY + board.height * cell + gap
                    rect.set(originX + slot.index * cell + inset, top, originX + (slot.index + 1) * cell - inset, top + thick)
                }
                Direction.UP -> {
                    val bottom = originY - gap
                    rect.set(originX + slot.index * cell + inset, bottom - thick, originX + (slot.index + 1) * cell - inset, bottom)
                }
            }
            tilePaint.color = Palette.gate(color)
            canvas.drawRoundRect(rect, thick / 2, thick / 2, tilePaint)
            if (slot == bumpedGate) {
                outlinePaint.color = Palette.ERROR
                outlinePaint.strokeWidth = cell * 0.05f
                canvas.drawRoundRect(rect, thick / 2, thick / 2, outlinePaint)
            }
        }
    }

    private fun drawArrow(canvas: Canvas, arrow: Arrow, left: Float, top: Float, alpha: Int, error: Boolean) {
        val pad = cell * 0.08f
        rect.set(left + pad, top + pad, left + cell - pad, top + cell - pad)
        tilePaint.color = when {
            error -> Palette.ERROR
            arrow.color != null -> Palette.gate(arrow.color)
            else -> Palette.PLAIN_ARROW
        }
        tilePaint.alpha = alpha
        canvas.drawRoundRect(rect, cell * 0.18f, cell * 0.18f, tilePaint)
        tilePaint.alpha = 255

        // Arrow glyph, drawn pointing right then rotated.
        val cx = left + cell / 2f
        val cy = top + cell / 2f
        val r = cell * 0.24f
        glyph.reset()
        glyph.moveTo(cx - r, cy)
        glyph.lineTo(cx + r, cy)
        glyph.moveTo(cx + r * 0.25f, cy - r * 0.7f)
        glyph.lineTo(cx + r, cy)
        glyph.lineTo(cx + r * 0.25f, cy + r * 0.7f)
        glyphPaint.strokeWidth = cell * 0.09f
        glyphPaint.alpha = alpha
        canvas.save()
        canvas.rotate(rotationOf(arrow.direction), cx, cy)
        canvas.drawPath(glyph, glyphPaint)
        canvas.restore()
    }

    private fun outlineCell(canvas: Canvas, x: Int, y: Int, color: Int) {
        val pad = cell * 0.04f
        rect.set(originX + x * cell + pad, originY + y * cell + pad, originX + (x + 1) * cell - pad, originY + (y + 1) * cell - pad)
        val alpha = outlinePaint.alpha
        outlinePaint.color = color
        outlinePaint.alpha = alpha
        outlinePaint.strokeWidth = cell * 0.06f
        canvas.drawRoundRect(rect, cell * 0.2f, cell * 0.2f, outlinePaint)
    }

    private fun rotationOf(direction: Direction): Float = when (direction) {
        Direction.RIGHT -> 0f
        Direction.DOWN -> 90f
        Direction.LEFT -> 180f
        Direction.UP -> 270f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rangoli?.recycle()
        rangoli = null
    }

    private companion object {
        const val FLIGHT_BASE_MS = 180L
        const val FLIGHT_PER_CELL_MS = 45L
        const val BUMP_MS = 260L
        const val WIN_REVEAL_MS = 700L
    }
}
