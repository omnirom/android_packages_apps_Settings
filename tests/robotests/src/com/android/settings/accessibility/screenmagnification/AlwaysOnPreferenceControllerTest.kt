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
import android.provider.DeviceConfig
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil.State.OFF
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settings.testutils.shadow.ShadowDeviceConfig
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

private const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED

@Config(shadows = [ShadowDeviceConfig::class, SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class AlwaysOnPreferenceControllerTest {
    private val prefKey = "prefKey"
    private val lifeCycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private val controller = AlwaysOnPreferenceController(context, prefKey)
    private val shadowContentResolver = shadowOf(context.contentResolver)
    private val preference = spy(SwitchPreferenceCompat(context)).apply { key = prefKey }
    private val preferenceManager = PreferenceManager(context)

    @Before
    fun setUp() {
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(preference)
        lifeCycleOwner.lifecycle.addObserver(controller)
        setWindowMagnificationSupported(context, true)
        setAlwaysOnSupported(true)
        controller.displayPreference(preferenceScreen)
        controller.updateState(preference)
        reset(preference)
    }

    @After
    fun cleanUp() {
        lifeCycleOwner.lifecycle.removeObserver(controller)
    }

    @Test
    fun performClick_switchDefaultStateForAlwaysOn_shouldReturnFalse() {
        preference.performClick()

        verify(preference).setChecked(false)
        assertThat(controller.isChecked()).isFalse()
        assertThat(preference.isChecked()).isFalse()
    }

    @Test
    fun updateState_disableAlwaysOn_shouldReturnFalse() {
        Settings.Secure.putInt(context.getContentResolver(), SETTING_KEY, OFF)

        controller.updateState(preference)

        verify(preference).setChecked(false)
        assertThat(controller.isChecked()).isFalse()
        assertThat(preference.isChecked()).isFalse()
    }

    @Test
    fun onResume_verifyRegisterCapabilityObserver() {
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        assertThat(
                shadowContentResolver.getContentObservers(
                    Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY
                    )
                )
            )
            .hasSize(1)
    }

    @Test
    fun onPause_verifyUnregisterCapabilityObserver() {
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        assertThat(
                shadowContentResolver.getContentObservers(
                    Settings.Secure.getUriFor(
                        Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CAPABILITY
                    )
                )
            )
            .isEmpty()
    }

    @Test
    fun updateState_windowModeOnly_preferenceBecomesUnavailable() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)

        controller.updateState(preference)
        assertThat(preference.isEnabled()).isFalse()
    }

    @Test
    fun updateState_fullscreenModeOnly_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.FULLSCREEN)

        controller.updateState(preference)
        assertThat(preference.isEnabled()).isTrue()
    }

    @Test
    fun updateState_switchMode_preferenceIsAvailable() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)

        controller.updateState(preference)
        assertThat(preference.isEnabled()).isTrue()
    }

    @Test
    fun preferenceEnabled_verifySummaryText() {
        updateState_fullscreenModeOnly_preferenceIsAvailable()

        assertThat(preference.summary.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_screen_magnification_always_on_summary)
            )
    }

    @Test
    fun preferenceDisabled_verifySummaryText() {
        updateState_windowModeOnly_preferenceBecomesUnavailable()

        assertThat(preference.summary.toString())
            .isEqualTo(
                context.getString(
                    R.string.accessibility_screen_magnification_always_on_unavailable_summary
                )
            )
    }

    @Test
    fun getAvailableStatus_supported_magnificationHasShortcut_returnAvailable() {
        setHasAnyMagnificationShortcut(true)

        setAlwaysOnSupported(true)
        assertGetAvailability(
            inSetupWizard = false,
            windowMagnificationSupported = true,
            expectedAvailability = AVAILABLE,
        )
    }

    @Test
    fun getAvailableStatus_supported_noMagnificationShortcuts_returnAvailableUnsearchable() {
        setHasAnyMagnificationShortcut(false)
        setAlwaysOnSupported(true)
        assertGetAvailability(
            inSetupWizard = false,
            windowMagnificationSupported = true,
            expectedAvailability = AVAILABLE_UNSEARCHABLE,
        )
    }

    @Test
    fun getAvailableStatus_notInSetupWizard_windowMagNotSupported_alwaysOnSupported_returnUnavailable() {
        setAlwaysOnSupported(true)
        assertGetAvailability(
            inSetupWizard = false,
            windowMagnificationSupported = false,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @Test
    fun getAvailableStatus_notInSetupWizard_windowMagNotSupported_alwaysOnNotSupported_returnUnavailable() {
        setAlwaysOnSupported(false)
        assertGetAvailability(
            inSetupWizard = false,
            windowMagnificationSupported = true,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
        )
    }

    @Test
    fun getAvailableStatus_inSetupWizard_returnConditionallyUnavailable() {
        setAlwaysOnSupported(true)
        assertGetAvailability(
            inSetupWizard = true,
            windowMagnificationSupported = true,
            expectedAvailability = CONDITIONALLY_UNAVAILABLE,
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
            val preferenceController =
                AlwaysOnPreferenceController(activityController.get(), prefKey)
            assertThat(preferenceController.availabilityStatus).isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }

    private fun setAlwaysOnSupported(supported: Boolean) {
        ShadowDeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_WINDOW_MANAGER,
            "AlwaysOnMagnifier__enable_always_on_magnifier",
            if (supported) "true" else "false",
            /* makeDefault= */ false,
        )
    }

    private fun setHasAnyMagnificationShortcut(hasShortcuts: Boolean) {
        if (hasShortcuts) {
            a11yManager.setAccessibilityShortcutTargets(
                HARDWARE,
                listOf<String>(AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME),
            )
        } else {
            a11yManager.setAccessibilityShortcutTargets(
                ShortcutConstants.UserShortcutType.ALL,
                listOf(),
            )
        }
    }
}
