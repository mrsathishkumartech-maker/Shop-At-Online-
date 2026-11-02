package com.sathishkumar.games.core

import android.view.HapticFeedbackConstants
import android.view.View

object Haptics {
    fun tap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun error(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}
