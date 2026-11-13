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
    private val boundary = Runnable { refresh() }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = java.lang.ref.WeakReference(this)
        scope.launch {
            try {
                combine(appContainer.settings.preferences, appContainer.repository.changes) { prefs, records -> prefs to records }.collect { (prefs, records) ->
                    config = prefs; history = records; refresh()
                }
            } catch (_: Exception) { hide(); handler.removeCallbacks(boundary) }
        }
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val name = event.packageName?.toString() ?: return
        // Our overlay itself may emit window events; an explicit Open action hides it first.
        if (name == packageName && overlay != null) return
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
            val window = focusWindow(schedules, prefs.focus.prayers, records.records, records.pauses, now, prefs.focus.bypass)
            val nextBoundary = (schedules.flatMap { it.prayers.values + it.sunrise } + listOfNotNull(window?.expiresAt)).filter { it > now }.minOrNull()
            nextBoundary?.let { handler.postDelayed(boundary, Duration.between(now, it).toMillis().coerceAtLeast(1)) }
            if (foreground !in prefs.focus.packages || foreground in EssentialApps.excluded(this@PrayerFocusService) || window == null) hide()
            else show(window)
        }
    }
    private fun show(window: FocusWindow) {
        if (displayed == window && overlay != null && displayedLanguage == config?.language) return
        hide()
        val strings = DeliveryLocale.context(this, config?.language)
        val density = resources.displayMetrics.density
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding((24*density).toInt(), (48*density).toInt(), (24*density).toInt(), (48*density).toInt())
            setBackgroundColor(Color.rgb(16,29,38)); importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        fun text(value: String, size: Float) = TextView(this).apply {
            text = value; textSize = size; setTextColor(Color.rgb(228,237,242)); setPadding(0, 12, 0, 20)
            layout.addView(this, LinearLayout.LayoutParams(-1, -2))
        }
        text(strings.getString(R.string.prayer_focus), 28f)
        text(org.hayya.salah.alerts.AlertNotifications.prayerName(this, window.target.prayer), 24f)
        text(strings.getString(R.string.focus_block_explanation), 18f)
        val log = Button(this).apply {
            text = strings.getString(R.string.record_prayer); minHeight = (56*density).toInt()
            setOnClickListener {
                this@PrayerFocusService.foreground = packageName; hide()
                startActivity(Intent(this@PrayerFocusService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("serviceDate", window.target.date.toString()).putExtra("prayer", window.target.prayer.name))
            }
        }
        layout.addView(log, LinearLayout.LayoutParams(-1, -2))
        val bypass = Button(this).apply {
            text = strings.getString(R.string.emergency_bypass); minHeight = (56*density).toInt()
            setOnClickListener {
                // Hide only after the explicit bypass has persisted. No prayer record is written.
                isEnabled = false
                scope.launch {
                    try { appContainer.settings.update { it.copy(focus = it.focus.copy(bypass = FocusBypass(window.target, window.expiresAt))) }; hide() }
                    catch (_: Exception) { isEnabled = true }
                }
            }
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(16,29,38))
            setPadding((16*density).toInt(), (24*density).toInt(), (16*density).toInt(), (24*density).toInt())
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(layout) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(bypass, LinearLayout.LayoutParams(-1, -2))
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left + (16*density).toInt(), bars.top + (16*density).toInt(), bars.right + (16*density).toInt(), bars.bottom + (16*density).toInt())
            insets
        }
        try {
            getSystemService(WindowManager::class.java).addView(root, WindowManager.LayoutParams(-1, -1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, android.graphics.PixelFormat.OPAQUE))
            overlay = root; displayed = window; displayedLanguage = config?.language
            androidx.core.view.ViewCompat.setAccessibilityPaneTitle(root, strings.getString(R.string.prayer_focus))
        } catch (_: Exception) { hide() }
    }
    private fun hide() {
        overlay?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }
        overlay = null; displayed = null
    }
    override fun onInterrupt() { hide() }
    override fun onUnbind(intent: Intent?): Boolean { hide(); return super.onUnbind(intent) }
    override fun onDestroy() { hide(); handler.removeCallbacks(boundary); scope.cancel(); if (instance?.get() === this) instance = null; super.onDestroy() }
    companion object {
        private var instance: java.lang.ref.WeakReference<PrayerFocusService>? = null
        fun onTimeChanged() { instance?.get()?.refresh() }
        fun releaseOverlay() { instance?.get()?.hide() }
    }
}
