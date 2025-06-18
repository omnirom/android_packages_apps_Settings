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

package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.SubSettings
import com.android.settings.accessibility.ColorInversionPreference.Companion.PREFERENCE_KEY
import com.android.settings.accessibility.ColorInversionPreference.Companion.SETTING_KEY
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class ColorInversionPreferenceTest {
    private val mockLifecycleContext = mock<PreferenceLifecycleContext>()
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    private val colorInversionPreference = ColorInversionPreference()

    @Before
    fun setUp() {
        SettingsSecureStore.get(appContext).setInt(SETTING_KEY, AccessibilityUtil.State.OFF)
    }

    @Test
    fun getIntent_returnColorInversionScreenIntent() {
        val intent = colorInversionPreference.intent(appContext)

        assertThat(intent).isNotNull()
        assertThat(intent!!.action).isEqualTo(Intent.ACTION_MAIN)
        assertThat(intent.component).isEqualTo(ComponentName(appContext, SubSettings::class.java))
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
            ToggleColorInversionPreferenceFragment::class.java.name
        )
        assertThat(
            intent.getIntExtra(
                MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY,
                0
            )
        ).isEqualTo(
            SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION
        )
    }

    @Ignore("b/398023330")
    @Test
    fun onStart_settingChanges_notifyPrefChange() {
        colorInversionPreference.onStart(mockLifecycleContext)
        SettingsSecureStore.get(appContext).setInt(SETTING_KEY, AccessibilityUtil.State.ON)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, times(1)).notifyPreferenceChange(PREFERENCE_KEY)
    }

    @Test
    fun onStop_settingChanges_doNotNotifyPrefChange() {
        colorInversionPreference.onStop(mockLifecycleContext)
        SettingsSecureStore.get(appContext).setInt(SETTING_KEY, AccessibilityUtil.State.ON)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun getSummary_colorInversionOn_verifySummary() {
        SettingsSecureStore.get(appContext).setInt(SETTING_KEY, AccessibilityUtil.State.ON)

        assertThat(colorInversionPreference.getSummary(appContext)).isEqualTo(
            appContext.getText(
                R.string.color_inversion_state_on
            )
        )
    }

    @Test
    fun getSummary_colorInversionOff_verifySummary() {
        SettingsSecureStore.get(appContext).setInt(SETTING_KEY, AccessibilityUtil.State.OFF)

        assertThat(colorInversionPreference.getSummary(appContext)).isEqualTo(
            appContext.getText(
                R.string.color_inversion_state_off
            )
        )
    }
}
