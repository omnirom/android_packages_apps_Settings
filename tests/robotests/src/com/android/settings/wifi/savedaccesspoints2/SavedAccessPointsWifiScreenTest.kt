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
package com.android.settings.wifi.savedaccesspoints2

import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.hotspot2.PasspointConfiguration
import android.net.wifi.hotspot2.pps.Credential
import android.net.wifi.hotspot2.pps.HomeSp
import android.os.HandlerThread
import androidx.fragment.app.testing.FragmentScenario
import androidx.preference.PreferenceFragmentCompat
import com.android.settings.Settings.SavedAccessPointsSettingsActivity
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowWifiManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow.extract
import org.robolectric.shadows.ShadowLooper

class SavedAccessPointsWifiScreenTest : SettingsCatalystTestCase() {
    override val flagName = Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4
    override val preferenceScreenCreator = SavedAccessPointsWifiScreen()

    @Test
    fun getLaunchIntent_returnsCorrectActivity() {
        val launchIntent = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(launchIntent.component?.className)
            .isEqualTo(SavedAccessPointsSettingsActivity::class.java.name)
    }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(SavedAccessPointsWifiScreen.KEY)
    }

    @Test
    @Config(shadows = [ShadowWifiManager::class])
    override fun migration() {
        val wifiManager =
            shadowOf(appContext.getSystemService(WifiManager::class.java)) as ShadowWifiManager
        wifiManager.setScanResults(emptyList<ScanResult>())

        // Add a standard wifi configuration to the wifi manager to make sure standard networks are
        // represented properly on screen.
        val wifiConfig =
            WifiConfiguration().apply {
                networkId = 0
                SSID = "Test Id"
                BSSID = "Test Id"
            }
        wifiManager.addNetwork(wifiConfig)

        // Add a passpoint configureation to the wifi manager to make sure subscribed networks are
        // represented properly on screen.
        val passpointConfig =
            PasspointConfiguration().apply {
                homeSp = HomeSp().apply { fqdn = "Test FQDN" }
                credential =
                    Credential().apply {
                        userCredential =
                            Credential.UserCredential().apply { username = "Test Username" }
                    }
            }
        wifiManager.addOrUpdatePasspointConfiguration(passpointConfig)

        super.migration()
    }

    override fun launchFragmentScenario(
        fragmentClass: Class<PreferenceFragmentCompat>
    ): FragmentScenario<PreferenceFragmentCompat> {
        val fragmentScenario = FragmentScenario.launch(fragmentClass)

        // Get the worker thread from the fragment.
        var workerThread: HandlerThread? = null
        fragmentScenario.onFragment {
            workerThread = (it as SavedAccessPointsWifiSettings2).mWorkerThread
        }

        // Get the ShadowLooper instance that is tied to the worker thread in the fragment.
        val workerThreadLooper = ShadowLooper.getAllLoopers().first { it.thread == workerThread }
        val workerThreadShadowLooper = extract<ShadowLooper>(workerThreadLooper)

        // Make sure all main thread tasks are run before continuing, so that the work that will run
        // on the worker thread is triggered.
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Make sure worker thread runs it's tasks before proceeding.
        workerThreadShadowLooper.idle()

        return fragmentScenario
    }
}
