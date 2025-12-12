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

package com.android.settings.accessibility.extradim.ui

import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.widget.TextView
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.extradim.data.IntensityDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.testutils.shadow.ShadowColorDisplayManager
import com.android.settingslib.widget.SliderPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

/** Tests for [IntensityPreference]. */
@RunWith(RobolectricTestRunner::class)
class IntensityPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var context: Context
    private lateinit var shadowColorDisplayManager: ShadowColorDisplayManager
    private lateinit var intensityPreference: IntensityPreference

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        intensityPreference = IntensityPreference(context)
        shadowColorDisplayManager =
            Shadow.extract(context.getSystemService(ColorDisplayManager::class.java))
    }

    @Test
    fun getKey_returnsSettingKey() {
        val key = intensityPreference.key
        assertThat(key).isEqualTo(IntensityDataStore.SETTING_KEY)
    }

    @Test
    fun getTitle_returnsIntensityTitle() {
        val title = intensityPreference.title
        assertThat(title).isEqualTo(R.string.reduce_bright_colors_intensity_preference_title)
    }

    @Test
    fun createWidget_returnsSliderPreference() {
        val widget = intensityPreference.createWidget(context)
        assertThat(widget).isInstanceOf(SliderPreference::class.java)
    }

    @Test
    fun createWidget_setsUpdatesContinuouslyToTrue() {
        val widget = intensityPreference.createWidget(context)
        assertThat(widget.updatesContinuously).isTrue()
    }

    @Test
    fun createWidget_setsHapticFeedbackModeToOnEnds() {
        val widget = intensityPreference.createWidget(context)

        val hapticFeedbackModeField: Int = ReflectionHelpers.getField(widget, "mHapticFeedbackMode")

        assertThat(hapticFeedbackModeField).isEqualTo(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS)
    }

    @Test
    fun bindWidget_setsTextStartAndTextEnd() {
        val viewHolder =
            intensityPreference.createAndBindWidget<SliderPreference>(context).inflateViewHolder()
        val textStartView = viewHolder.findViewById(android.R.id.text1) as TextView
        val textEndView = viewHolder.findViewById(android.R.id.text2) as TextView

        assertThat(textStartView.text)
            .isEqualTo(context.getString(R.string.brightness_intensity_start_label))
        assertThat(textEndView.text)
            .isEqualTo(context.getString(R.string.brightness_intensity_end_label))
    }

    @Test
    fun getMinValue_returnMinValue() {
        val minValue = intensityPreference.getMinValue(context)
        assertThat(minValue).isEqualTo(IntensityDataStore.MIN_VALUE)
    }

    @Test
    fun getMaxValue_returnMaxValue() {
        val minValue = intensityPreference.getMaxValue(context)
        assertThat(minValue).isEqualTo(IntensityDataStore.MAX_VALUE)
    }

    @Test
    fun isEnabled_extraDimOn_returnTrue() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = true

        assertThat(intensityPreference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_extraDimOff_returnFalse() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = false

        assertThat(intensityPreference.isEnabled(context)).isFalse()
    }

    @Test
    fun onCreate_extraDimTurnedOff_widgetBecomesDisabled() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = true
        val widget = intensityPreference.createAndBindWidget<SliderPreference>(context)
        val lifecycleContext = getPreferenceLifecycleContext(widget, intensityPreference)
        intensityPreference.onCreate(lifecycleContext)
        assertThat(widget.isEnabled).isTrue()

        shadowColorDisplayManager.isReduceBrightColorsActivated = false
        SettingsSecureStore.get(context).notifyChange(PreferenceChangeReason.STATE)

        assertThat(widget.isEnabled).isFalse()
    }

    @Test
    fun onCreate_extraDimTurnedOn_widgetBecomesEnabled() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = false
        val widget = intensityPreference.createAndBindWidget<SliderPreference>(context)
        val lifecycleContext = getPreferenceLifecycleContext(widget, intensityPreference)
        intensityPreference.onCreate(lifecycleContext)
        assertThat(widget.isEnabled).isFalse()

        shadowColorDisplayManager.isReduceBrightColorsActivated = true
        SettingsSecureStore.get(context).notifyChange(PreferenceChangeReason.STATE)

        assertThat(widget.isEnabled).isTrue()
    }

    @Test
    fun onDestroy_observerRemoved() {
        val widget = intensityPreference.createAndBindWidget<SliderPreference>(context)
        val lifecycleContext = getPreferenceLifecycleContext(widget, intensityPreference)

        intensityPreference.onCreate(lifecycleContext)
        assertThat(SettingsSecureStore.get(context).hasAnyObserver()).isTrue()

        intensityPreference.onDestroy(lifecycleContext)
        assertThat(SettingsSecureStore.get(context).hasAnyObserver()).isFalse()
    }

    @Test
    fun getReadPermissions_returnsSettingsSecureStoreReadPermissions() {
        assertThat(intensityPreference.getReadPermissions(context))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsSettingsSecureStoreWritePermissions() {
        assertThat(intensityPreference.getWritePermissions(context))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(intensityPreference.getReadPermit(context, 0, 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(intensityPreference.getWritePermit(context, 0, 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    private fun getPreferenceLifecycleContext(
        preference: Preference,
        metadata: IntensityPreference,
    ): PreferenceLifecycleContext {
        return mock {
            on { notifyPreferenceChange(preference.key) }
                .then { metadata.bind(preference, metadata) }
        }
    }
}
