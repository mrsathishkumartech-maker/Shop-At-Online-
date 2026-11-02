package com.sathishkumar.games.kiranasort.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sathishkumar.games.kiranasort.logic.Item
import com.sathishkumar.games.kiranasort.logic.KiranaSession
import com.sathishkumar.games.kiranasort.logic.Move
import com.sathishkumar.games.kiranasort.logic.MoveResult
import com.sathishkumar.games.kiranasort.logic.SLOTS_PER_SHELF
import com.sathishkumar.games.kiranasort.logic.SlotRef
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

/**
 * Shows the shelves and lets the player drag items between them. Dropping anywhere on a shelf
 * puts the item in that shelf's slot under the finger, or its first free slot.
 */
class ShelfView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onMove: ((MoveResult) -> Unit)? = null
    var onRejected: (() -> Unit)? = null

    private var session: KiranaSession? = null
    private val painter = ItemPainter()

    private val shelfRects = mutableListOf<RectF>()
    private var slotSize = 0f

    private val wood = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8B5A2B") }
    private val woodEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#5E3B1A") }
    private val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3A2A1E") }
    private val slotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#4A3626") }
    private val badgeText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val badgeBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#B0000000") }
    private val sparkle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE082") }
    private val rect = RectF()

    // Drag state.
    private var dragFrom: SlotRef? = null
    private var dragItem: Item? = null
    private var dragX = 0f
    private var dragY = 0f

    private class Pop(val shelf: Int, val start: Long)
    private val pops = mutableListOf<Pop>()

    fun bind(session: KiranaSession) {
        this.session = session
        dragFrom = null
        pops.clear()
        layoutShelves()
        invalidate()
    }

    /** Call after the board changes outside a drag (extra shelf, shuffle, restart). */
    fun refresh(clearedShelves: List<Int> = emptyList()) {
        val now = SystemClock.uptimeMillis()
        clearedShelves.forEach { pops += Pop(it, now) }
        layoutShelves()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = layoutShelves()

    private fun layoutShelves() {
        shelfRects.clear()
        val count = session?.board?.shelves?.size ?: return
        if (width == 0 || height == 0) return
        val columns = if (count <= 4) 1 else 2
        val rows = ceil(count / columns.toFloat()).toInt()
        val gap = 12f * resources.displayMetrics.density
        val cellW = (width - gap * (columns + 1)) / columns
        val cellH = (height - gap * (rows + 1)) / rows
        // Each shelf is 3 slots wide plus padding; keep slots square.
        slotSize = min(cellW / (SLOTS_PER_SHELF + 0.6f), cellH / 1.45f)
        val shelfW = slotSize * (SLOTS_PER_SHELF + 0.6f)
        val shelfH = slotSize * 1.45f
        val totalW = columns * shelfW + (columns - 1) * gap
        val totalH = rows * shelfH + (rows - 1) * gap
        val left0 = (width - totalW) / 2f
        val top0 = (height - totalH) / 2f
        for (i in 0 until count) {
            val col = i % columns
            val row = i / columns
            val l = left0 + col * (shelfW + gap)
            val t = top0 + row * (shelfH + gap)
            shelfRects += RectF(l, t, l + shelfW, t + shelfH)
        }
    }

    private fun slotCenter(ref: SlotRef): Pair<Float, Float> {
        val r = shelfRects[ref.shelf]
        val x = r.left + slotSize * 0.3f + slotSize * (ref.slot + 0.5f)
        val y = r.top + slotSize * 0.72f
        return x to y
    }

    private fun slotAt(x: Float, y: Float): SlotRef? {
        for ((i, r) in shelfRects.withIndex()) {
            if (!r.contains(x, y)) continue
            val slot = ((x - r.left - slotSize * 0.3f) / slotSize).toInt().coerceIn(0, SLOTS_PER_SHELF - 1)
            return SlotRef(i, slot)
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val s = session ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (s.isOver) return false
                val ref = slotAt(event.x, event.y) ?: return false
                val item = s.board.itemAt(ref) ?: return false
                dragFrom = ref
                dragItem = item
                dragX = event.x
                dragY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragFrom == null) return false
                dragX = event.x
                dragY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val from = dragFrom ?: return false
                dragFrom = null
                dragItem = null
                val drop = slotAt(event.x, event.y)
                val target = when {
                    drop == null || drop.shelf == from.shelf -> null
                    s.board.itemAt(drop) == null -> drop
                    else -> s.board.firstEmptySlot(drop.shelf)
                }
                val result = target?.let { s.move(Move(from, it)) }
                if (result != null) {
                    val now = SystemClock.uptimeMillis()
                    result.cleared.forEach { (shelf, _) -> pops += Pop(shelf, now) }
                    performClick()
                    onMove?.invoke(result)
                } else if (drop != null && drop.shelf != from.shelf) {
                    onRejected?.invoke()
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                dragFrom = null
                dragItem = null
                invalidate()
                return true
            }
        }
        return false
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val s = session ?: return
        if (shelfRects.size != s.board.shelves.size) layoutShelves()
        if (shelfRects.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        var animating = false
        badgeText.textSize = slotSize * 0.22f

        for ((i, shelf) in s.board.shelves.withIndex()) {
            val r = shelfRects[i]
            // Back panel, slots and plank.
            canvas.drawRoundRect(r, slotSize * 0.12f, slotSize * 0.12f, back)
            for (slot in 0 until SLOTS_PER_SHELF) {
                val (cx, cy) = slotCenter(SlotRef(i, slot))
                canvas.drawCircle(cx, cy + slotSize * 0.05f, slotSize * 0.36f, slotPaint)
            }
            rect.set(r.left, r.bottom - slotSize * 0.28f, r.right, r.bottom)
            canvas.drawRoundRect(rect, slotSize * 0.08f, slotSize * 0.08f, wood)
            rect.set(r.left, r.bottom - slotSize * 0.08f, r.right, r.bottom)
            canvas.drawRect(rect, woodEdge)

            // Peek at the next hidden row, faded and raised.
            shelf.layers.firstOrNull()?.forEachIndexed { slot, item ->
                if (item != null) {
                    val (cx, cy) = slotCenter(SlotRef(i, slot))
                    painter.draw(canvas, item, cx, cy - slotSize * 0.18f, slotSize * 0.55f, alpha = 70)
                }
            }

            // Front row.
            shelf.front.forEachIndexed { slot, item ->
                val ref = SlotRef(i, slot)
                if (item != null && ref != dragFrom) {
                    val (cx, cy) = slotCenter(ref)
                    painter.draw(canvas, item, cx, cy, slotSize * 0.72f)
                }
            }

            // How many items are still hidden on this shelf.
            if (shelf.hiddenCount > 0) {
                val bx = r.right - slotSize * 0.2f
                val by = r.top + slotSize * 0.2f
                canvas.drawCircle(bx, by, slotSize * 0.17f, badgeBg)
                canvas.drawText("+${shelf.hiddenCount}", bx, by + badgeText.textSize * 0.35f, badgeText)
            }
        }

        // Sparkles where a triple just cleared.
        val it = pops.iterator()
        while (it.hasNext()) {
            val p = it.next()
            val t = (now - p.start) / POP_MS.toFloat()
            if (t >= 1f || p.shelf >= shelfRects.size) { it.remove(); continue }
            val r = shelfRects[p.shelf]
            sparkle.alpha = (255 * (1 - t)).toInt()
            for (k in 0 until 8) {
                val a = k * Math.PI / 4
                val d = slotSize * (0.3f + t * 0.9f)
                canvas.drawCircle(
                    r.centerX() + (d * kotlin.math.cos(a)).toFloat(),
                    r.centerY() + (d * sin(a)).toFloat(),
                    slotSize * 0.06f * (1 - t), sparkle,
                )
            }
            animating = true
        }

        // The dragged item follows the finger, slightly enlarged.
        val item = dragItem
        if (item != null) painter.draw(canvas, item, dragX, dragY - slotSize * 0.3f, slotSize * 0.85f)

        if (animating) postInvalidateOnAnimation()
    }

    private companion object {
        const val POP_MS = 450L
    }
}
