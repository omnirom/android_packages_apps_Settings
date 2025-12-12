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
import android.provider.Settings.Secure.AccessibilityMagnificationCursorFollowingMode
import android.view.InputDevice
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.screenmagnification.dialogs.CursorFollowingModeChooser
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/** Tests for [CursorFollowingModePreferenceController] */
@Config(shadows = [ShadowInputDevice::class])
@RunWith(RobolectricTestRunner::class)
class CursorFollowingModePreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var controller: CursorFollowingModePreferenceController
    private val prefKey = "prefKey"
    private val capabilitySettingKey = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY
    private val lifeCycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowContentResolver = shadowOf(context.contentResolver)
    private val preference = Preference(context).apply { key = prefKey }
    private val preferenceManager = PreferenceManager(context)

    @Before
    fun setUp() {
        fragmentScenario = launchFragment<Fragment>(initialState = INITIALIZED)
        fragmentScenario.onFragment { fragment ->
            controller = CursorFollowingModePreferenceController(fragment.requireContext(), prefKey)

            fragment.lifecycle.addObserver(controller)
            controller.setFragmentManager(fragment.childFragmentManager)
        }

        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(preference)
        preferenceManager.setPreferences(preferenceScreen)
        lifeCycleOwner.lifecycle.addObserver(controller)
        controller.displayPreference(preferenceScreen)
    }

    @After
    fun cleanUp() {
        ShadowInputDevice.reset()
    }

    @Test
    fun updateState_windowModeOnly_preferenceBecomesUnavailable() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)

        controller.updateState(preference)
        assertThat(preference.isEnabled).isFalse()
    }

    @Test
    fun updateState_fullscreenModeOnly_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.FULLSCREEN)

        controller.updateState(preference)

        assertThat(preference.isEnabled).isTrue()
    }

    @Test
    fun updateState_switchMode_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)

        controller.updateState(preference)

        assertThat(preference.isEnabled).isTrue()
    }

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

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    fun clickPreference_triggerShowDialog() {
        controller.handlePreferenceTreeClick(preference)

        fragmentScenario.onFragment { fragment ->
            assertDialogShown(fragment, CursorFollowingModeChooser::class.java)
        }
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    fun getAvailabilityStatus_inSetupWizard_returnConditionallyUnavailable() {
        assertGetAvailability(
            inSetupWizard = true,
            hasConnectedMouse = true,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    fun getAvailabilityStatus_flagOff_returnConditionallyUnavailable() {
        assertGetAvailability(
            inSetupWizard = false,
            hasConnectedMouse = true,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    fun getAvailabilityStatus_hasConnectedMouse_returnAvailable() {
        assertGetAvailability(
            inSetupWizard = false,
            hasConnectedMouse = true,
            expectedAvailability = AVAILABLE,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    fun getAvailabilityStatus_noConnectedMouse_returnConditionallyUnavailable() {
        assertGetAvailability(
            inSetupWizard = false,
            hasConnectedMouse = false,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @Test
    fun getSummary_disabled_verifyText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)
        controller.displayPreference(preference.preferenceManager.preferenceScreen)

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_magnification_cursor_following_unavailable_summary
                )
            )
    }

    @Test
    fun getSummary_modeCenter_verifyText() {
        setCursorFollowingMode(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_center)
            )
    }

    @Test
    fun getSummary_modeEdge_verifyText() {
        setCursorFollowingMode(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_edge)
            )
    }

    @Test
    fun getSummary_modeContinuous_verifyText() {
        setCursorFollowingMode(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_continuous)
            )
    }

    private fun assertGetAvailability(
        inSetupWizard: Boolean,
        hasConnectedMouse: Boolean,
        expectedAvailability: Int,
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
            setHasConnectedMouse(hasConnectedMouse)

            val preferenceController =
                CursorFollowingModePreferenceController(activityController.get(), prefKey)
            assertThat(preferenceController.availabilityStatus).isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }

    private fun setCursorFollowingMode(@AccessibilityMagnificationCursorFollowingMode mode: Int) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE,
            mode,
        )
    }

    private fun setHasConnectedMouse(hasConnectedMouse: Boolean) {
        if (hasConnectedMouse) {
            val device =
                ShadowInputDevice.makeInputDevicebyIdWithSources(
                    /* id= */ 1,
                    InputDevice.SOURCE_MOUSE,
                )
            ShadowInputDevice.addDevice(device.id, device)
        } else {
            ShadowInputDevice.reset()
        }
    }
}
