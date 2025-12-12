/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.accessibility

import android.content.Context
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.KEY_GESTURE
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.R as SettingsLibR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

@RunWith(RobolectricTestRunner::class)
class MagnificationPreferenceControllerTest {
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()
    private lateinit var context: Context
    private lateinit var controller: MagnificationPreferenceController
    private lateinit var shadowAccessibilityManager: ShadowAccessibilityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowAccessibilityManager =
            Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
        controller = MagnificationPreferenceController(context, "magnification")
    }

    @Test
    fun getAvailabilityStatus_shouldReturnAvailable() {
        assertThat(controller.availabilityStatus).isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    fun getSummary_magnificationEnabled_returnShortcutOnWithSummary() {
        shadowAccessibilityManager.setAccessibilityShortcutTargets(
            UserShortcutType.TRIPLETAP,
            listOf(MAGNIFICATION_CONTROLLER_NAME),
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.preference_summary_default_combination,
                    context.getText(R.string.accessibility_summary_shortcut_enabled),
                    context.getText(R.string.magnification_feature_summary),
                )
            )
    }

    @Test
    fun getSummary_magnificationDisabled_returnShortcutOffWithSummary() {
        shadowAccessibilityManager.setAccessibilityShortcutTargets(
            UserShortcutType.TRIPLETAP,
            listOf(),
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.preference_summary_default_combination,
                    context.getText(R.string.generic_accessibility_feature_shortcut_off),
                    context.getText(R.string.magnification_feature_summary),
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    fun getSummary_magnificationGestureEnabled_returnShortcutOnWithSummary() {
        shadowAccessibilityManager.setAccessibilityShortcutTargets(
            UserShortcutType.TWOFINGER_DOUBLETAP,
            listOf(MAGNIFICATION_CONTROLLER_NAME),
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.preference_summary_default_combination,
                    context.getText(R.string.accessibility_summary_shortcut_enabled),
                    context.getText(R.string.magnification_feature_summary),
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    fun getSummary_magnificationGestureDisabled_returnShortcutOffWithSummary() {
        shadowAccessibilityManager.setAccessibilityShortcutTargets(
            UserShortcutType.TRIPLETAP or UserShortcutType.TWOFINGER_DOUBLETAP,
            listOf(),
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.preference_summary_default_combination,
                    context.getText(R.string.generic_accessibility_feature_shortcut_off),
                    context.getText(R.string.magnification_feature_summary),
                )
            )
    }

    @Test
    fun getSummary_defaultShortcut_returnShortcutOffWithSummary() {
        shadowAccessibilityManager.setAccessibilityShortcutTargets(
            DEFAULT,
            listOf(MAGNIFICATION_CONTROLLER_NAME),
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.preference_summary_default_combination,
                    context.getText(R.string.generic_accessibility_feature_shortcut_off),
                    context.getText(R.string.magnification_feature_summary),
                )
            )
    }

    @EnableFlags(com.android.hardware.input.Flags.FLAG_ENABLE_TALKBACK_AND_MAGNIFIER_KEY_GESTURES)
    @Test
    fun getSummary_keyGestureShortcut_returnShortcutOffWithSummary() {
        shadowAccessibilityManager.setAccessibilityShortcutTargets(
            KEY_GESTURE,
            listOf(MAGNIFICATION_CONTROLLER_NAME),
        )

        assertThat(controller.summary.toString())
            .isEqualTo(
                context.getString(
                    SettingsLibR.string.preference_summary_default_combination,
                    context.getText(R.string.generic_accessibility_feature_shortcut_off),
                    context.getText(R.string.magnification_feature_summary),
                )
            )
    }
}
