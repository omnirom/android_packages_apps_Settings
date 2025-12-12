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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class MagnifyKeyboardSwitchPreferenceTest {
    @get:Rule(order = 0) val settingsStoreRule = SettingsStoreRule()
    @get:Rule(order = 1) val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = MagnifyKeyboardSwitchPreference()

    @Test
    fun key() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_screen_magnification_nav_ime_title)
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
    @TestParameters(
        value =
            [
                "{defaultValue: false, expectedChecked: false}",
                "{defaultValue: true, expectedChecked: true}",
            ]
    )
    fun isChecked_defaultValue(defaultValue: Boolean, expectedChecked: Boolean) {
        Settings.Secure.clearProviderForTest()
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_magnification_magnify_keyboard_default,
            defaultValue,
        )

        val preferenceWidget = createMagnifyKeyboardWidget()
        assertThat(preferenceWidget.isChecked).isEqualTo(expectedChecked)
    }

    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
    @Test
    @TestParameters(
        value =
            [
                "{settingsEnabled: false, expectedChecked: true}",
                "{settingsEnabled: true, expectedChecked: false}",
            ]
    )
    fun performClick(settingsEnabled: Boolean, expectedChecked: Boolean) {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)
        getStorage()
            .setBoolean(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME,
                settingsEnabled,
            )
        val preferenceWidget = createMagnifyKeyboardWidget()
        assertThat(preferenceWidget.isChecked).isEqualTo(settingsEnabled)

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isEqualTo(expectedChecked)
        assertThat(
                getStorage()
                    .getBoolean(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MAGNIFY_NAV_AND_IME)
            )
            .isEqualTo(expectedChecked)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    fun isAvailable_featureFlagDisabled_windowMagnificationSupported_disabled() {
        setWindowMagnificationSupported(context, true)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    fun isAvailable_featureFlagDisabled_windowMagnificationNotSupported_disabled() {
        setWindowMagnificationSupported(context, false)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    fun isAvailable_featureFlagEnabled_windowMagnificationSupported_enabled() {
        setWindowMagnificationSupported(context, true)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MAGNIFY_NAV_BAR_AND_IME)
    fun isAvailable_featureFlagEnabled_windowMagnificationNotSupported_disabled() {
        setWindowMagnificationSupported(context, false)

        assertThat(preference.isAvailable(context)).isFalse()
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

    @Test
    fun getSummary_windowMode_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.WINDOW)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(
                    R.string.accessibility_screen_magnification_nav_ime_unavailable_summary
                )
            )
    }

    @Test
    fun getSummary_fullScreenMode_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.FULLSCREEN)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_screen_magnification_nav_ime_summary)
            )
    }

    @Test
    fun getSummary_allMode_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_screen_magnification_nav_ime_summary)
            )
    }

    private fun createMagnifyKeyboardWidget(): SwitchPreferenceCompat =
        preference.createAndBindWidget<SwitchPreferenceCompat>(context).apply {
            inflateViewHolder()
        }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}
