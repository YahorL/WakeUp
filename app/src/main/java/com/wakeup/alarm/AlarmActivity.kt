package com.wakeup.alarm

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.wakeup.ui.theme.WakeUpTheme

class AlarmActivity : ComponentActivity() {
    private var alarmId: Long = -1L
    private var snoozesUsed: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showWhenLockedAndTurnScreenOn()
        alarmId = intent.getLongExtra(AlarmContract.EXTRA_ALARM_ID, -1L)
        snoozesUsed = intent.getIntExtra(AlarmContract.EXTRA_SNOOZES_USED, 0)
        setContent { WakeUpTheme { Text("Alarm $alarmId firing") } }
    }

    private fun showWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
