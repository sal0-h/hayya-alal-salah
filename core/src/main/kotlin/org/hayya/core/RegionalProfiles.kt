// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.core

import java.time.ZoneId

/** Named regional presets to compare, never an inferred religious choice or auto-selection. */
fun regionalProfiles(zone: ZoneId): List<Profile> = when (zone.id) {
    "Asia/Riyadh" -> listOf(Profile.UMM_AL_QURA)
    "Asia/Dubai" -> listOf(Profile.DUBAI)
    "Asia/Qatar" -> listOf(Profile.QATAR)
    "Asia/Kuwait" -> listOf(Profile.KUWAIT)
    "Asia/Karachi" -> listOf(Profile.KARACHI)
    "Africa/Cairo" -> listOf(Profile.EGYPTIAN)
    "Asia/Singapore" -> listOf(Profile.SINGAPORE)
    "Europe/Istanbul", "Asia/Istanbul" -> listOf(Profile.TURKEY)
    "Europe/Paris" -> listOf(Profile.FRANCE)
    "Europe/Moscow" -> listOf(Profile.RUSSIA)
    "Asia/Tehran" -> listOf(Profile.TEHRAN,Profile.JAFARI)
    "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
    "America/Toronto", "America/Vancouver" -> listOf(Profile.ISNA)
    else -> emptyList()
}
