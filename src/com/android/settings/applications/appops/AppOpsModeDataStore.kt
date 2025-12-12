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

package com.android.settings.applications.appops

import android.app.AppOpsManager
import android.app.AppOpsManager.OnOpChangedListener
import android.content.Context
import android.content.pm.PackageManager
import android.os.UserHandle
import com.android.settings.applications.PackageInfoProvider
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionGranted
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.utils.appops.addAppOpsModeObserver
import com.android.settingslib.utils.appops.removeAppOpsModeObserver

/**
 * Data store to manipulate app ops mode for given package.
 *
 * By virtue of `AppOpsModeObservable`, there is only one [OnOpChangedListener] registered to
 * [AppOpsManager] even multiple data stores are created for the same [op] (could be verified via
 * `adb shell dumpsys appops`).
 *
 * @param preferenceKey preference key to notify when app ops is changed
 * @param context context
 * @param packageInfoProvider provider of package info
 * @param op app op to observe
 * @param permission permission to check in case of default op mode
 * @param setModeByUid whether to set op mode by uid (true) or by package (false), a null value
 *   means automatic detection by [android.app.AppOpsManager.opIsUidAppOpPermission]
 * @param modeForDisabled op mode to be used in case of disabled
 */
class AppOpsModeDataStore(
    private val preferenceKey: String,
    private val context: Context,
    private val packageInfoProvider: PackageInfoProvider,
    private val op: Int,
    private val permission: String?,
    private val setModeByUid: Boolean?,
    private val modeForDisabled: Int = AppOpsManager.MODE_ERRORED,
) : AbstractKeyedDataObservable<String>(), KeyValueStore, KeyedObserver<String> {

    private val appOpsManager = context.getSystemService(AppOpsManager::class.java)!!

    private val opStr: String
        get() = AppOpsManager.opToPublicName(op)

    override fun contains(key: String) = key == preferenceKey

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T =
        packageInfoProvider.packageInfo?.applicationInfo?.let { appInfo ->
            when (appOpsManager.checkOpNoThrow(op, appInfo.uid, appInfo.packageName)) {
                AppOpsManager.MODE_ALLOWED -> true
                AppOpsManager.MODE_DEFAULT -> {
                    if (permission == null) {
                        false
                    } else {
                        val packageInfo = context.getPackageInfoWithPermissions(appInfo.packageName)
                        isPermissionGranted(packageInfo, permission)
                    }
                }
                else -> false
            }
        } as T

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        val appInfo = packageInfoProvider.packageInfo?.applicationInfo ?: return
        val mode = if (value == true) AppOpsManager.MODE_ALLOWED else modeForDisabled
        when (setModeByUid ?: AppOpsManager.opIsUidAppOpPermission(op)) {
            true -> appOpsManager.setUidMode(op, appInfo.uid, mode) // preferred
            else -> appOpsManager.setMode(op, appInfo.uid, packageInfoProvider.packageName, mode)
        }
        val permission = AppOpsManager.opToPermission(op)
        if (permission != null) {
            context.packageManager.updatePermissionFlags(
                permission,
                appInfo.packageName,
                PackageManager.FLAG_PERMISSION_USER_SET,
                PackageManager.FLAG_PERMISSION_USER_SET,
                UserHandle.getUserHandleForUid(appInfo.uid),
            )
        }
    }

    override fun onFirstObserverAdded() {
        context.addAppOpsModeObserver(
            opStr,
            packageInfoProvider.packageName,
            this,
            HandlerExecutor.Companion.main,
        )
    }

    override fun onLastObserverRemoved() {
        context.removeAppOpsModeObserver(opStr, packageInfoProvider.packageName, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(preferenceKey, reason)
    }
}
