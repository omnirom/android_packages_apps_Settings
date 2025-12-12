/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.wfd

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.FEATURE_WIFI_DIRECT
import android.media.MediaRouter
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

class WifiDisplayScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_CONNECTED_DEVICES_25Q4

    override val preferenceScreenCreator = WifiDisplayScreen()

    private val mockMediaRouter: MediaRouter = mock()
    private val mockLifecycleContext: PreferenceLifecycleContext = mock()
    private val mockPackageManager: PackageManager = mock()

    // Define an open inner class so Mockito can spy on it.
    open inner class TestContext(base: Context) : ContextWrapper(base) {
        override fun getPackageManager(): PackageManager = mockPackageManager
    }

    private val context: Context = spy(TestContext(appContext))

    @Before
    fun setUp() {
        mockLifecycleContext.stub {
            on { getSystemService(MediaRouter::class.java) } doReturn mockMediaRouter
        }
        context.stub {
            on { getSystemService(Context.MEDIA_ROUTER_SERVICE) } doReturn mockMediaRouter
        }
    }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(WifiDisplayScreen.KEY)
    }

    @Test
    fun title_returnsCorrectResource() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.wifi_display_settings_title)
    }

    @Test
    fun icon_returnsCorrectResource() {
        assertThat(preferenceScreenCreator.icon).isEqualTo(R.drawable.ic_cast_24dp)
    }

    @Test
    fun highlightMenuKey_returnsCorrectResource() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_connected_devices)
    }

    @Test
    fun keywords_returnsCorrectResource() {
        assertThat(preferenceScreenCreator.keywords)
            .isEqualTo(R.string.keywords_display_cast_screen)
    }

    @Test
    fun fragmentClass_returnsWifiDisplaySettings() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(WifiDisplaySettings::class.java)
    }

    @Test
    fun hasCompleteHierarchy_returnsFalse() {
        assertThat(preferenceScreenCreator.hasCompleteHierarchy()).isFalse()
    }

    @Test
    fun getMetricsCategory_returnsCorrectValue() {
        assertThat(preferenceScreenCreator.getMetricsCategory())
            .isEqualTo(SettingsEnums.WFD_WIFI_DISPLAY)
    }

    @Test
    fun getPreferenceHierarchy_returnsEmptyHierarchy() {
        val testScope = CoroutineScope(Dispatchers.Unconfined)

        val hierarchy = preferenceScreenCreator.getPreferenceHierarchy(context, testScope)

        assertThat(hierarchy).isNotNull()
        hierarchy.forEach { fail("Preference hierarchy should be empty") }
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val intent: Intent? = preferenceScreenCreator.getLaunchIntent(context, null)

        assertThat(intent?.component?.className)
            .isEqualTo(Settings.WifiDisplaySettingsActivity::class.java.name)
    }

    @Test
    fun isAvailable_whenConfigIsTrue_returnsTrue() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI_DIRECT) } doReturn true }

        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_whenDisplayServiceIsMissing_returnsFalse() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI_DIRECT) } doReturn true }
        context.stub { on { getSystemService(Context.DISPLAY_SERVICE) } doReturn null }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_whenWifiP2pServiceIsMissing_returnsFalse() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI_DIRECT) } doReturn true }
        context.stub { on { getSystemService(Context.WIFI_P2P_SERVICE) } doReturn null }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_whenFeatureIsMissing_returnsFalse() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI_DIRECT) } doReturn false }

        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_mediaRouterNotAvailable_returnsDisconnected() {
        context.stub { on { getSystemService(Context.MEDIA_ROUTER_SERVICE) } doReturn null }

        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(context.getString(R.string.disconnected))
    }

    @Test
    fun getSummary_noRoutes_returnsDisconnected() {
        // MediaRouter has no routes.
        mockMediaRouter.stub { on { routeCount } doReturn 0 }

        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(context.getString(R.string.disconnected))
    }

    @Test
    fun getSummary_noSelectedRemoteDisplayRoute_returnsDisconnected() {
        // A remote display route exists but is not selected.
        val mockRoute = mock(MediaRouter.RouteInfo::class.java)
        mockRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn false
        }
        mockMediaRouter.stub {
            on { routeCount } doReturn 1
            on { getRouteAt(0) } doReturn mockRoute
        }

        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(context.getString(R.string.disconnected))
    }

    @Test
    fun getSummary_routeIsConnecting_returnsDisconnected() {
        // A remote display route is selected but still connecting.
        val mockRoute = mock(MediaRouter.RouteInfo::class.java)
        mockRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn true
        }
        mockMediaRouter.stub {
            on { routeCount } doReturn 1
            on { getRouteAt(0) } doReturn mockRoute
        }

        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(context.getString(R.string.disconnected))
    }

    @Test
    fun getSummary_connectedRouteWithStatus_returnsRouteStatus() {
        // Arrange: A fully connected route provides a specific status string.
        val expectedStatus = "Casting to Living Room TV"
        val mockRoute = mock(MediaRouter.RouteInfo::class.java)
        mockRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
            on { status } doReturn expectedStatus
        }
        mockMediaRouter.stub {
            on { routeCount } doReturn 1
            on { getRouteAt(0) } doReturn mockRoute
        }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(expectedStatus)
    }

    @Test
    fun getSummary_connectedRouteWithoutStatus_returnsConnectedString() {
        // Arrange: A fully connected route has no specific status.
        val mockRoute = mock(MediaRouter.RouteInfo::class.java)
        mockRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
            on { status } doReturn null
        }
        mockMediaRouter.stub {
            on { routeCount } doReturn 1
            on { getRouteAt(0) } doReturn mockRoute
        }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(context.getString(R.string.wifi_display_status_connected))
    }

    @Test
    fun getSummary_multipleConnectedRoutes_returnsFirstRouteStatus() {
        // Arrange: A fully connected route has no specific status.
        val expectedStatus = "Casting to Living Room 1"
        val mockRoute1 = mock(MediaRouter.RouteInfo::class.java)
        mockRoute1.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
            on { status } doReturn expectedStatus
        }
        val mockRoute2 = mock(MediaRouter.RouteInfo::class.java)
        mockRoute2.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
            on { status } doReturn "Casting to Living Room 2"
        }
        mockMediaRouter.stub {
            on { routeCount } doReturn 2
            on { getRouteAt(0) } doReturn mockRoute1
            on { getRouteAt(1) } doReturn mockRoute2
        }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(expectedStatus)
    }

    @Test
    fun getSummary_connectedRouteWithEmptyStatus_returnsConnectedString() {
        // Arrange: A fully connected route has an empty string for its status.
        val mockRoute = mock(MediaRouter.RouteInfo::class.java)
        mockRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
            on { status } doReturn ""
        }
        mockMediaRouter.stub {
            on { routeCount } doReturn 1
            on { getRouteAt(0) } doReturn mockRoute
        }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(context.getString(R.string.wifi_display_status_connected))
    }

    @Test
    fun getSummary_findsCorrectRouteAmongstOthers() {
        val expectedStatus = "Correctly casting"
        // Route 1: Not a remote display route
        val nonDisplayRoute = mock(MediaRouter.RouteInfo::class.java)
        nonDisplayRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn false
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
        }
        // Route 2: Remote display, but not selected
        val notSelectedRoute = mock(MediaRouter.RouteInfo::class.java)
        notSelectedRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn false
        }
        // Route 3: Remote display, selected, but connecting
        val connectingRoute = mock(MediaRouter.RouteInfo::class.java)
        connectingRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn true
        }
        // Route 4: The correct, connected remote display route
        val correctRoute = mock(MediaRouter.RouteInfo::class.java)
        correctRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
            on { status } doReturn expectedStatus
        }
        // Route 5: Another connected route that should be ignored due to the 'break'
        val anotherConnectedRoute = mock(MediaRouter.RouteInfo::class.java)
        anotherConnectedRoute.stub {
            on { matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) } doReturn true
            on { isSelected } doReturn true
            on { isConnecting } doReturn false
            on { status } doReturn "Should not see this"
        }
        mockMediaRouter.stub {
            on { routeCount } doReturn 5
            on { getRouteAt(0) } doReturn nonDisplayRoute
            on { getRouteAt(1) } doReturn notSelectedRoute
            on { getRouteAt(2) } doReturn connectingRoute
            on { getRouteAt(3) } doReturn correctRoute
            on { getRouteAt(4) } doReturn anotherConnectedRoute
        }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        val summary = preferenceScreenCreator.getSummary(context)

        assertThat(summary).isEqualTo(expectedStatus)
    }

    @Test
    override fun migration() {
        mockPackageManager.stub { on { hasSystemFeature(FEATURE_WIFI_DIRECT) } doReturn true }
        super.migration()
    }
}
