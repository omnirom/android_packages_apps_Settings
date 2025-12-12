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

package com.android.settings.accessibility.detail.a11yservice.data

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadow.api.Shadow

/** Tests for [ShortcutDataStore]. */
@RunWith(RobolectricTestParameterInjector::class)
class ShortcutDataStoreTest {
    @get:Rule val settingStoreRule = SettingsStoreRule()
    private val testScope = TestScope()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fakeComponentName = ComponentName("FakePackage", "StandardA11yService")
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private val serviceInfo =
        spy(
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )
        )

    @After
    fun cleanUp() {
        testScope.cancel()
    }

    @Test
    fun init_serviceTargetSdkLessThanR_saveHardwareShortcutAsPreferredShortcut() {
        serviceInfo.resolveInfo.serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.Q
        PreferredShortcuts.saveUserShortcutType(
            context,
            PreferredShortcut(
                serviceInfo.componentName.flattenToString(),
                SOFTWARE or QUICK_SETTINGS,
            ),
        )

        createDataStore()

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                    context,
                    serviceInfo.componentName.flattenToString(),
                    DEFAULT,
                )
            )
            .isEqualTo(HARDWARE)
    }

    @Test
    fun init_serviceTargetSdkAtLeastR_preferredShortcutsUnchanged() {
        val preferredShortcuts = SOFTWARE or QUICK_SETTINGS
        PreferredShortcuts.saveUserShortcutType(
            context,
            PreferredShortcut(serviceInfo.componentName.flattenToString(), preferredShortcuts),
        )

        createDataStore()

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                    context,
                    serviceInfo.componentName.flattenToString(),
                    DEFAULT,
                )
            )
            .isEqualTo(preferredShortcuts)
    }

    @TestParameters(
        value =
            [
                "{targetSdkIsAtLeastR: true, isAccessibilityTool: true, " +
                    "hasQsTileName: true, expectedDefaultShortcutType: $QUICK_SETTINGS}",
                "{targetSdkIsAtLeastR: true, isAccessibilityTool: true, " +
                    "hasQsTileName: false, expectedDefaultShortcutType: $SOFTWARE}",
                "{targetSdkIsAtLeastR: true, isAccessibilityTool: false, " +
                    "hasQsTileName: false, expectedDefaultShortcutType: $SOFTWARE}",
                "{targetSdkIsAtLeastR: false, isAccessibilityTool: true, " +
                    "hasQsTileName: false, expectedDefaultShortcutType: $HARDWARE}",
            ]
    )
    @Test
    fun turnOnShortcut_turnsOnDefaultShortcutType(
        targetSdkIsAtLeastR: Boolean,
        isAccessibilityTool: Boolean,
        hasQsTileName: Boolean,
        expectedDefaultShortcutType: Int,
    ) {

        if (!targetSdkIsAtLeastR) {
            serviceInfo.resolveInfo.serviceInfo.applicationInfo.targetSdkVersion =
                Build.VERSION_CODES.Q
        }
        serviceInfo.isAccessibilityTool = isAccessibilityTool

        if (hasQsTileName) {
            whenever(serviceInfo.tileServiceName).thenReturn("Fake Tile name")
        }
        val storage = createDataStore()

        storage.setBoolean("key", true)

        assertThat(a11yManager.getAccessibilityShortcutTargets(expectedDefaultShortcutType))
            .contains(serviceInfo.componentName.flattenToString())
    }

    private fun createDataStore(): ShortcutDataStore =
        ShortcutDataStore(
            context,
            serviceInfo,
            testScope.backgroundScope,
            SettingsSecureStore.get(context),
        )
}
