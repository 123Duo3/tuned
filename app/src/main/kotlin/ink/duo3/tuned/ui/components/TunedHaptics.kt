package ink.duo3.tuned.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.staticCompositionLocalOf

val LocalTunedHapticFeedbackEnabled = staticCompositionLocalOf { true }

fun View.performTunedThresholdHapticFeedback() {
    performHapticFeedback(tunedThresholdHapticFeedbackConstant())
}

private fun tunedThresholdHapticFeedbackConstant(): Int =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            HapticFeedbackConstants.SEGMENT_FREQUENT_TICK

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 ->
            HapticFeedbackConstants.TEXT_HANDLE_MOVE

        else -> HapticFeedbackConstants.CLOCK_TICK
    }
