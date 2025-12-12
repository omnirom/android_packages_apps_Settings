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

import android.content.Context
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.applications.getPackageInfo
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class AppsStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType = DeviceStateAppFunctionType.GET_APPS

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        return AppListRepositoryImpl(context)
            .loadAndMaybeExcludeSystemApps(context.userId, excludeSystemApp = true)
            .map {
                val items =
                    listOf(
                        DeviceStateItem(
                            key = "version",
                            purpose = "version",
                            jsonValue = context.getPackageInfo(it.packageName)?.versionName,
                        ),
                        DeviceStateItem(
                            key = "packageName",
                            purpose = "packageName",
                            jsonValue = it.packageName,
                        ),
                    )
                PerScreenDeviceStates(
                    description = "App: " + it.applicationInfo.loadLabel(context.packageManager),
                    deviceStateItems = items,
                )
            }
    }
}
