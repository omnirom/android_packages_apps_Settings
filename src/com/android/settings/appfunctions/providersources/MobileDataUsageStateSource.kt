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

package com.android.settings.appfunctions.providersources

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.NetworkTemplate
import com.android.settings.Utils
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.datausage.lib.DataUsageFormatter
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import java.util.concurrent.TimeUnit

class MobileDataUsageStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val networkStatsManager = context.getSystemService(NetworkStatsManager::class.java)

        // Determine the most recent cycle.
        val allTimeNetworkStats =
            networkStatsManager.queryDetailsForDevice(
                MOBILE_NETWORK_TEMPLATE,
                Long.MIN_VALUE,
                Long.MAX_VALUE,
            )
        var endTimeMs = 0L
        allTimeNetworkStats.forEachBucket { bucket ->
            endTimeMs = maxOf(endTimeMs, bucket.endTimeStamp)
        }
        val startTimeMs = endTimeMs - CYCLE_LENGTH_MS

        val networkStats =
            networkStatsManager.querySummary(MOBILE_NETWORK_TEMPLATE, startTimeMs, endTimeMs)

        val startTimeByUid = mutableMapOf<Int, Long>()
        val endTimeByUid = mutableMapOf<Int, Long>()
        val foregroundBytesByUid = mutableMapOf<Int, Long>()
        val backgroundBytesByUid = mutableMapOf<Int, Long>()
        networkStats.forEachBucket { bucket ->
            startTimeByUid.update(bucket.uid, Long.MAX_VALUE) { minOf(it, bucket.startTimeStamp) }
            endTimeByUid.update(bucket.uid, Long.MIN_VALUE) { maxOf(it, bucket.endTimeStamp) }
            val mapToUpdate =
                if (bucket.state == NetworkStats.Bucket.STATE_FOREGROUND) {
                    foregroundBytesByUid
                } else {
                    backgroundBytesByUid
                }
            mapToUpdate.update(bucket.uid, 0L) { it + bucket.rxBytes + bucket.txBytes }
        }

        val dataUsageFormatter = DataUsageFormatter(context)

        val deviceStateItems = mutableListOf<DeviceStateItem>()
        for (app in sharedDeviceStateData.installedApplications) {
            val startTimeMs = startTimeByUid[app.info.uid] ?: continue
            val endTimeMs = endTimeByUid[app.info.uid] ?: continue
            val foregroundBytes = foregroundBytesByUid[app.info.uid] ?: 0L
            val backgroundBytes = backgroundBytesByUid[app.info.uid] ?: 0L
            val totalBytes = foregroundBytes + backgroundBytes

            deviceStateItems.add(
                DeviceStateItem(
                    key = "mobile_data_usage_cycle_${app.info.packageName}",
                    purpose = "mobile_data_usage_cycle_${app.info.packageName}",
                    jsonValue = Utils.formatDateRange(context, startTimeMs, endTimeMs),
                    hintText = "App: ${app.label}",
                )
            )
            deviceStateItems.add(
                DeviceStateItem(
                    key = "mobile_data_usage_total_${app.info.packageName}",
                    purpose = "mobile_data_usage_total_${app.info.packageName}",
                    jsonValue = dataUsageFormatter.formatDataUsage(totalBytes),
                    hintText = "App: ${app.label}",
                )
            )
            deviceStateItems.add(
                DeviceStateItem(
                    key = "mobile_data_usage_foreground_${app.info.packageName}",
                    purpose = "mobile_data_usage_foreground_${app.info.packageName}",
                    jsonValue = dataUsageFormatter.formatDataUsage(foregroundBytes),
                    hintText = "App: ${app.label}",
                )
            )
            deviceStateItems.add(
                DeviceStateItem(
                    key = "mobile_data_usage_background_${app.info.packageName}",
                    purpose = "mobile_data_usage_background_${app.info.packageName}",
                    jsonValue = dataUsageFormatter.formatDataUsage(backgroundBytes),
                    hintText = "App: ${app.label}",
                )
            )
        }

        return listOf(
            PerScreenDeviceStates(
                description =
                    "Mobile Data Usage. This app data usage screen displays the total data consumed by this application. This includes usage from all SIM cards and different networks if the user has multiple.",
                deviceStateItems = deviceStateItems,
            )
        )
    }

    companion object {
        private val MOBILE_NETWORK_TEMPLATE =
            NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE).build()

        /**
         * This value is based on
         * [com.android.settings.datausage.lib.NetworkCycleDataRepository.queryCyclesAsFourWeeks].
         */
        private val CYCLE_LENGTH_MS = TimeUnit.DAYS.toMillis(4 * 7)
    }
}

private fun NetworkStats.forEachBucket(action: (NetworkStats.Bucket) -> Unit) {
    val bucket = NetworkStats.Bucket()
    while (getNextBucket(bucket)) {
        action(bucket)
    }
}

private fun <K, V> MutableMap<K, V>.update(key: K, default: V, block: (V) -> V) {
    put(key, block(getOrDefault(key, default)))
}
