package com.sathishkumar.games.arrowescape.logic

import kotlin.random.Random

enum class MotifKind { PETALS, DOTS, LEAVES, SCALLOPS, DIAMONDS }

/** One ring of the rangoli, at [radius] (0..1 of the full radius). */
data class RangoliRing(
    val kind: MotifKind,
    val radius: Float,
    val size: Float,
    val colorIndex: Int,
    val rotationDegrees: Float,
)

/** A rangoli design: rings of repeated motifs with [symmetry]-fold rotational symmetry. */
data class RangoliDesign(
    val symmetry: Int,
    val paletteIndex: Int,
    val rings: List<RangoliRing>,
)

/** Turns a seed into a rangoli design. Pure, so each level always gets the same rangoli. */
object RangoliDesigner {
    const val PALETTE_COUNT = 6
    const val COLORS_PER_PALETTE = 5

    fun design(seed: Long): RangoliDesign {
        val random = Random(seed)
        val symmetry = listOf(6, 8, 8, 10, 12, 16).random(random)
        val ringCount = 4 + random.nextInt(3)
        val rings = (0 until ringCount).map { i ->
            val radius = 0.92f - i * (0.8f / ringCount)
            RangoliRing(
                kind = MotifKind.entries.random(random),
                radius = radius,
                size = (0.1f + random.nextFloat() * 0.08f).coerceAtMost(radius),
                colorIndex = random.nextInt(COLORS_PER_PALETTE),
                rotationDegrees = if (i % 2 == 0) 0f else 180f / symmetry,
            )
        }
        return RangoliDesign(symmetry, random.nextInt(PALETTE_COUNT), rings)
    }
}
