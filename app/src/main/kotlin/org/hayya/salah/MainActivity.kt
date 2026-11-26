// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import org.hayya.core.Prayer
import org.hayya.core.PrayerKey
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        org.hayya.salah.focus.PrayerFocusService.onOwnWindowFocus(hasFocus,window.decorView)
    }

    private var requested by mutableStateOf<PrayerKey?>(null)
    override fun attachBaseContext(base: Context) {
        val language = base.getSharedPreferences("locale-bootstrap", Context.MODE_PRIVATE).getString("language", "system")
        if (android.os.Build.VERSION.SDK_INT < 33 && language != null && language != "system") {
            val locale = Locale.forLanguageTag(language)
            val config = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)); setLayoutDirection(locale) }
            super.attachBaseContext(base.createConfigurationContext(config))
        } else super.attachBaseContext(base)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readRequest(intent)
        setContent { AppRoot(appContainer, requested) { requested = null } }
    }
    override fun onStart() { super.onStart(); appContainer.refreshIntegrations() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readRequest(intent) }
    private fun readRequest(intent: Intent?) {
        requested = runCatching { PrayerKey(LocalDate.parse(intent?.getStringExtra("serviceDate")),
            Prayer.valueOf(intent?.getStringExtra("prayer") ?: error("No target"))) }.getOrNull()
    }
}
