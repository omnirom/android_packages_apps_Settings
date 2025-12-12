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
package com.android.settings.spa.app.appinfo

import android.app.appfunctions.AppFunctionManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.appfunctions.AppFunctionAccessUtil
import com.android.settings.appfunctions.AppFunctionManagerWrapperImpl
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@Composable
fun ManageTargetAppFunctionAccessPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val isAvailable =
        remember {
                flow {
                        emit(
                            AppFunctionAccessUtil.isAppFunctionAccessEnabled(context) &&
                                AppFunctionManagerWrapperImpl(context)
                                    .isValidTarget(app.packageName)
                        )
                    }
                    .flowOn(Dispatchers.Default)
            }
            .collectAsStateWithLifecycle(initialValue = false)
    if (!isAvailable.value) return

    val summary = stringResource(R.string.manage_target_app_function_access_settings_summary)
    Preference(
        object : PreferenceModel {
            override val title =
                stringResource(R.string.manage_target_app_function_access_settings_title)
            override val summary = { summary }
            override val onClick = { startManageAppFunctionAccessActivity(context, app) }
        }
    )
}

private fun startManageAppFunctionAccessActivity(context: Context, app: ApplicationInfo) {
    val intent =
        Intent(AppFunctionManager.ACTION_MANAGE_TARGET_APP_FUNCTION_ACCESS)
            .putExtra(Intent.EXTRA_PACKAGE_NAME, app.packageName)
    context.startActivityAsUser(intent, app.userHandle)
}
