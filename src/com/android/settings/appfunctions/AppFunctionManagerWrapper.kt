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
package com.android.settings.appfunctions

import android.app.appfunctions.AppFunctionManager
import android.content.Context
import com.android.settings.appfunctions.AppFunctionAccessUtil.isAppFunctionAccessEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A helper class used for tests. AppFunctionManager cannot be mocked out because it's marked final.
 * This class can be mocked out instead.
 */
interface AppFunctionManagerWrapper {
    /** Returns {@code true} when the packageName is a valid agent */
    suspend fun isValidAgent(packageName: String): Boolean

    /** Returns {@code false} when the packageName is a valid agent */
    suspend fun isValidTarget(packageName: String): Boolean
}

/** [AppFunctionManagerWrapper] helper class implementation */
class AppFunctionManagerWrapperImpl(context: Context) : AppFunctionManagerWrapper {
    private val isAppFunctionAccessEnabled = isAppFunctionAccessEnabled(context)
    private val appFunctionManager =
        context.getSystemService(Context.APP_FUNCTION_SERVICE) as AppFunctionManager?

    override suspend fun isValidAgent(packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!isAppFunctionAccessEnabled) {
                return@withContext false
            }

            return@withContext appFunctionManager?.validAgents?.contains(packageName) ?: false
        }

    override suspend fun isValidTarget(packageName: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!isAppFunctionAccessEnabled) {
                return@withContext false
            }

            return@withContext appFunctionManager?.validTargets?.contains(packageName) ?: false
        }
}
