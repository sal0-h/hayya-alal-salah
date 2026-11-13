// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.alerts

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.media.*
import android.os.*
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.hayya.salah.appContainer
import org.json.JSONObject

class AlarmPlayback : Service() {
    private var player: MediaPlayer? = null
    private var focus: AudioFocusRequest? = null
    private var audioGranted = false
    private var prepared = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopSelf() }
    private val audio get() = getSystemService(AudioManager::class.java)
    private fun normalAudioAllowed() = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
        getSystemService(NotificationManager::class.java).currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
    private val soundPolicy = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (active?.isFajr == false && (!normalAudioAllowed() || intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY)) stopSelf()
        }
    }
    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(this, soundPolicy, IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        val entry = runCatching { AlarmEntry.fromJson(JSONObject(intent?.getStringExtra("event") ?: "")) }.getOrNull()
            ?: return START_NOT_STICKY.also { stopSelf() }
        val previous = active
        if (previous?.token == entry.token || previous?.isFajr == true && !entry.isFajr) return START_NOT_STICKY
        releasePlayer()
        state.value = entry
        if (!AlertNotifications.ready(this, entry.isFajr) || !entry.isFajr &&
            (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL || getSystemService(NotificationManager::class.java).currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL)) {
            stopSelf(); return START_NOT_STICKY
        }
        val notification = AlertNotifications.build(this, entry, playing = true)
        if (Build.VERSION.SDK_INT >= 29) startForeground(AlertNotifications.PLAYBACK_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(AlertNotifications.PLAYBACK_ID, notification)
        // No overlapping notification with a stale stop action while the foreground copy is visible.
        getSystemService(NotificationManager::class.java).cancel(entry.id, 1)
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, if (entry.isFajr) 10 * 60_000L else 6 * 60_000L)
        serviceScope.launch {
            try {
                val file = if (AlarmCoordinator.unlocked(this@AlarmPlayback) && entry.sound?.startsWith("builtin_") == false)
                    withContext(Dispatchers.IO) {
                        appContainer.repository.snapshot().audio.firstOrNull { it.id == entry.sound }?.let { filesDir.resolve("audio").resolve(it.fileName) }?.takeIf { it.isFile }
                    } else null
                if (active?.token != entry.token) return@launch
                val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(attributes)
                    .setOnAudioFocusChangeListener { change ->
                        when (change) {
                            AudioManager.AUDIOFOCUS_GAIN -> {
                                audioGranted = true
                                if (prepared) { if (active?.isFajr == true || normalAudioAllowed()) player?.start() else stopSelf() }
                            }
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                audioGranted = false; if (prepared) player?.pause()
                            }
                            AudioManager.AUDIOFOCUS_LOSS -> stopSelf()
                        }
                    }.build()
                focus = request
                if (audio.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { stopSelf(); return@launch }
                audioGranted = true
                val media = MediaPlayer()
                player = media
                media.setAudioAttributes(attributes)
                media.setWakeMode(this@AlarmPlayback, PowerManager.PARTIAL_WAKE_LOCK)
                if (file != null) media.setDataSource(file.absolutePath)
                else assets.openFd(when (entry.sound) { "builtin_adhan_2" -> "audio/adhan_2.mp3"; "builtin_fajr" -> "audio/fajr.mp3"; else -> if (entry.isFajr) "audio/fajr.mp3" else "audio/adhan_1.mp3" }).use {
                    media.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
                media.isLooping = entry.isFajr
                media.setOnPreparedListener {
                    prepared = true
                    if (active?.token == entry.token && audioGranted && (entry.isFajr || normalAudioAllowed())) it.start()
                }
                media.setOnCompletionListener { stopSelf() }
                media.setOnErrorListener { _, _, _ -> stopSelf(); true }
                media.prepareAsync()
            } catch (_: Exception) { stopSelf() }
        }
        return START_NOT_STICKY
    }
    private fun releasePlayer() {
        prepared = false; audioGranted = false
        player?.release(); player = null
        focus?.let { audio.abandonAudioFocusRequest(it) }; focus = null
    }
    override fun onDestroy() {
        val entry = active
        unregisterReceiver(soundPolicy)
        state.value = null; serviceScope.cancel(); handler.removeCallbacks(timeout); releasePlayer()
        stopForeground(STOP_FOREGROUND_REMOVE)
        // A finished ordinary Adhan still offers explicit recording. Fajr dismissal never logs.
        if (entry != null && !entry.isFajr && !suppressed.remove(entry.token)) AlertNotifications.show(this, entry)
        super.onDestroy()
    }
    companion object {
        private const val STOP = "org.hayya.salah.STOP_AUDIO"
        private val suppressed = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        private val state = MutableStateFlow<AlarmEntry?>(null)
        val current = state.asStateFlow()
        val active get() = state.value
        fun start(context: Context, entry: AlarmEntry) {
            try { ContextCompat.startForegroundService(context, Intent(context, AlarmPlayback::class.java).putExtra("event", entry.json().toString())) }
            catch (_: IllegalStateException) { AlertNotifications.show(context, entry) }
            catch (_: SecurityException) { AlertNotifications.show(context, entry) }
        }
        fun stop(context: Context) { active?.let { suppressed.add(it.token) }; context.stopService(Intent(context, AlarmPlayback::class.java)) }
        fun stopPending(context: Context): PendingIntent = PendingIntent.getService(context, 0,
            Intent(context, AlarmPlayback::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
