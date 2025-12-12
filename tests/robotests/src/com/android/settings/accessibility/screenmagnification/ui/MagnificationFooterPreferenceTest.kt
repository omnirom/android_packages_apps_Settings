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
import android.content.pm.PackageManager
import android.icu.text.MessageFormat
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.text.Html
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.screenmagnification.OneFingerPanningPreferenceController
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.android.settings.testutils.shadow.ShadowInputDevice.makeFullKeyboardInputDevicebyId
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.FooterPreference
import com.google.common.truth.Truth.assertThat
import kotlin.toString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@Config(shadows = [SettingsShadowResources::class, ShadowInputDevice::class])
@RunWith(RobolectricTestParameterInjector::class)
class MagnificationFooterPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = MagnificationFooterPreference()
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
    private val touchOnlyDefaultSummary =
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
    private val keyboardOnlySummary =
        Html.fromHtml(
                MessageFormat.format(
                    context.getString(
                        R.string.accessibility_screen_magnification_keyboard_summary,
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

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("html_description")
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }

    @DisableFlags(
        Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE,
        com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES,
    )
    @Test
    fun getTitle_touchScreenSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOff() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = "$keyboardOnlyShortcutsFlagOffSummary\n\n$touchOnlyDefaultSummary",
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @EnableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun getTitle_touchScreenSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOn() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardTouchSummary,
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenSupported_noHardKeyboard_oneFingerPanningFlagOff() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = true,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = touchOnlyDefaultSummary,
        )
    }

    @DisableFlags(
        Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE,
        com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES,
    )
    @Test
    fun getTitle_touchScreenNotSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOff() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = false,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardOnlyShortcutsFlagOffSummary,
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @EnableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun getTitle_touchScreenNotSupported_hasHardKeyboard_oneFingerPanningFlagOff_shortcutsFlagOn() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = false,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardOnlySummary,
        )
    }

    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenNotSupported_noHardKeyboard_oneFingerPanningFlagOff() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = false,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = touchOnlyDefaultSummary,
        )
    }

    @DisableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenSupported_hasHardKeyboard_oneFingerSettingsOff_shortcutsFlagOff() {
        assertContentDescriptionAndTitle(
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
    fun getTitle_touchScreenSupported_hasHardKeyboard_oneFingerSettingsOff_shortcutsFlagOn() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardTouchSummary,
        )
    }

    @EnableFlags(
        Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE,
        com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES,
    )
    @Test
    fun getTitle_touchScreenSupported_hasHardKeyboard_oneFingerSettingsOn_shortcutsFlagOn() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = true,
            expectedSummary = keyboardTouchSummary,
        )
    }

    @DisableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenSupported_hasHardKeyboard_oneFingerSettingsOn_shortcutsFlagOff() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = true,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = true,
            expectedSummary =
                "$keyboardOnlyShortcutsFlagOffSummary\n\n$oneFingerPanningOnDefaultSummary",
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenSupported_noHardKeyboard_oneFingerSettingsOff() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = true,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = oneFingerPanningOffDefaultSummary,
        )
    }

    @DisableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenNotSupported_hasHardKeyboard_oneFingerSettingsOff() {
        assertContentDescriptionAndTitle(
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
    fun getTitle_touchScreenNotSupported_hasHardKeyboard_oneFingerSettingsOff_shortcutsFlagOn() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = false,
            hardKeyboardAvailable = true,
            oneFingerPanningEnabled = false,
            expectedSummary = keyboardOnlySummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenNotSupported_noHardKeyboard_oneFingerSettingsOn() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = false,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = true,
            expectedSummary = oneFingerPanningOnDefaultSummary,
        )
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    fun getTitle_touchScreenNotSupported_noHardKeyboard_oneFingerSettingsOff() {
        assertContentDescriptionAndTitle(
            touchScreenSupported = false,
            hardKeyboardAvailable = false,
            oneFingerPanningEnabled = false,
            expectedSummary = oneFingerPanningOffDefaultSummary,
        )
    }

    private fun assertContentDescriptionAndTitle(
        touchScreenSupported: Boolean,
        hardKeyboardAvailable: Boolean,
        oneFingerPanningEnabled: Boolean,
        expectedSummary: String,
    ) {
        setTouchScreenSupported(touchScreenSupported)
        setHardKeyboardAvailable(hardKeyboardAvailable)
        setOneFingerPanningEnabled(oneFingerPanningEnabled)
        val widget = createFooterPreferenceWidget()
        assertThat(widget.contentDescription).isEqualTo("About magnification\n\n$expectedSummary")
        assertThat(preference.getTitle(context).toString()).isEqualTo(expectedSummary)
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

    private fun createFooterPreferenceWidget(): FooterPreference =
        preference.createAndBindWidget<FooterPreference>(context).apply { inflateViewHolder() }
}
