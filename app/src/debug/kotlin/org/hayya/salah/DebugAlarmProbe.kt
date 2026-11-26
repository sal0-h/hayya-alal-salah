// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.content.*
import android.media.AudioAttributes
import android.media.AudioManager
import org.hayya.salah.alerts.*
import org.json.JSONObject

/** Shell-only observation for an isolated emulator. Absent from QA/release. No private data. */
class DebugAlarmProbe : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        resultCode = android.app.Activity.RESULT_OK
        resultData = JSONObject().apply {
            put("unlocked",AlarmCoordinator.unlocked(context))
            put("fajrActive",AlarmPlayback.active?.isFajr == true)
            put("bundled",AlarmPlayback.bundledSource)
            put("alarmAudioActive",context.getSystemService(AudioManager::class.java).activePlaybackConfigurations.any { it.audioAttributes.usage == AudioAttributes.USAGE_ALARM })
        }.toString()
    }
}
