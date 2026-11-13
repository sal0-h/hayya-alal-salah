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
    fun save(context: Context, config: AppPreferences) {
        val appearance = config.appearance
        context.createDeviceProtectedStorageContext().getSharedPreferences("delivery-locale",0).edit {
            putString("language",config.language); putString("theme",appearance.theme.name); putString("palette",appearance.palette.name)
            putString("time",appearance.time.name); putString("numerals",appearance.numerals.name); putBoolean("reducedMotion",appearance.reducedMotion)
        }
    }
    fun appearance(context: Context): AppearancePreferences {
        val prefs = context.createDeviceProtectedStorageContext().getSharedPreferences("delivery-locale",0)
        fun <T : Enum<T>> value(key: String, fallback: T, parse: (String) -> T) = runCatching { parse(prefs.getString(key,fallback.name) ?: fallback.name) }.getOrDefault(fallback)
        return AppearancePreferences(value("theme",ThemePreference.SYSTEM,ThemePreference::valueOf),value("palette",Palette.TEAL,Palette::valueOf),
            value("time",TimePreference.SYSTEM,TimePreference::valueOf),value("numerals",NumeralPreference.SYSTEM,NumeralPreference::valueOf),prefs.getBoolean("reducedMotion",false))
    }
    fun clear(context: Context) { context.createDeviceProtectedStorageContext().getSharedPreferences("delivery-locale",0).edit { clear() } }
    fun context(base: Context, selectedLanguage: String? = null): Context {
        if (Build.VERSION.SDK_INT >= 33) return base // Respect Android's per-app language, including system Settings changes.
        val language = selectedLanguage ?: base.createDeviceProtectedStorageContext().getSharedPreferences("delivery-locale", 0).getString("language", "system")
        if (language == null || language == "system") return base
        val locale = Locale.forLanguageTag(language)
        val configuration = Configuration(base.resources.configuration).apply { setLocales(LocaleList(locale)); setLayoutDirection(locale) }
        return base.createConfigurationContext(configuration)
    }
}
