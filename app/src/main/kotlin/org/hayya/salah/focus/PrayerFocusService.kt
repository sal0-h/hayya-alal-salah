// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.focus

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.*
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.*
import java.time.Duration
import java.time.Instant

/** Observes package/window metadata only. Never retrieves nodes, text, gestures or keys. */
class PrayerFocusService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var foreground: String? = null
    private var lastWindowEventAt = 0L
    private var config: AppPreferences? = null
    private var history: HistorySnapshot? = null
    private var observation: Job? = null
    private var refreshJob: Job? = null
    private var overlay: android.view.View? = null
    private var displayed: FocusWindow? = null
    private var displayedLanguage: String? = null
    private var displayedAppearance: AppearancePreferences? = null
    private var sessionBypass: FocusBypass? = null
    private val boundary = Runnable { refresh() }
    private val screenAvailable get() = getSystemService(PowerManager::class.java).isInteractive &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked
    private val systemEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                cancelRefresh()
                hide()
            } else refresh()
        }
    }
    private var observingSystemEvents = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        disconnect()
        instance = java.lang.ref.WeakReference(this)
        ContextCompat.registerReceiver(this, systemEvents, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        observingSystemEvents = true
        observation = scope.launch {
            try {
                combine(appContainer.settings.preferences, appContainer.repository.changes) { prefs, records -> prefs to records }.collect { (prefs, records) ->
                    config = prefs; history = records
                    serviceInfo = serviceInfo.apply { eventTypes = if (prefs.focus.enabled) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED else 0 }
                    connection.value = true
                    refresh()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { disconnect() }
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !screenAvailable) return
        val name = event.packageName?.toString() ?: return
        // A pane disappearing can report its old owner after the new window takes focus.
        if (Build.VERSION.SDK_INT >= 28 &&
            event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED != 0) return
        // Own focus comes directly from our Activity. Ignore older external events
        // that are dispatched after a newer window was already observed.
        if (name == packageName || event.eventTime < maxOf(ownFocusedAt, lastWindowEventAt)) return
        lastWindowEventAt = event.eventTime
        foreground = name
        refresh()
    }
    private fun cancelRefresh() {
        refreshJob?.cancel()
        handler.removeCallbacks(boundary)
    }
    private fun refresh() {
        cancelRefresh()
        val prefs = config ?: return hide()
        val records = history ?: return hide()
        val place = prefs.place ?: return hide()
        if (!screenAvailable || ownWindowFocused || !prefs.focus.enabled ||
            foreground !in prefs.focus.packages || foreground in EssentialApps.excluded(this)) return hide()
        val started = Instant.now()
        if (displayed?.let { started < it.startsAt || started >= it.expiresAt } == true) hide()
        val date = started.atZone(place.zone).toLocalDate()
        refreshJob = scope.launch {
            val schedules = withContext(Dispatchers.Default) { (-1L..2L).mapNotNull { day ->
                (LocalPrayerCalculator().calculate(date.plusDays(day), place.point, place.zone, place.calculation) as? CalculationResult.Available)?.schedule
            } }
            val now = Instant.now()
            // DataStore may commit and emit a newer bypass while schedules are being calculated.
            val bypass = sessionBypass?.takeIf { it.expiresAt > now }
                ?: appContainer.settings.preferences.catch { emit(prefs) }.first().focus.bypass
            val window = focusWindow(schedules, prefs.focus.prayers, records.records, records.pauses, now, bypass)
            val localDate = now.atZone(place.zone).toLocalDate()
            val followingDate = localDate.plusDays(1)
            val pauseBoundary = if (records.pauses.any { (localDate in it) != (followingDate in it) })
                followingDate.atStartOfDay(place.zone).toInstant() else null
            val nextBoundary = (schedules.flatMap { it.prayers.values + it.sunrise } +
                listOfNotNull(window?.expiresAt, pauseBoundary)).filter { it > now }.minOrNull()
            nextBoundary?.let { handler.postDelayed(boundary, Duration.between(now, it).toMillis().coerceAtLeast(1)) }
            if (!screenAvailable || ownWindowFocused || foreground !in prefs.focus.packages ||
                foreground in EssentialApps.excluded(this@PrayerFocusService) || window == null) hide()
            else show(window)
        }
    }
    private fun show(window: FocusWindow) {
        if (displayed == window && overlay != null && displayedLanguage == config?.language && displayedAppearance == config?.appearance) return
        hide()
        val root = focusOverlayContent(this,config?.language ?: "system",config?.appearance ?: AppearancePreferences(),window.target.prayer,
            onRecord = {
                foreground = packageName; hide()
                startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("serviceDate",window.target.date.toString()).putExtra("prayer",window.target.prayer.name))
            }, onBypass = {
                val bypass = FocusBypass(window.target,window.expiresAt)
                sessionBypass = bypass
                hide() // Emergency control works immediately, including if persistent storage fails.
                scope.launch {
                    try {
                        appContainer.settings.update { it.copy(focus = it.focus.copy(bypass = bypass)) }
                        if (sessionBypass == bypass) {
                            sessionBypass = null
                            refresh()
                        }
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { /* Keep the immediate escape until its normal expiry. */ }
                }
            })
        try {
            getSystemService(WindowManager::class.java).addView(root, WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.OPAQUE))
            overlay = root; displayed = window; displayedLanguage = config?.language; displayedAppearance = config?.appearance
        } catch (_: Exception) { hide() }
    }
    private fun hide() {
        overlay?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        overlay = null; displayed = null
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig); hide(); refresh()
    }
    override fun onInterrupt() {
        cancelRefresh()
        foreground = null
        lastWindowEventAt = android.os.SystemClock.uptimeMillis()
        hide()
    }
    private fun disconnect() {
        if (instance?.get() === this) {
            connection.value = false
            instance = null
        }
        if (observingSystemEvents) {
            unregisterReceiver(systemEvents)
            observingSystemEvents = false
        }
        observation?.cancel()
        observation = null
        config = null
        history = null
        onInterrupt()
    }
    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }
    override fun onDestroy() {
        disconnect()
        scope.cancel()
        super.onDestroy()
    }
    companion object {
        private val connection = kotlinx.coroutines.flow.MutableStateFlow(false)
        val connected: kotlinx.coroutines.flow.StateFlow<Boolean> = connection
        private var instance: java.lang.ref.WeakReference<PrayerFocusService>? = null
        private var ownWindow: java.lang.ref.WeakReference<android.view.View>? = null
        private var ownFocusedAt = 0L
        private val ownWindowFocused get() = ownWindow?.get()?.hasWindowFocus() == true
        fun onOwnWindowFocus(focused: Boolean, view: android.view.View) {
            if (focused) {
                ownWindow = java.lang.ref.WeakReference(view)
                ownFocusedAt = android.os.SystemClock.uptimeMillis()
                instance?.get()?.let { it.foreground = it.packageName; it.refresh() }
            } else if (ownWindow?.get() === view) {
                ownWindow = null
                instance?.get()?.refresh()
            }
        }
        fun releaseOverlay() { instance?.get()?.let { it.hide(); it.disableSelf() } }
    }
}
