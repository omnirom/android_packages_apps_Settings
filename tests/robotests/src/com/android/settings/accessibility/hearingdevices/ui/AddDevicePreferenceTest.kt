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

import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter.STATE_OFF
import android.bluetooth.BluetoothAdapter.STATE_ON
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.SubSettings
import com.android.settings.accessibility.HearingDevicePairingFragment
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowBluetoothAdapter
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
class AddDevicePreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = AddDevicePreference(context)
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: ShadowBluetoothAdapter = shadowOf(bluetoothManager?.adapter)
    private val packageManager: ShadowPackageManager = shadowOf(context.packageManager)

    @Test
    fun isAvailable_bluetoothConfigSupported_returnTrue() {
        packageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, true)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_bluetoothConfigNotSupported_returnFalse() {
        packageManager.setSystemFeature(PackageManager.FEATURE_BLUETOOTH, false)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_bluetoothEnabled_assertSummaryEmpty() {
        bluetoothAdapter.setState(STATE_ON)

        assertThat(preference.getSummary(context)).isEqualTo("")
    }

    @Test
    fun getSummary_bluetoothDisabled_assertSummaryCorrect() {
        bluetoothAdapter.setState(STATE_OFF)

        assertThat(preference.getSummary(context))
            .isEqualTo(context.getString(R.string.connected_device_add_device_summary))
    }

    @Test
    fun intent_returnCorrectIntent() {
        val intent = preference.intent(context)

        assertThat(intent).isNotNull()
        assertThat(intent.action).isEqualTo(Intent.ACTION_MAIN)
        assertThat(intent.component).isEqualTo(ComponentName(context, SubSettings::class.java))
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(HearingDevicePairingFragment::class.java.name)
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, 0))
            .isEqualTo(SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS)
    }
}
