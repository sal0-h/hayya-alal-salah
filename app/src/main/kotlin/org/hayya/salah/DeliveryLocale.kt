// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** Minimal language configuration available to lock-screen alerts; no credential data. */
object DeliveryLocale {
    fun save(context: Context, language: String) {
        context.createDeviceProtectedStorageContext().getSharedPreferences("delivery-locale", 0).edit { putString("language", language) }
    }
    fun context(base: Context, selectedLanguage: String? = null): Context {
        if (Build.VERSION.SDK_INT >= 33) return base // Respect Android's per-app language, including system Settings changes.
        val language = selectedLanguage ?: base.createDeviceProtectedStorageContext().getSharedPreferences("delivery-locale", 0).getString("language", "system")
        if (language == null || language == "system") return base
        val locale = Locale.forLanguageTag(language)
        val configuration = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)); setLayoutDirection(locale) }
        return base.createConfigurationContext(configuration)
    }
}
