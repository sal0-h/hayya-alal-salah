// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.hayya.core.GeoPoint
import org.hayya.core.Profile
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/** Synthetic fixtures only. This activity is absent from QA and release APKs. */
class ScreenshotActivity : ComponentActivity() {
    companion object {
        var language = "en"
        var scale = 1f
        var dark = false
    }
    override fun attachBaseContext(base: Context) {
        val configuration = Configuration(base.resources.configuration).apply {
            setLocales(LocaleList(Locale.forLanguageTag(language)))
            setLayoutDirection(Locale.forLanguageTag(language))
            fontScale = scale
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HayyaTheme {
                Surface {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Today(ManualSetup("Baku · Bakı", GeoPoint(40.4093, 49.8671), ZoneId.of("Asia/Baku"), Profile.MWL),
                            Clock.fixed(Instant.parse("2026-11-13T08:00:00Z"), ZoneOffset.UTC))
                    }
                }
            }
        }
    }
}
