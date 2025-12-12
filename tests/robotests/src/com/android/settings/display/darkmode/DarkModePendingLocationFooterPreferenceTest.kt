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

package com.android.settings.display.darkmode

import android.app.UiModeManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.View
import android.view.accessibility.Flags
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.preference.footer.R as SettingsLibR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class DarkModePendingLocationFooterPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val mockUiModeManager = mock<UiModeManager>()
    private val mockLocationManager = mock<LocationManager>()
    private val context =
        spy(ApplicationProvider.getApplicationContext<Context>()) {
            on { getSystemService(UiModeManager::class.java) } doReturn mockUiModeManager
            on { getSystemService(LocationManager::class.java) } doReturn mockLocationManager
        }
    private val preference = DarkModePendingLocationFooterPreference()

    @Before
    fun setUp() {
        SettingsSecureStore.get(context)
            .setInt(
                DarkModePendingLocationFooterPreference.NIGHT_MODE_SETTING_KEY,
                UiModeManager.MODE_NIGHT_CUSTOM,
            )
    }

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("dark_theme_connection_footer")
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.twilight_mode_pending_location)
    }

    @Test
    fun isAvailable_autoNightModeAndLocationEnabled_returnTrue() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_AUTO }
        mockLocationManager.stub { on { isLocationEnabled } doReturn true }

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_notAutoNightMode_returnFalse() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM }
        mockLocationManager.stub { on { isLocationEnabled } doReturn true }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_locationDisabled_returnFalse() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_AUTO }
        mockLocationManager.stub { on { isLocationEnabled } doReturn false }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_lastLocationNotNull_returnFalse() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_AUTO }
        mockLocationManager.stub {
            on { isLocationEnabled } doReturn true
            on { lastLocation } doReturn Location("mock")
        }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun onStart_settingChanges_notifyPrefChange() {
        val mockLifecycleContext = mock<PreferenceLifecycleContext>()

        preference.onStart(mockLifecycleContext)
        SettingsSecureStore.get(context)
            .setInt(
                DarkModePendingLocationFooterPreference.NIGHT_MODE_SETTING_KEY,
                UiModeManager.MODE_NIGHT_AUTO,
            )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext)
            .notifyPreferenceChange(DarkModePendingLocationFooterPreference.KEY)
    }

    @Test
    fun onStop_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext = mock<PreferenceLifecycleContext>()

        preference.onStart(mockLifecycleContext)
        preference.onStop(mockLifecycleContext)
        SettingsSecureStore.get(context)
            .setInt(
                DarkModePendingLocationFooterPreference.NIGHT_MODE_SETTING_KEY,
                UiModeManager.MODE_NIGHT_AUTO,
            )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never())
            .notifyPreferenceChange(DarkModePendingLocationFooterPreference.KEY)
    }

    @Test
    fun iconIsVisible() {
        val widget = preference.createAndBindWidget<FooterPreference>(context)
        val prefViewHolder = widget.inflateViewHolder()
        val iconView = prefViewHolder.itemView.findViewById<View>(SettingsLibR.id.icon_frame)

        assertThat(iconView).isNotNull()
        assertThat(iconView!!.visibility).isEqualTo(View.VISIBLE)
    }

    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun order() {
        val widget = preference.createAndBindWidget<FooterPreference>(context)

        assertThat(widget.order)
            .isEqualTo(DarkModePreferenceOrderUtil.Order.LOCATION_CONNECTION_FOOTER.value)
    }
}
