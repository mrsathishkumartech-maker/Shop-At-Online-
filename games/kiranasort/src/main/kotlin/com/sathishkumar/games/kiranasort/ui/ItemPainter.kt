package com.sathishkumar.games.kiranasort.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sathishkumar.games.kiranasort.logic.Item

/** Draws each shop item as a simple flat illustration inside a square of side [size]. */
class ItemPainter {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()
    private val oval = RectF()

    fun draw(canvas: Canvas, item: Item, cx: Float, cy: Float, size: Float, alpha: Int = 255) {
        val r = size / 2f
        fill.alpha = 255
        stroke.alpha = 255
        canvas.saveLayerAlpha(cx - r * 1.2f, cy - r * 1.2f, cx + r * 1.2f, cy + r * 1.2f, alpha)
        when (item) {
            Item.CHAI -> {
                // Clay kulhad cup with tea on top.
                path.reset()
                path.moveTo(cx - r * 0.7f, cy - r * 0.55f)
                path.lineTo(cx + r * 0.7f, cy - r * 0.55f)
                path.lineTo(cx + r * 0.45f, cy + r * 0.8f)
                path.lineTo(cx - r * 0.45f, cy + r * 0.8f)
                path.close()
                fill.color = Color.parseColor("#B5562B"); canvas.drawPath(path, fill)
                oval.set(cx - r * 0.7f, cy - r * 0.75f, cx + r * 0.7f, cy - r * 0.35f)
                fill.color = Color.parseColor("#C98A4B"); canvas.drawOval(oval, fill)
                stroke.color = Color.WHITE; stroke.alpha = 160; stroke.strokeWidth = r * 0.08f
                canvas.drawLine(cx - r * 0.1f, cy - r * 0.95f, cx - r * 0.05f, cy - r * 1.15f, stroke)
                canvas.drawLine(cx + r * 0.2f, cy - r * 0.95f, cx + r * 0.25f, cy - r * 1.15f, stroke)
            }
            Item.MANGO -> {
                canvas.save(); canvas.rotate(-25f, cx, cy)
                oval.set(cx - r * 0.6f, cy - r * 0.8f, cx + r * 0.6f, cy + r * 0.85f)
                fill.color = Color.parseColor("#FFB000"); canvas.drawOval(oval, fill)
                oval.set(cx - r * 0.6f, cy - r * 0.1f, cx + r * 0.2f, cy + r * 0.85f)
                fill.color = Color.parseColor("#FF7A00"); fill.alpha = 120; canvas.drawOval(oval, fill); fill.alpha = 255
                canvas.restore()
                oval.set(cx + r * 0.05f, cy - r * 1.0f, cx + r * 0.75f, cy - r * 0.65f)
                fill.color = Color.parseColor("#2E9E4F"); canvas.drawOval(oval, fill)
            }
            Item.LADDOO -> {
                fill.color = Color.parseColor("#F59A23"); canvas.drawCircle(cx, cy, r * 0.75f, fill)
                fill.color = Color.parseColor("#D9771A")
                for ((dx, dy) in listOf(-0.3f to -0.3f, 0.25f to -0.2f, -0.1f to 0.2f, 0.35f to 0.3f, -0.4f to 0.35f, 0.05f to -0.5f)) {
                    canvas.drawCircle(cx + dx * r, cy + dy * r, r * 0.08f, fill)
                }
                fill.color = Color.parseColor("#FFFFFF"); fill.alpha = 90
                canvas.drawCircle(cx - r * 0.3f, cy - r * 0.35f, r * 0.18f, fill); fill.alpha = 255
            }
            Item.BANGLES -> {
                stroke.strokeWidth = r * 0.16f
                val colors = listOf("#E5484D", "#F5B700", "#30A46C")
                colors.forEachIndexed { i, c ->
                    stroke.color = Color.parseColor(c)
                    val dy = (i - 1) * r * 0.32f
                    oval.set(cx - r * 0.75f, cy + dy - r * 0.3f, cx + r * 0.75f, cy + dy + r * 0.3f)
                    canvas.drawOval(oval, stroke)
                }
            }
            Item.SAMOSA -> {
                path.reset()
                path.moveTo(cx, cy - r * 0.8f)
                path.lineTo(cx + r * 0.85f, cy + r * 0.7f)
                path.lineTo(cx - r * 0.85f, cy + r * 0.7f)
                path.close()
                fill.color = Color.parseColor("#E0A040"); canvas.drawPath(path, fill)
                stroke.color = Color.parseColor("#B07020"); stroke.strokeWidth = r * 0.07f
                canvas.drawLine(cx, cy - r * 0.6f, cx - r * 0.2f, cy + r * 0.65f, stroke)
                canvas.drawLine(cx, cy - r * 0.6f, cx + r * 0.25f, cy + r * 0.65f, stroke)
            }
            Item.JALEBI -> {
                stroke.color = Color.parseColor("#FF8C1A"); stroke.strokeWidth = r * 0.16f
                path.reset()
                var angle = 0.0
                var rad = r * 0.08f
                path.moveTo(cx, cy)
                while (rad < r * 0.75f) {
                    angle += 0.35
                    rad += r * 0.022f
                    path.lineTo(cx + (rad * kotlin.math.cos(angle)).toFloat(), cy + (rad * kotlin.math.sin(angle)).toFloat())
                }
                canvas.drawPath(path, stroke)
            }
            Item.COCONUT -> {
                fill.color = Color.parseColor("#6B4423"); canvas.drawCircle(cx, cy, r * 0.75f, fill)
                fill.color = Color.parseColor("#3D2412")
                canvas.drawCircle(cx - r * 0.2f, cy - r * 0.2f, r * 0.1f, fill)
                canvas.drawCircle(cx + r * 0.2f, cy - r * 0.2f, r * 0.1f, fill)
                canvas.drawCircle(cx, cy + r * 0.12f, r * 0.1f, fill)
            }
            Item.BANANA -> {
                stroke.color = Color.parseColor("#FFD43B"); stroke.strokeWidth = r * 0.36f
                oval.set(cx - r * 0.75f, cy - r * 1.0f, cx + r * 0.75f, cy + r * 0.6f)
                canvas.drawArc(oval, 20f, 140f, false, stroke)
                stroke.color = Color.parseColor("#6B4423"); stroke.strokeWidth = r * 0.14f
                canvas.drawPoint(cx + r * 0.72f, cy - r * 0.05f, stroke)
            }
            Item.CHILLI -> {
                stroke.color = Color.parseColor("#D7263D"); stroke.strokeWidth = r * 0.3f
                oval.set(cx - r * 0.9f, cy - r * 0.9f, cx + r * 0.6f, cy + r * 0.6f)
                canvas.drawArc(oval, 0f, 110f, false, stroke)
                stroke.color = Color.parseColor("#2E9E4F"); stroke.strokeWidth = r * 0.14f
                canvas.drawLine(cx + r * 0.6f, cy - r * 0.15f, cx + r * 0.7f, cy - r * 0.55f, stroke)
            }
            Item.LEMON -> {
                path.reset()
                path.moveTo(cx - r * 0.85f, cy)
                path.quadTo(cx, cy - r * 1.1f, cx + r * 0.85f, cy)
                path.quadTo(cx, cy + r * 1.1f, cx - r * 0.85f, cy)
                path.close()
                fill.color = Color.parseColor("#E8E23A"); canvas.drawPath(path, fill)
                fill.color = Color.WHITE; fill.alpha = 100
                canvas.drawCircle(cx - r * 0.2f, cy - r * 0.2f, r * 0.12f, fill); fill.alpha = 255
            }
        }
        canvas.restore()
    }
}
