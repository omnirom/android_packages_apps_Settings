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
package com.android.settings.testutils.shadow

import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.hotspot2.PasspointConfiguration
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(value = WifiManager::class)
open class ShadowWifiManager : org.robolectric.shadows.ShadowWifiManager() {

    val fqdnToPasspointConfiguration: MutableMap<String, PasspointConfiguration> = mutableMapOf()
    val matchingWifiConfigs: List<Pair<WifiConfiguration, Map<Int, List<ScanResult>>>> =
        mutableListOf()

    @Implementation
    fun getPasspointConfigurations(): List<PasspointConfiguration> =
        ArrayList(fqdnToPasspointConfiguration.values)

    @Implementation
    fun getAllMatchingWifiConfigs(
        scanResults: List<ScanResult>
    ): List<Pair<WifiConfiguration, Map<Int, List<ScanResult>>>> = matchingWifiConfigs

    // Make implementation public so it can be called in tests.
    @Implementation
    public override fun addNetwork(config: WifiConfiguration): Int {
        return super.addNetwork(config)
    }

    @Implementation
    fun addOrUpdatePasspointConfiguration(config: PasspointConfiguration) {

        val fqdn =
            config.homeSp.fqdn
                ?: throw IllegalStateException(
                    "FQDN is null, unable to add or update PasspointConfiguration"
                )
        fqdnToPasspointConfiguration[fqdn] = config
    }
}
