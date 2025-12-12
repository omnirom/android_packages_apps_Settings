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

package com.android.settings.accessibility.extensions

import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.accessibility.AccessibilityUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

/** Tests for AccessibilityServiceInfo extension functions */
@RunWith(RobolectricTestRunner::class)
class AccessibilityServiceInfoExtTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowAccessibilityManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private val fakeComponentName = ComponentName("FakePackage", "StandardA11yService")

    @Test
    fun getFeatureName() {
        val serviceInfo =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )

        assertThat(serviceInfo.getFeatureName(context).toString()).isEqualTo("StandardA11yService")
    }

    @Test
    fun isServiceWarningRequired_required_returnTrue() {
        val serviceInfo =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )

        assertThat(serviceInfo.isServiceWarningRequired(context)).isTrue()
    }

    @Test
    fun isServiceWarningRequired_notRequired_returnFalse() {
        val serviceInfo =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )
        shadowAccessibilityManager.setAccessibilityServiceWarningExempted(fakeComponentName)

        assertThat(serviceInfo.isServiceWarningRequired(context)).isFalse()
    }

    @Test
    fun targetSdkIsAtLeast() {
        val serviceInfo =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )
        val targetSdk = serviceInfo.resolveInfo.serviceInfo.applicationInfo.targetSdkVersion

        assertThat(serviceInfo.targetSdkIsAtLeast(targetSdk + 1)).isFalse()
        assertThat(serviceInfo.targetSdkIsAtLeast(targetSdk)).isTrue()
        assertThat(serviceInfo.targetSdkIsAtLeast(targetSdk - 1)).isTrue()
    }

    @Test
    fun useService_turnOn_enabledServiceSettingContainsService() {
        val serviceInfo =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )
        serviceInfo.useService(context, enabled = true)

        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
            )
            .contains(fakeComponentName)
    }

    @Test
    fun useService_turnOff_enabledServiceSettingDoesNotService() {
        val serviceInfo =
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )
        serviceInfo.useService(context, enabled = false)

        assertThat(
                AccessibilityUtils.getEnabledServicesFromSettings(context, UserHandle.myUserId())
            )
            .doesNotContain(fakeComponentName)
    }
}
