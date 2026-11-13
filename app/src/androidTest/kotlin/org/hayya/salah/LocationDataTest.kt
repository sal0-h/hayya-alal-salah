// SPDX-License-Identifier: GPL-3.0-or-later
package org.hayya.salah

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.hayya.core.*
import org.hayya.salah.location.OfflineCities
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationDataTest {
    @Test fun packagedZoneTreeAndFourScriptCityAliasesWorkWithoutNetwork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cities = OfflineCities(context)
        for (query in listOf("Baku", "Bakı", "Баку", "باكو")) {
            val matches = cities.search(query)
            assertTrue("Missing public Baku alias: $query", matches.any { it.name == "Baku" && it.zone == "Asia/Baku" })
        }
        assertEquals("Asia/Baku", ZoneSuggestion.at(GeoPoint(40.4093,49.8671)))
        assertTrue(cities.search("Gəncə").isNotEmpty())
        assertTrue(cities.search("x").isEmpty())
        val permissions = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertFalse("android.permission.INTERNET" in permissions)
        assertFalse("android.permission.ACCESS_BACKGROUND_LOCATION" in permissions)
    }
}
