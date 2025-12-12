/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.accessibilityservice.AccessibilityShortcutInfo
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.SettingsActivity
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.detail.a11yservice.A11yServicePreferenceFragment
import com.android.settings.accessibility.screenmagnification.ui.MagnificationPreferenceFragment
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.androidx.fragment.FragmentController

/** Tests for [AccessibilityDetailsSettingsFragment]. */
@Config(shadows = [ShadowDevicePolicyManager::class, ShadowRestrictedLockUtilsInternal::class])
@RunWith(RobolectricTestRunner::class)
class AccessibilityDetailsSettingsFragmentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        val a11yServiceInfo =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                    context,
                    A11Y_SERVICE_COMPONENT,
                    /* isAlwaysOnService= */ false,
                )
                .apply { isAccessibilityTool = true }
        val a11yShortcutInfo: AccessibilityShortcutInfo = mock<AccessibilityShortcutInfo>()
        whenever(a11yShortcutInfo.componentName).thenReturn(A11Y_ACTIVITY_COMPONENT)
        a11yManager.setInstalledAccessibilityServiceList(listOf(a11yServiceInfo))
        a11yManager.setInstalledAccessibilityShortcutListAsUser(listOf(a11yShortcutInfo))
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun onCreate_afterSuccessfullyLaunch_shouldBeFinished() {
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, A11Y_SERVICE_COMPONENT.flattenToString())

        val fragment = startFragment(intent)

        assertThat(fragment.activity?.isFinishing).isTrue()
    }

    @Test
    fun onCreate_hasValidExtraComponentName_launchExpectedFragment() {
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, A11Y_SERVICE_COMPONENT.flattenToString())

        val fragment = startFragment(intent)

        assertStartActivityWithExpectedFragment(A11yServicePreferenceFragment::class.java.getName())
    }

    @Test
    fun onCreate_hasInvalidExtraComponentName_launchAccessibilitySettings() {
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, "$PACKAGE_NAME/.service")

        startFragment(intent)

        assertStartActivityWithExpectedFragment(AccessibilitySettings::class.java.getName())
    }

    @Test
    fun onCreate_hasNoExtraComponentName_launchAccessibilitySettings() {
        startFragment(/* intent= */ null)

        assertStartActivityWithExpectedFragment(AccessibilitySettings::class.java.getName())
    }

    @Test
    fun onCreate_extraComponentNameIsDisallowed_launchAccessibilitySettings() {
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, A11Y_SERVICE_COMPONENT.flattenToString())
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        (shadowOf(dpm) as ShadowDevicePolicyManager).setPermittedAccessibilityServices(listOf())

        startFragment(intent)

        assertStartActivityWithExpectedFragment(AccessibilitySettings::class.java.getName())
    }

    @Test
    fun onCreate_a11yActivityComponentName_launchA11yActivityFragment() {
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_COMPONENT_NAME, A11Y_ACTIVITY_COMPONENT.flattenToString())

        startFragment(intent)

        assertStartActivityWithExpectedFragment(
            LaunchAccessibilityActivityPreferenceFragment::class.java.getName()
        )
    }

    @Test
    fun onCreate_magnificationComponentName_launchMagnificationFragment() {
        val intent = Intent()
        intent.putExtra(
            Intent.EXTRA_COMPONENT_NAME,
            AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME.flattenToString(),
        )

        startFragment(intent)

        assertStartActivityWithExpectedFragment(
            MagnificationPreferenceFragment::class.java.getName()
        )
    }

    @Test
    fun onCreate_accessibilityButton_launchAccessibilityButtonFragment() {
        val intent = Intent()
        intent.putExtra(
            Intent.EXTRA_COMPONENT_NAME,
            AccessibilityShortcutController.ACCESSIBILITY_BUTTON_COMPONENT_NAME.flattenToString(),
        )

        startFragment(intent)

        assertStartActivityWithExpectedFragment(AccessibilityButtonFragment::class.java.getName())
    }

    @Test
    fun onCreate_hearingAidsComponentName_launchAccessibilityHearingAidsFragment() {
        val intent = Intent()
        intent.putExtra(
            Intent.EXTRA_COMPONENT_NAME,
            AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME
                .flattenToString(),
        )

        startFragment(intent)

        assertStartActivityWithExpectedFragment(
            AccessibilityHearingAidsFragment::class.java.getName()
        )
    }

    @Test
    fun getMetricsCategory_returnsCorrectCategory() {
        val fragment = AccessibilityDetailsSettingsFragment()

        assertThat(fragment.metricsCategory).isEqualTo(SettingsEnums.ACCESSIBILITY_DETAILS_SETTINGS)
    }

    private fun startFragment(intent: Intent?): AccessibilityDetailsSettingsFragment {
        val fragmentController =
            FragmentController.of(AccessibilityDetailsSettingsFragment(), intent).create().visible()

        return fragmentController.get()
    }

    private fun assertStartActivityWithExpectedFragment(fragmentName: String) {
        val intent = Shadows.shadowOf(context as Application).nextStartedActivity
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(fragmentName)
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar"
        private val A11Y_SERVICE_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yService")
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")
    }
}
