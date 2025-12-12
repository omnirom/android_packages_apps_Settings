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

import android.app.AppGlobals
import android.app.admin.DevicePolicyManager
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SUPERVISION
import android.app.role.RoleManager.ROLE_SYSTEM_SUPERVISION
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Secure.USER_SETUP_COMPLETE
import android.provider.Settings.Secure.getInt
import android.provider.Settings.SettingNotFoundException
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.settings.supervision.EnableSupervisionDialogFragment.Companion.DIALOG_DISMISSED
import com.android.settings.supervision.EnableSupervisionDialogFragment.Companion.DIALOG_NEGATIVE_BUTTON_CLICKED
import com.android.settings.supervision.EnableSupervisionDialogFragment.Companion.DIALOG_POSITIVE_BUTTON_CLICKED
import com.android.settingslib.supervision.SupervisionLog.TAG
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

/**
 * Activity for enabling device supervision.
 *
 * This activity is only available to the system supervision role and allowlisted packages. It
 * enables device supervision and finishes the activity with `Activity.RESULT_OK`.
 */
class EnableSupervisionActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = callingPackage
        if (packageName == null) {
            Log.w(TAG, "Calling package is null. Cannot proceed with supervision setup.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        if (canSkipUserConfirmation(packageName)) {
            enableSupervisionAndFinish()
        } else {
            requestSupervision(savedInstanceState)
        }
    }

    /**
     * Whether explicit user confirmation can be skipped when enabling supervision.
     *
     * Explicit user confirmation can be skipped if any of the following conditions are [true]:
     *
     * The user has not yet completed the user setup flow. The calling package holds the
     * [ROLE_SYSTEM_SUPERVISION] role and is the profile owner.
     *
     * @return [true] if explicit user confirmation can be skipped; [false] otherwise.
     */
    @VisibleForTesting
    fun canSkipUserConfirmation(packageName: String): Boolean {
        val isCallerSystemSupervision = packageName == systemSupervisionPackageName
        val hasUserSetupCompleted = hasUserSetupCompleted()
        val isCallerProfileOwner = isProfileOwner(packageName)
        return !hasUserSetupCompleted ||
            (isCallerSystemSupervision && isCallerProfileOwner) ||
            canBypassConfirmationDialog(packageName)
    }

    fun canBypassConfirmationDialog(packageName: String): Boolean {
        if (!Flags.enableConfirmationDialogBypass()) {
            return false
        }
        return isAllowedPackage(packageName) &&
            AppGlobals.getPackageManager().isDeviceUpgrading() &&
            Build.VERSION.SDK_INT_FULL == Build.VERSION_CODES_FULL.BAKLAVA_1
    }

    @VisibleForTesting
    fun isAllowedPackage(packageName: String): Boolean {
        val resourceId =
            resources.getIdentifier(CONFIG_ALLOWED_SUPERVISION_ROLE_PACKAGES, "string", "android")
        if (resourceId == 0) {
            Log.w(TAG, "Cannot find resource for: $CONFIG_ALLOWED_SUPERVISION_ROLE_PACKAGES")
            return false
        }

        val config =
            try {
                getString(resourceId)
            } catch (e: android.content.res.Resources.NotFoundException) {
                Log.w(TAG, "Cannot get resource: $CONFIG_ALLOWED_SUPERVISION_ROLE_PACKAGES", e)
                return false
            }
        val packageNames = config.split(PACKAGES_LIST_SEPARATOR)
        return packageNames.contains(packageName)
    }

    private fun hasUserSetupCompleted(): Boolean {
        return try {
            getInt(contentResolver, USER_SETUP_COMPLETE) != 0
        } catch (_: SettingNotFoundException) {
            false
        }
    }

    private suspend fun enableSupervision(packageName: String): Boolean {
        if (!grantSupervisionRole(packageName)) {
            Log.w(
                TAG,
                "Failed to grant supervision role for $packageName. Cannot enable supervision.",
            )
            return false
        }

        val supervisionManager = getSystemService(SupervisionManager::class.java)
        if (supervisionManager == null) {
            Log.w(TAG, "SupervisionManager is null or not accessible on this device.")
            return false
        }

        supervisionManager.setSupervisionEnabled(true)
        return true
    }

    private suspend fun grantSupervisionRole(packageName: String): Boolean {
        val executor = ContextCompat.getMainExecutor(this)
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(TAG, "RoleManager is null. Cannot grant supervision role.")
            return false
        }
        return suspendCoroutine { continuation ->
            roleManager.addRoleHolderAsUser(
                ROLE_SUPERVISION,
                packageName,
                RoleManager.MANAGE_HOLDERS_FLAG_DONT_KILL_APP,
                user,
                executor,
            ) { isSuccessful ->
                if (!isSuccessful) {
                    Log.w(TAG, "Failed to add ROLE_SUPERVISION for package: $packageName.")
                }
                continuation.resumeWith(Result.success(isSuccessful))
            }
        }
    }

    fun requestSupervision(savedInstanceState: Bundle?) {
        // Only show the dialog if it hasn't been shown before.
        // The FragmentManager will reattach the existing dialog fragment if it exists.
        if (savedInstanceState == null) {
            setFragmentResultListeners()

            val message = getIntent().getCharSequenceExtra(EXTRA_SUPERVISION_DIALOG_EXPLANATION)
            val appName = getIntent().getCharSequenceExtra(EXTRA_SUPERVISION_APP_NAME)

            val dialog = EnableSupervisionDialogFragment.newInstance(message, appName)
            dialog.show(supportFragmentManager, "enable_supervision_dialog")
        }
    }

    private fun setFragmentResultListeners() {
        supportFragmentManager.setFragmentResultListener(DIALOG_POSITIVE_BUTTON_CLICKED, this) {
            _,
            _ ->
            enableSupervisionAndFinish()
        }
        supportFragmentManager.setFragmentResultListener(DIALOG_NEGATIVE_BUTTON_CLICKED, this) {
            _,
            _ ->
            setResult(RESULT_CANCELED)
            finish()
        }
        supportFragmentManager.setFragmentResultListener(DIALOG_DISMISSED, this) { _, _ ->
            setResult(RESULT_CANCELED)
            if (!isFinishing) {
                finish()
            }
        }
    }

    private fun isProfileOwner(packageName: String): Boolean {
        val devicePolicyManager = getSystemService(DevicePolicyManager::class.java)
        if (devicePolicyManager == null) return false
        return devicePolicyManager.isProfileOwnerApp(packageName)
    }

    private fun enableSupervisionAndFinish() {
        val packageName = callingPackage
        if (packageName == null) {
            Log.w(TAG, "Calling package is null. Cannot proceed with supervision setup.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        lifecycleScope.launch {
            if (enableSupervision(packageName)) {
                Log.i(TAG, "Supervision successfully enabled for $packageName.")
                setResult(RESULT_OK)
            } else {
                Log.e(TAG, "Failed to enable supervision for $packageName.")
                setResult(RESULT_CANCELED)
            }
            finish()
        }
    }

    companion object {
        /**
         * Message provided by the supervision app to be displayed as the dialog's first paragraph
         */
        const val EXTRA_SUPERVISION_DIALOG_EXPLANATION = "supervision_dialog_explanation"
        /**
         * The name of the app proposing to supervise the device. This is used in the dialog's
         * supervision warning paragraph.
         */
        const val EXTRA_SUPERVISION_APP_NAME = "supervision_app_name"
        /**
         * This config is temporary and intentionally not exposed as a system API and must be
         * accessed by name.
         */
        private const val CONFIG_ALLOWED_SUPERVISION_ROLE_PACKAGES =
            "config_allowedSupervisionRolePackages"
        /** Separator for the `config_allowedSupervisionRolePackages` list of packages. */
        private const val PACKAGES_LIST_SEPARATOR = ";"
    }
}
