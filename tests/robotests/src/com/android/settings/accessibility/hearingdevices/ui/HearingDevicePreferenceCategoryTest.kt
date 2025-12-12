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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.bluetooth.BluetoothDeviceUpdater
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
class HearingDevicePreferenceCategoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val packageManager: ShadowPackageManager = shadowOf(context.packageManager)
    private val preferenceCategory =
        TestHearingDevicePreferenceCategory().apply {
            createAndBindWidget<PreferenceCategory>(context)
            prepareForTest()
        }

    @Test
    fun isAvailable_bluetoothSupported_returnTrue() {
        packageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true)

        assertThat(preferenceCategory.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_bluetoothNotSupported_returnFalse() {
        packageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false)

        assertThat(preferenceCategory.isAvailable(context)).isFalse()
    }

    @Test
    fun onDeviceAdded_preferenceCategoryVisible() {
        preferenceCategory.onDeviceAdded(mock<Preference>())

        assertThat(preferenceCategory.category?.preferenceCount).isEqualTo(1)
        assertThat(preferenceCategory.category?.isVisible).isTrue()
    }

    @Test
    fun onDeviceRemoved_addedPreferenceFirst_preferenceCategoryInVisible() {
        val preference = mock<Preference>()
        preferenceCategory.onDeviceAdded(preference)
        preferenceCategory.onDeviceRemoved(preference)

        assertThat(preferenceCategory.category?.preferenceCount).isEqualTo(0)
        assertThat(preferenceCategory.category?.isVisible).isFalse()
    }

    @Test
    fun onCreate_deviceUpdaterCreatedAndInitialized() {
        val mockPackageManager =
            mock<PackageManager> {
                on { hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) } doReturn true
            }
        val mockPreferenceLifecycleContext =
            mock<PreferenceLifecycleContext> { on { packageManager } doReturn mockPackageManager }

        preferenceCategory.onCreate(mockPreferenceLifecycleContext)

        assertThat(preferenceCategory.deviceUpdater).isNotNull()
        verify(preferenceCategory.deviceUpdater!!).setPrefContext(mockPreferenceLifecycleContext)
        verify(preferenceCategory.deviceUpdater!!).forceUpdate()
    }

    @Test
    fun onStart_deviceUpdaterRegisterCallback() {
        val mockPackageManager =
            mock<PackageManager> {
                on { hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) } doReturn true
            }
        val mockPreferenceLifecycleContext =
            mock<PreferenceLifecycleContext> { on { packageManager } doReturn mockPackageManager }

        preferenceCategory.onCreate(mockPreferenceLifecycleContext)
        preferenceCategory.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceCategory.deviceUpdater).isNotNull()
        verify(preferenceCategory.deviceUpdater!!).registerCallback()
    }

    @Test
    fun onStop_deviceUpdaterUnregisterCallback() {
        val mockPackageManager =
            mock<PackageManager> {
                on { hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) } doReturn true
            }
        val mockPreferenceLifecycleContext =
            mock<PreferenceLifecycleContext> { on { packageManager } doReturn mockPackageManager }

        preferenceCategory.onCreate(mockPreferenceLifecycleContext)
        preferenceCategory.onStop(mockPreferenceLifecycleContext)

        assertThat(preferenceCategory.deviceUpdater).isNotNull()
        verify(preferenceCategory.deviceUpdater!!).unregisterCallback()
    }

    class TestHearingDevicePreferenceCategory(
        override val key: String = "test_key",
        override val title: Int = 0,
    ) : HearingDevicePreferenceCategory(key, title) {
        override fun createDeviceUpdater(context: Context) = mock<BluetoothDeviceUpdater>()

        fun prepareForTest() {
            category =
                spy(category) { on { this?.preferenceManager } doReturn mock<PreferenceManager>() }
        }
    }
}
