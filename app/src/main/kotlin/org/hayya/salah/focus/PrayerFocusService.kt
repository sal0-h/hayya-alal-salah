// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.widget.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import org.hayya.core.*
import org.hayya.salah.*
import org.hayya.salah.R
import java.time.Duration
import java.time.Instant

/** Observes package/window metadata only. Never retrieves nodes, text, gestures or keys. */
class PrayerFocusService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var foreground: String? = null
    private var config: AppPreferences? = null
    private var history: HistorySnapshot? = null
    private var refreshJob: Job? = null
    private var overlay: android.view.View? = null
    private var displayed: FocusWindow? = null
    private var displayedLanguage: String? = null
    private var displayedAppearance: AppearancePreferences? = null
    private var sessionBypass: FocusBypass? = null
    private val boundary = Runnable { refresh() }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = java.lang.ref.WeakReference(this)
        connection.value = true
        scope.launch {
            try {
                combine(appContainer.settings.preferences, appContainer.repository.changes) { prefs, records -> prefs to records }.collect { (prefs, records) ->
                    config = prefs; history = records
                    serviceInfo = serviceInfo.apply { eventTypes = if (prefs.focus.enabled) AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOWS_CHANGED else 0 }
                    refresh()
                }
            } catch (_: Exception) { hide(); handler.removeCallbacks(boundary) }
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val name = event.packageName?.toString() ?: return
        // Our own focused window is authoritative over delayed external package events.
        if (ownWindowFocused && name != packageName) return
        // Our overlay itself may emit window events; an explicit Open action hides it first.
        if (name == packageName && event.className?.toString().orEmpty() !in setOf(MainActivity::class.java.name, org.hayya.salah.alerts.RingingActivity::class.java.name)) return
        foreground = name
        refresh()
    }
    private fun refresh() {
        refreshJob?.cancel(); handler.removeCallbacks(boundary)
        val prefs = config ?: return hide()
        val records = history ?: return hide()
        val place = prefs.place ?: return hide()
        if (!prefs.focus.enabled || foreground !in prefs.focus.packages || foreground in EssentialApps.excluded(this)) return hide()
        refreshJob = scope.launch {
            val now = Instant.now()
            val schedules = withContext(Dispatchers.Default) { (-1L..2L).mapNotNull { day ->
                (LocalPrayerCalculator().calculate(now.atZone(place.zone).toLocalDate().plusDays(day), place.point, place.zone, place.calculation) as? CalculationResult.Available)?.schedule
            } }
            val window = focusWindow(schedules, prefs.focus.prayers, records.records, records.pauses, now, sessionBypass?.takeIf { it.expiresAt > now } ?: prefs.focus.bypass)
            val nextBoundary = (schedules.flatMap { it.prayers.values + it.sunrise } + listOfNotNull(window?.expiresAt)).filter { it > now }.minOrNull()
            nextBoundary?.let { handler.postDelayed(boundary, Duration.between(now, it).toMillis().coerceAtLeast(1)) }
            if (foreground !in prefs.focus.packages || foreground in EssentialApps.excluded(this@PrayerFocusService) || window == null) hide()
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
                    runCatching { appContainer.settings.update { it.copy(focus = it.focus.copy(bypass = bypass)) } }
                        .onSuccess { if (sessionBypass == bypass) sessionBypass = null }
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
    override fun onInterrupt() { hide() }
    override fun onUnbind(intent: Intent?): Boolean { connection.value = false; hide(); return super.onUnbind(intent) }
    override fun onDestroy() { connection.value = false; hide(); handler.removeCallbacks(boundary); scope.cancel(); if (instance?.get() === this) instance = null; super.onDestroy() }
    companion object {
        private val connection = kotlinx.coroutines.flow.MutableStateFlow(false)
        val connected: kotlinx.coroutines.flow.StateFlow<Boolean> = connection
        private var instance: java.lang.ref.WeakReference<PrayerFocusService>? = null
        private var ownWindowFocused = false
        fun onOwnWindowFocus(focused: Boolean) {
            ownWindowFocused = focused
            if (focused) instance?.get()?.let { it.foreground = it.packageName; it.refresh() }
        }
        fun onTimeChanged() { instance?.get()?.refresh() }
        fun releaseOverlay() { instance?.get()?.let { it.hide(); it.disableSelf() } }
    }
}
