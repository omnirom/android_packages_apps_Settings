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

package com.android.settings.applications

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.UserHandle
import com.android.settingslib.utils.applications.PackageObservable

/** Provides [PackageInfo] for given package. */
interface PackageInfoProvider {
    /** Package name. */
    val packageName: String

    /**
     * Package info.
     *
     * Use [PackageObservable] to observe package changes (e.g. update, uninstall) and refresh this
     * property.
     */
    var packageInfo: PackageInfo?
}

/** Returns [PackageInfo] of given package, null if not found. */
fun Context.getPackageInfo(packageName: String) =
    try {
        packageManager.getPackageInfo(packageName, 0)
    } catch (_: NameNotFoundException) {
        null
    }

/** Returns [PackageInfo] of given package with activities flags set, null if not found. */
fun Context.getPackageInfoWithActivities(packageName: String) =
    try {
        packageManager.getPackageInfoAsUser(
            packageName,
            PackageManager.GET_ACTIVITIES,
            UserHandle.myUserId(),
        )
    } catch (_: Exception) {
        null
    }

/** Returns [PackageInfo] of given package with permissions flags set, null if not found. */
fun Context.getPackageInfoWithPermissions(packageName: String) =
    try {
        packageManager.getPackageInfoAsUser(
            packageName,
            PackageManager.GET_PERMISSIONS,
            UserHandle.myUserId(),
        )
    } catch (_: Exception) {
        null
    }

/** Returns true if the permission was requested for the given package, false otherwise. */
fun isPermissionRequested(packageInfo: PackageInfo?, permission: String): Boolean =
    packageInfo?.requestedPermissions?.let { permission in it } ?: false

/** Returns true if the permission was granted for the given package, false otherwise. */
fun isPermissionGranted(packageInfo: PackageInfo?, permission: String): Boolean {
    val index = packageInfo?.requestedPermissions?.indexOf(permission) ?: return false

    val flags = packageInfo.requestedPermissionsFlags?.getOrNull(index) ?: return false

    return (flags and REQUESTED_PERMISSION_GRANTED) != 0
}
