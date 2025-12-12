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

package com.android.settings.accessibility.textreading.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.TooltipSliderPreference
import com.android.settings.accessibility.textreading.data.DisplaySizeDataStore
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SliderPreference
import com.google.android.setupcompat.util.WizardManagerHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Test for [DisplaySizePreference]. */
@RunWith(RobolectricTestRunner::class)
class DisplaySizePreferenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @EntryPoint private val entryPoint = EntryPoint.DISPLAY_SETTINGS
    private val preference = DisplaySizePreference(context, entryPoint)
    private val dataStore = preference.storage(context) as DisplaySizeDataStore
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)

    @Test
    fun getReadPermission_returnEmptyPermission() {
        assertThat(preference.getReadPermissions(context)).isEqualTo(Permissions.EMPTY)
    }

    @Test
    fun getWritePermissions_containsWriteSecureSettingsPermission() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(Permissions.allOf(Manifest.permission.WRITE_SECURE_SETTINGS))
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(context = context, callingPid = 0, callingUid = 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(context = context, callingPid = 0, callingUid = 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getSensitivityLevel_returnsNoSensitivity() {
        assertThat(preference.sensitivityLevel).isEqualTo(SensitivityLevel.NO_SENSITIVITY)
    }

    @Test
    fun getDisplaySizePreview_initialStateMatchStorageData() {
        assertThat(preference.displaySizePreview.value).isEqualTo(dataStore.displaySizeData.value)
    }

    @Test
    fun verifyWidgetInitialState() {
        val sliderPreference =
            preference.createAndBindWidget<SliderPreference>(context, preferenceScreen)

        assertThat(sliderPreference.title.toString())
            .isEqualTo(context.getString(R.string.screen_zoom_title))
        assertThat(sliderPreference.summary.toString())
            .isEqualTo(context.getString(R.string.screen_zoom_short_summary))
        assertThat(sliderPreference.key).isEqualTo(DisplaySizePreference.KEY)
        assertThat(sliderPreference.value).isEqualTo(dataStore.displaySizeData.value.currentIndex)
    }

    @Test
    fun createWidget_notInSetupWizard_returnsSliderPreference() {
        val widget = preference.createWidget(context)

        assertThat(widget).isInstanceOf(SliderPreference::class.java)
        assertThat(widget).isNotInstanceOf(TooltipSliderPreference::class.java)
    }

    @Test
    fun createWidget_inSetupWizard_returnsTooltipSliderPreference() {
        val intent = Intent().apply { putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true) }
        val activity = Robolectric.buildActivity(Activity::class.java, intent).setup().get()

        val widget = preference.createWidget(activity)

        assertThat(widget).isInstanceOf(TooltipSliderPreference::class.java)
    }

    @Test
    fun getIncrementStep() {
        assertThat(preference.getIncrementStep(context)).isEqualTo(1)
    }

    @Test
    fun getMinValue() {
        assertThat(preference.getMinValue(context)).isEqualTo(0)
    }

    @Test
    fun getMaxValue() {
        assertThat(preference.getMaxValue(context))
            .isEqualTo(dataStore.displaySizeData.value.values.size - 1)
    }

    @Test
    fun verifyDataStoreValueWontChangeWhileSlidingTheSlider() {
        val valueSize = preference.displaySizePreview.value.values.size
        assumeTrue(
            "Skip the test because there is only one value. We can't change the value",
            valueSize > 1,
        )

        val dataStoreInitialValue = preference.displaySizePreview.value.currentIndex
        val sliderPreference =
            preference.createAndBindWidget<SliderPreference>(context, preferenceScreen)
        val slider =
            sliderPreference.run {
                inflateViewHolder()
                slider!!
            }

        preference.onStartTrackingTouch(slider)
        val newValue = (slider.value + 1) % valueSize
        preference.onValueChange(slider, newValue, true)

        val storage = preference.storage(context) as DisplaySizeDataStore
        assertThat(storage.getInt(preference.key)).isEqualTo(dataStoreInitialValue)
    }

    @Test
    fun verifyDatastoreValueIsUpdatedWhenStopSlidingTheSlider() {
        val valueSize = preference.displaySizePreview.value.values.size
        assumeTrue(
            "Skip the test because there is only one value. We can't change the value",
            valueSize > 1,
        )

        val sliderPreference =
            preference.createAndBindWidget<SliderPreference>(context, preferenceScreen)
        val slider =
            sliderPreference.run {
                inflateViewHolder()
                slider!!
            }

        preference.onStartTrackingTouch(slider)
        val newValue = (slider.value + 1) % valueSize
        slider.value = newValue
        preference.onStopTrackingTouch(slider)

        val storage = preference.storage(context) as DisplaySizeDataStore
        assertThat(storage.getInt(preference.key)).isEqualTo(newValue)
    }

    @Test
    fun verifyDisplaySizePreviewDataChangesWhenOnValueChangeIsCalled() {
        val sliderPreference =
            preference.createAndBindWidget<SliderPreference>(context, preferenceScreen)
        val slider =
            sliderPreference.run {
                inflateViewHolder()
                slider!!
            }
        val sliderIndex = slider.value.toInt()
        assertThat(sliderIndex).isEqualTo(preference.displaySizePreview.value.currentIndex)

        val newIndex = sliderIndex + 1
        preference.onValueChange(slider, newIndex.toFloat(), true)

        assertThat(preference.displaySizePreview.value.currentIndex).isEqualTo(newIndex)
    }
}