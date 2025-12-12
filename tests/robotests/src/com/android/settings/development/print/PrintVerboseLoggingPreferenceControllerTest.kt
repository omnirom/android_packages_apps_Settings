/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.development

import android.content.Context
import android.os.SystemProperties
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.development.PrintVerboseLoggingController.Companion.PRINT_DEBUG_LOG_PROP
import com.android.settings.development.PrintVerboseLoggingController.Companion.PRINT_DEBUG_LOG_PROP_DISABLED
import com.android.settings.development.PrintVerboseLoggingController.Companion.PRINT_DEBUG_LOG_PROP_ENABLED
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.shadows.ShadowSystemProperties

@RunWith(AndroidJUnit4::class)
class PrintVerboseLoggingPreferenceControllerTest {

    @get:Rule val mSetFlagsRule = SetFlagsRule()

    val mContext: Context = ApplicationProvider.getApplicationContext()

    val mController = PrintVerboseLoggingController(mContext)

    val mPreference = mock<SwitchPreference>()

    val mPreferenceScreen = mock<PreferenceScreen>()

    @Before
    fun setup() {
        whenever(mPreferenceScreen.findPreference<Preference>(mController.getPreferenceKey()))
            .thenReturn(mPreference)
        mController.displayPreference(mPreferenceScreen)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun getAvailabilityStatus_unanavailableWhenFlagDisabled() {
        assertThat(mController.isAvailable()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun getAvailabilityStatus_availableWhenFlagEnabled() {
        assertThat(mController.isAvailable()).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun toggleOnSetsProp() {
        ShadowSystemProperties.override(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_DISABLED)
        mController.onPreferenceChange(mPreference, true)
        assertThat(SystemProperties.get(PRINT_DEBUG_LOG_PROP))
            .isEqualTo(PRINT_DEBUG_LOG_PROP_ENABLED)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun toggleOffSetsProp() {
        ShadowSystemProperties.override(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_ENABLED)
        mController.onPreferenceChange(mPreference, false)
        assertThat(SystemProperties.get(PRINT_DEBUG_LOG_PROP))
            .isEqualTo(PRINT_DEBUG_LOG_PROP_DISABLED)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun enabledPropPersistsAsEnabledToggle() {
        ShadowSystemProperties.override(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_ENABLED)
        mController.updateState(mPreference)
        verify(mPreference, times(1)).setChecked(true)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun disabledPropPersistsAsDisabledToggle() {
        ShadowSystemProperties.override(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_DISABLED)
        mController.updateState(mPreference)
        verify(mPreference, times(1)).setChecked(false)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun onPreferenceChangeNullNewValue() {
        // Non-boolean value
        val newValue = mock<Object>()
        assertThat(mController.onPreferenceChange(mPreference, newValue)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun updateStateNullPreference() {
        // Should not throw exception.
        val genericPreference = mock<Preference>()
        mController.updateState(genericPreference)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_PRINT_DEBUG_OPTION)
    fun onDeveloperOptionsSwitchDisabled_disablesVerboseLogging() {
        ShadowSystemProperties.override(PRINT_DEBUG_LOG_PROP, PRINT_DEBUG_LOG_PROP_ENABLED)
        mController.onDeveloperOptionsSwitchDisabled()
        assertThat(SystemProperties.get(PRINT_DEBUG_LOG_PROP))
            .isEqualTo(PRINT_DEBUG_LOG_PROP_DISABLED)
        verify(mPreference, times(1)).setEnabled(false)
    }
}
