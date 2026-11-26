// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah.focus

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager

object EssentialApps {
    fun excluded(context: Context): Set<String> = buildSet {
        add(context.packageName)
        addAll(listOf("android", "com.android.systemui", "com.android.settings", "com.android.phone", "com.android.server.telecom",
            "com.android.incallui", "com.android.emergency", "com.android.permissioncontroller", "com.google.android.permissioncontroller",
            "com.samsung.android.incallui", "com.samsung.android.app.telephonyui", "com.samsung.android.dialer"))
        listOf(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), Intent(Intent.ACTION_DIAL),
            Intent(android.provider.Settings.ACTION_SETTINGS), Intent(Intent.ACTION_SENDTO, android.net.Uri.fromParts("sms", "", null))).forEach {
            context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.let(::add)
        }
        context.getSystemService(InputMethodManager::class.java).enabledInputMethodList.forEach { add(it.packageName) }
    }
    fun selectable(context: Context): List<Pair<String, String>> {
        val excluded = excluded(context)
        return context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName !in excluded && it.activityInfo.applicationInfo.uid >= 10000 }
            .map { it.activityInfo.packageName to it.loadLabel(context.packageManager).toString() }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
    }
}
