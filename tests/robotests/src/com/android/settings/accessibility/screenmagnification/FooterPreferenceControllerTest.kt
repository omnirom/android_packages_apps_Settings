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
import android.content.pm.PackageManager
import android.icu.text.MessageFormat
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.text.Html
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityFooterPreference
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.android.settings.testutils.shadow.ShadowInputDevice.makeFullKeyboardInputDevicebyId
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowPackageManager

/** Tests for [FooterPreferenceController] */
@Config(shadows = [SettingsShadowResources::class, ShadowInputDevice::class])
@RunWith(RobolectricTestParameterInjector::class)
class FooterPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val prefKey = "prefKey"
    private val lifeCycleOwner = TestLifecycleOwner(initialState = Lifecycle.State.INITIALIZED)
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val oneFingerPanningOnDefaultSummary =
        Html.fromHtml(
                MessageFormat.format(
                    context.getString(
                        R.string.accessibility_screen_magnification_summary_one_finger_panning_on
                    ),
                    1,
                    2,
                    3,
                    4,
                    5,
                ),
                Html.FROM_HTML_MODE_COMPACT,
            )
            .toString()
    private val oneFingerPanningOffDefaultSummary =
        Html.fromHtml(
                MessageFormat.format(
                    context.getString(
                        R.string.accessibility_screen_magnification_summary_one_finger_panning_off
                    ),
                    1,
                    2,
                    3,
                    4,
                    5,
                ),
                Html.FROM_HTML_MODE_COMPACT,
            )
            .toString()
    private val defaultSummary =
        Html.fromHtml(
                MessageFormat.format(
                    context.getString(R.string.accessibility_screen_magnification_summary),
                    1,
                    2,
                    3,
                    4,
                    5,
                ),
                Html.FROM_HTML_MODE_COMPACT,
            )
            .toString()
    private val metaString = context.getString(R.string.modifier_keys_meta)
    private val altString = context.getString(R.string.modifier_keys_alt)
    private val keyboardSummary =
        Html.fromHtml(
                MessageFormat.format(
                    context.getString(
                        R.string.accessibility_screen_magnification_keyboard_summary,
                        metaString,
                        altString,
                        metaString,
                        altString,
                    ),
                    1,
                    2,
                    3,
                    4,
                ),
                Html.FROM_HTML_MODE_COMPACT,
            )
            .toString()
    private val keyboardOnlyShortcutsFlagOffSummary =
        Html.fromHtml(
                MessageFormat.format(
                    context.getString(
                        R.string
                            .accessibility_screen_magnification_keyboard_shortcuts_flag_off_summary,
                        metaString,
                        altString,
                    ),
                    1,
                    2,
                    3,
                    4,
                ),
                Html.FROM_HTML_MODE_COMPACT,
            )
            .toString()
    private val keyboardTouchSummary =
        Html.fromHtml(
                MessageFormat.format(
                    context.getString(
                        R.string.accessibility_screen_magnification_keyboard_touch_summary,
                        metaString,
                        altString,
                    ),
                    1,
                    2,
                    3,
                    4,
                    5,
                ),
                Html.FROM_HTML_MODE_COMPACT,
            )
            .toString()
    private val shadowContentResolver = shadowOf(context.contentResolver)
    private val controller = FooterPreferenceController(context, prefKey)
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
    private val preference = AccessibilityFooterPreference(context).apply { key = prefKey }

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_enable_a11y_magnification_single_panning,
            false,
        )
        setWindowMagnificationSupported(context, /* supported= */ true)
        preferenceScreen.addPreference(preference)
        lifeCycleOwner.lifecycle.addObserver(controller)
        controller.displayPreference(preferenceScreen)
    }

    @After
    fun cleanUp() {
        lifeCycleOwner.lifecycle.removeObserver(controller)
        ShadowInputDevice.reset()
    }

    @Test
    fun onStart_verifyRegisterOneFingerPanningObserver() {
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)

        assertThat(
                shadowContentResolver.getContentObservers(
                    Settings.Secure.getUriFor(OneFingerPanningPreferenceController.SETTING_KEY)
                )
            )
            .hasSize(1)
    }

    @Test
    fun onStop_verifyUnregisterOneFingerPanningObserver() {
        onStart_verifyRegisterOneFingerPanningObserver()
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)

        assertThat(
                shadowContentResolver.getContentObservers(
                    Settings.Secure.getUriFor(OneFingerPanningPreferenceController.SETTING_KEY)
                )
            )
            .isEmpty()
    }

    @Test
    fun verifyContentDescription() {
        controller.updateState(preference)

        assertThat(preference.contentDescription.toString())
            .isEqualTo("About magnification\n\n$defaultSummary")
    }

    @DisableFlags(
        Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE,
        com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES,
    )
    @Test
    fun touchScreenSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOff_verifySummary() {
        assertSummary(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = "$keyboardOnlyShortcutsFlagOffSummary\n\n$defaultSummary",
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @EnableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun touchScreenSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOn_verifySummary() {
        assertSummary(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardTouchSummary,
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun touchScreenSupported_noHardKeyboard_oneFingerPanningFlagOff_verifySummary() {
        assertSummary(
            touchScreenSupported = true,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = defaultSummary,
        )
    }

    @DisableFlags(
        Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE,
        com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES,
    )
    @Test
    fun touchScreenNotSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOff_verifySummary() {
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardOnlyShortcutsFlagOffSummary,
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @EnableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun touchScreenNotSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOn_verifySummary() {
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardSummary,
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun touchScreenNotSupported_noHardKeyboard_oneFingerPanningFlagOff_verifySummary() {
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = defaultSummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @DisableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun touchScreenSupported_hasHardKeyboard_oneFingerSettingsOff_shortcutsFlagOff_verifySummary() {
        assertSummary(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary =
                "$keyboardOnlyShortcutsFlagOffSummary\n\n$oneFingerPanningOffDefaultSummary",
        )
    }

    @EnableFlags(
        Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE,
        com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES,
    )
    @Test
    fun touchScreenSupported_hasHardKeyboard_oneFingerSettingsOff_shortcutsFlagOn_verifySummary() {
        assertSummary(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardTouchSummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun touchScreenSupported_noHardKeyboard_oneFingerSettingsOff_verifySummary() {
        assertSummary(
            touchScreenSupported = true,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = oneFingerPanningOffDefaultSummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @DisableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun touchScreenNotSupported_hasHardKeyboard_oneFingerSettingsOff_shortcutsFlagOff_verifySummary() {
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardOnlyShortcutsFlagOffSummary,
        )
    }

    @EnableFlags(
        Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE,
        com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES,
    )
    @Test
    fun touchScreenNotSupported_hasHardKeyboard_oneFingerSettingsOff_shortcutsFlagOn_verifySummary() {
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardSummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun touchScreenNotSupported_noHardKeyboard_oneFingerSettingsOn_verifySummary() {
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = true,
            expectedSummary = oneFingerPanningOnDefaultSummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun touchScreenNotSupported_noHardKeyboard_oneFingerSettingsOff_verifySummary() {
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = oneFingerPanningOffDefaultSummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun onStart_turnOnOneFingerPanning_verifySummary() {
        lifeCycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        assertSummary(
            touchScreenSupported = false,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = oneFingerPanningOffDefaultSummary,
        )

        setOneFingerPanningEnabled(true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(preference.summary.toString()).isEqualTo(oneFingerPanningOnDefaultSummary)
    }

    private fun assertSummary(
        touchScreenSupported: Boolean,
        hardKeyboardAvailable: Boolean,
        oneFingerPanningEnabled: Boolean,
        expectedSummary: String,
    ) {
        setTouchScreenSupported(touchScreenSupported)
        setHardKeyboardAvailable(hardKeyboardAvailable)
        setOneFingerPanningEnabled(oneFingerPanningEnabled)

        controller.updateState(preference)

        assertThat(preference.summary.toString()).isEqualTo(expectedSummary)
    }

    private fun setTouchScreenSupported(supported: Boolean) {
        val shadowPackageManager: ShadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_TOUCHSCREEN, supported)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_FAKETOUCH, supported)
    }

    private fun setOneFingerPanningEnabled(enabled: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            OneFingerPanningPreferenceController.SETTING_KEY,
            if (enabled) AccessibilityUtil.State.ON else AccessibilityUtil.State.OFF,
        )
    }

    private fun setHardKeyboardAvailable(available: Boolean) {
        if (available) {
            // The deviceId needs to be >= 1 in order to be considered as a hard keyboard
            val deviceId = 2
            ShadowInputDevice.addDevice(deviceId, makeFullKeyboardInputDevicebyId(deviceId))
        } else {
            ShadowInputDevice.reset()
        }
    }
}
