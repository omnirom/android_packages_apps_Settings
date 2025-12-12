/*
 * Copyright (C) 2024 The Android Open Source Project
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

@file:Suppress("DEPRECATION", "MissingPermission")

package com.android.settings.wifi.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TetheringManager
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.os.UserManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager

/**
 * Gets the {@link android.os.UserManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.userManager: UserManager?
    get() = applicationContext.getSystemService(UserManager::class.java)

/** Return true if the context user is an admin user. */
val Context.isAdminUser
    get() = userManager?.isAdminUser

/**
 * Gets the {@link android.net.wifi.WifiManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.wifiManager: WifiManager?
    get() = applicationContext.getSystemService(WifiManager::class.java)

/** Return the {@link android.net.wifi.WifiManager.SoftApConfiguration}. */
var Context.wifiSoftApConfig
    get() = wifiManager?.softApConfiguration
    set(value) {
        value?.also { wifiManager?.softApConfiguration = it }
    }

/** Gets/Sets the SSID for the Soft AP. */
var Context.wifiSoftApSsid
    get() = wifiSoftApConfig?.ssid
    set(value) {
        wifiSoftApConfig?.apply {
            SoftApConfiguration.Builder(this).setSsid(value).build().let { wifiSoftApConfig = it }
        }
    }

/** Gets the tethered Wi-Fi hotspot enabled state. */
val Context.wifiApState
    get() = wifiManager?.wifiApState

/** Gets/Sets the Wi-Fi enabled state. */
var Context.isWifiEnabled: Boolean
    get() = wifiManager?.isWifiEnabled == true
    set(value) {
        wifiManager?.isWifiEnabled = value
    }

/**
 * Gets the {@link android.telephony.TelephonyManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
@get:SuppressLint("MissingPermission")
val Context.telephonyManager: TelephonyManager?
    get() = applicationContext.getSystemService(TelephonyManager::class.java)

/** Returns the number of logical modems currently configured to be activated. */
val Context.activeModemCount
    get() = telephonyManager?.activeModemCount ?: 0

/**
 * Gets the {@link android.telephony.TelephonyManager} system service for Subscription ID.
 *
 * Use application context to get system services to avoid memory leaks.
 */
fun Context.telephonyManager(subId: Int): TelephonyManager? =
    telephonyManager?.createForSubscriptionId(subId)

/**
 * Gets the {@link android.telephony.SubscriptionManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.subscriptionManager: SubscriptionManager?
    get() = applicationContext.getSystemService(SubscriptionManager::class.java)

/**
 * Gets the {@link android.net.TetheringManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.tetheringManager: TetheringManager?
    get() = applicationContext.getSystemService(TetheringManager::class.java)

/**
 * Gets the {@link android.net.ConnectivityManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.connectivityManager: ConnectivityManager?
    get() = applicationContext.getSystemService(ConnectivityManager::class.java)

/** Return true if the default network is a Wi-Fi network */
val Context.isDefaultNetworkWifi: Boolean
    get() =
        connectivityManager
            ?.getNetworkCapabilities(connectivityManager?.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

/**
 * Gets the {@link android.location.LocationManager} system service.
 *
 * Use application context to get system services to avoid memory leaks.
 */
val Context.locationManager: LocationManager?
    get() = applicationContext.getSystemService(LocationManager::class.java)
