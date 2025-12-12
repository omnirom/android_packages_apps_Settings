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
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.internal.accessibility.AccessibilityShortcutController.FONT_SIZE_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityQuickSettingUtils
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.TooltipSliderPreference
import com.android.settings.accessibility.textreading.data.FontSizeDataStore
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.R as SettingsLibR
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowApplication

/** Test for [FontSizePreference]. */
@RunWith(RobolectricTestRunner::class)
class FontSizePreferenceTest {
    @get:Rule
    val activityScenario: ActivityScenarioRule<EmptyFragmentActivity> =
        ActivityScenarioRule(EmptyFragmentActivity::class.java)
    @EntryPoint private val entryPoint = EntryPoint.DISPLAY_SETTINGS

    private lateinit var context: Activity
    private lateinit var preference: FontSizePreference
    private lateinit var dataStore: FontSizeDataStore
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var preferenceScreen: PreferenceScreen

    @Before
    fun setUp() {
        activityScenario.scenario.onActivity { activity -> context = activity }
        preference = FontSizePreference(context, entryPoint)
        dataStore = preference.storage(context) as FontSizeDataStore
        preferenceManager = PreferenceManager(context)
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
    }

    @Test
    fun getReadPermission_returnEmptyPermission() {
        assertThat(preference.getReadPermissions(context)).isEqualTo(Permissions.EMPTY)
    }

    @Test
    fun getWritePermissions_needsPermissionsForSystemAndSecureSettings() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(
                Permissions.allOf(
                    Manifest.permission.WRITE_SECURE_SETTINGS,
                    Manifest.permission.WRITE_SETTINGS,
                )
            )
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
    fun getFontSizePreview_initialStateMatchStorageData() {
        assertThat(preference.fontSizePreview.value).isEqualTo(dataStore.fontSizeData.value)
    }

    @Test
    fun verifyWidgetInitialState() {
        val tooltipSliderPreference =
            preference.createAndBindWidget<TooltipSliderPreference>(context, preferenceScreen)

        assertThat(tooltipSliderPreference.title.toString())
            .isEqualTo(context.getString(R.string.title_font_size))
        assertThat(tooltipSliderPreference.summary.toString())
            .isEqualTo(context.getString(R.string.short_summary_font_size))
        assertThat(tooltipSliderPreference.key).isEqualTo(FontSizePreference.KEY)
        assertThat(tooltipSliderPreference.value)
            .isEqualTo(dataStore.fontSizeData.value.currentIndex)
    }

    @Test
    fun verifySliderStateDescription() {
        val valueSize = preference.fontSizePreview.value.values.size
        assumeTrue(
            "Skip the test because there is only one value. We can't change the value.",
            valueSize > 1,
        )

        val sliderPreference =
            preference.createAndBindWidget<TooltipSliderPreference>(context, preferenceScreen)
        val slider =
            sliderPreference.run {
                inflateViewHolder()
                slider!!
            }
        val sliderIndex = slider.value.toInt()
        val initialSliderStateDescription = slider.stateDescription
        val initialValue = preference.fontSizePreview.value.values[sliderIndex]
        val newIndex = (sliderIndex + 1) % valueSize
        assertThat(sliderIndex).isEqualTo(preference.fontSizePreview.value.currentIndex)
        slider.value = newIndex.toFloat()
        val updatedSliderStateDescription = slider.stateDescription
        val updatedValue = preference.fontSizePreview.value.values[newIndex]

        assertThat(initialSliderStateDescription)
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.font_scale_percentage,
                    (initialValue * 100).toInt(),
                )
            )
        assertThat(updatedSliderStateDescription)
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.font_scale_percentage,
                    (updatedValue * 100).toInt(),
                )
            )
    }

    @Test
    fun getKeywords() {
        assertThat(preference.keywords).isEqualTo(R.string.keywords_font_size)
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
            .isEqualTo(dataStore.fontSizeData.value.values.size - 1)
    }

    @Test
    fun verifyFontSizePreviewDataChangesWhenSliderValueIsChanged() {
        val valueSize = preference.fontSizePreview.value.values.size
        assumeTrue(
            "Skip the test because there is only one value. We can't change the value.",
            valueSize > 1,
        )

        val sliderPreference =
            preference.createAndBindWidget<TooltipSliderPreference>(context, preferenceScreen)
        val slider =
            sliderPreference.run {
                inflateViewHolder()
                slider!!
            }
        val sliderIndex = slider.value.toInt()
        val newIndex = (sliderIndex + 1) % valueSize
        assertThat(sliderIndex).isEqualTo(preference.fontSizePreview.value.currentIndex)
        slider.value = newIndex.toFloat()

        assertThat(preference.fontSizePreview.value.currentIndex).isEqualTo(newIndex)
    }

    @Test
    fun verifyChangeFontSize_showTooltipView() {
        verifyFontSizePreviewDataChangesWhenSliderValueIsChanged()

        val shadowApplication: ShadowApplication = shadowOf(context.application)
        assertThat(shadowApplication.latestPopupWindow).isNotNull()
    }

    @Test
    fun verifyChangeFontSize_toolTipShownBefore_dontShowTooltipAgain() {
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(
            context,
            FONT_SIZE_COMPONENT_NAME,
        )

        verifyFontSizePreviewDataChangesWhenSliderValueIsChanged()

        val shadowApplication: ShadowApplication = shadowOf(context.application)
        assertThat(shadowApplication.latestPopupWindow).isNull()
    }
}
