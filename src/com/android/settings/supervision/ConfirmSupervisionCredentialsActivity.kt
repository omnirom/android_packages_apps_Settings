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
import android.Manifest.permission.SET_BIOMETRIC_DIALOG_ADVANCED
import android.Manifest.permission.USE_BIOMETRIC_INTERNAL
import android.app.ActivityManager
import android.app.role.RoleManager
import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.hardware.biometrics.BiometricPrompt.AuthenticationCallback
import android.os.Binder
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Process
import android.os.UserHandle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.supervision.SupervisionLog.TAG

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

    private lateinit var mSupervisingUser: UserHandle
    @VisibleForTesting var mProfileStarted = false
    private var mBiometricPromptShown = false

    private val userStateChangeReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val userId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1)

                when (intent.action) {
                    Intent.ACTION_USER_STOPPED -> {
                        if (mSupervisingUser.identifier != userId) {
                            return
                        }
                        // Restart the supervising profile since it was stopped.
                        val activityManager = getSystemService(ActivityManager::class.java)
                        if (!activityManager.startProfile(mSupervisingUser)) {
                            errorHandler("Unable to start supervising user")
                            return
                        } else {
                            mProfileStarted = true
                        }
                        if (!mBiometricPromptShown) {
                            showBiometricPrompt(mSupervisingUser.identifier)
                            mBiometricPromptShown = true
                        }
                    }
                }
            }
        }

    @VisibleForTesting
    val mAuthenticationCallback =
        object : AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                Log.w(TAG, "onAuthenticationError(errorCode=$errorCode, errString=$errString)")
                setResult(RESULT_CANCELED)
                finish()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                val authController =
                    SupervisionAuthController.getInstance(
                        this@ConfirmSupervisionCredentialsActivity
                    )
                authController.startSession(taskId)
                setResult(RESULT_OK)
                finish()
            }

            override fun onAuthenticationFailed() {
                setResult(RESULT_CANCELED)
                finish()
            }
        }

    private val getResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            setResult(result.resultCode)
            finish()
        }

    @RequiresPermission(
        allOf = [USE_BIOMETRIC_INTERNAL, SET_BIOMETRIC_DIALOG_ADVANCED, INTERACT_ACROSS_USERS_FULL]
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!callerHasSupervisionRole() && !callerIsSystemUid()) {
            errorHandler(
                "Calling uid ${Binder.getCallingUid()} is not supervision role holder or SYSTEM_UID."
            )
            return
        }
        val supervisingUser = supervisingUserHandle
        if (supervisingUser == null) {
            errorHandler("No supervising user exists, cannot verify credentials.")
            return
        }
        mSupervisingUser = supervisingUser
        val intentFilter = IntentFilter().apply { addAction(Intent.ACTION_USER_STOPPED) }
        registerReceiver(userStateChangeReceiver, intentFilter, RECEIVER_NOT_EXPORTED)

        if (savedInstanceState != null) {
            mProfileStarted = savedInstanceState.getBoolean(KEY_PROFILE_STARTED, mProfileStarted)
            mBiometricPromptShown =
                savedInstanceState.getBoolean(KEY_BIOMETRIC_PROMPT_SHOWN, mBiometricPromptShown)
        } else {
            // BiometricPrompt persists through configuration change, so only create it on initial
            // activity creation.
            if (!isSupervisingCredentialSet) {
                // Redirects to the setup supervision flow when credential is not set.
                val setupIntent = Intent(this, SetupSupervisionActivity::class.java)
                getResultLauncher.launch(setupIntent)
                return
            }
            val forceConfirmation = intent.getBooleanExtra(EXTRA_FORCE_CONFIRMATION, false)
            if (
                forceConfirmation ||
                    !SupervisionAuthController.getInstance(this).isSessionActive(taskId)
            ) {
                val activityManager = getSystemService(ActivityManager::class.java)
                if (!activityManager.startProfile(mSupervisingUser)) {
                    errorHandler("Unable to start supervising user, cannot verify credentials.")
                    return
                } else {
                    mProfileStarted = true
                }
                val isUserRunning = activityManager.isUserRunning(mSupervisingUser.identifier)
                if (isUserRunning) {
                    showBiometricPrompt(mSupervisingUser.identifier)
                    mBiometricPromptShown = true
                }
            } else {
                Log.i(TAG, "Bypassing authentication due to active session")
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PROFILE_STARTED, mProfileStarted)
        outState.putBoolean(KEY_BIOMETRIC_PROMPT_SHOWN, mBiometricPromptShown)
        super.onSaveInstanceState(outState)
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    @VisibleForTesting
    public override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(userStateChangeReceiver)
        // Do not stop the profile on configuration change, since the authentication session in
        // BiometricPrompt is still active.
        if (mProfileStarted && isFinishing) {
            tryStopProfile()
        }
    }

    @RequiresPermission(allOf = [USE_BIOMETRIC_INTERNAL, SET_BIOMETRIC_DIALOG_ADVANCED])
    fun showBiometricPrompt(userId: Int) {
        getBiometricPrompt()
            .authenticateUser(
                CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                mAuthenticationCallback,
                userId,
            )
    }

    @RequiresPermission(value = SET_BIOMETRIC_DIALOG_ADVANCED)
    @VisibleForTesting
    fun getBiometricPrompt(): BiometricPrompt {
        val builder =
            BiometricPrompt.Builder(this)
                .setTitle(getString(R.string.supervision_full_screen_pin_verification_title))
                .setConfirmationRequired(true)
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)

        val supportSupervisionRecovery =
            getSystemService(SupervisionManager::class.java)?.getSupervisionRecoveryInfo() != null

        if (!supportSupervisionRecovery) {
            return builder.build()
        }

        val listener =
            DialogInterface.OnClickListener { _: DialogInterface?, _: Int ->
                onForgotPinFallbackClicked()
            }

        return builder
            .addFallbackOption(
                getString(R.string.supervision_auth_prompt_forgot_pin_button_label),
                BiometricManager.ICON_TYPE_ACCOUNT,
                ContextCompat.getMainExecutor(this),
                listener,
            )
            .build()
    }

    @VisibleForTesting
    fun onForgotPinFallbackClicked() {
        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
        metricsFeatureProvider.action(
            this,
            SettingsEnums.ACTION_SUPERVISION_FORGOT_PIN_DURING_PIN_INVOCATION,
        )
        val intent =
            Intent(this, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_RECOVERY
            }
        getResultLauncher.launch(intent)
    }

    private fun callerHasSupervisionRole(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(TAG, "null RoleManager")
            return false
        }
        return roleManager
            .getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
            .contains(callingPackage)
    }

    private fun callerIsSystemUid(): Boolean {
        val callingUid = Binder.getCallingUid()
        return UserHandle.getAppId(callingUid) == Process.SYSTEM_UID
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, MANAGE_USERS])
    private fun tryStopProfile() {
        val supervisingUser = supervisingUserHandle
        if (supervisingUser == null) {
            Log.w(TAG, "Cannot stop supervising profile because it does not exist.")
            return
        }
        val activityManager = getSystemService(ActivityManager::class.java)
        if (!activityManager.stopProfile(supervisingUser)) {
            Log.w(TAG, "Could not stop the supervising profile.")
        } else {
            mProfileStarted = false
        }
    }

    private fun errorHandler(errStr: String? = null) {
        errStr?.let { Log.e(TAG, it) }
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        // If true, force confirmation of supervision credentials, regardless of active auth session
        @VisibleForTesting const val EXTRA_FORCE_CONFIRMATION = "force_confirmation"
        // Whether the supervising profile was started by this activity
        const val KEY_PROFILE_STARTED = "profile_started"
        const val KEY_BIOMETRIC_PROMPT_SHOWN = "biometric_prompt_shown"
    }
}
