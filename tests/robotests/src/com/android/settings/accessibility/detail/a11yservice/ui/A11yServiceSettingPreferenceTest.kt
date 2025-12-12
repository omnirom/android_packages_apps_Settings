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

package com.android.settings.accessibility.detail.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf

/** Tests for [A11yServiceSettingPreference] */
@RunWith(RobolectricTestParameterInjector::class)
class A11yServiceSettingPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private var activityScenario: ActivityScenario<FragmentActivity>? = null

    @After
    fun cleanUp() {
        activityScenario?.close()
    }

    @Test
    fun isIndexable_returnFalse() {
        val preference = A11yServiceSettingPreference(mock())
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun getKey_returnCorrectKey() {
        val preference = A11yServiceSettingPreference(mock())
        assertThat(preference.key).isEqualTo("accessibility_service_settings")
    }

    @Test
    fun getTitle_returnCorrectTitle() {
        val preference = A11yServiceSettingPreference(mock())
        assertThat(preference.title).isEqualTo(R.string.accessibility_menu_item_settings)
    }

    @Test
    fun getIntent_hasSettingsActivityCanResolveActivity_returnIntent() {
        val mockShortcutInfo =
            createMockServiceInfo(hasSettingsActivity = true, canResolveActivity = true)
        val preference = A11yServiceSettingPreference(mockShortcutInfo)

        val actualIntent = preference.intent(context)

        assertThat(actualIntent).isNotNull()
        assertThat(actualIntent!!.component)
            .isEqualTo(ComponentName(PACKAGE, SETTINGS_ACTIVITY_NAME))
    }

    @Test
    fun getIntent_hasSettingsActivityCannotResolveActivity_returnNull() {
        val mockShortcutInfo =
            createMockServiceInfo(hasSettingsActivity = true, canResolveActivity = false)
        val preference = A11yServiceSettingPreference(mockShortcutInfo)

        assertThat(preference.intent(context)).isNull()
    }

    @Test
    fun getIntent_hasNoSettingsActivity_returnNull() {
        val mockShortcutInfo =
            createMockServiceInfo(hasSettingsActivity = false, canResolveActivity = false)
        val preference = A11yServiceSettingPreference(mockShortcutInfo)

        assertThat(preference.intent(context)).isNull()
    }

    @TestParameters(
        value =
            [
                "{hasIntent: true, isInSetupWizard: true, expected: false}",
                "{hasIntent: true, isInSetupWizard: false, expected: true}",
                "{hasIntent: false, isInSetupWizard: true, expected: false}",
            ]
    )
    @Test
    fun isAvailable_returnCorrectValue(
        hasIntent: Boolean,
        isInSetupWizard: Boolean,
        expected: Boolean,
    ) {
        val mockShortcutInfo =
            createMockServiceInfo(hasSettingsActivity = hasIntent, canResolveActivity = hasIntent)
        val preference = A11yServiceSettingPreference(mockShortcutInfo)

        assertThat(preference.isAvailable(createContext(isInSetupWizard))).isEqualTo(expected)
    }

    private fun createMockServiceInfo(
        hasSettingsActivity: Boolean,
        canResolveActivity: Boolean,
    ): AccessibilityServiceInfo {
        val mockServiceInfo: AccessibilityServiceInfo = mock {
            on { componentName } doReturn ComponentName(PACKAGE, A11Y_SERVICE_NAME)
            on { settingsActivityName } doReturn
                if (hasSettingsActivity) SETTINGS_ACTIVITY_NAME else null
        }

        val settingActivityComponent = ComponentName(PACKAGE, SETTINGS_ACTIVITY_NAME)
        if (hasSettingsActivity) {
            shadowPackageManager.addActivityIfNotPresent(settingActivityComponent)
        }
        if (canResolveActivity) {
            shadowPackageManager.addIntentFilterForActivity(
                settingActivityComponent,
                IntentFilter(Intent.ACTION_MAIN),
            )
        }
        return mockServiceInfo
    }

    private fun createContext(inSetupWizard: Boolean = false): Context {
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(context, FragmentActivity::class.java)
        )
        var startedActivity: Context? = null
        val intent = Intent(context, FragmentActivity::class.java)
        if (inSetupWizard) {
            intent.putExtra(EXTRA_IS_SETUP_FLOW, true)
        }
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }

    companion object {
        private const val SETTINGS_ACTIVITY_NAME = "SettingsActivity"
        private const val A11Y_SERVICE_NAME = "A11yService"
        private const val PACKAGE = "foo.bar"
    }
}
