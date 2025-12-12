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

package com.android.settings.accessibility.colorcorrection.ui

import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.colorcorrection.ui.IntensityPreference.Companion.DEFAULT_SATURATION_LEVEL
import com.android.settings.accessibility.colorcorrection.ui.IntensityPreference.Companion.SATURATION_MAX
import com.android.settings.accessibility.colorcorrection.ui.IntensityPreference.Companion.SATURATION_MIN
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SliderPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

/** Tests for [IntensityPreference]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class IntensityPreferenceTest {
    @get:Rule val settingStoreRule = SettingsStoreRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = IntensityPreference(context)

    @Before
    fun setUp() {
        ShadowSystemClock.reset()
    }

    @After
    fun cleanUp() {
        ShadowSystemClock.reset()
    }

    @Test
    fun bindWidget_verifyMinMaxDefaultValue() {
        val widget = preference.createAndBindWidget<SliderPreference>(context)

        assertThat(widget.min).isEqualTo(SATURATION_MIN)
        assertThat(widget.max).isEqualTo(SATURATION_MAX)
        assertThat(widget.value).isEqualTo(DEFAULT_SATURATION_LEVEL)
    }

    @Test
    fun isEnabled_colorCorrectionOff_returnsFalse() {
        setColorCorrectionSetting(
            enabled = false,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isEnabled_colorCorrectionOn_grayScaleMode_returnsFalse() {
        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_SIMULATE_MONOCHROMACY,
        )

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isEnabled_colorCorrectionOn_modeDisabled_returnFalse() {
        setColorCorrectionSetting(enabled = true, mode = AccessibilityManager.DALTONIZER_DISABLED)

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isEnabled_colorCorrectionOn_modeEnabled_notGrayScaleMode_returnTrue() {
        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun setSliderPosition_min_settingsUpdated() {
        val widget = preference.createAndBindWidget<SliderPreference>(context)
        widget.value = SATURATION_MIN

        assertThat(
                SettingsSecureStore.get(context)
                    .getInt(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL)
            )
            .isEqualTo(SATURATION_MIN)
    }

    @Test
    fun setSliderPosition_max_settingsUpdated() {
        val widget = preference.createAndBindWidget<SliderPreference>(context)
        widget.value = SATURATION_MAX

        assertThat(
                SettingsSecureStore.get(context)
                    .getInt(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL)
            )
            .isEqualTo(SATURATION_MAX)
    }

    @Test
    fun setSliderPosition_tooLarge_settingsNotUpdated() {
        SettingsSecureStore.get(context)
            .setInt(
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                SATURATION_MAX,
            )

        val widget = preference.createAndBindWidget<SliderPreference>(context)
        widget.value = SATURATION_MAX + 1

        assertThat(
                SettingsSecureStore.get(context)
                    .getInt(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL)
            )
            .isEqualTo(SATURATION_MAX)
    }

    @Test
    fun setSliderPosition_tooSmall_settingsNotUpdated() {
        SettingsSecureStore.get(context)
            .setInt(
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                SATURATION_MIN,
            )

        val widget = preference.createAndBindWidget<SliderPreference>(context)
        widget.value = SATURATION_MIN - 1

        assertThat(
                SettingsSecureStore.get(context)
                    .getInt(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL)
            )
            .isEqualTo(SATURATION_MIN)
    }

    @Test
    fun setSliderPosition_inRange_settingsUpdated() {
        SettingsSecureStore.get(context)
            .setInt(
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL,
                SATURATION_MIN,
            )

        val widget = preference.createAndBindWidget<SliderPreference>(context)
        widget.value = 5

        assertThat(
                SettingsSecureStore.get(context)
                    .getInt(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_SATURATION_LEVEL)
            )
            .isEqualTo(5)
    }

    @Test
    fun turnOffColorCorrection_disableIntensityWidget() = runTest {
        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )
        val widget = preference.createAndBindWidget<SliderPreference>(context)
        assertThat(widget.isEnabled).isTrue()
        preference.onStart(getPreferenceLifecycleContext(widget, preference, this))

        setColorCorrectionSetting(
            enabled = false,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )
        ShadowLooper.idleMainLooper()
        advanceUntilIdle()
        triggerFrame()

        assertThat(widget.isEnabled).isFalse()
    }

    @Test
    fun turnOnColorCorrection_enableIntensityWidget() = runTest {
        setColorCorrectionSetting(
            enabled = false,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )
        val widget = preference.createAndBindWidget<SliderPreference>(context)
        assertThat(widget.isEnabled).isFalse()
        preference.onStart(getPreferenceLifecycleContext(widget, preference, this))

        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )
        ShadowLooper.idleMainLooper()
        advanceUntilIdle()
        triggerFrame()

        assertThat(widget.isEnabled).isTrue()
    }

    @Test
    fun turnOffColorCorrectionAfterOnStop_widgetEnableStateNotChanged() = runTest {
        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )
        val widget = preference.createAndBindWidget<SliderPreference>(context)
        assertThat(widget.isEnabled).isTrue()
        val lifecycleContext = getPreferenceLifecycleContext(widget, preference, this)
        preference.onStart(lifecycleContext)
        preference.onStop(lifecycleContext)

        setColorCorrectionSetting(
            enabled = false,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )
        ShadowLooper.idleMainLooper()
        advanceUntilIdle()
        triggerFrame()

        verify(lifecycleContext, never()).notifyPreferenceChange(any())
        assertThat(widget.isEnabled).isTrue()
    }

    @Test
    fun chooseGrayScaleMode_colorCorrectionOn_disableIntensityWidget() = runTest {
        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )
        val widget = preference.createAndBindWidget<SliderPreference>(context)
        assertThat(widget.isEnabled).isTrue()
        val lifecycleContext = getPreferenceLifecycleContext(widget, preference, this)
        preference.onStart(lifecycleContext)

        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_SIMULATE_MONOCHROMACY,
        )
        ShadowLooper.idleMainLooper()
        advanceUntilIdle()
        triggerFrame()

        assertThat(widget.isEnabled).isFalse()
    }

    @Test
    fun getSummary_colorCorrectionOff_returnUnavailableSummary() {
        setColorCorrectionSetting(
            enabled = false,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )

        assertThat(preference.getSummary(context))
            .isEqualTo(context.getString(R.string.daltonizer_saturation_unavailable_summary))
    }

    @Test
    fun getSummary_colorCorrectionOn_returnEmptySummary() {
        setColorCorrectionSetting(
            enabled = true,
            mode = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY,
        )

        assertThat(preference.getSummary(context)).isEqualTo("")
    }

    @Test
    fun getTitle_returnCorrectTitle() {
        assertThat(preference.title).isEqualTo(R.string.daltonizer_saturation_title)
    }

    private fun setColorCorrectionSetting(enabled: Boolean, mode: Int) {
        preference
            .storage(context)
            .setBoolean(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, enabled)
        preference.storage(context).setInt(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, mode)
    }

    private fun getPreferenceLifecycleContext(
        preference: Preference,
        metadata: IntensityPreference,
        coroutineScope: CoroutineScope,
    ): PreferenceLifecycleContext {
        return mock {
            on { lifecycleScope }.thenReturn(coroutineScope)
            on { notifyPreferenceChange(preference.key) }
                .then { metadata.bind(preference, metadata) }
        }
    }

    private fun triggerFrame() {
        ShadowSystemClock.advanceBy(ShadowChoreographer.getFrameDelay())
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
