package com.sathishkumar.games.kingdomguard.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sathishkumar.games.kingdomguard.logic.Battle
import com.sathishkumar.games.kingdomguard.logic.BattleEvent
import com.sathishkumar.games.kingdomguard.logic.EnemyKind
import com.sathishkumar.games.kingdomguard.logic.Phase
import com.sathishkumar.games.kingdomguard.logic.Point
import com.sathishkumar.games.kingdomguard.logic.Rank
import com.sathishkumar.games.kingdomguard.logic.Tile
import kotlin.math.floor
import kotlin.math.min

/**
 * Draws the battlefield and runs the fight: each frame advances the [Battle] by the time
 * since the last frame (times [speed]). Drag a soldier onto an identical one to merge,
 * or onto an empty pad to move it. Tap a soldier to see its range.
 */
class BattleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onEvent: ((BattleEvent) -> Unit)? = null
    /** Called whenever gold, lives, soldiers or the phase may have changed. */
    var onChanged: (() -> Unit)? = null
    var speed = 1f
    /** When true the fight is frozen (e.g. a dialog is open). */
    var paused = false

    private var battle: Battle? = null
    private var tile = 0f
    private var ox = 0f
    private var oy = 0f
    private var lastFrame = 0L

    private val dp = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }
    private val rect = RectF()

    private var dragFrom: Int? = null
    private var dragX = 0f
    private var dragY = 0f
    private var selected: Int? = null

    private class Flash(val from: Point, val to: Point, val rank: Rank, val start: Long)
    private class Floater(val at: Point, val label: String, val start: Long)
    private val flashes = mutableListOf<Flash>()
    private val floaters = mutableListOf<Floater>()

    fun bind(battle: Battle) {
        this.battle = battle
        flashes.clear()
        floaters.clear()
        dragFrom = null
        selected = null
        lastFrame = 0L
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = layoutMap()

    private fun layoutMap() {
        val b = battle ?: return
        if (width == 0) return
        tile = min(width / b.map.width.toFloat(), height / b.map.height.toFloat())
        ox = (width - tile * b.map.width) / 2
        oy = (height - tile * b.map.height) / 2
    }

    private fun sx(p: Point) = ox + p.x * tile
    private fun sy(p: Point) = oy + p.y * tile
    private fun padCenter(b: Battle, pad: Int): Point = b.map.pads[pad].let { Point(it.x + 0.5f, it.y + 0.5f) }

    private fun padAt(b: Battle, x: Float, y: Float): Int? {
        val t = Tile(floor((x - ox) / tile).toInt(), floor((y - oy) / tile).toInt())
        return b.map.pads.indexOf(t).takeIf { it >= 0 }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val b = battle ?: return false
        if (b.phase == Phase.GAME_OVER) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val pad = padAt(b, event.x, event.y)
                if (pad == null || b.pads[pad] == null) { selected = null; invalidate(); return true }
                dragFrom = pad
                dragX = event.x; dragY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> { dragX = event.x; dragY = event.y }
            MotionEvent.ACTION_UP -> {
                val from = dragFrom ?: return true
                dragFrom = null
                val to = padAt(b, event.x, event.y)
                if (to == null || to == from) {
                    selected = if (selected == from) null else from
                } else if (b.drag(from, to)) {
                    selected = to
                    performClick()
                    onChanged?.invoke()
                }
            }
            MotionEvent.ACTION_CANCEL -> dragFrom = null
        }
        invalidate()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val b = battle ?: return
        if (tile == 0f) layoutMap()
        if (tile == 0f) return
        val now = SystemClock.uptimeMillis()

        // Advance the fight.
        if (lastFrame != 0L && b.phase == Phase.FIGHT && !paused) {
            val dt = ((now - lastFrame) / 1000f).coerceAtMost(0.05f) * speed
            // Small fixed steps keep fast enemies from skipping past soldiers.
            var left = dt
            while (left > 0f && b.phase == Phase.FIGHT) {
                val step = min(left, 0.02f)
                b.step(step)
                left -= step
            }
            val events = b.drainEvents()
            for (e in events) {
                when (e) {
                    is BattleEvent.Shot -> flashes += Flash(padCenter(b, e.pad), e.target, e.rank, now)
                    is BattleEvent.Kill -> floaters += Floater(e.at, "+${e.gold}", now)
                    else -> Unit
                }
                onEvent?.invoke(e)
            }
            if (events.isNotEmpty()) onChanged?.invoke()
            if (b.phase != Phase.FIGHT) layoutMap()
        }
        lastFrame = now

        drawMap(canvas, b)
        drawRoute(canvas, b)

        // Range of the selected or dragged soldier.
        val showRange = dragFrom ?: selected
        if (showRange != null) b.pads[showRange]?.let { s ->
            val c = padCenter(b, showRange)
            paint.color = Color.argb(45, 255, 255, 255)
            canvas.drawCircle(sx(c), sy(c), s.rank.range * tile, paint)
            stroke.color = Color.argb(160, 255, 255, 255)
            stroke.strokeWidth = 1.5f * dp
            canvas.drawCircle(sx(c), sy(c), s.rank.range * tile, stroke)
        }

        // Merge hints while dragging.
        val dragging = dragFrom?.let { b.pads[it] }
        if (dragging != null) {
            for ((i, s) in b.pads.withIndex()) {
                if (i == dragFrom) continue
                val canMerge = s != null && s.rank == dragging.rank && dragging.rank.next != null
                if (s != null && !canMerge) continue
                val t = b.map.pads[i]
                rect.set(ox + t.x * tile + 2 * dp, oy + t.y * tile + 2 * dp, ox + (t.x + 1) * tile - 2 * dp, oy + (t.y + 1) * tile - 2 * dp)
                stroke.color = if (canMerge) Color.parseColor("#FFD60A") else Color.argb(150, 255, 255, 255)
                stroke.strokeWidth = (if (canMerge) 3f else 1.5f) * dp
                canvas.drawRoundRect(rect, 6 * dp, 6 * dp, stroke)
            }
        }

        // Soldiers.
        for ((i, s) in b.pads.withIndex()) {
            if (s == null || i == dragFrom) continue
            val c = padCenter(b, i)
            drawSoldier(canvas, sx(c), sy(c), s.rank)
        }

        // Enemies.
        for (e in b.enemies) {
            val p = b.route.pointAt(e.distance)
            drawEnemy(canvas, sx(p), sy(p), e.kind, e.hp / e.maxHp)
        }

        // Shots.
        val fi = flashes.iterator()
        while (fi.hasNext()) {
            val f = fi.next()
            val t = (now - f.start) / 140f
            if (t >= 1f) { fi.remove(); continue }
            stroke.color = RANK_COLORS[f.rank.ordinal]
            stroke.alpha = (255 * (1 - t)).toInt()
            stroke.strokeWidth = (2 + f.rank.ordinal * 0.4f) * dp
            canvas.drawLine(sx(f.from), sy(f.from), sx(f.to), sy(f.to), stroke)
            stroke.alpha = 255
        }

        // "+gold" floaters.
        text.textSize = 13 * dp
        text.color = Color.parseColor("#FFD60A")
        val gi = floaters.iterator()
        while (gi.hasNext()) {
            val f = gi.next()
            val t = (now - f.start) / 700f
            if (t >= 1f) { gi.remove(); continue }
            text.alpha = (255 * (1 - t)).toInt()
            canvas.drawText(f.label, sx(f.at), sy(f.at) - t * 24 * dp, text)
        }
        text.alpha = 255

        // The soldier being dragged follows the finger.
        if (dragging != null) drawSoldier(canvas, dragX, dragY - 12 * dp, dragging.rank)

        if (b.phase == Phase.FIGHT || flashes.isNotEmpty() || floaters.isNotEmpty()) postInvalidateOnAnimation()
    }

    private fun drawMap(canvas: Canvas, b: Battle) {
        val m = b.map
        for (y in 0 until m.height) for (x in 0 until m.width) {
            val t = Tile(x, y)
            rect.set(ox + x * tile, oy + y * tile, ox + (x + 1) * tile, oy + (y + 1) * tile)
            paint.color = when (m.charAt(t)) {
                '#' -> if ((x + y) % 2 == 0) Color.parseColor("#5B8C51") else Color.parseColor("#548449")
                'P' -> Color.parseColor("#6B9E5E")
                else -> Color.parseColor("#E9D8A6")
            }
            canvas.drawRect(rect, paint)
            when (m.charAt(t)) {
                '#' -> if ((x * 7 + y * 3) % 5 == 0) { // trees and rocks for texture
                    paint.color = Color.parseColor("#2D6A4F")
                    canvas.drawCircle(rect.centerX(), rect.centerY(), tile * 0.3f, paint)
                } else if ((x * 5 + y * 11) % 7 == 0) {
                    paint.color = Color.parseColor("#8D99AE")
                    canvas.drawCircle(rect.centerX(), rect.centerY() + tile * 0.1f, tile * 0.22f, paint)
                }
                'P' -> {
                    rect.inset(tile * 0.1f, tile * 0.1f)
                    paint.color = Color.parseColor("#B08968")
                    canvas.drawRoundRect(rect, tile * 0.15f, tile * 0.15f, paint)
                    rect.inset(tile * 0.06f, tile * 0.06f)
                    paint.color = Color.parseColor("#C9A27E")
                    canvas.drawRoundRect(rect, tile * 0.12f, tile * 0.12f, paint)
                }
            }
        }
        // Spawn cave and castle.
        val s = m.spawn
        paint.color = Color.parseColor("#3D2C2E")
        rect.set(ox + s.x * tile + tile * 0.1f, oy + s.y * tile + tile * 0.1f, ox + (s.x + 1) * tile - tile * 0.1f, oy + (s.y + 1) * tile)
        canvas.drawArc(rect, 180f, 180f, true, paint)
        val c = m.castle
        val cx = ox + (c.x + 0.5f) * tile
        val cy = oy + (c.y + 0.5f) * tile
        paint.color = Color.parseColor("#ADB5BD")
        canvas.drawRect(cx - tile * 0.9f, cy - tile * 0.35f, cx + tile * 0.9f, cy + tile * 0.5f, paint)
        for (k in -1..1 step 2) canvas.drawRect(cx + k * tile * 0.75f - tile * 0.25f, cy - tile * 0.75f, cx + k * tile * 0.75f + tile * 0.25f, cy + tile * 0.5f, paint)
        paint.color = Color.parseColor("#495057")
        canvas.drawArc(cx - tile * 0.25f, cy - tile * 0.1f, cx + tile * 0.25f, cy + tile * 0.9f, 180f, 180f, true, paint)
        paint.color = Color.parseColor("#E63946")
        canvas.drawRect(cx - tile * 0.04f, cy - tile * 1.1f, cx + tile * 0.04f, cy - tile * 0.6f, paint)
        canvas.drawRect(cx + tile * 0.04f, cy - tile * 1.1f, cx + tile * 0.35f, cy - tile * 0.9f, paint)
    }

    /** Faint arrows along the road so the route the pathfinder chose is visible. */
    private fun drawRoute(canvas: Canvas, b: Battle) {
        val pts = b.route.points
        stroke.color = Color.argb(70, 120, 80, 40)
        stroke.strokeWidth = tile * 0.12f
        for (i in 1 until pts.size) canvas.drawLine(sx(pts[i - 1]), sy(pts[i - 1]), sx(pts[i]), sy(pts[i]), stroke)
    }

    private fun drawSoldier(canvas: Canvas, x: Float, y: Float, rank: Rank) {
        val r = tile * (0.3f + rank.ordinal * 0.012f)
        paint.color = Color.argb(60, 0, 0, 0)
        canvas.drawOval(x - r, y + r * 0.55f, x + r, y + r * 0.95f, paint)
        paint.color = RANK_COLORS[rank.ordinal]
        canvas.drawCircle(x, y, r, paint)
        stroke.color = Color.WHITE
        stroke.strokeWidth = 2 * dp
        canvas.drawCircle(x, y, r, stroke)
        // Helmet plume for higher ranks.
        if (rank.ordinal >= Rank.KNIGHT.ordinal) {
            paint.color = Color.parseColor("#FFD60A")
            canvas.drawCircle(x, y - r, r * 0.28f, paint)
        }
        text.color = Color.WHITE
        text.textSize = r * 1.05f
        canvas.drawText((rank.ordinal + 1).toString(), x, y + text.textSize * 0.36f, text)
    }

    private fun drawEnemy(canvas: Canvas, x: Float, y: Float, kind: EnemyKind, hpFraction: Float) {
        val r = tile * when (kind) {
            EnemyKind.GOBLIN -> 0.2f
            EnemyKind.ORC -> 0.26f
            EnemyKind.TROLL -> 0.33f
            EnemyKind.WARLORD -> 0.45f
        }
        paint.color = when (kind) {
            EnemyKind.GOBLIN -> Color.parseColor("#80B918")
            EnemyKind.ORC -> Color.parseColor("#386641")
            EnemyKind.TROLL -> Color.parseColor("#6C757D")
            EnemyKind.WARLORD -> Color.parseColor("#7209B7")
        }
        canvas.drawCircle(x, y, r, paint)
        paint.color = Color.parseColor("#FFBA08")
        canvas.drawCircle(x - r * 0.35f, y - r * 0.2f, r * 0.16f, paint)
        canvas.drawCircle(x + r * 0.35f, y - r * 0.2f, r * 0.16f, paint)
        if (kind == EnemyKind.WARLORD) {
            paint.color = Color.parseColor("#FFD60A")
            canvas.drawRect(x - r * 0.5f, y - r * 1.25f, x + r * 0.5f, y - r * 0.9f, paint)
        }
        // Health bar.
        val w = r * 2
        rect.set(x - w / 2, y - r - 7 * dp, x + w / 2, y - r - 4 * dp)
        paint.color = Color.argb(160, 0, 0, 0)
        canvas.drawRect(rect, paint)
        rect.right = rect.left + w * hpFraction.coerceIn(0f, 1f)
        paint.color = if (hpFraction > 0.5f) Color.parseColor("#70E000") else if (hpFraction > 0.25f) Color.parseColor("#FFBA08") else Color.parseColor("#E63946")
        canvas.drawRect(rect, paint)
    }

    companion object {
        val RANK_COLORS = intArrayOf(
            Color.parseColor("#8D6E63"), // recruit
            Color.parseColor("#607D8B"), // militia
            Color.parseColor("#1E88E5"), // spearman
            Color.parseColor("#43A047"), // archer
            Color.parseColor("#8E24AA"), // knight
            Color.parseColor("#F4511E"), // captain
            Color.parseColor("#D81B60"), // paladin
            Color.parseColor("#FDD835"), // champion
        )
    }
}
