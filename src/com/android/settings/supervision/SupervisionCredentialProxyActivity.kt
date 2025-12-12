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

import android.Manifest.permission.INTERACT_ACROSS_USERS_FULL
import android.Manifest.permission.MANAGE_USERS
import android.annotation.RequiresPermission
import android.app.ActivityManager
import android.app.ComponentCaller
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import com.android.internal.widget.LockPatternUtils
import com.android.settings.password.ChooseLockGeneric
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * An Activity that acts as a proxy to set the supervising user's credentials.
 *
 * This Activity is responsible for starting the profile of the supervising user and launching the
 * activity to allow the user to enter their lock screen credentials. The result of this credential
 * setup is then propagated back to the caller. After the credential setup flow completes, this
 * Activity attempts to stop the supervising user's profile.
 *
 * It requires either [android.Manifest.permission.INTERACT_ACROSS_USERS_FULL] or
 * [android.Manifest.permission.MANAGE_USERS] permissions to interact with other users.
 */
class SupervisionCredentialProxyActivity : FragmentActivity() {
    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Post the heavy logic to run after the current drawing pass.
        // This allows the activity transition animation to run smoothly first.
        window.decorView.post {
            val supervisingUser = supervisingUserHandle
            if (supervisingUser == null) {
                errorHandler("SupervisingUserHandle is null")
                return@post
            }
            val activityManager = getSystemService(ActivityManager::class.java)
            if (!activityManager.startProfile(supervisingUser)) {
                errorHandler("Unable to start supervising user, cannot set credentials.")
                return@post
            }

            val intent =
                Intent(this, ChooseLockGeneric::class.java).apply {
                    // To go directly to setting up a PIN
                    putExtra(
                        LockPatternUtils.PASSWORD_TYPE_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
                    )
                }
            startActivityForResultAsUser(
                intent,
                REQUEST_CODE_SUPERVISION_CREDENTIALS_PROXY,
                supervisingUser,
            )
        }
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller,
    ) {
        if (requestCode != REQUEST_CODE_SUPERVISION_CREDENTIALS_PROXY) {
            errorHandler("Unexpected request code: $requestCode")
            return
        }
        val supervisingUser = supervisingUserHandle
        if (supervisingUser == null) {
            errorHandler("Cannot stop supervising profile because it does not exist.")
            return
        }
        val activityManager = getSystemService(ActivityManager::class.java)
        if (!activityManager.stopProfile(supervisingUser)) {
            errorHandler("Could not stop the supervising profile.")
            return
        }
        setResult(resultCode, data)
        finish()
    }

    private fun errorHandler(errStr: String) {
        Log.w(TAG, errStr)
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        @VisibleForTesting const val REQUEST_CODE_SUPERVISION_CREDENTIALS_PROXY = 10
    }
}
