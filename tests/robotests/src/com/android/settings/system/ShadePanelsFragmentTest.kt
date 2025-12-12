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

package com.android.settings.system

import android.app.Application
import android.content.Context
import android.hardware.devicestate.DeviceState
import android.hardware.devicestate.DeviceStateManager
import android.os.Build
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import com.android.settings.R
import com.android.settings.system.ShadePanelsFragment.Companion.KEY_DUAL_SHADE_PREFERENCE
import com.android.settings.system.ShadePanelsFragment.Companion.KEY_SINGLE_SHADE_PREFERENCE
import com.android.settingslib.preference.PreferenceFragment
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.android.systemui.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.Mockito.`when` as whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.BAKLAVA])
class ShadePanelsFragmentTest {

    @get:Rule
    val setFlagsRule = SetFlagsRule()

    @get:Rule
    val mockitoRule: MockitoRule? = MockitoJUnit.rule()

    @Mock
    private lateinit var mockDeviceStateManager: DeviceStateManager

    private lateinit var context: Context
    private lateinit var fragmentScenario: FragmentScenario<ShadePanelsFragment>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        context = RuntimeEnvironment.getApplication()

        whenever(mockDeviceStateManager.supportedDeviceStates).thenReturn(emptyList<DeviceState>())

        shadowOf(context.applicationContext as Application)
            .setSystemService(Context.DEVICE_STATE_SERVICE, mockDeviceStateManager)
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    fun onRadioButtonClicked_dualShadeSelected_updatesSettingAndRadioButton() {
        launchFragment(dualShadeEnabled = false)
        fragmentScenario.onFragment { fragment ->
            fragment.onAttach(context)
            assertDualShadeEnabled(false)

            val radioButton = checkNotNull(fragment.findRadioButton(KEY_DUAL_SHADE_PREFERENCE))
            assertThat(radioButton.isChecked).isFalse()

            fragment.onRadioButtonClicked(radioButton)

            assertThat(radioButton.isChecked).isTrue()
            assertDualShadeEnabled(true)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    fun onRadioButtonClicked_singleShadeSelected_updatesSettingAndRadioButton() {
        launchFragment(dualShadeEnabled = true)
        fragmentScenario.onFragment { fragment ->
            fragment.onAttach(context)
            assertDualShadeEnabled(true)

            val radioButton = checkNotNull(fragment.findRadioButton(KEY_SINGLE_SHADE_PREFERENCE))
            assertThat(radioButton.isChecked).isFalse()

            fragment.onRadioButtonClicked(radioButton)

            assertThat(radioButton.isChecked).isTrue()
            assertDualShadeEnabled(false)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_SCENE_CONTAINER)
    fun searchIndexProvider_sceneContainerEnabled_isIndexed() {
        val allKeys =
            ShadePanelsFragment.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(
                /* context = */ context,
                /* enabled = */ true
            ).map { it.key }
        val nonIndexableKeys =
            ShadePanelsFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(context)

        assertThat(allKeys).isNotEmpty()
        assertThat(nonIndexableKeys).isEmpty()
    }

    @Test
    @DisableFlags(Flags.FLAG_SCENE_CONTAINER)
    fun searchIndexProvider_sceneContainerDisabled_isNotIndexed() {
        val allKeys =
            ShadePanelsFragment.SEARCH_INDEX_DATA_PROVIDER.getRawDataToIndex(
                /* context = */ context,
                /* enabled = */ true
            ).map { it.key }
        val nonIndexableKeys =
            ShadePanelsFragment.SEARCH_INDEX_DATA_PROVIDER.getNonIndexableKeys(context)

        assertThat(allKeys).isNotEmpty()
        assertThat(nonIndexableKeys).containsAtLeastElementsIn(allKeys)
    }

    private fun assertDualShadeEnabled(isEnabled: Boolean) {
        val contentResolver = context.contentResolver
        val settingValue = Settings.Secure.getInt(contentResolver, Settings.Secure.DUAL_SHADE, -1)
        assertThat(settingValue).isEqualTo(if (isEnabled) 1 else 0)
    }

    private fun launchFragment(dualShadeEnabled: Boolean) {
        // Set the initial setting value.
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.DUAL_SHADE,
            if (dualShadeEnabled) 1 else 0
        )
        fragmentScenario = launchFragmentInContainer(themeResId = R.style.Theme_Panel)
    }

    private fun PreferenceFragment.findRadioButton(key: String): SelectorWithWidgetPreference? {
        return preferenceScreen.findPreference<SelectorWithWidgetPreference>(key)
    }
}
