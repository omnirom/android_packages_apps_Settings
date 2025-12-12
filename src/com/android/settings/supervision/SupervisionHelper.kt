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

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.app.supervision.SupervisionManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager.MATCH_ALL
import android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS
import android.content.res.Resources
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import com.android.settings.supervision.ipc.SupervisionMessengerClient.Companion.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION
import com.android.settingslib.supervision.SupervisionLog.TAG

object SupervisionHelper {
    const val INSTALL_SUPERVISION_APP_ACTION =
        "android.app.supervision.action.INSTALL_SUPERVISION_APP"
}

val Context.isSupervisingCredentialSet: Boolean
    get() {
        val supervisingUserId = supervisingUserHandle?.identifier ?: return false
        return getSystemService(KeyguardManager::class.java)?.isDeviceSecure(supervisingUserId) ==
            true
    }

val Context.supervisingUserHandle: UserHandle?
    get() = getSystemService(UserManager::class.java).supervisingUserHandle

val UserManager?.supervisingUserHandle: UserHandle?
    get() = this?.users?.firstOrNull { it.userType == USER_TYPE_PROFILE_SUPERVISING }?.userHandle

/** Returns the package name of the system supervision app, or null if not found. */
val Context.systemSupervisionPackageName: String?
    get() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(TAG, "RoleManager service not available.")
            return null
        }

        val roleHolders =
            roleManager.getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION) ?: emptyList<String>()
        if (roleHolders.isEmpty()) Log.w(TAG, "No package holding the system supervision role.")

        // supervision role is exclusive, only one app may hold this role in a user
        return roleHolders.firstOrNull()
    }

fun Context.hasNecessarySupervisionComponent(
    packageName: String? =
        resources.getString(com.android.internal.R.string.config_systemSupervision),
    matchAll: Boolean = false,
): Boolean {
    if (packageName == null) return false

    val intent = Intent(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION).setPackage(packageName)
    val resolveInfoFlag = if (matchAll) (MATCH_ALL or MATCH_DISABLED_COMPONENTS) else 0
    return packageManager?.queryIntentServices(intent, resolveInfoFlag)?.isNotEmpty() == true
}

fun Context.getSupervisionAppInstallIntent(): Intent {
    val supervisionPackage =
        resources.getString(com.android.internal.R.string.config_systemSupervision)
    return Intent(SupervisionHelper.INSTALL_SUPERVISION_APP_ACTION).setPackage(supervisionPackage)
}

fun Context.getSupervisionAppInstallActivityInfo(): ActivityInfo? {
    val intent = getSupervisionAppInstallIntent()
    return packageManager
        ?.queryIntentActivities(intent, MATCH_ALL or MATCH_DISABLED_COMPONENTS)
        ?.firstOrNull()
        ?.activityInfo
}

/**
 * Returns the package names of the supervision apps.
 *
 * <p> Note that this is different from the system supervision app.
 */
val Context.supervisionRoleHolders: List<String>
    get() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(TAG, "RoleManager service not available.")
            return emptyList()
        }
        return roleManager.getRoleHolders(RoleManager.ROLE_SUPERVISION) ?: emptyList()
    }

/** Returns whether any users except the current user are supervised on this device. */
fun Context.areAnyUsersExceptCurrentSupervised(
    supervisionManager: SupervisionManager,
    userManager: UserManager,
): Boolean {
    return userManager.users.any {
        userId != it.id && supervisionManager.isSupervisionEnabledForUser(it.id)
    }
}

/**
 * Disables supervision, deletes the supervising profile and recovery info. Returns whether all
 * supervision data was deleted.
 */
fun Context.deleteSupervisionData(): Boolean {
    val userManager = getSystemService(UserManager::class.java)
    val supervisionManager = getSystemService(SupervisionManager::class.java)
    if (userManager == null || supervisionManager == null) {
        Log.e(TAG, "Can't delete supervision data; system services cannot be found.")
        return false
    }

    if (areAnyUsersExceptCurrentSupervised(supervisionManager, userManager)) {
        Log.e(TAG, "Can't delete supervision data; one or more users on the device are supervised.")
        return false
    }

    val supervisingUser = supervisingUserHandle?.identifier
    if (supervisingUser == null) {
        Log.e(TAG, "Can't delete supervision data; supervising user does not exist.")
        return false
    }

    supervisionManager.setSupervisionEnabled(false)
    supervisionManager.setSupervisionRecoveryInfo(null)
    return userManager.removeUserEvenWhenDisallowed(supervisingUser)
}

/** Checks if the current profile owner is one of the known supervision packages. */
fun Context.isSupervisionPackageProfileOwner(): Boolean {
    val dpm = getSystemService(DevicePolicyManager::class.java)
    if (dpm == null) {
        Log.e(TAG, "Can't check profile owner; DevicePolicyManager service not available.")
        return false
    }

    val profileOwnerPackageName = dpm.profileOwner?.packageName ?: return false

    val supervisedPackages =
        listOfNotNull(
            readDefaultSupervisionPackageNameFromResources(),
            readSystemSupervisionPackageNameFromResources(),
        )
    return profileOwnerPackageName in supervisedPackages
}

/** Reads the default supervision package name from resources. Returns null in case of error. */
fun Context.readDefaultSupervisionPackageNameFromResources(): String? {
    return try {
        resources
            .getString(com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent)
            .let { ComponentName.unflattenFromString(it)?.packageName }
            .also { packageName ->
                if (packageName == null) {
                    Log.e(
                        TAG,
                        "Default supervision package name not defined or invalid in resources.",
                    )
                }
            }
    } catch (e: Resources.NotFoundException) {
        Log.e(TAG, "Could not find defaultSupervisionProfileOwnerComponent resource.", e)
        null
    }
}

/** Reads the system supervision package name from resources. Returns null in case of error. */
fun Context.readSystemSupervisionPackageNameFromResources(): String? {
    return try {
        resources.getString(com.android.internal.R.string.config_systemSupervision)
    } catch (e: Resources.NotFoundException) {
        Log.e(TAG, "Could not find systemSupervision resource.", e)
        null
    }
}
