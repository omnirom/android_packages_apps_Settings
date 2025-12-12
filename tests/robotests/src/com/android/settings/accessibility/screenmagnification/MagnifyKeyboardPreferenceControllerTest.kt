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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.internal.R
import com.android.server.accessibility.Flags
import com.android.settings.accessibility.AccessibilityUtil.State.OFF
import com.android.settings.accessibility.AccessibilityUtil.State.ON
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME

/** Tests for [MagnifyKeyboardPreferenceController] */
@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class MagnifyKeyboardPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val prefKey = "prefKey"
    private val lifeCycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val controller = MagnifyKeyboardPreferenceController(context, prefKey)
    private val shadowContentResolver = shadowOf(context.contentResolver)
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private val preference = spy(SwitchPreferenceCompat(context)).apply { key = prefKey }

    @Before
    fun setUp() {
        preferenceScreen.addPreference(preference)
        lifeCycleOwner.lifecycle.addObserver(controller)
        controller.displayPreference(preferenceScreen)
        controller.updateState(preference)
        setWindowMagnificationSupported(context, true)
        reset(preference)
    }

    @After
    fun cleanUp() {
        lifeCycleOwner.lifecycle.removeObserver(controller)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    fun getAvailabilityStatus_defaultState_disabled() {
        val status: Int = controller.getAvailabilityStatus()

        assertThat(status).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    fun getAvailabilityStatus_featureFlagEnabled_enabled() {
        val status: Int = controller.getAvailabilityStatus()

        assertThat(status).isEqualTo(AVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    fun getAvailabilityStatus_featureFlagEnabled_windowMagnificationNotSupported_disabled() {
        setWindowMagnificationSupported(context, false)

        val status: Int = controller.getAvailabilityStatus()

        assertThat(status).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    fun isChecked_settingsEnabled_returnTrue() {
        Settings.Secure.putInt(context.getContentResolver(), SETTING_KEY, ON)
        controller.updateState(preference)

        verify(preference).setChecked(true)
        assertThat(controller.isChecked()).isTrue()
        assertThat(preference.isChecked()).isTrue()
    }

    @Test
    fun isChecked_settingsDisabled_returnTrue() {
        Settings.Secure.putInt(context.getContentResolver(), SETTING_KEY, OFF)
        controller.updateState(preference)

        verify(preference).setChecked(false)
        assertThat(controller.isChecked()).isFalse()
        assertThat(preference.isChecked()).isFalse()
    }

    @Test
    fun performClick_switchDisabled_shouldReturnEnable() {
        Settings.Secure.putInt(context.getContentResolver(), SETTING_KEY, OFF)
        controller.updateState(preference)

        preference.performClick()

        verify(preference).setChecked(true)
        // assertThat(mController.isChecked()).isTrue();
        assertThat(preference.isChecked()).isTrue()
    }

    @Test
    fun performClick_switchEnabled_shouldReturnDisable() {
        Settings.Secure.putInt(context.getContentResolver(), SETTING_KEY, ON)
        controller.updateState(preference)

        preference.performClick()

        verify(preference).setChecked(false)
        // assertThat(mController.isChecked()).isFalse();
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
    fun isChecked_defaultValueOn() {
        Settings.Secure.clearProviderForTest()
        SettingsShadowResources.overrideResource(
            R.bool.config_magnification_magnify_keyboard_default,
            true,
        )

        controller.updateState(preference)
        assertThat(preference.isChecked()).isTrue()
    }

    @Test
    fun isChecked_defaultValueOff() {
        Settings.Secure.clearProviderForTest()
        SettingsShadowResources.overrideResource(
            R.bool.config_magnification_magnify_keyboard_default,
            false,
        )

        controller.updateState(preference)
        assertThat(preference.isChecked()).isFalse()
    }
}
