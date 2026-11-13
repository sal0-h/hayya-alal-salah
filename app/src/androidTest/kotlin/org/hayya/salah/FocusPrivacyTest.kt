// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.hayya.salah.focus.EssentialApps
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusPrivacyTest {
    @Test fun installedServiceHasNoContentKeysGesturesOrScreenshotCapabilities() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val service = context.getSystemService(AccessibilityManager::class.java).installedAccessibilityServiceList
            .single { it.resolveInfo.serviceInfo.packageName == context.packageName }
        val prohibited = AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT or
            AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS or AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES
        assertEquals(0, service.capabilities and prohibited)
        if (android.os.Build.VERSION.SDK_INT >= 30) assertEquals(0, service.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT)
        assertEquals(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED, service.eventTypes)
        val excluded = EssentialApps.excluded(context)
        assertTrue(context.packageName in excluded)
        assertTrue("com.android.systemui" in excluded)
        assertTrue("com.android.phone" in excluded)
        assertTrue(EssentialApps.selectable(context).none { it.first in excluded })
    }
}
