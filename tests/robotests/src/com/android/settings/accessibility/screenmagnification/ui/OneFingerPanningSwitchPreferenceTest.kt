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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class OneFingerPanningSwitchPreferenceTest {
    @get:Rule(order = 0) val settingsStoreRule = SettingsStoreRule()
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = OneFingerPanningSwitchPreference()

    @Test
    fun key() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_magnification_one_finger_panning_title)
    }

    @Test
    fun getReadPermissions_returnsSettingsSecureStoreReadPermissions() {
        assertThat(preference.getReadPermissions(context))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsSettingsSecureStoreWritePermissions() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    @TestParameters(
        customName = "FullScreenMode",
        value = ["{mode: ${MagnificationMode.FULLSCREEN}, expectedEnabled: true}"],
    )
    @TestParameters(
        customName = "WindowMode",
        value = ["{mode: ${MagnificationMode.WINDOW}, expectedEnabled: false}"],
    )
    @TestParameters(
        customName = "AllMode",
        value = ["{mode: ${MagnificationMode.ALL}, expectedEnabled: true}"],
    )
    fun isEnabled(@MagnificationMode mode: Int, expectedEnabled: Boolean) {
        MagnificationCapabilities.setCapabilities(context, mode)

        assertThat(preference.isEnabled(context)).isEqualTo(expectedEnabled)
    }

    @Test
    fun getSummary_windowMode_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(
                    R.string.accessibility_magnification_one_finger_panning_summary_unavailable
                )
            )
    }

    @Test
    fun getSummary_fullScreenModeOnly_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.FULLSCREEN)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_one_finger_panning_summary)
            )
    }

    @Test
    fun getSummary_allMode_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_one_finger_panning_summary)
            )
    }

    @Test
    @TestParameters(
        value =
            [
                "{defaultValue: false, expectedChecked: false}",
                "{defaultValue: true, expectedChecked: true}",
            ]
    )
    fun isChecked_defaultValue(defaultValue: Boolean, expectedChecked: Boolean) {
        Settings.Secure.clearProviderForTest()
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_enable_a11y_magnification_single_panning,
            defaultValue,
        )

        val preferenceWidget = createOneFingerPanningWidget()
        assertThat(preferenceWidget.isChecked).isEqualTo(expectedChecked)
    }

    @Test
    @TestParameters(
        value =
            [
                "{settingsEnabled: false, expectedChecked: true}",
                "{settingsEnabled: true, expectedChecked: false}",
            ]
    )
    fun performClick(settingsEnabled: Boolean, expectedChecked: Boolean) {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)
        getStorage()
            .setBoolean(
                Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED,
                settingsEnabled,
            )
        val preferenceWidget = createOneFingerPanningWidget()
        assertThat(preferenceWidget.isChecked).isEqualTo(settingsEnabled)

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isEqualTo(expectedChecked)
        assertThat(
                getStorage().getBoolean(Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED)
            )
            .isEqualTo(expectedChecked)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    @TestParameters(
        "{inSetupWizard: false, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: false, supportWindowMag: true, expectedAvailable: true}",
        "{inSetupWizard: true, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: true, supportWindowMag: true, expectedAvailable: false}",
    )
    fun isAvailable_flagOn(
        inSetupWizard: Boolean,
        supportWindowMag: Boolean,
        expectedAvailable: Boolean,
    ) {
        assertIsAvailable(inSetupWizard, supportWindowMag, expectedAvailable)
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    @TestParameters(
        "{inSetupWizard: false, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: false, supportWindowMag: true, expectedAvailable: false}",
        "{inSetupWizard: true, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: true, supportWindowMag: true, expectedAvailable: false}",
    )
    fun isAvailable_flagOff(
        inSetupWizard: Boolean,
        supportWindowMag: Boolean,
        expectedAvailable: Boolean,
    ) {
        assertIsAvailable(inSetupWizard, supportWindowMag, expectedAvailable)
    }

    private fun assertIsAvailable(
        inSetupWizard: Boolean,
        windowMagnificationSupported: Boolean,
        expectedAvailability: Boolean,
    ) {
        var activityController: ActivityController<ComponentActivity>? = null
        try {
            activityController =
                ActivityController.of(
                        ComponentActivity(),
                        Intent().apply { putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard) },
                    )
                    .create()
                    .start()
                    .postCreate(null)
                    .resume()
            setWindowMagnificationSupported(context, windowMagnificationSupported)
            assertThat(preference.isAvailable(activityController.get()))
                .isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }

    private fun createOneFingerPanningWidget(): SwitchPreferenceCompat =
        preference.createAndBindWidget<SwitchPreferenceCompat>(context).apply {
            inflateViewHolder()
        }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}
