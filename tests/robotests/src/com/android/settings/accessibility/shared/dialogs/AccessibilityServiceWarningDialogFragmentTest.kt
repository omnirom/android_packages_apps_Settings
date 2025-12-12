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

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragment
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment.Companion.RESULT_STATUS_ALLOW
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment.Companion.RESULT_STATUS_DENY
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment.Companion.getResultDialogContext
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment.Companion.getResultStatus
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Tests for [AccessibilityServiceWarningDialogFragment] */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceWarningDialogFragmentTest {
    @get:Rule val mockito = MockitoJUnit.rule()
    private val requestKey = "requestFromTest"
    private val sourceDialogEnum = DialogEnums.ENABLE_WARNING_FROM_TOGGLE
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
    @Mock private lateinit var mockFragResultListener: FragmentResultListener
    @Captor lateinit var responseCaptor: ArgumentCaptor<Bundle>

    @Before
    fun setUp() {
        shadowAccessibilityManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
        fragmentScenario = launchFragment(themeResId = androidx.appcompat.R.style.Theme_AppCompat)
        fragmentScenario.onFragment { frag ->
            fragment = frag
            fragment.childFragmentManager.setFragmentResultListener(
                requestKey,
                fragment,
                mockFragResultListener,
            )
        }
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        fragmentScenario.close()
    }

    @Test
    fun clickAllowButton_replyAllow() {
        getPermissionAllowButton(launchDialog())!!.performClick()

        verify(mockFragResultListener).onFragmentResult(eq(requestKey), responseCaptor.capture())
        val response = responseCaptor.value
        assertThat(getResultStatus(response)).isEqualTo(RESULT_STATUS_ALLOW)
        assertThat(getResultDialogContext(response)).isEqualTo(sourceDialogEnum)
    }

    @Test
    fun clickDeny_replyDeny() {
        getPermissionDenyButton(launchDialog())!!.performClick()

        verify(mockFragResultListener).onFragmentResult(eq(requestKey), responseCaptor.capture())
        val response = responseCaptor.value
        assertThat(getResultStatus(response)).isEqualTo(RESULT_STATUS_DENY)
        assertThat(getResultDialogContext(response)).isEqualTo(sourceDialogEnum)
    }

    @Test
    fun clickUninstallButton_sendUninstallRequest() {
        val alertDialog = launchDialog()
        getUninstallAppButton(alertDialog)!!.performClick()

        verify(mockFragResultListener, never()).onFragmentResult(any(), any())
        val intent =
            Shadows.shadowOf(alertDialog.context as ContextWrapper).peekNextStartedActivity()

        assertThat(intent.action).isEqualTo(Intent.ACTION_UNINSTALL_PACKAGE)
        assertThat(intent.data).isEqualTo(("package:${fakeComponentName.packageName}").toUri())
    }

    @Test
    fun getMetricsCategory() {
        assertThat(AccessibilityServiceWarningDialogFragment().metricsCategory)
            .isEqualTo(SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_ENABLE)
    }

    private fun launchDialog(): AlertDialog {
        AccessibilityServiceWarningDialogFragment.showDialog(
            fragmentManager = fragment.childFragmentManager,
            componentName = fakeComponentName,
            source = sourceDialogEnum,
            requestKey = requestKey,
        )
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        return ShadowDialog.getLatestDialog() as AlertDialog
    }

    private fun getPermissionAllowButton(alertDialog: AlertDialog): Button? {
        return alertDialog.findViewById(
            com.android.internal.R.id.accessibility_permission_enable_allow_button
        )
    }

    private fun getPermissionDenyButton(alertDialog: AlertDialog): Button? {
        return alertDialog.findViewById(
            com.android.internal.R.id.accessibility_permission_enable_deny_button
        )
    }

    private fun getUninstallAppButton(alertDialog: AlertDialog): Button? {
        return alertDialog.findViewById(
            com.android.internal.R.id.accessibility_permission_enable_uninstall_button
        )
    }
}
