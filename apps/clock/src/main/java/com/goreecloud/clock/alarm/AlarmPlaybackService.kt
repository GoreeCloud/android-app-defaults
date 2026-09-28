package com.goreecloud.clock.alarm

import android.app.AudioManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.goreecloud.clock.ClockApplication
import com.goreecloud.clock.system.NotificationChannels

class AlarmPlaybackService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private var rampStartedAt: Long = 0L
    private var rampDurationSeconds: Int = 0
    private var rampRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensure(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
        val alarm = (application as ClockApplication).alarmStore.get(alarmId)
        if (alarm == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(
            AlarmReceiver.notificationId(alarm.id),
            AlarmNotificationFactory.build(this, alarm),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        startAlarm(alarm)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopAlarm()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAlarm(alarm: Alarm) {
        stopAlarm()

        if (alarm.soundKey != AlarmSound.SILENT) {
            requestAudioFocus()
            mediaPlayer = createPlayer(AlarmSoundCatalog.resolveUri(alarm.soundKey))
                ?: if (alarm.soundKey != AlarmSound.DEFAULT) {
                    createPlayer(AlarmSoundCatalog.resolveUri(AlarmSound.DEFAULT))
                } else {
                    null
                }

            mediaPlayer?.let { player ->
                player.isLooping = true
                rampDurationSeconds = alarm.gradualVolumeSeconds.coerceIn(0, 60)
                rampStartedAt = SystemClock.elapsedRealtime()
                applyRampVolume(player)
                player.start()
                scheduleRamp()
            }
        }

        if (alarm.vibrate) {
            vibrator()?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0L, 500L, 500L, 500L), 0),
            )
        }
    }

    private fun createPlayer(uri: android.net.Uri?): MediaPlayer? {
        if (uri == null) return null
        return runCatching {
            MediaPlayer.create(this, uri, null, audioAttributes, 0)
        }.getOrNull()
    }

    private fun requestAudioFocus() {
        val audioManager = getSystemService(AudioManager::class.java)
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(audioAttributes)
            .setAcceptsDelayedFocusGain(false)
            .setOnAudioFocusChangeListener { change ->
                if (change == AudioManager.AUDIOFOCUS_LOSS) stopSelf()
            }
            .build()
        focusRequest = request
        runCatching { audioManager.requestAudioFocus(request) }
    }

    private fun scheduleRamp() {
        if (rampDurationSeconds <= 0 || mediaPlayer == null) return
        val task = object : Runnable {
            override fun run() {
                val player = mediaPlayer ?: return
                applyRampVolume(player)
                val current = AlarmVolumeRamp.volumeAt(
                    SystemClock.elapsedRealtime() - rampStartedAt,
                    rampDurationSeconds,
                )
                if (current < 1f) handler.postDelayed(this, RAMP_INTERVAL_MILLIS)
            }
        }
        rampRunnable = task
        handler.postDelayed(task, RAMP_INTERVAL_MILLIS)
    }

    private fun applyRampVolume(player: MediaPlayer) {
        val volume = AlarmVolumeRamp.volumeAt(
            SystemClock.elapsedRealtime() - rampStartedAt,
            rampDurationSeconds,
        )
        player.setVolume(volume, volume)
    }

    private fun stopAlarm() {
        rampRunnable?.let(handler::removeCallbacks)
        rampRunnable = null
        runCatching { mediaPlayer?.stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator()?.cancel()

        val audioManager = getSystemService(AudioManager::class.java)
        focusRequest?.let { runCatching { audioManager.abandonAudioFocusRequest(it) } }
        focusRequest = null
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

    companion object {
        private const val ACTION_START = "com.goreecloud.clock.START_ALARM_PLAYBACK"
        private const val RAMP_INTERVAL_MILLIS = 500L

        fun start(context: Context, alarmId: Long): Boolean = try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmPlaybackService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId),
            )
            true
        } catch (_: RuntimeException) {
            false
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmPlaybackService::class.java))
        }
    }
}
