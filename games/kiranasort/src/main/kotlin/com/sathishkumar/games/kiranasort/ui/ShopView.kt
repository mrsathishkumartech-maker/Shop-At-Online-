package com.sathishkumar.games.kiranasort.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.sathishkumar.games.kiranasort.logic.Item
import com.sathishkumar.games.kiranasort.logic.ShopStage

/** A storefront that gets wider and fancier at each [ShopStage]. */
class ShopView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {
    var stage: ShopStage = ShopStage.STREET_STALL
        set(value) { field = value; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        color = Color.WHITE
    }
    private val rect = RectF()
    private val items = ItemPainter()

    override fun onDraw(canvas: Canvas) {
        val level = stage.ordinal // 0..4
        val w = width.toFloat()
        val h = height.toFloat()
        // Ground.
        paint.color = Color.parseColor("#5C5470")
        canvas.drawRect(0f, h * 0.88f, w, h, paint)

        val shopW = w * (0.4f + level * 0.14f)
        val shopH = h * (0.42f + level * 0.09f)
        val left = (w - shopW) / 2f
        val top = h * 0.88f - shopH

        // Building.
        paint.color = Color.parseColor(listOf("#8B5A2B", "#C0803E", "#D9A05B", "#E8D5B7", "#F2F2F2")[level])
        rect.set(left, top, left + shopW, h * 0.88f)
        canvas.drawRect(rect, paint)

        // Striped awning.
        val awningH = shopH * 0.16f
        val stripes = 6 + level * 2
        for (i in 0 until stripes) {
            paint.color = if (i % 2 == 0) Color.parseColor("#E5484D") else Color.WHITE
            val sw = (shopW + 20f) / stripes
            canvas.drawRect(left - 10f + i * sw, top, left - 10f + (i + 1) * sw, top + awningH, paint)
        }

        // Sign.
        paint.color = Color.parseColor("#1B4965")
        val signH = shopH * 0.15f
        rect.set(left + shopW * 0.1f, top - signH - 6f, left + shopW * 0.9f, top - 6f)
        canvas.drawRoundRect(rect, 10f, 10f, paint)
        text.textSize = signH * 0.55f
        canvas.drawText(stage.title, rect.centerX(), rect.centerY() + text.textSize * 0.35f, text)

        // Counter / shelves with goods: more shelves for bigger shops.
        val shelfCount = 1 + level
        val goods = Item.entries
        val innerTop = top + awningH + shopH * 0.06f
        val innerH = h * 0.88f - innerTop - shopH * 0.08f
        val rowH = innerH / shelfCount
        val perRow = 3 + level
        for (row in 0 until shelfCount) {
            val y = innerTop + rowH * (row + 1)
            paint.color = Color.parseColor("#5E3B1A")
            canvas.drawRect(left + shopW * 0.06f, y - 6f, left + shopW * 0.94f, y, paint)
            val size = minOf(rowH * 0.7f, shopW * 0.8f / perRow)
            for (k in 0 until perRow) {
                val cx = left + shopW * 0.1f + (shopW * 0.8f) * (k + 0.5f) / perRow
                items.draw(canvas, goods[(row * perRow + k) % goods.size], cx, y - size * 0.55f, size)
            }
        }
    }
}
