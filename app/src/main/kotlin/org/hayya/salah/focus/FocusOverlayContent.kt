// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.focus

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.hayya.core.Prayer
import org.hayya.salah.*
import org.hayya.salah.R
import org.hayya.salah.alerts.AlertNotifications

/** Shared native overlay view, with a pinned emergency action and scrollable large text. */
internal fun focusOverlayContent(base: Context, language: String, appearance: AppearancePreferences,
    prayer: Prayer, onRecord: () -> Unit, onBypass: () -> Unit): View {
    val context = DeliveryLocale.context(base,language)
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value*density).toInt()
    val dark = appearance.theme == ThemePreference.DARK || appearance.theme == ThemePreference.SYSTEM &&
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    val backgroundColor = if (dark) 0xff101d26.toInt() else 0xfff8fafc.toInt()
    val foreground = if (dark) 0xffe4edf2.toInt() else 0xff172d3a.toInt()
    val accent = when (appearance.palette) {
        Palette.TEAL -> if (dark) 0xff76c9cf.toInt() else 0xff086d79.toInt()
        Palette.INK -> if (dark) 0xffa6c8ff.toInt() else 0xff204d86.toInt()
        Palette.PLUM -> if (dark) 0xffe5aadc.toInt() else 0xff743768.toInt()
    }
    val arabic = context.resources.configuration.locales[0].language == "ar"
    val font = ResourcesCompat.getFont(context,if (arabic) R.font.plex_arabic_regular else R.font.plex_sans_regular)
    val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8),dp(16),dp(8),dp(24)) }
    fun text(value: String, size: Float) = TextView(context).apply {
        text = value; textSize = size; typeface = font; setTextColor(foreground)
        setPadding(0,dp(6),0,dp(12)); textDirection = View.TEXT_DIRECTION_FIRST_STRONG
        if (size >= 24) ViewCompat.setAccessibilityHeading(this,true)
        body.addView(this,LinearLayout.LayoutParams(-1,-2))
    }
    text(context.getString(R.string.prayer_focus),28f)
    text(AlertNotifications.prayerName(context,prayer),24f)
    text(context.getString(R.string.focus_block_explanation),18f)
    fun button(label: Int, action: () -> Unit) = Button(context).apply {
        text = context.getString(label); typeface = font; isAllCaps = false; minHeight = dp(56)
        setTextColor(accent); setPadding(dp(12),dp(12),dp(12),dp(12)); stateListAnimator = null
        background = GradientDrawable().apply { setColor(Color.TRANSPARENT); cornerRadius = dp(4).toFloat(); setStroke(dp(1).coerceAtLeast(1),accent) }
        setOnClickListener { action() }
    }
    body.addView(button(R.string.record_prayer,onRecord),LinearLayout.LayoutParams(-1,-2))
    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor)
        layoutDirection = context.resources.configuration.layoutDirection
        setPadding(dp(16),dp(16),dp(16),dp(16))
    }
    root.addView(ScrollView(context).apply { isFillViewport = true; addView(body) },LinearLayout.LayoutParams(-1,0,1f))
    root.addView(button(R.string.emergency_bypass,onBypass),LinearLayout.LayoutParams(-1,-2))
    ViewCompat.setOnApplyWindowInsetsListener(root) { view,insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(bars.left+dp(16),bars.top+dp(16),bars.right+dp(16),bars.bottom+dp(16)); insets
    }
    ViewCompat.setAccessibilityPaneTitle(root,context.getString(R.string.prayer_focus))
    return root
}
