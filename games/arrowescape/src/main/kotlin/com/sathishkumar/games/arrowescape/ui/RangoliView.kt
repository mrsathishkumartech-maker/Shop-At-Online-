package com.sathishkumar.games.arrowescape.ui

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import com.sathishkumar.games.arrowescape.logic.RangoliDesign
import com.sathishkumar.games.arrowescape.logic.RangoliDesigner

/** A square view showing one rangoli. Used in the win dialog and the collection book. */
class RangoliView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val painter = RangoliPainter()
    private var design: RangoliDesign? = null

    fun setSeed(seed: Long) {
        design = RangoliDesigner.design(seed)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val d = design ?: return
        val r = minOf(width, height) / 2f
        painter.draw(canvas, width / 2f, height / 2f, r * 0.95f, d)
    }
}
