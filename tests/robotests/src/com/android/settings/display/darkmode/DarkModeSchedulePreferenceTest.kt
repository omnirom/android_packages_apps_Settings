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
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.location.Location
import android.location.LocationManager
import android.os.PowerManager
import androidx.preference.DropDownPreference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.BedtimeSettingsUtils
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class DarkModeSchedulePreferenceTest {
    private val mockUiModeManager = mock<UiModeManager>()
    private val mockLocationManager = mock<LocationManager>()
    private val context =
        spy(ApplicationProvider.getApplicationContext<Context>()) {
            on { getSystemService(UiModeManager::class.java) } doReturn mockUiModeManager
            on { getSystemService(LocationManager::class.java) } doReturn mockLocationManager
        }
    private val configNightNo = Configuration()
    private val configNightYes = Configuration()
    private val bedtimeActivityInfo = ActivityInfo()
    private val bedtimeSettingsUtils: BedtimeSettingsUtils = BedtimeSettingsUtils(context)
    private val shadowPowerManager = shadowOf(context.getSystemService(PowerManager::class.java))
    private val preference = DarkModeSchedulePreference(mockUiModeManager, BedtimeSettings(context))

    @Before
    fun setUp() {
        configNightNo.uiMode = Configuration.UI_MODE_NIGHT_NO
        configNightYes.uiMode = Configuration.UI_MODE_NIGHT_YES

        mockUiModeManager.stub { on { setNightModeActivated(anyBoolean()) } doReturn true }
        mockLocationManager.stub {
            on { isLocationEnabled } doReturn true
            on { lastLocation } doReturn Location("mock")
        }

        SettingsShadowResources.overrideResource(
            com.android.internal.R.string.config_systemWellbeing,
            "wellbeing",
        )
        context.resources.configuration.updateFrom(configNightNo)
        preference.onCreate(mock<PreferenceLifecycleContext>())
    }

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("dark_ui_auto_mode")
    }

    @Test
    fun keyWords() {
        assertThat(preference.keywords).isEqualTo(R.string.keywords_dark_ui_mode)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title).isEqualTo(R.string.dark_ui_auto_mode_title)
    }

    @Test
    @TestParameters(value = ["{powerSaveModeEnabled: false}", "{powerSaveModeEnabled: true}"])
    fun isEnabled(powerSaveModeEnabled: Boolean) {
        shadowPowerManager!!.setIsPowerSaveMode(powerSaveModeEnabled)

        assertThat(preference.isEnabled(context)).isEqualTo(!powerSaveModeEnabled)
    }

    @Test
    fun bind_nightMode_dropDownValueChangedToNone() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_YES }

        val widget = preference.createAndBindWidget<DropDownPreference>(context)

        assertThat(widget.value).isEqualTo(context.getString(R.string.dark_ui_auto_mode_never))
    }

    @Test
    fun bind_nightModeAuto_dropDownValueChangedToAuto() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_AUTO }

        val widget = preference.createAndBindWidget<DropDownPreference>(context)

        assertThat(widget.value).isEqualTo(context.getString(R.string.dark_ui_auto_mode_auto))
    }

    @Test
    fun bind_nightModeCustom_dropDownValueChangedToCustom() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM }

        val widget = preference.createAndBindWidget<DropDownPreference>(context)

        assertThat(widget.value).isEqualTo(context.getString(R.string.dark_ui_auto_mode_custom))
    }

    @Test
    fun bind_nightModeCustom_bedtimeNotInstalled_dropDownValueChangedToCustom() {
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
        }

        val widget = preference.createAndBindWidget<DropDownPreference>(context)

        assertThat(widget.value).isEqualTo(context.getString(R.string.dark_ui_auto_mode_custom))
    }

    @Test
    fun bind_nightModeCustom_bedtimeDisabled_dropDownValueChangedToCustom() {
        bedtimeSettingsUtils.installBedtimeSettings(
            "wellbeing", /* wellbeingPackage */
            false, /* enabled */
        )
        val preference = DarkModeSchedulePreference(mockUiModeManager, BedtimeSettings(context))
        preference.onCreate(mock<PreferenceLifecycleContext>())
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
        }

        val widget = preference.createAndBindWidget<DropDownPreference>(context)

        assertThat(widget.value).isEqualTo(context.getString(R.string.dark_ui_auto_mode_custom))
    }

    @Test
    fun bind_nightModeCustom_bedtimeEnabled_dropDownValueChangedToCustomBedtime() {
        bedtimeSettingsUtils.installBedtimeSettings(
            "wellbeing", /* wellbeingPackage */
            true, /* enabled */
        )
        mockUiModeManager.stub {
            on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM
            on { nightModeCustomType } doReturn UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
        }
        bedtimeActivityInfo.enabled = true

        val widget = preference.createAndBindWidget<DropDownPreference>(context)

        assertThat(widget.value)
            .isEqualTo(
                context.getString(com.android.settings.R.string.dark_ui_auto_mode_custom_bedtime)
            )
    }

    @Test
    fun onPreferenceChange_sameValue_returnFalse() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_AUTO }

        assertThat(
                preference.onPreferenceChange(
                    preference.createAndBindWidget<DropDownPreference>(context),
                    context.getString(R.string.dark_ui_auto_mode_auto),
                )
            )
            .isFalse()
    }

    @Test
    fun onPreferenceChange_dropDownValueIsNone_configurationIsNightMode_nightModeChangeToYes() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM }
        context.resources.configuration.updateFrom(configNightYes)

        preference.onPreferenceChange(
            preference.createAndBindWidget<DropDownPreference>(context),
            context.getString(R.string.dark_ui_auto_mode_never),
        )

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_YES
    }

    @Test
    fun onPreferenceChange_dropDownValueIsNone_configurationIsNonNightMode_nightModeChangeToNo() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM }
        context.resources.configuration.updateFrom(configNightNo)

        preference.onPreferenceChange(
            preference.createAndBindWidget<DropDownPreference>(context),
            context.getString(R.string.dark_ui_auto_mode_never),
        )

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_NO
    }

    @Test
    fun onPreferenceChange_dropDownValueIsAuto_nightModeChangeToAuto() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_CUSTOM }

        preference.onPreferenceChange(
            preference.createAndBindWidget<DropDownPreference>(context),
            context.getString(R.string.dark_ui_auto_mode_auto),
        )

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_AUTO
    }

    @Test
    fun onPreferenceChange_dropDownValueIsCustom_nightModeChangeToCustom() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_YES }

        preference.onPreferenceChange(
            preference.createAndBindWidget<DropDownPreference>(context),
            context.getString(R.string.dark_ui_auto_mode_custom),
        )

        verify(mockUiModeManager).nightMode = UiModeManager.MODE_NIGHT_CUSTOM
    }

    @Test
    fun onPreferenceChange_dropDownValueIsBedtime_nightModeCustomTypeChangeToBedtime() {
        mockUiModeManager.stub { on { nightMode } doReturn UiModeManager.MODE_NIGHT_YES }

        preference.onPreferenceChange(
            preference.createAndBindWidget<DropDownPreference>(context),
            context.getString(R.string.dark_ui_auto_mode_custom_bedtime),
        )

        verify(mockUiModeManager).nightModeCustomType = UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME
    }
}
