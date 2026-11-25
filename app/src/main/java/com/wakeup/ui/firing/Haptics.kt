package com.wakeup.ui.firing

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/** The three haptic cues the wake-up flow uses. */
internal fun View.tick() = performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

internal fun View.success() = performHapticFeedback(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS,
)

internal fun View.error() = performHapticFeedback(
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS,
)
