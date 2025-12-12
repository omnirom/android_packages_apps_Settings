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
import android.content.pm.verify.domain.DomainVerificationManager
import com.android.settings.R
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.applications.intentpicker.IntentPickerUtils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class OpenByDefaultStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val deviceStateItems = mutableListOf<DeviceStateItem>()
        for (app in sharedDeviceStateData.installedApplications) {
            val appName = app.label
            val packageName = app.info.packageName
            val domainVerificationManager =
                context.getSystemService(DomainVerificationManager::class.java)
            val userState =
                IntentPickerUtils.getDomainVerificationUserState(
                    domainVerificationManager,
                    app.info.packageName,
                )
            val stringResId =
                if (userState?.isLinkHandlingAllowed ?: false) R.string.app_launch_open_in_app
                else R.string.app_launch_open_in_browser
            deviceStateItems.add(
                DeviceStateItem(
                    key = "preferred_settings_enabled_package_$packageName",
                    purpose = "preferred_settings_enabled_package_$packageName",
                    jsonValue = context.getString(stringResId),
                    hintText = "App: $appName",
                )
            )
        }

        return listOf(
            PerScreenDeviceStates(
                description = "Open by default",
                deviceStateItems = deviceStateItems,
            )
        )
    }
}
