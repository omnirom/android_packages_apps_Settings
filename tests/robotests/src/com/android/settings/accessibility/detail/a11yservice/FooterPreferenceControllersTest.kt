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

package com.android.settings.accessibility.detail.a11yservice

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInfo
import android.text.Html
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityFooterPreference
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settingslib.accessibility.AccessibilityUtils
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

/** Test for classes in FooterPreferenceControllers */
@RunWith(RobolectricTestParameterInjector::class)
class FooterPreferenceControllersTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Context>()
    private val packageManager: ShadowPackageManager = spy(shadowOf(context.packageManager))
    private val preferenceManager = PreferenceManager(context)
    private val preference = AccessibilityFooterPreference(context).apply { key = PREF_KEY }

    @Before
    fun setUp() {
        packageManager.installPackage(
            PackageInfo().apply { packageName = PLACEHOLDER_PACKAGE_NAME }
        )

        packageManager.addServiceIfNotPresent(PLACEHOLDER_A11Y_SERVICE)
        val preferenceScreen = preferenceManager.createPreferenceScreen(context)
        preferenceScreen.addPreference(preference)
        preferenceManager.setPreferences(preferenceScreen)
    }

    @Test
    fun verifyHtmlFooterText() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        val footerHtmlDescription = "<b>default html description</b><br/>"
        whenever(a11yServiceInfo.loadHtmlDescription(any())).thenReturn(footerHtmlDescription)

        val footerPreferenceController =
            AccessibilityServiceHtmlFooterPreferenceController(context, PREF_KEY)
        footerPreferenceController.initialize(a11yServiceInfo)
        footerPreferenceController.displayPreference(preference.preferenceManager.preferenceScreen)

        assertThat(preference.contentDescription.toString())
            .isEqualTo("About ${DEFAULT_LABEL}\n\ndefault html description\n")
        assertThat(preference.summary.toString())
            .isEqualTo(
                Html.fromHtml(
                        footerHtmlDescription,
                        Html.FROM_HTML_MODE_COMPACT,
                        /* imageGetter= */ null,
                        /* tagHandler= */ null,
                    )
                    .toString()
            )
    }

    @Test
    @TestParameters(
        value =
            [
                "{serviceEnabled: true, crashed: false}",
                "{serviceEnabled: false, crashed: false}",
                "{serviceEnabled: false, crashed: true}",
            ]
    )
    fun showPlainDescriptionText(serviceEnabled: Boolean, crashed: Boolean) {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        val plainTextDescription = "default plain description"
        whenever(a11yServiceInfo.loadDescription(any())).thenReturn(plainTextDescription)
        setServiceEnabled(serviceInfo = a11yServiceInfo, enabled = serviceEnabled)
        a11yServiceInfo.crashed = crashed

        val footerPreferenceController =
            AccessibilityServiceFooterPreferenceController(context, PREF_KEY)
        footerPreferenceController.initialize(a11yServiceInfo)
        footerPreferenceController.displayPreference(preference.preferenceManager.preferenceScreen)

        assertThat(preference.contentDescription.toString())
            .isEqualTo("About ${DEFAULT_LABEL}\n\ndefault plain description")
        assertThat(preference.summary.toString()).isEqualTo(plainTextDescription)
    }

    @Test
    fun serviceEnabled_crashed_showServiceCrashedText() {
        val a11yServiceInfo = spy(createA11yServiceInfo())
        val plainTextDescription = "default plain description"
        whenever(a11yServiceInfo.loadDescription(any())).thenReturn(plainTextDescription)
        setServiceEnabled(serviceInfo = a11yServiceInfo, enabled = true)
        a11yServiceInfo.crashed = true

        val footerPreferenceController =
            AccessibilityServiceFooterPreferenceController(context, PREF_KEY)
        footerPreferenceController.initialize(a11yServiceInfo)
        footerPreferenceController.displayPreference(preference.preferenceManager.preferenceScreen)

        assertThat(preference.contentDescription.toString())
            .isEqualTo("About ${DEFAULT_LABEL}\n\nThis service is malfunctioning.")
        assertThat(preference.summary.toString())
            .isEqualTo(context.getString(R.string.accessibility_description_state_stopped))
    }

    private fun createA11yServiceInfo(
        isAlwaysOnService: Boolean = false
    ): AccessibilityServiceInfo {
        return AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                PLACEHOLDER_A11Y_SERVICE,
                isAlwaysOnService,
            )
            .apply { isAccessibilityTool = true }
    }

    private fun setServiceEnabled(serviceInfo: AccessibilityServiceInfo, enabled: Boolean) {
        AccessibilityUtils.setAccessibilityServiceState(context, serviceInfo.componentName, enabled)
    }

    companion object {
        private const val PREF_KEY = "prefKey"
        private const val PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example"
        private const val A11Y_SERVICE_CLASS_NAME = "fakeA11yServiceClass"
        private const val DEFAULT_LABEL = A11Y_SERVICE_CLASS_NAME
        private val PLACEHOLDER_A11Y_SERVICE =
            ComponentName(PLACEHOLDER_PACKAGE_NAME, A11Y_SERVICE_CLASS_NAME)
    }
}
