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

import android.app.TimePickerDialog
import android.app.UiModeManager
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import java.time.LocalTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class DarkModeEndTimePreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mockUiModeManager = mock<UiModeManager>()
    private val preference = EndTimePreference(mockUiModeManager)

    @Before
    fun setUp() {
        mockUiModeManager.stub { on { customNightModeEnd } doReturn LocalTime.of(3, 40) }
        SettingsSecureStore.get(context)
            .setInt(
                DarkModeCustomTimePreference.NIGHT_MODE_CUSTOM_TYPE_SETTING_KEY,
                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE,
            )
    }

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("dark_theme_end_time")
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.night_display_end_time_title)
    }

    @Test
    fun getSummary() {
        assertThat(preference.getSummary(context))
            .isEqualTo(TimeFormatter(context).of(mockUiModeManager.customNightModeEnd))
    }

    @Test
    fun performClick_showDialog_updateNightModeEndWhenClickDialogSaveBtn() {
        val fragmentScenario = launchFragment<Fragment>(initialState = Lifecycle.State.RESUMED)
        fragmentScenario.onFragment { fragment ->
            val mockLifecycleContext =
                mock<PreferenceLifecycleContext>().stub {
                    on { lifecycleOwner } doReturn fragment
                    on { childFragmentManager } doReturn fragment.childFragmentManager
                }
            preference.onCreate(mockLifecycleContext)

            val widget: Preference = preference.createAndBindWidget(context)
            widget.performClick()

            assertDialogShown(fragment, DarkModeTimePicker::class.java)

            reset(mockUiModeManager)
            val timePickerDialog = ShadowDialog.getLatestDialog() as TimePickerDialog
            timePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE).performClick()
            ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            verify(mockUiModeManager).customNightModeEnd = LocalTime.of(3, 40)
            verify(mockLifecycleContext).notifyPreferenceChange(preference.key)
        }
    }

    @Test
    fun isAvailable_nightModeCustom_nightModeCustomTypeSchedule_returnTrue() {
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE
        }

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_nightModeNotCustom_returnFalse() {
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_YES
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE
        }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_customerTypeNotSchedule_returnFalse() {
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_YES
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
        }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun onStart_settingChanges_notifyPrefChange() {
        val mockLifecycleContext = mock<PreferenceLifecycleContext>()

        preference.onStart(mockLifecycleContext)
        SettingsSecureStore.get(context)
            .setInt(
                DarkModeCustomTimePreference.NIGHT_MODE_CUSTOM_TYPE_SETTING_KEY,
                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME,
            )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext).notifyPreferenceChange(EndTimePreference.KEY)
    }

    @Test
    fun onStop_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext = mock<PreferenceLifecycleContext>()

        preference.onStart(mockLifecycleContext)
        preference.onStop(mockLifecycleContext)
        SettingsSecureStore.get(context)
            .setInt(
                DarkModeCustomTimePreference.NIGHT_MODE_CUSTOM_TYPE_SETTING_KEY,
                UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME,
            )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(EndTimePreference.KEY)
    }
}
