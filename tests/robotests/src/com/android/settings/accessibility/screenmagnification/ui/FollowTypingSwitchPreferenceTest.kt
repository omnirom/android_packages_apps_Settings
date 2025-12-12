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
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestParameterInjector::class)
class FollowTypingSwitchPreferenceTest {
    @get:Rule(order = 0) val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = FollowTypingSwitchPreference()

    @Test
    fun key() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_screen_magnification_follow_typing_title)
    }

    @Test
    fun getSummary() {
        assertThat(preference.summary)
            .isEqualTo(R.string.accessibility_screen_magnification_follow_typing_summary)
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
                "{settingsEnabled: false, expectedChecked: true}",
                "{settingsEnabled: true, expectedChecked: false}",
            ]
    )
    fun performClick(settingsEnabled: Boolean, expectedChecked: Boolean) {
        getStorage()
            .setBoolean(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED,
                settingsEnabled,
            )
        val preferenceWidget = createFollowTypingWidget()
        assertThat(preferenceWidget.isChecked).isEqualTo(settingsEnabled)

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isEqualTo(expectedChecked)
        assertThat(
                getStorage()
                    .getBoolean(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED)
            )
            .isEqualTo(expectedChecked)
    }

    @Test
    @TestParameters(
        value =
            [
                "{inSetupWizard: false, supportWindowMag: false, expectedAvailable: false}",
                "{inSetupWizard: false, supportWindowMag: true, expectedAvailable: true}",
                "{inSetupWizard: true, supportWindowMag: false, expectedAvailable: false}",
                "{inSetupWizard: true, supportWindowMag: true, expectedAvailable: false}",
            ]
    )
    fun isAvailable(inSetupWizard: Boolean, supportWindowMag: Boolean, expectedAvailable: Boolean) {
        assertIsAvailable(inSetupWizard, supportWindowMag, expectedAvailable)
    }

    private fun assertIsAvailable(
        inSetupWizard: Boolean,
        windowMagnificationSupported: Boolean,
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
            setWindowMagnificationSupported(context, windowMagnificationSupported)
            assertThat(preference.isAvailable(activityController.get()))
                .isEqualTo(expectedAvailability)
        } finally {
            activityController?.destroy()
        }
    }

    private fun createFollowTypingWidget(): SwitchPreferenceCompat =
        preference.createAndBindWidget<SwitchPreferenceCompat>(context).apply {
            inflateViewHolder()
        }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}
