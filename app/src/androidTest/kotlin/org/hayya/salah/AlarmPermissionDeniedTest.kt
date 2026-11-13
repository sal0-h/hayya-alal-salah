// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.hayya.core.*
import org.hayya.salah.alerts.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class AlarmPermissionDeniedTest {
    @Test fun deniedAccessNeverInstallsAnActiveFajrWakeUp() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val required = InstrumentationRegistry.getArguments().getString("expectDenied") == "true"
        if (!required) assumeTrue("Run scripts/integration_ci.sh to exercise OS denial", !AlertNotifications.ready(context,true))
        assertFalse(AlertNotifications.ready(context,true))
        if (android.os.Build.VERSION.SDK_INT >= 31) assertFalse(AlarmCoordinator.exactAccess(context))
        val app = context.appContainer
        val original = app.settings.preferences.first()
        try {
            app.settings.update { AppPreferences(ManualSetup("Public Baku fixture",GeoPoint(40.4093,49.8671),ZoneId.of("Asia/Baku"),Profile.MWL),
                fajr = FajrPreferences(enabled = true)) }
            AlarmCoordinator.reconcile(context)
            assertTrue(AlarmSnapshot(context).read().none { it.isFajr })
            assertNull(AlarmPlayback.active)
        } finally { app.settings.update { original }; AlarmCoordinator.cancelAll(context) }
    }
}
