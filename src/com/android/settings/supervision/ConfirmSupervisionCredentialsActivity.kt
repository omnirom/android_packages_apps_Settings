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

import android.Manifest.permission.USE_BIOMETRIC
import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.settings.R

/**
 * Activity for confirming supervision credentials using device credential authentication.
 *
 * This activity displays an authentication prompt to the user, requiring them to authenticate using
 * their device credentials (PIN, pattern, or password). It is specifically designed for verifying
 * credentials for supervision purposes.
 *
 * It returns `Activity.RESULT_OK` if authentication succeeds, and `Activity.RESULT_CANCELED` if
 * authentication fails or is canceled by the user.
 *
 * Usage:
 * 1. Start this activity using `startActivityForResult()`.
 * 2. Handle the result in `onActivityResult()`.
 *
 * Permissions:
 * - Requires `android.permission.USE_BIOMETRIC`.
 */
class ConfirmSupervisionCredentialsActivity : FragmentActivity() {
    private val mAuthenticationCallback =
        object : AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w(TAG, "onAuthenticationError(errorCode=$errorCode, errString=$errString)")
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                setResult(Activity.RESULT_OK)
                finish()
            }

            override fun onAuthenticationFailed() {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

    @RequiresPermission(USE_BIOMETRIC)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO(b/392961554): Check if caller is the SYSTEM_SUPERVISION role holder. Call
        // RoleManager#getRoleHolders(SYSTEM_SUPERVISION) and check if getCallingPackage() is in the
        // list.
        if (checkCallingOrSelfPermission(USE_BIOMETRIC) == PackageManager.PERMISSION_GRANTED) {
            showBiometricPrompt()
        }
    }

    @RequiresPermission(USE_BIOMETRIC)
    fun showBiometricPrompt() {
        // TODO(b/392961554): adapts to new user profile type to trigger PIN verification dialog.
        val biometricPrompt =
            BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.supervision_full_screen_pin_verification_title))
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        biometricPrompt.authenticate(
            CancellationSignal(),
            ContextCompat.getMainExecutor(this),
            mAuthenticationCallback,
        )
    }

    companion object {
        // TODO(b/392961554): remove this tag and use shared tag after http://ag/31997167 is
        // submitted.
        const val TAG = "SupervisionSettings"
    }
}
