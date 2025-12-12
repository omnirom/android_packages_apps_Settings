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

import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.testing.launchFragment
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.bluetooth.HearingAidPairingDialogFragment
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.shadow.ShadowBluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager
import com.android.settingslib.bluetooth.HearingAidInfo
import com.android.settingslib.bluetooth.HearingAidProfile
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowBluetoothUtils::class])
@RunWith(RobolectricTestRunner::class)
class AvailableHearingDevicePreferenceCategoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)
    private val preferenceCategory = AvailableHearingDevicePreferenceCategory(context, 0)

    @Before
    fun setUp() {
        val mockCachedDeviceManager =
            mock<CachedBluetoothDeviceManager> {
                on { findDevice(any()) } doReturn mock<CachedBluetoothDevice>()
            }
        val mockLocalBluetoothManager =
            mock<LocalBluetoothManager> {
                on { cachedDeviceManager } doReturn mockCachedDeviceManager
            }
        ShadowBluetoothUtils.sLocalBluetoothManager = mockLocalBluetoothManager
    }

    @Test
    fun onActiveDeviceChanged_validDevice_launchDialog() {
        launchFragment<Fragment>(themeResId = androidx.appcompat.R.style.Theme_AppCompat).use {
            fragmentScenario ->
            fragmentScenario.onFragment { fragment ->
                assertThat(fragment).isNotNull()
                val category: PreferenceCategory =
                    preferenceCategory.createAndBindWidget(context, preferenceScreen)
                preferenceScreen.addPreference(category)
                val mockPackageManager =
                    mock<PackageManager> {
                        on { hasSystemFeature(PackageManager.FEATURE_BLUETOOTH) } doReturn true
                    }
                val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
                    on { childFragmentManager } doReturn fragment.childFragmentManager
                    on { packageManager } doReturn mockPackageManager
                    on { applicationContext } doReturn mock<Context>()
                    on { getSystemService(AudioManager::class.java) } doReturn mock<AudioManager>()
                }

                preferenceCategory.onCreate(preferenceLifecycleContext)
                preferenceCategory.onActiveDeviceChanged(
                    getAshaHearingDevice(),
                    BluetoothProfile.HEARING_AID,
                )

                assertDialogShown(fragment, HearingAidPairingDialogFragment::class.java)
            }
        }
    }

    private fun getAshaHearingDevice(): CachedBluetoothDevice {
        return mock<CachedBluetoothDevice> {
            on { profiles } doReturn listOf(mock<HearingAidProfile>())
            on { isConnectedAshaHearingAidDevice } doReturn true
            on { deviceMode } doReturn HearingAidInfo.DeviceMode.MODE_BINAURAL
            on { deviceSide } doReturn HearingAidInfo.DeviceSide.SIDE_LEFT
            on { subDevice } doReturn null
            on { address } doReturn "11:22:33:44:55:66"
        }
    }
}
