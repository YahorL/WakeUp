package com.wakeup.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.wakeup.R
import com.wakeup.di.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlarmService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Pause/resume the ringtone while the user is actively working a mission. These arrive
        // while the service is already foreground, so we just adjust the player and return.
        when (intent?.action) {
            AlarmContract.ACTION_PAUSE_SOUND -> {
                player?.takeIf { it.isPlaying }?.pause()
                return START_STICKY
            }
            AlarmContract.ACTION_RESUME_SOUND -> {
                player?.let { if (!it.isPlaying) it.start() }
                return START_STICKY
            }
        }

        val alarmId = intent?.alarmId ?: -1L
        val snoozesUsed = intent?.snoozesUsed ?: 0
        if (alarmId < 0) { stopSelf(); return START_NOT_STICKY }

        acquireWakeLock()
        val notification = buildNotification(alarmId, snoozesUsed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(AlarmContract.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(AlarmContract.NOTIFICATION_ID, notification)
        }
        scope.launch { ringFor(alarmId) }
        return START_STICKY
    }

    private suspend fun ringFor(alarmId: Long) {
        val alarm = Graph.repository.getAlarm(alarmId)
        val configured = alarm?.soundUri?.let { Uri.parse(it) }
        val default = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        // Try the configured ringtone, then fall back to the default if it can't be played (spec §10).
        player = buildPlayer(configured) ?: buildPlayer(default)
        player?.apply {
            if (alarm?.gradualVolume == true) setVolume(0f, 0f)
            start()
        }
        if (alarm?.vibrate != false) startVibration()
        if (alarm?.gradualVolume == true) rampVolume(alarm.rampSeconds)
    }

    /** Builds and prepares a looping alarm MediaPlayer for [uri], or null if it can't be prepared. */
    private fun buildPlayer(uri: Uri?): MediaPlayer? {
        if (uri == null) return null
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
                )
                isLooping = true
                setDataSource(this@AlarmService, uri)
                prepare()
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun rampVolume(seconds: Int) {
        val steps = 20
        for (i in 1..steps) {
            val v = i / steps.toFloat()
            player?.setVolume(v, v)
            delay((seconds * 1000L) / steps)
        }
    }

    private fun startVibration() {
        vibrator = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).also {
            val pattern = longArrayOf(0, 500, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION") it.vibrate(pattern, 0)
            }
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "wakeup:alarm").apply { acquire(10 * 60 * 1000L) }
    }

    private fun buildNotification(alarmId: Long, snoozesUsed: Int): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(AlarmContract.NOTIFICATION_CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH)
                    .apply { setBypassDnd(true); setSound(null, null) }
            )
        }
        val fsPending = PendingIntent.getActivity(
            this, alarmId.toInt(), AlarmActivity.launchIntent(this, alarmId, snoozesUsed),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, AlarmContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Alarm")
            .setContentText("Tap to wake up")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(fsPending) // tapping returns to the wake-up screen
            .setFullScreenIntent(fsPending, true)
            .build()
    }

    override fun onDestroy() {
        player?.run { stop(); release() }; player = null
        vibrator?.cancel(); vibrator = null
        wakeLock?.takeIf { it.isHeld }?.release(); wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun stop(context: Context) = context.stopService(Intent(context, AlarmService::class.java))

        fun pauseSound(context: Context) = send(context, AlarmContract.ACTION_PAUSE_SOUND)

        fun resumeSound(context: Context) = send(context, AlarmContract.ACTION_RESUME_SOUND)

        private fun send(context: Context, action: String) =
            context.startService(Intent(context, AlarmService::class.java).setAction(action))
    }
}
