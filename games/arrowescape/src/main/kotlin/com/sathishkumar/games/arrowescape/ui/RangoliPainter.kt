package com.sathishkumar.games.arrowescape.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.sathishkumar.games.arrowescape.logic.MotifKind
import com.sathishkumar.games.arrowescape.logic.RangoliDesign
import com.sathishkumar.games.arrowescape.logic.RangoliRing

/** Draws a [RangoliDesign] as rings of motifs with chalk-white outlines. */
class RangoliPainter {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val chalk = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        alpha = 220
    }
    private val path = Path()
    private val oval = RectF()

    fun draw(canvas: Canvas, cx: Float, cy: Float, radius: Float, design: RangoliDesign) {
        chalk.strokeWidth = (radius * 0.012f).coerceAtLeast(1f)

        // Base disc and outer dotted border.
        fill.color = Palette.rangoli(design.paletteIndex, 0)
        fill.alpha = 60
        canvas.drawCircle(cx, cy, radius, fill)
        fill.alpha = 255
        val borderDots = design.symmetry * 4
        for (i in 0 until borderDots) {
            canvas.save()
            canvas.rotate(360f / borderDots * i, cx, cy)
            fill.color = Palette.rangoli(design.paletteIndex, i % 2 + 3)
            canvas.drawCircle(cx, cy - radius * 0.97f, radius * 0.022f, fill)
            canvas.restore()
        }

        for (ring in design.rings) {
            fill.color = Palette.rangoli(design.paletteIndex, ring.colorIndex)
            for (k in 0 until design.symmetry) {
                canvas.save()
                canvas.rotate(360f / design.symmetry * k + ring.rotationDegrees, cx, cy)
                drawMotif(canvas, cx, cy - ring.radius * radius, ring.size * radius, ring)
                canvas.restore()
            }
        }

        // Centre flower.
        fill.color = Palette.rangoli(design.paletteIndex, 2)
        canvas.drawCircle(cx, cy, radius * 0.1f, fill)
        canvas.drawCircle(cx, cy, radius * 0.1f, chalk)
        fill.color = Palette.rangoli(design.paletteIndex, 0)
        canvas.drawCircle(cx, cy, radius * 0.045f, fill)
    }

    /** Draws one motif centred at (x, y), pointing away from the centre (towards -y). */
    private fun drawMotif(canvas: Canvas, x: Float, y: Float, s: Float, ring: RangoliRing) {
        when (ring.kind) {
            MotifKind.PETALS -> {
                path.reset()
                path.moveTo(x, y + s)
                path.cubicTo(x + s * 0.9f, y + s * 0.2f, x + s * 0.4f, y - s * 0.7f, x, y - s)
                path.cubicTo(x - s * 0.4f, y - s * 0.7f, x - s * 0.9f, y + s * 0.2f, x, y + s)
                path.close()
                canvas.drawPath(path, fill)
                canvas.drawPath(path, chalk)
            }
            MotifKind.DOTS -> {
                canvas.drawCircle(x, y, s * 0.45f, fill)
                canvas.drawCircle(x, y, s * 0.45f, chalk)
                canvas.drawCircle(x, y - s * 0.75f, s * 0.15f, fill)
            }
            MotifKind.LEAVES -> {
                path.reset()
                path.moveTo(x, y + s)
                path.quadTo(x + s * 0.8f, y, x, y - s)
                path.quadTo(x - s * 0.8f, y, x, y + s)
                path.close()
                canvas.drawPath(path, fill)
                canvas.drawPath(path, chalk)
                canvas.drawLine(x, y + s * 0.8f, x, y - s * 0.8f, chalk)
            }
            MotifKind.SCALLOPS -> {
                oval.set(x - s, y - s * 0.8f, x + s, y + s * 1.2f)
                canvas.drawArc(oval, 180f, 180f, true, fill)
                canvas.drawArc(oval, 180f, 180f, false, chalk)
            }
            MotifKind.DIAMONDS -> {
                path.reset()
                path.moveTo(x, y - s)
                path.lineTo(x + s * 0.55f, y)
                path.lineTo(x, y + s)
                path.lineTo(x - s * 0.55f, y)
                path.close()
                canvas.drawPath(path, fill)
                canvas.drawPath(path, chalk)
                canvas.drawCircle(x, y, s * 0.15f, chalk)
            }
        }
    }
}
