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
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

private const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED

/** Tests for [FollowTypingPreferenceController] */
@RunWith(RobolectricTestRunner::class)
class FollowTypingPreferenceControllerTest {
    private val prefKey = "prefKey"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller = FollowTypingPreferenceController(context, prefKey)
    private val preference = SwitchPreferenceCompat(context).apply { key = prefKey }
    private val preferenceManager = PreferenceManager(context)

    @Before
    fun setUp() {
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(preference)
        preferenceManager.setPreferences(preferenceScreen)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun getAvailabilityStatus_inSetupWizard_returnConditionallyUnavailable() {
        assertGetAvailability(
            inSetupWizard = true,
            windowMagnificationSupported = true,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @Test
    fun getAvailabilityStatus_windowMagnificationNotSupported_returnConditionallyUnavailable() {
        assertGetAvailability(
            inSetupWizard = false,
            windowMagnificationSupported = false,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @Test
    fun getAvailabilityStatus_windowMagnificationSupported_returnAvailable() {
        assertGetAvailability(
            inSetupWizard = false,
            windowMagnificationSupported = true,
            expectedAvailability = AVAILABLE,
        )
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
        windowMagnificationSupported: Boolean,
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
            setWindowMagnificationSupported(context, windowMagnificationSupported)
            val preferenceController =
                FollowTypingPreferenceController(activityController.get(), prefKey)
            assertThat(preferenceController.availabilityStatus).isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }
}
