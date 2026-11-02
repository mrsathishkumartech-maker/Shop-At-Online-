package com.sathishkumar.games.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class AdPolicyTest {
    @Test
    fun noInterstitialsUpToLevelTen_thenEveryThirdLevel() {
        val policy = AdPolicy()
        val shown = (1..22).filter { policy.shouldShowInterstitial(it) }
        assertEquals(listOf(13, 16, 19, 22), shown)
    }
}
