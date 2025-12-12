/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions.providers

import android.app.appsearch.GenericDocument
import android.content.Context
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateProviderExecutorResult
import com.android.settings.appfunctions.providersources.AdaptiveBrightnessStateSource
import com.android.settings.appfunctions.providersources.AppsStateSource
import com.android.settings.appfunctions.providersources.AppsStorageStateSource
import com.android.settings.appfunctions.providersources.BatterySaverStateSource
import com.android.settings.appfunctions.providersources.BatteryStatusStateSource
import com.android.settings.appfunctions.providersources.BubblesStateSource
import com.android.settings.appfunctions.providersources.DeviceStateSource
import com.android.settings.appfunctions.providersources.LockScreenStateSource
import com.android.settings.appfunctions.providersources.ManagedProfileStateSource
import com.android.settings.appfunctions.providersources.MediaOutputStateSource
import com.android.settings.appfunctions.providersources.MobileDataUsageStateSource
import com.android.settings.appfunctions.providersources.MobileNetworkStateSource
import com.android.settings.appfunctions.providersources.NfcStateSource
import com.android.settings.appfunctions.providersources.NotificationHistoryStateSource
import com.android.settings.appfunctions.providersources.NotificationsStateSource
import com.android.settings.appfunctions.providersources.OpenByDefaultStateSource
import com.android.settings.appfunctions.providersources.RecentAppsStateSource
import com.android.settings.appfunctions.providersources.ScreenTimeoutStateSource
import com.android.settings.appfunctions.providersources.SharedDeviceStateData
import com.android.settings.appfunctions.providersources.WifiStatusStateSource
import com.android.settings.appfunctions.providersources.ZenModesStateSource
import com.android.settings.fuelgauge.batteryusage.BatteryUsageStateSource
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/**
 * A [DeviceStateExecutor] that gathers device state information directly from Android APIs rather
 * than using Catalyst.
 *
 * @param context The application context.
 */
class AndroidApiStateProviderExecutor(private val context: Context) : DeviceStateExecutor {
    private val sharedDeviceStateData = SharedDeviceStateData(context)

    // List of all active DeviceStateSource
    private val settingStates: List<DeviceStateSource> =
        listOf(
            AdaptiveBrightnessStateSource(),
            AppsStorageStateSource(),
            BatterySaverStateSource(),
            BatteryStatusStateSource(),
            BatteryUsageStateSource(),
            BubblesStateSource(),
            LockScreenStateSource(),
            ManagedProfileStateSource(),
            MediaOutputStateSource(),
            MobileDataUsageStateSource(),
            MobileNetworkStateSource(),
            NfcStateSource(),
            NotificationHistoryStateSource(),
            NotificationsStateSource(),
            OpenByDefaultStateSource(),
            RecentAppsStateSource(),
            ScreenTimeoutStateSource(),
            WifiStatusStateSource(),
            ZenModesStateSource(),
            AppsStateSource(),
            // Add other sources instances here
        )

    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateProviderExecutorResult {
        sharedDeviceStateData.initialize()

        val states = coroutineScope {
            val semaphore = Semaphore(MAX_PARALLELISM)

            settingStates
                .filter { it.appFunctionType == appFunctionType }
                .map { provider ->
                    async {
                        try {
                            withTimeout(PER_SCREEN_TIMEOUT_MS) {
                                semaphore.withPermit {
                                    val providerName = provider::class.simpleName
                                    try {
                                        Log.v(TAG, "Getting device state from $providerName")
                                        val state = provider.get(context, sharedDeviceStateData)
                                        Log.v(TAG, "Got device state from $providerName")
                                        state
                                    } catch (e: Exception) {
                                        Log.e(
                                            TAG,
                                            "Error getting device state from $providerName",
                                            e,
                                        )
                                        null
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            Log.e(
                                TAG,
                                "Timed out getting state from provider: $provider::class.simpleName",
                                e,
                            )
                            null
                        }
                    }
                }
                .mapNotNull { it.await() }
        }

        return DeviceStateProviderExecutorResult(states = states.flatten())
    }

    companion object {
        private const val TAG = "AndroidApiStateProviderExecutor"
        private const val MAX_PARALLELISM = 3
        private val PER_SCREEN_TIMEOUT_MS = 5.seconds
    }
}
