package com.sathishkumar.games.chaistall.logic

import java.math.BigDecimal
import java.math.RoundingMode

/** Formats rupees the Indian way: ₹99,999 then ₹1.25 L (lakh) then ₹3.4 Cr (crore). */
object Money {
    private const val LAKH = 100_000.0
    private const val CRORE = 10_000_000.0

    fun format(amount: Double): String {
        val a = if (amount.isFinite()) maxOf(0.0, amount) else 0.0
        return when {
            a < LAKH -> "₹" + indianGrouping(a.toLong())
            a < CRORE -> "₹" + decimals(a / LAKH) + " L"
            else -> {
                val crores = a / CRORE
                if (crores < 1000) "₹" + decimals(crores) + " Cr"
                else "₹" + indianGrouping(crores.toLong()) + " Cr"
            }
        }
    }

    /** Income per second, e.g. "₹340/s". */
    fun perSecond(amount: Double): String = format(amount) + "/s"

    private fun decimals(value: Double): String {
        val places = if (value < 10) 2 else if (value < 100) 1 else 0
        return BigDecimal.valueOf(value).setScale(places, RoundingMode.DOWN).stripTrailingZeros().toPlainString()
    }

    /** 1234567 -> "12,34,567". */
    private fun indianGrouping(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val groups = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$groups,$last3"
    }
}
