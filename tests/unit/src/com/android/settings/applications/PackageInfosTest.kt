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

import android.content.pm.PackageInfo
import android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageInfosTest {
    @Test
    fun isPermissionGranted_nullPackageInfo_returnsFalse() {
        assertFalse(isPermissionGranted(null, "android.permission.CAMERA"))
    }

    @Test
    fun isPermissionGranted_nullRequestedPermissions_returnsFalse() {
        val packageInfo = createPackageInfo(null, null)

        assertFalse(isPermissionGranted(packageInfo, "android.permission.CAMERA"))
    }

    @Test
    fun isPermissionGranted_permissionNotRequested_returnsFalse() {
        val packageInfo =
            createPackageInfo(
                requestedPermissions = arrayOf("android.permission.INTERNET"),
                requestedPermissionsFlags = intArrayOf(0),
            )

        assertFalse(isPermissionGranted(packageInfo, "android.permission.CAMERA"))
    }

    @Test
    fun isPermissionGranted_permissionGranted_returnsTrue() {
        val packageInfo =
            createPackageInfo(
                requestedPermissions = arrayOf("android.permission.CAMERA"),
                requestedPermissionsFlags = intArrayOf(REQUESTED_PERMISSION_GRANTED),
            )

        assertTrue(isPermissionGranted(packageInfo, "android.permission.CAMERA"))
    }

    @Test
    fun isPermissionGranted_permissionNotGranted_returnsFalse() {
        val packageInfo =
            createPackageInfo(
                requestedPermissions = arrayOf("android.permission.CAMERA"),
                requestedPermissionsFlags = intArrayOf(0),
            )

        assertFalse(isPermissionGranted(packageInfo, "android.permission.CAMERA"))
    }

    @Test
    fun isPermissionGranted_permissionNotInArray_returnsFalse() {
        val packageInfo =
            createPackageInfo(
                requestedPermissions = arrayOf("android.permission.INTERNET"),
                requestedPermissionsFlags = intArrayOf(REQUESTED_PERMISSION_GRANTED),
            )

        assertFalse(isPermissionGranted(packageInfo, "android.permission.CAMERA"))
    }

    private fun createPackageInfo(
        requestedPermissions: Array<String>?,
        requestedPermissionsFlags: IntArray?,
    ): PackageInfo {
        val packageInfo = PackageInfo()
        packageInfo.requestedPermissions = requestedPermissions
        packageInfo.requestedPermissionsFlags = requestedPermissionsFlags
        return packageInfo
    }
}
