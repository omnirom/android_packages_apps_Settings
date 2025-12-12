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

package com.android.settings.accessibility.shared.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestParameterInjector::class)
class LaunchAppInfoPreferenceTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private var activityScenario: ActivityScenario<FragmentActivity>? = null
    private val launchAppInfoPreference =
        LaunchAppInfoPreference(key = "appInfo", packageName = PACKAGE)

    @After
    fun cleanUp() {
        activityScenario?.close()
    }

    @Test
    fun isIndexable_returnFalse() {
        assertThat(launchAppInfoPreference.indexable).isFalse()
    }

    @TestParameters(
        value =
            [
                "{isInSetupWizard: true, isPackageInstalled: false, expected: false}",
                "{isInSetupWizard: true, isPackageInstalled: true, expected: false}",
                "{isInSetupWizard: false, isPackageInstalled: false, expected: false}",
                "{isInSetupWizard: false, isPackageInstalled: true, expected: true}",
            ]
    )
    @Test
    fun isAvailable(isInSetupWizard: Boolean, isPackageInstalled: Boolean, expected: Boolean) {
        val mockPackageManager =
            mock<PackageManager> {
                on { isPackageAvailable(PACKAGE) } doReturn (isPackageInstalled)
            }
        val context =
            spy(createContext(isInSetupWizard)) {
                on { packageManager } doReturn (mockPackageManager)
            }

        assertThat(launchAppInfoPreference.isAvailable(context)).isEqualTo(expected)
    }

    @Test
    fun intent_isPackageInstalled_returnIntent() {
        val mockPackageManager =
            mock<PackageManager> { on { isPackageAvailable(PACKAGE) } doReturn true }
        val context = spy(createContext()) { on { packageManager } doReturn (mockPackageManager) }

        val intent = launchAppInfoPreference.intent(context)
        assertThat(intent).isNotNull()
        assertThat(intent!!.action).isEqualTo("android.settings.APPLICATION_DETAILS_SETTINGS")
        assertThat(intent.data).isEqualTo("package:$PACKAGE".toUri())
    }

    private fun createContext(inSetupWizard: Boolean = false): Context {
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(context, FragmentActivity::class.java)
        )
        var startedActivity: Context? = null
        val intent = Intent(context, FragmentActivity::class.java)
        if (inSetupWizard) {
            intent.putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard)
        }
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }

    companion object {
        private const val PACKAGE = "foo.bar"
    }
}
