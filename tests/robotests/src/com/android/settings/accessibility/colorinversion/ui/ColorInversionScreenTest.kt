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

package com.android.settings.accessibility.colorinversion.ui

import android.app.settings.SettingsEnums
import android.provider.Settings.ACTION_COLOR_INVERSION_SETTINGS
import com.android.settings.R
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ToggleColorInversionPreferenceFragment
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.shadows.ShadowLooper

/** Tests for [ColorInversionScreen]. */
class ColorInversionScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    override val flagName = Flags.FLAG_CATALYST_COLOR_INVERSION
    override val preferenceScreenCreator = ColorInversionScreen()

    @Before
    fun setUp() {
        SettingsSecureStore.get(appContext).setBoolean(ColorInversionScreen.SETTING_KEY, false)
    }

    @Test
    fun getLaunchIntent_returnColorInversionScreenIntent() {
        val prefKey = "fakePrefKey"
        val mockPrefMetadata = mock<PreferenceMetadata>() { on { key } doReturn prefKey }
        val launchIntent = preferenceScreenCreator.getLaunchIntent(appContext, mockPrefMetadata)

        assertThat(launchIntent).isNotNull()
        assertThat(launchIntent!!.action).isEqualTo(ACTION_COLOR_INVERSION_SETTINGS)
        assertThat(launchIntent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(prefKey)
    }

    @Test
    fun onCreate_shownAsEntrypoint_settingChanges_notifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "other_screen_key"
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        SettingsSecureStore.get(appContext).setBoolean(ColorInversionScreen.SETTING_KEY, true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, times(1))
            .notifyPreferenceChange(preferenceScreenCreator.bindingKey)
    }

    @Test
    fun onCreate_shownAsContainer_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
            }

        preferenceScreenCreator.onCreate(mockLifecycleContext)
        SettingsSecureStore.get(appContext).setBoolean(ColorInversionScreen.SETTING_KEY, true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun onDestroy_shownAsEntryPoint_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn "other_screen_key"
            }
        preferenceScreenCreator.onCreate(mockLifecycleContext)
        preferenceScreenCreator.onDestroy(mockLifecycleContext)

        SettingsSecureStore.get(appContext).setBoolean(ColorInversionScreen.SETTING_KEY, true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun onDestroy_shownAsContainer_settingChanges_doNotNotifyPrefChange() {
        val mockLifecycleContext =
            mock<PreferenceLifecycleContext> {
                on { preferenceScreenKey } doReturn preferenceScreenCreator.key
            }
        preferenceScreenCreator.onCreate(mockLifecycleContext)
        preferenceScreenCreator.onDestroy(mockLifecycleContext)

        SettingsSecureStore.get(appContext).setBoolean(ColorInversionScreen.SETTING_KEY, true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        verify(mockLifecycleContext, never()).notifyPreferenceChange(any())
    }

    @Test
    fun getSummary_colorInversionOn_verifySummary() {
        SettingsSecureStore.get(appContext).setBoolean(ColorInversionScreen.SETTING_KEY, true)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getText(R.string.color_inversion_state_on))
    }

    @Test
    fun getSummary_colorInversionOff_verifySummary() {
        SettingsSecureStore.get(appContext).setBoolean(ColorInversionScreen.SETTING_KEY, false)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getText(R.string.color_inversion_state_off))
    }

    @Test
    fun getTitle_returnCorrectTitle() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_display_inversion_preference_title)
    }

    @Test
    fun getIcon_returnCorrectIcon() {
        assertThat(preferenceScreenCreator.icon).isEqualTo(R.drawable.ic_color_inversion)
    }

    @Test
    fun getHighlightMenuKey_returnCorrectHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getKeywords_returnCorrectKeywords() {
        assertThat(preferenceScreenCreator.keywords).isEqualTo(R.string.keywords_color_inversion)
    }

    @Test
    fun getMetricsCategory_returnCorrectMetricsCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS)
    }

    @Test
    fun getFragmentClass_returnCorrectFragmentClass() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(ToggleColorInversionPreferenceFragment::class.java)
    }
}
