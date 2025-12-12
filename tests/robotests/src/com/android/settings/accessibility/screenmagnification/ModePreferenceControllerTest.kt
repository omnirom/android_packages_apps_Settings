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
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/** Tests for [ModePreferenceController] */
@RunWith(RobolectricTestRunner::class)
class ModePreferenceControllerTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var controller: ModePreferenceController
    private val prefKey = "prefKey"
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferenceManager = PreferenceManager(context)
    private val preference = Preference(context).apply { key = prefKey }

    @Before
    fun setUp() {
        fragmentScenario = launchFragment<Fragment>(initialState = INITIALIZED)
        fragmentScenario.onFragment { fragment ->
            controller = ModePreferenceController(fragment.requireContext(), prefKey)

            fragment.lifecycle.addObserver(controller)
            controller.setFragmentManager(fragment.childFragmentManager)
        }

        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(preference)
        preferenceManager.setPreferences(preferenceScreen)
        controller.displayPreference(preferenceScreen)
    }

    @Test
    fun getSummary_windowModeOnly_returnWindowOnlySummary() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)
        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_magnification_area_settings_window_screen_summary
                )
            )
    }

    @Test
    fun clickPreference_triggerShowDialog() {
        controller.handlePreferenceTreeClick(preference)

        fragmentScenario.onFragment { fragment ->
            assertDialogShown(fragment, MagnificationModeChooser::class.java)
        }
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
            val preferenceController = ModePreferenceController(activityController.get(), prefKey)
            assertThat(preferenceController.availabilityStatus).isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }
}
