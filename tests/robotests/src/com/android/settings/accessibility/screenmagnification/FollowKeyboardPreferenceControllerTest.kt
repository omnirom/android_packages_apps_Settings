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
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.screenmagnification.FollowKeyboardPreferenceController.Companion.SETTING_KEY
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/** Tests for [FollowKeyboardPreferenceController] */
@Config(shadows = [ShadowInputDevice::class])
@RunWith(RobolectricTestRunner::class)
class FollowKeyboardPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val prefKey = "prefKey"
    private val lifeCycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller = FollowKeyboardPreferenceController(context, prefKey)
    private val preference = Preference(context).apply { key = prefKey }
    private val preferenceManager = PreferenceManager(context)

    @Before
    fun setUp() {
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(preference)
        preferenceManager.setPreferences(preferenceScreen)
        lifeCycleOwner.lifecycle.addObserver(controller)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun getAvailabilityStatus_inSetupWizard_returnConditionallyUnavailable() {
        assertGetAvailability(
            inSetupWizard = true,
            hasHardwareKeyboard = true,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @Test
    fun getAvailabilityStatus_noHardwareKeyboard_returnConditionallyUnavailable() {
        assertGetAvailability(
            inSetupWizard = false,
            hasHardwareKeyboard = false,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @Test
    fun getAvailabilityStatus_hasHardwareKeyboard_returnAvailable() {
        assertGetAvailability(
            inSetupWizard = false,
            hasHardwareKeyboard = true,
            expectedAvailability = AVAILABLE,
        )
    }

    @Test
    fun isChecked_default_returnFalse() {
         assertThat(controller.isChecked).isFalse()
    }

    @Test
    fun isChecked_settingOn_returnTrue() {
        Settings.Secure.putInt(context.contentResolver, SETTING_KEY, AccessibilityUtil.State.ON)

        assertThat(controller.isChecked).isTrue()
    }

    @Test
    fun isChecked_settingOff_returnFalse() {
        Settings.Secure.putInt(context.contentResolver, SETTING_KEY, AccessibilityUtil.State.OFF)

        assertThat(controller.isChecked).isFalse()
    }

    @Test
    fun setChecked_turnOn_settingSetsOn() {
        controller.setChecked(true)

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    SETTING_KEY,
                    AccessibilityUtil.State.OFF,
                )
            )
            .isEqualTo(AccessibilityUtil.State.ON)
    }

    @Test
    fun setChecked_turnOff_settingSetsOff() {
        controller.setChecked(false)

        assertThat(
                Settings.Secure.getInt(
                    context.contentResolver,
                    SETTING_KEY,
                    AccessibilityUtil.State.OFF,
                )
            )
            .isEqualTo(AccessibilityUtil.State.OFF)
    }

    private fun assertGetAvailability(
        inSetupWizard: Boolean,
        hasHardwareKeyboard: Boolean,
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
            setHardwareKeyboard(hasHardwareKeyboard)
            val preferenceController =
                FollowKeyboardPreferenceController(activityController.get(), prefKey)
            assertThat(preferenceController.availabilityStatus).isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }

    private fun setHardwareKeyboard(hasConnectedMouse: Boolean) {
        if (hasConnectedMouse) {
            val device = ShadowInputDevice.makeFullKeyboardInputDevicebyId(/* id= */ 1)
            ShadowInputDevice.addDevice(device.id, device)
        } else {
            ShadowInputDevice.reset()
        }
    }
}
