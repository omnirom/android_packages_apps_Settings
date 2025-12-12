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
import android.provider.DeviceConfig
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.settings.R
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.testutils.AccessibilityTestUtils.setWindowMagnificationSupported
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settings.testutils.shadow.ShadowDeviceConfig
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
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow

@Config(shadows = [ShadowDeviceConfig::class, SettingsShadowResources::class])
@RunWith(RobolectricTestParameterInjector::class)
class AlwaysOnSwitchPreferenceTest {
    @get:Rule(order = 0) val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = AlwaysOnSwitchPreference()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @Test
    fun key() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED)
    }

    @Test
    fun getTitle() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_screen_magnification_always_on_title)
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
                    R.string.accessibility_screen_magnification_always_on_unavailable_summary
                )
            )
    }

    @Test
    fun getSummary_fullScreenMode_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.FULLSCREEN)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_screen_magnification_always_on_summary)
            )
    }

    @Test
    fun getSummary_allMode_verifySummaryText() {
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)

        assertThat(preference.getSummary(context))
            .isEqualTo(
                context.getString(R.string.accessibility_screen_magnification_always_on_summary)
            )
    }

    @Test
    @TestParameters(
        value =
            [
                "{magnificationShortcut: false, expectedIndexed: false}",
                "{magnificationShortcut: true, expectedIndexed: true}",
            ]
    )
    fun isIndexable(magnificationShortcut: Boolean, expectedIndexed: Boolean) {
        setHasAnyMagnificationShortcut(magnificationShortcut)
        setAlwaysOnSupported(true)

        assertThat(preference.isIndexable(context)).isEqualTo(expectedIndexed)
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
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.ALL)
        getStorage()
            .setBoolean(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED,
                settingsEnabled,
            )
        val preferenceWidget = createAlwaysOnWidget()
        assertThat(preferenceWidget.isChecked).isEqualTo(settingsEnabled)

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isEqualTo(expectedChecked)
        assertThat(
                getStorage()
                    .getBoolean(Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED)
            )
            .isEqualTo(expectedChecked)
    }

    @Test
    @TestParameters(
        value =
            [
                "{supportAlwaysOn: false, inSetupWizard: false, supportWindowMag: false, expectedAvailable: false}",
                "{supportAlwaysOn: false, inSetupWizard: false, supportWindowMag: true, expectedAvailable: false}",
                "{supportAlwaysOn: false, inSetupWizard: true, supportWindowMag: false, expectedAvailable: false}",
                "{supportAlwaysOn: false, inSetupWizard: true, supportWindowMag: true, expectedAvailable: false}",
                "{supportAlwaysOn: true, inSetupWizard: false, supportWindowMag: false, expectedAvailable: false}",
                "{supportAlwaysOn: true, inSetupWizard: false, supportWindowMag: true, expectedAvailable: true}",
                "{supportAlwaysOn: true, inSetupWizard: true, supportWindowMag: false, expectedAvailable: false}",
                "{supportAlwaysOn: true, inSetupWizard: true, supportWindowMag: true, expectedAvailable: false}",
            ]
    )
    fun isAvailable(
        supportAlwaysOn: Boolean,
        inSetupWizard: Boolean,
        supportWindowMag: Boolean,
        expectedAvailable: Boolean,
    ) {
        setAlwaysOnSupported(supportAlwaysOn)
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

    private fun createAlwaysOnWidget(): SwitchPreferenceCompat =
        preference.createAndBindWidget<SwitchPreferenceCompat>(context).apply {
            inflateViewHolder()
        }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}
