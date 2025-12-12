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
package com.android.settings.supervision

import android.app.Application
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settings.supervision.ipc.PreferenceDataApi
import com.android.settings.supervision.ipc.PreferenceDataRequest
import com.android.settings.supervision.ipc.SupportedApp
import com.android.settings.supervision.ipc.SupportedAppsApi
import com.android.settings.supervision.ipc.SupportedAppsRequest
import com.android.settingslib.ipc.ApiDescriptor
import com.android.settingslib.ipc.ApiHandler
import com.android.settingslib.ipc.MessengerService
import com.android.settingslib.ipc.PermissionChecker

class TestSupervisionMessengerService :
    MessengerService(
        listOf(TestPreferenceDataApiImp(), TestSupportedAppsApiImpl()),
        PermissionChecker { _, _, _ -> true },
    )

class TestPreferenceDataApiImp :
    ApiHandler<PreferenceDataRequest, Map<String, PreferenceData>>,
    ApiDescriptor<PreferenceDataRequest, Map<String, PreferenceData>> by PreferenceDataApi() {
    @Volatile var hasPermission = true
    @Volatile var invokeException: Exception? = null
    @Volatile var preferenceData: Map<String, PreferenceData> = mapOf()

    override fun hasPermission(
        application: Application,
        callingPid: Int,
        callingUid: Int,
        request: PreferenceDataRequest,
    ): Boolean {
        return hasPermission
    }

    override suspend fun invoke(
        application: Application,
        callingPid: Int,
        callingUid: Int,
        request: PreferenceDataRequest,
    ): Map<String, PreferenceData> {
        invokeException?.let { throw it }
        return preferenceData
    }
}

class TestSupportedAppsApiImpl :
    ApiHandler<SupportedAppsRequest, Map<String, List<SupportedApp>>>,
    ApiDescriptor<SupportedAppsRequest, Map<String, List<SupportedApp>>> by SupportedAppsApi() {
    @Volatile var hasPermission = true
    @Volatile var invokeException: Exception? = null
    @Volatile
    var supportedApps: Map<String, List<SupportedApp>> =
        mapOf(
            "browser_filters_supported_apps" to
                listOf(
                    SupportedApp(
                        title = "Supported app",
                        summary = "App summary",
                        packageName = "com.android.chrome",
                    )
                )
        )

    override fun hasPermission(
        application: Application,
        callingPid: Int,
        callingUid: Int,
        request: SupportedAppsRequest,
    ): Boolean {
        return hasPermission
    }

    override suspend fun invoke(
        application: Application,
        callingPid: Int,
        callingUid: Int,
        request: SupportedAppsRequest,
    ): Map<String, List<SupportedApp>> {
        invokeException?.let { throw it }
        return supportedApps
    }
}
