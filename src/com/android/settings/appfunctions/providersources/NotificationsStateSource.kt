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

import android.app.INotificationManager
import android.app.usage.IUsageStatsManager
import android.app.usage.UsageEvents
import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import android.os.ServiceManager
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settingslib.utils.StringUtil
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import java.util.concurrent.TimeUnit

class NotificationsStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_NOTIFICATIONS

    // TODO: prototype implementation, to be replaced in b/426005115
    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val packageManager = context.packageManager
        val notificationManager =
            INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE)
            )
        val usageStatsManager =
            IUsageStatsManager.Stub.asInterface(
                ServiceManager.getService(Context.USAGE_STATS_SERVICE)
            )

        val nowMs = System.currentTimeMillis()
        val lastNotificationTimeMsByPackage =
            queryLastNotificationTimes(context, nowMs, usageStatsManager)

        val deviceStateItems = mutableListOf<DeviceStateItem>()
        for (app in sharedDeviceStateData.installedApplications) {
            val packageName = app.info.packageName
            val appName = app.label
            val uid = app.info.uid
            val areNotificationsEnabled =
                notificationManager?.areNotificationsEnabledForPackage(packageName, uid) ?: false
            val lastNotificationTimeAgoMs =
                lastNotificationTimeMsByPackage[packageName]?.let { nowMs - it }
            val formattedLastNotificationTimeAgo =
                lastNotificationTimeAgoMs?.let {
                    StringUtil.formatRelativeTime(
                            context,
                            it.toDouble(),
                            true, // withSeconds
                            RelativeDateTimeFormatter.Style.LONG,
                        )
                        .toString()
                }

            deviceStateItems.add(
                DeviceStateItem(
                    key = "notifications_enabled_package_$packageName",
                    purpose = "notifications_enabled_package_$packageName",
                    jsonValue = areNotificationsEnabled.toString(),
                    hintText = "App: $appName",
                )
            )
            formattedLastNotificationTimeAgo?.let { timeAgo ->
                deviceStateItems.add(
                    DeviceStateItem(
                        key = "notifications_last_notification_time_package_$packageName",
                        purpose = "notifications_last_notification_time_package_$packageName",
                        jsonValue = timeAgo,
                        hintText = "App: $appName",
                    )
                )
            }
        }

        return listOf(
            PerScreenDeviceStates(
                description =
                    "Notifications Settings Screen. Note that to get to the notification settings for a given package, the intent uri is intent:#Intent;action=android.settings.APP_NOTIFICATION_SETTINGS;S.android.provider.extra.APP_PACKAGE=\$packageName;end",
                deviceStateItems = deviceStateItems,
            )
        )
    }

    private fun queryLastNotificationTimes(
        context: Context,
        nowMs: Long,
        usageStatsManager: IUsageStatsManager,
    ): Map<String, Long> {
        val startTimeMs = nowMs - LAST_NOTIFICATION_TIME_CUTOFF_MS
        return usageStatsManager
            .queryEvents(startTimeMs, nowMs, context.packageName)
            .getNotificationEvents()
            .groupBy { it.packageName }
            .mapValues { (_, packageEvents) -> packageEvents.maxOf { it.notificationTimeMs } }
    }

    private companion object {
        /**
         * This value comes from
         * [com.android.settings.spa.notification.AppNotificationRepository.DAYS_TO_CHECK].
         */
        val LAST_NOTIFICATION_TIME_CUTOFF_MS = TimeUnit.DAYS.toMillis(7)
    }
}

private data class NotificationEvent(val packageName: String, val notificationTimeMs: Long)

private fun UsageEvents.getNotificationEvents() = sequence {
    val event = UsageEvents.Event()
    while (getNextEvent(event)) {
        if (event.eventType == UsageEvents.Event.NOTIFICATION_INTERRUPTION) {
            yield(
                NotificationEvent(
                    packageName = event.packageName,
                    notificationTimeMs = event.timeStamp,
                )
            )
        }
    }
}
