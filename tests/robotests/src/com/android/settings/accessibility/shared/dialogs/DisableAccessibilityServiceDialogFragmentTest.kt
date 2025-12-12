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

package com.android.settings.accessibility.shared.dialogs

import android.app.settings.SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_DISABLE
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.useService
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.accessibility.AccessibilityUtils
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Tests for [DisableAccessibilityServiceDialogFragment] */
@RunWith(RobolectricTestRunner::class)
class DisableAccessibilityServiceDialogFragmentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowAccessibilityManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private val fakeComponentName = ComponentName("FakePackage", "StandardA11yService")
    private val serviceInfo =
        AccessibilityTestUtils.createAccessibilityServiceInfo(
            context,
            fakeComponentName,
            /* isAlwaysOnService= */ false,
        )
    private lateinit var fragmentScenario: FragmentScenario<Fragment>
    private lateinit var fragment: Fragment

    @Before
    fun setUp() {
        shadowAccessibilityManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        fragmentScenario = launchFragment(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
        fragmentScenario.onFragment { frag -> fragment = frag }
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        fragmentScenario.close()
    }

    @Test
    fun showDialog_verifyUI() {
        val alertDialog = launchDialog()

        assertThat(shadowOf(alertDialog).title.toString())
            .isEqualTo(context.getString(R.string.disable_service_title, "StandardA11yService"))

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).text.toString())
            .isEqualTo(context.getString(R.string.accessibility_dialog_button_stop))

        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).text.toString())
            .isEqualTo(context.getString(R.string.accessibility_dialog_button_cancel))
    }

    @Test
    fun clickPositiveButton_serviceTurnedOff() {
        val alertDialog = launchDialog()

        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
            )
            .doesNotContain(fakeComponentName)
    }

    @Test
    fun clickNegativeButton_serviceKeepsOn() {
        serviceInfo.useService(context, enabled = true)

        val alertDialog = launchDialog()

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
            )
            .contains(fakeComponentName)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(DisableAccessibilityServiceDialogFragment().metricsCategory)
            .isEqualTo(DIALOG_ACCESSIBILITY_SERVICE_DISABLE)
    }

    private fun launchDialog(): AlertDialog {
        DisableAccessibilityServiceDialogFragment.showDialog(
            fragmentManager = fragment.childFragmentManager,
            componentName = fakeComponentName,
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        return ShadowDialog.getLatestDialog() as AlertDialog
    }
}
