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

package com.android.settings.appfunctions.providersources

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Provides data shared by multiple [DeviceStateSource]s, which is computed lazily. */
class SharedDeviceStateData(private val context: Context) {
    lateinit var installedApplications: List<InstalledApplication>

    suspend fun initialize() {
        if (::installedApplications.isInitialized) return
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val repo = AppListRepositoryImpl(appContext)
            val packageManager = appContext.packageManager
            val applicationsInfo = repo.loadAndMaybeExcludeSystemApps(appContext.userId, true)

            installedApplications =
                applicationsInfo.mapNotNull { info ->
                    try {
                        InstalledApplication(
                            info = info,
                            label = packageManager.getApplicationLabel(info).toString(),
                        )
                    } catch (e: Exception) {
                        // error handling: if one app fails, log it and skip it.
                        Log.w(
                            "SharedDeviceStateData",
                            "Could not load label for ${info.packageName}",
                            e,
                        )
                        null
                    }
                }
        }
    }

    data class InstalledApplication(val info: ApplicationInfo, val label: String)
}
