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
import android.provider.Settings.Secure.AccessibilityMagnificationCursorFollowingMode
import android.view.InputDevice
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.lifecycle.Lifecycle.State.INITIALIZED
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.screenmagnification.dialogs.CursorFollowingModeChooser
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@Config(shadows = [ShadowInputDevice::class])
@RunWith(RobolectricTestParameterInjector::class)
class CursorFollowingPreferenceTest {
    @get:Rule(order = 0) val settingsStoreRule = SettingsStoreRule()
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = CursorFollowingPreference()

    @Test
    fun key() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_magnification_cursor_following_title)
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
    fun getSummary_disabled_verifyText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)

        assertThat(preference.getSummary(context))
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

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_center)
            )
    }

    @Test
    fun getSummary_modeEdge_verifyText() {
        setCursorFollowingMode(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE
        )

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_edge)
            )
    }

    @Test
    fun getSummary_modeContinuous_verifyText() {
        setCursorFollowingMode(
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS
        )

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_magnification_cursor_following_continuous)
            )
    }

    @Test
    fun performClick_showDialog() {
        val mockLifecycleContext = mock<PreferenceLifecycleContext>()
        val fragmentScenario = launchFragment<Fragment>(initialState = INITIALIZED)
        fragmentScenario.onFragment { fragment ->
            mockLifecycleContext.stub {
                on { childFragmentManager } doReturn fragment.childFragmentManager
            }
            preference.onCreate(mockLifecycleContext)

            val widget: Preference = preference.createAndBindWidget(context)
            widget.performClick()

            assertDialogShown(fragment, CursorFollowingModeChooser::class.java)
        }
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

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    @TestParameters(
        "{inSetupWizard: false, hasConnectedMouse: false, expectedAvailable: false}",
        "{inSetupWizard: false, hasConnectedMouse: true, expectedAvailable: true}",
        "{inSetupWizard: true, hasConnectedMouse: false, expectedAvailable: false}",
        "{inSetupWizard: true, hasConnectedMouse: true, expectedAvailable: false}",
    )
    fun isAvailable_flagOn(
        inSetupWizard: Boolean,
        hasConnectedMouse: Boolean,
        expectedAvailable: Boolean,
    ) {
        assertIsAvailable(inSetupWizard, hasConnectedMouse, expectedAvailable)
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_CURSOR_FOLLOWING_DIALOG)
    @Test
    @TestParameters(
        "{inSetupWizard: false, hasConnectedMouse: false, expectedAvailable: false}",
        "{inSetupWizard: false, hasConnectedMouse: true, expectedAvailable: false}",
        "{inSetupWizard: true, hasConnectedMouse: false, expectedAvailable: false}",
        "{inSetupWizard: true, hasConnectedMouse: true, expectedAvailable: false}",
    )
    fun isAvailable_flagOff(
        inSetupWizard: Boolean,
        hasConnectedMouse: Boolean,
        expectedAvailable: Boolean,
    ) {
        assertIsAvailable(inSetupWizard, hasConnectedMouse, expectedAvailable)
    }

    private fun assertIsAvailable(
        inSetupWizard: Boolean,
        hasConnectedMouse: Boolean,
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
            setHasConnectedMouse(hasConnectedMouse)
            assertThat(preference.isAvailable(activityController.get()))
                .isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }

    private fun setCursorFollowingMode(@AccessibilityMagnificationCursorFollowingMode mode: Int) {
        getStorage().setInt(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE, mode)
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

    private fun getStorage(): KeyValueStore = preference.storage(context)
}
