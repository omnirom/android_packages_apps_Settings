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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyResources.Strings.Settings.WORK_PROFILE_DISABLE_USAGE_ACCESS_WARNING
import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import com.android.settings.R
import com.android.settingslib.spa.lifecycle.collectAsCallbackWithLifecycle
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.model.app.AppOpsPermissionController
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.IPackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

object UsageDataAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "UsageAccess"

    override fun createModel(context: Context) = UsageDataAppListModel(context)
}

data class UsageDataAppRecord(
    override val app: ApplicationInfo,
    val isChangeable: Boolean,
    val controller: AppOpsPermissionController,
) : AppRecord

class UsageDataAppListModel(
    private val context: Context,
    private val packageManagers: IPackageManagers = PackageManagers,
    private val dpm: DevicePolicyManager =
        context.getSystemService(DevicePolicyManager::class.java)!!,
) : TogglePermissionAppListModel<UsageDataAppRecord> {
    override val pageTitleResId = R.string.usage_access
    override val switchTitleResId = R.string.permit_usage_access
    override val footerResId = R.string.usage_access_description
    override val enhancedConfirmationKey: String = AppOpsManager.OPSTR_GET_USAGE_STATS

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow
            .map { userId -> packageManagers.getAppOpPermissionPackages(userId, PERMISSION) }
            .combine(appListFlow) { packageNames, appList ->
                appList.map { app ->
                    createRecord(app = app, hasRequestPermission = app.packageName in packageNames)
                }
            }

    override fun transformItem(app: ApplicationInfo) =
        with(packageManagers) {
            createRecord(app = app, hasRequestPermission = app.hasRequestPermission(PERMISSION))
        }

    override fun filter(userIdFlow: Flow<Int>, recordListFlow: Flow<List<UsageDataAppRecord>>) =
        recordListFlow.map { recordList -> recordList.filter { it.isChangeable } }

    @Composable
    override fun isAllowed(record: UsageDataAppRecord): () -> Boolean? =
        record.controller.isAllowedFlow.collectAsCallbackWithLifecycle()

    override fun isChangeable(record: UsageDataAppRecord): Boolean = record.isChangeable

    override fun setAllowed(record: UsageDataAppRecord, newAllowed: Boolean) {
        if (!newAllowed && dpm.isProfileOwnerApp(record.app.packageName)) {
            showDisableUsageAccessWarningDialog()
        }

        record.controller.setAllowed(newAllowed)
    }

    @VisibleForTesting
    fun showDisableUsageAccessWarningDialog() {
        AlertDialog.Builder(context)
            .setIcon(com.android.internal.R.drawable.ic_dialog_alert_material)
            .setTitle(android.R.string.dialog_alert_title)
            .setMessage(
                dpm.resources.getString(WORK_PROFILE_DISABLE_USAGE_ACCESS_WARNING) {
                    context.getString(R.string.work_profile_usage_access_warning)
                }
            )
            .setPositiveButton(R.string.okay, null)
            .show()
    }

    private fun createRecord(
        app: ApplicationInfo,
        hasRequestPermission: Boolean,
    ): UsageDataAppRecord {
        return UsageDataAppRecord(
            app = app,
            isChangeable = hasRequestPermission,
            controller =
                AppOpsPermissionController(
                    context = context,
                    app = app,
                    appOps = APP_OPS,
                    permission = PERMISSION,
                ),
        )
    }

    companion object {
        private val APP_OPS = AppOps(op = AppOpsManager.OP_GET_USAGE_STATS, setModeByUid = true)

        private const val PERMISSION: String = Manifest.permission.PACKAGE_USAGE_STATS
    }
}
