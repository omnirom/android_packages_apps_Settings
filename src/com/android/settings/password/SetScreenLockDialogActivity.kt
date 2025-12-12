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

package com.android.settings.password

import android.Manifest
import android.annotation.RequiresPermission
import android.app.KeyguardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.UserInfo
import android.hardware.biometrics.BiometricManager
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.android.internal.app.AlertActivity
import com.android.internal.app.SetScreenLockDialogContract
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.theme.R

class SetScreenLockDialogActivity :
    AlertActivity(), DialogInterface.OnClickListener, DialogInterface.OnDismissListener {

    private var mReason: Int = SetScreenLockDialogContract.LAUNCH_REASON_UNKNOWN
    private var mOriginUserId: Int = UserHandle.USER_NULL

    @RequiresPermission(Manifest.permission.HIDE_OVERLAY_WINDOWS)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mReason =
            intent.getIntExtra(
                SetScreenLockDialogContract.EXTRA_LAUNCH_REASON,
                SetScreenLockDialogContract.LAUNCH_REASON_UNKNOWN,
            )
        mOriginUserId =
            intent.getIntExtra(
                SetScreenLockDialogContract.EXTRA_ORIGIN_USER_ID,
                UserHandle.USER_NULL,
            )

        if (mReason == SetScreenLockDialogContract.LAUNCH_REASON_UNKNOWN) {
            Log.e(TAG, "Invalid launch reason: $mReason")
            finish()
            return
        }

        val km = getSystemService(KeyguardManager::class.java)
        if (km == null) {
            Log.e(TAG, "Error fetching keyguard manager")
            return
        }
        if (km.isDeviceSecure) {
            Log.w(TAG, "Closing the activity since screen lock is already set")
            return
        }

        Log.d(TAG, "Launching screen lock setup dialog due to $mReason")

        val builder: AlertDialog.Builder
        builder =
            if (SettingsThemeHelper.isExpressiveTheme(getApplicationContext())) {
                AlertDialog.Builder(this, R.style.Theme_AlertDialog_SettingsLib_Expressive)
            } else {
                AlertDialog.Builder(this)
            }

        builder
            .setTitle(com.android.internal.R.string.set_up_screen_lock_title)
            .setOnDismissListener(this)
            .setPositiveButton(com.android.internal.R.string.set_up_screen_lock_action_label, this)
            .setNegativeButton(com.android.internal.R.string.cancel, this)

        setLaunchUserSpecificMessage(builder)

        val dialog = builder.create()
        dialog.create()
        window.setHideOverlayWindows(true)
        dialog.getButton(BUTTON_POSITIVE).setFilterTouchesWhenObscured(true)
        dialog.show()
    }

    override fun onDismiss(dialog: DialogInterface) {
        finish()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        if (which == BUTTON_POSITIVE) {
            val setNewLockIntent =
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                    putExtra(
                        Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                }
            startActivity(setNewLockIntent)
        } else {
            finish()
        }
    }

    @RequiresPermission(
        anyOf =
            [
                Manifest.permission.MANAGE_USERS,
                Manifest.permission.CREATE_USERS,
                Manifest.permission.QUERY_USERS,
            ]
    )
    private fun setLaunchUserSpecificMessage(builder: AlertDialog.Builder) {
        when (mReason) {
            SetScreenLockDialogContract.LAUNCH_REASON_PRIVATE_SPACE_SETTINGS_ACCESS -> {
                builder.setMessage(
                    com.android.internal.R.string.private_space_set_up_screen_lock_message
                )
                return
            }

            SetScreenLockDialogContract.LAUNCH_REASON_RESET_PRIVATE_SPACE_SETTINGS_ACCESS -> {
                builder.setMessage(
                    com.android.internal.R.string.private_space_set_up_screen_lock_for_reset
                )
                return
            }
        }

        val userManager = applicationContext.getSystemService(UserManager::class.java)
        if (userManager != null) {
            val userInfo: UserInfo? = userManager.getUserInfo(mOriginUserId)
            if (userInfo?.isPrivateProfile == true) {
                builder.setMessage(
                    com.android.internal.R.string.private_space_set_up_screen_lock_message
                )
            }
        }
    }

    companion object {
        private const val TAG = "SetSetScreenLockDialog"
    }
}
