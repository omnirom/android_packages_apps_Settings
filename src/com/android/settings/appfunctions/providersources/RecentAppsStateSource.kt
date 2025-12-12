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

import android.app.usage.UsageStats
import android.content.Context
import android.icu.text.RelativeDateTimeFormatter
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.applications.AppsPreferenceController
import com.android.settings.applications.RecentAppStatsMixin
import com.android.settingslib.utils.StringUtil
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class RecentAppsStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val recentAppStatsMixin = RecentAppStatsMixin(context, RECENT_APP_COUNT)
        recentAppStatsMixin.loadDisplayableRecentApps(RECENT_APP_COUNT)
        val deviceStateItems = mutableListOf<DeviceStateItem>()
        for (app in recentAppStatsMixin.recentApps) {
            val stats: UsageStats? = app.mUsageStats
            if (stats == null) continue

            deviceStateItems.add(
                DeviceStateItem(
                    key = "recent_apps_category_package_${stats.packageName}",
                    purpose = "recent_apps_category_package_${stats.packageName}",
                    jsonValue =
                        StringUtil.formatRelativeTime(
                            context,
                            ((System.currentTimeMillis() - stats.lastTimeUsed).toDouble()),
                            false,
                            RelativeDateTimeFormatter.Style.LONG,
                        ) as String?,
                    hintText = "App: ${stats.packageName}",
                )
            )
        }

        return listOf(
            PerScreenDeviceStates(description = "Apps", deviceStateItems = deviceStateItems)
        )
    }

    companion object {
        private const val RECENT_APP_COUNT = AppsPreferenceController.SHOW_RECENT_APP_COUNT
    }
}
