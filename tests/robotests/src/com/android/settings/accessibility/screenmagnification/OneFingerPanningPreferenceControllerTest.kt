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

package com.android.settings.accessibility.screenmagnification

import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil.State.OFF
import com.android.settings.accessibility.AccessibilityUtil.State.ON
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.screenmagnification.OneFingerPanningPreferenceController.Companion.SETTING_KEY
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class OneFingerPanningPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val prefKey = "prefKey"
    private val capabilitySettingKey = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY
    private val lifeCycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowContentResolver = shadowOf(context.contentResolver)
    private val controller = OneFingerPanningPreferenceController(context, prefKey)
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private val switchPreference = SwitchPreferenceCompat(context).apply { key = prefKey }

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_enable_a11y_magnification_single_panning,
            false,
        )
        setWindowMagnificationSupported(context, /* supported= */ true)
        preferenceScreen.addPreference(switchPreference)
        lifeCycleOwner.lifecycle.addObserver(controller)
        controller.displayPreference(preferenceScreen)
    }

    @After
    fun cleanUp() {
        lifeCycleOwner.lifecycle.removeObserver(controller)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun onResume_verifyRegisterCapabilityObserver() {
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        assertThat(
                shadowContentResolver.getContentObservers(
                    Settings.Secure.getUriFor(capabilitySettingKey)
                )
            )
            .hasSize(1)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun onPause_verifyUnregisterCapabilityObserver() {
        onResume_verifyRegisterCapabilityObserver()
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)

        assertThat(
                shadowContentResolver.getContentObservers(
                    Settings.Secure.getUriFor(capabilitySettingKey)
                )
            )
            .isEmpty()
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
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
    fun updateState_verifyEnablement(@MagnificationMode mode: Int, expectedEnabled: Boolean) {
        MagnificationCapabilities.setCapabilities(context, mode)
        controller.updateState(switchPreference)

        assertThat(switchPreference.isEnabled).isEqualTo(expectedEnabled)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun isChecked_defaultUnChecked() {
        controller.updateState(switchPreference)

        assertThat(controller.isChecked).isFalse()
        assertThat(switchPreference.isChecked).isFalse()
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    @TestParameters(
        value =
            [
                "{oneFingerPanningSettingsEnabled: false, expectedChecked: false}",
                "{oneFingerPanningSettingsEnabled: true, expectedChecked: true}",
            ]
    )
    fun isChecked(oneFingerPanningSettingsEnabled: Boolean, expectedChecked: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            SETTING_KEY,
            if (oneFingerPanningSettingsEnabled) ON else OFF,
        )

        controller.updateState(switchPreference)

        assertThat(controller.isChecked).isEqualTo(expectedChecked)
        assertThat(switchPreference.isChecked).isEqualTo(expectedChecked)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getSummary_switchModeAndSettingsOff_defaultSummaryTextUsed() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)
        Settings.Secure.putInt(context.contentResolver, SETTING_KEY, OFF)

        controller.updateState(switchPreference)

        assertThat(switchPreference.summary.toString()).isEqualTo(defaultSummary())
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getSummary_switchModeAndSettingsOn_defaultSummaryTextUsed() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)
        Settings.Secure.putInt(context.contentResolver, SETTING_KEY, ON)

        controller.updateState(switchPreference)

        assertThat(switchPreference.summary.toString()).isEqualTo(defaultSummary())
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getSummary_windowModeOnly_unavailableSummaryTextUsed() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)

        controller.updateState(switchPreference)

        assertThat(switchPreference.summary.toString()).isEqualTo(unavailableSummary())
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
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
        Settings.Secure.putInt(
            context.contentResolver,
            SETTING_KEY,
            if (settingsEnabled) ON else OFF,
        )
        controller.updateState(switchPreference)

        switchPreference.performClick()

        assertThat(controller.isChecked).isEqualTo(expectedChecked)
        assertThat(switchPreference.isChecked).isEqualTo(expectedChecked)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    @TestParameters(
        "{inSetupWizard: false, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: false, supportWindowMag: true, expectedAvailable: true}",
        "{inSetupWizard: true, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: true, supportWindowMag: true, expectedAvailable: false}",
    )
    fun getAvailability_flagOn(
        inSetupWizard: Boolean,
        supportWindowMag: Boolean,
        expectedAvailable: Boolean,
    ) {
        assertGetAvailability(inSetupWizard, supportWindowMag, expectedAvailable)
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    @TestParameters(
        "{inSetupWizard: false, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: false, supportWindowMag: true, expectedAvailable: false}",
        "{inSetupWizard: true, supportWindowMag: false, expectedAvailable: false}",
        "{inSetupWizard: true, supportWindowMag: true, expectedAvailable: false}",
    )
    fun getAvailability_flagOff(
        inSetupWizard: Boolean,
        supportWindowMag: Boolean,
        expectedAvailable: Boolean,
    ) {
        assertGetAvailability(inSetupWizard, supportWindowMag, expectedAvailable)
    }

    private fun assertGetAvailability(
        inSetupWizard: Boolean,
        supportWindowMag: Boolean,
        expectedAvailable: Boolean,
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
            setWindowMagnificationSupported(activityController.get(), supportWindowMag)

            val preferenceController =
                OneFingerPanningPreferenceController(activityController.get(), prefKey)
            assertThat(preferenceController.availabilityStatus)
                .isEqualTo(if (expectedAvailable) AVAILABLE else CONDITIONALLY_UNAVAILABLE)
        } finally {
            activityController?.destroy()
        }
    }

    private fun defaultSummary(): String? {
        return context.getString(R.string.accessibility_magnification_one_finger_panning_summary)
    }

    private fun unavailableSummary(): String? {
        return context.getString(
            R.string.accessibility_magnification_one_finger_panning_summary_unavailable
        )
    }
}
