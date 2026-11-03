package com.sathishkumar.games.chaistall.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sathishkumar.games.chaistall.logic.Location
import com.sathishkumar.games.chaistall.logic.Product
import kotlin.math.sin

/**
 * The stall for the selected location: skyline, cart, kettle with steam, the snacks on sale
 * and one figure per helper. Tapping it serves a customer; [onServe] returns the money made.
 */
class StallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var onServe: (() -> String)? = null

    private var location = Location.ROADSIDE
    private var products: List<Product> = listOf(Product.CHAI)
    private var helpers = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()
    private val path = Path()

    private class Floater(val label: String, val x: Float, val y: Float, val start: Long)
    private val floaters = mutableListOf<Floater>()

    fun show(location: Location, products: List<Product>, helpers: Int) {
        if (location == this.location && products == this.products && helpers == this.helpers) return
        this.location = location
        this.products = products
        this.helpers = helpers
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) return true
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val label = onServe?.invoke() ?: return true
            floaters += Floater(label, event.x, event.y, SystemClock.uptimeMillis())
            performClick()
            invalidate()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val now = SystemClock.uptimeMillis()
        val dp = resources.displayMetrics.density

        // Sky and skyline: busier for bigger cities.
        canvas.drawColor(SKY[location.ordinal])
        val buildings = 3 + location.ordinal * 4
        for (i in 0 until buildings) {
            val bw = w / buildings
            val bh = h * (0.25f + ((i * 37) % 10) / 10f * (0.15f + location.ordinal * 0.1f))
            paint.color = Color.argb(70, 30, 20, 60)
            canvas.drawRect(i * bw, h * 0.72f - bh, (i + 1) * bw - 2 * dp, h * 0.72f, paint)
        }

        // Road.
        paint.color = Color.parseColor("#4A4458")
        canvas.drawRect(0f, h * 0.72f, w, h, paint)
        paint.color = Color.parseColor("#F5F0E6")
        for (i in 0 until 6) {
            val x = i * w / 5 - w / 20
            canvas.drawRect(x, h * 0.88f, x + w / 10, h * 0.9f, paint)
        }

        // Cart body.
        val cartL = w * 0.22f
        val cartR = w * 0.78f
        val cartT = h * 0.42f
        val cartB = h * 0.78f
        paint.color = Color.parseColor("#2E7D5B")
        rect.set(cartL, cartT, cartR, cartB)
        canvas.drawRoundRect(rect, 10 * dp, 10 * dp, paint)
        paint.color = Color.parseColor("#F5B700")
        canvas.drawRect(cartL, cartT, cartR, cartT + 6 * dp, paint)

        // Wheels.
        paint.color = Color.parseColor("#222222")
        canvas.drawCircle(cartL + (cartR - cartL) * 0.2f, cartB, h * 0.07f, paint)
        canvas.drawCircle(cartL + (cartR - cartL) * 0.8f, cartB, h * 0.07f, paint)

        // Striped awning on poles.
        paint.color = Color.parseColor("#6B4423")
        canvas.drawRect(cartL + 4 * dp, h * 0.16f, cartL + 8 * dp, cartT, paint)
        canvas.drawRect(cartR - 8 * dp, h * 0.16f, cartR - 4 * dp, cartT, paint)
        val stripes = 8
        for (i in 0 until stripes) {
            paint.color = if (i % 2 == 0) Color.parseColor("#E5484D") else Color.WHITE
            val sw = (cartR - cartL + 24 * dp) / stripes
            val l = cartL - 12 * dp + i * sw
            path.reset()
            path.moveTo(l, h * 0.12f)
            path.lineTo(l + sw, h * 0.12f)
            path.lineTo(l + sw, h * 0.22f)
            path.quadTo(l + sw / 2, h * 0.27f, l, h * 0.22f)
            path.close()
            canvas.drawPath(path, paint)
        }

        // Sign.
        text.color = Color.WHITE
        text.textSize = h * 0.07f
        text.isFakeBoldText = true
        canvas.drawText(location.title, w / 2, cartB - h * 0.05f, text)

        // Kettle with steam.
        val kx = cartL + (cartR - cartL) * 0.15f
        val ky = cartT - h * 0.02f
        paint.color = Color.parseColor("#B0B7C3")
        rect.set(kx - h * 0.06f, ky - h * 0.1f, kx + h * 0.06f, ky)
        canvas.drawRoundRect(rect, 6 * dp, 6 * dp, paint)
        paint.color = Color.WHITE
        for (k in 0 until 3) {
            val t = ((now / 900.0 + k / 3.0) % 1.0).toFloat()
            paint.alpha = (180 * (1 - t)).toInt()
            canvas.drawCircle(kx + sin(t * 6f + k) * 4 * dp, ky - h * 0.12f - t * h * 0.12f, (3 + t * 4) * dp, paint)
        }
        paint.alpha = 255

        // Goods on the counter.
        text.textSize = h * 0.11f
        val start = cartL + (cartR - cartL) * 0.32f
        val span = (cartR - cartL) * 0.6f
        products.forEachIndexed { i, p ->
            val x = start + span * (i + 0.5f) / Product.entries.size
            canvas.drawText(p.emoji, x, cartT - h * 0.015f, text)
        }

        // Helpers stand beside the cart.
        for (i in 0 until helpers) {
            val x = if (i % 2 == 0) cartR + (16 + i * 10) * dp else cartL - (16 + i * 10) * dp
            paint.color = HELPER[i % HELPER.size]
            rect.set(x - 7 * dp, h * 0.56f, x + 7 * dp, h * 0.74f)
            canvas.drawRoundRect(rect, 5 * dp, 5 * dp, paint)
            paint.color = Color.parseColor("#8D5524")
            canvas.drawCircle(x, h * 0.52f, 6 * dp, paint)
        }

        // "+₹" floaters from taps.
        text.textSize = 18 * dp
        text.color = Color.parseColor("#FFE082")
        val it = floaters.iterator()
        while (it.hasNext()) {
            val f = it.next()
            val t = (now - f.start) / 800f
            if (t >= 1f) { it.remove(); continue }
            text.alpha = (255 * (1 - t)).toInt()
            canvas.drawText(f.label, f.x, f.y - t * 60 * dp, text)
        }
        text.alpha = 255

        postInvalidateOnAnimation()
    }

    private companion object {
        val SKY = intArrayOf(
            Color.parseColor("#FFD8A8"), // roadside: warm morning
            Color.parseColor("#A5D8FF"), // Chennai: sea breeze
            Color.parseColor("#FFC9C9"), // Mumbai: evening
            Color.parseColor("#D0BFFF"), // Delhi: dusk
        )
        val HELPER = intArrayOf(Color.parseColor("#1971C2"), Color.parseColor("#C2255C"), Color.parseColor("#2B8A3E"))
    }
}
