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

import android.Manifest
import android.app.Activity.RESULT_CANCELED
import android.app.admin.DevicePolicyManager
import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo
import android.app.supervision.SupervisionRecoveryInfo.EXTRA_SUPERVISION_RECOVERY_INFO
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.password.ChooseLockGeneric
import com.android.settingslib.supervision.SupervisionIntentProvider
import com.android.settingslib.supervision.SupervisionLog

/** Activity class for device supervision pin recovery flow. */
class SupervisionPinRecoveryActivity : FragmentActivity() {
    // ActivityResultLaunchers
    private val contract = ActivityResultContracts.StartActivityForResult()
    private val confirmPinLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract) { result -> onPinConfirmed(result.resultCode) }

    private val verificationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract) { result ->
            onVerification(result.resultCode, result.data)
        }
    private val setPinLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract) { result -> onPinSet(result.resultCode) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val actionType = intent.action
        when (actionType) {
            ACTION_SETUP -> startRecoverySetup()
            ACTION_SETUP_VERIFIED -> startConfirmPin()
            ACTION_RECOVERY -> startVerification()
            ACTION_UPDATE -> startConfirmPin()
            ACTION_POST_SETUP_VERIFY -> startConfirmPin()
            else -> handleError("PIN recovery result unknown actionType: $actionType")
        }
    }

    private fun startRecoverySetup() {
        val setupIntent =
            SupervisionIntentProvider.getPinRecoveryIntent(
                this,
                SupervisionIntentProvider.PinRecoveryAction.SET,
            )
        if (setupIntent != null) {
            verificationLauncher.launch(setupIntent)
        } else {
            handleError("No activity found for SETUP PIN recovery.")
        }
    }

    private fun startConfirmPin() {
        val confirmPinIntent =
            SupervisionIntentProvider.getConfirmSupervisionCredentialsIntent(
                context = this,
                forceConfirm = true,
            )
        if (confirmPinIntent != null) {
            confirmPinLauncher.launch(confirmPinIntent)
        } else {
            handleError("No activity found for confirm PIN.")
        }
    }

    private fun startVerification() {
        val recoveryIntent =
            SupervisionIntentProvider.getPinRecoveryIntent(
                this,
                SupervisionIntentProvider.PinRecoveryAction.VERIFY,
            )
        if (recoveryIntent != null) {
            val supervisionManager = getSystemService(SupervisionManager::class.java)
            val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()

            recoveryIntent.apply {
                // Pass along any available recovery information.
                putExtra(EXTRA_SUPERVISION_RECOVERY_INFO, recoveryInfo)
                verificationLauncher.launch(this)
            }
        } else {
            handleError("No activity found for VERIFY PIN recovery.")
        }
    }

    private fun onPinConfirmed(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            val nextAction = intent.action
            when (nextAction) {
                ACTION_SETUP_VERIFIED -> {
                    val setIntent =
                        SupervisionIntentProvider.getPinRecoveryIntent(
                            this,
                            SupervisionIntentProvider.PinRecoveryAction.SET_VERIFIED,
                        )
                    if (setIntent != null) {
                        verificationLauncher.launch(setIntent)
                    } else {
                        handleError("No activity found for SET_VERIFIED PIN recovery.")
                    }
                }
                ACTION_UPDATE -> {
                    val updatePinIntent =
                        SupervisionIntentProvider.getPinRecoveryIntent(
                            this,
                            SupervisionIntentProvider.PinRecoveryAction.UPDATE,
                        )
                    if (updatePinIntent != null) {
                        verificationLauncher.launch(updatePinIntent)
                    } else {
                        handleError("No activity found for UPDATE PIN recovery.")
                    }
                }
                ACTION_POST_SETUP_VERIFY -> {
                    val postSetupVerifyIntent =
                        SupervisionIntentProvider.getPinRecoveryIntent(
                            this,
                            SupervisionIntentProvider.PinRecoveryAction.POST_SETUP_VERIFY,
                        )
                    if (postSetupVerifyIntent != null) {
                        val supervisionManager = getSystemService(SupervisionManager::class.java)
                        val recoveryInfo = supervisionManager?.getSupervisionRecoveryInfo()
                        postSetupVerifyIntent.apply {
                            putExtra(EXTRA_SUPERVISION_RECOVERY_INFO, recoveryInfo)
                            verificationLauncher.launch(postSetupVerifyIntent)
                        }
                    } else {
                        handleError("No activity found for post setup PIN recovery verify.")
                    }
                }
                else -> handleError("Unknown action after PIN confirmation: $nextAction")
            }
        } else {
            handleError("PIN confirmation failed with result: $resultCode")
        }
    }

    private fun onVerification(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val action = intent.action
            when (action) {
                ACTION_RECOVERY -> startResetPinActivity() // reset PIN after verification
                ACTION_SETUP,
                ACTION_UPDATE,
                ACTION_SETUP_VERIFIED,
                ACTION_POST_SETUP_VERIFY -> {
                    if (data != null) {
                        val recoveryInfo =
                            data.getParcelableExtra(
                                EXTRA_SUPERVISION_RECOVERY_INFO,
                                SupervisionRecoveryInfo::class.java,
                            )
                        if (recoveryInfo != null) {
                            val supervisionManager =
                                getSystemService(SupervisionManager::class.java)
                            supervisionManager?.setSupervisionRecoveryInfo(recoveryInfo)
                            handleSuccess()
                        } else {
                            handleError(
                                "Cannot save recovery info, no valid recovery info from result."
                            )
                        }
                    } else {
                        handleError("Cannot save recovery info, no result data.")
                    }
                }
                else -> handleError("Unknown action after verification: $action")
            }
        } else {
            if (intent.action == ACTION_RECOVERY) {
                logRecoveryResult(false)
            }
            handleError(
                "Verification process failed with result: $resultCode, action: ${intent.action}"
            )
        }
    }

    private fun onPinSet(resultCode: Int) {
        if (resultCode != RESULT_CANCELED) {
            // After the new PIN being set.
            logRecoveryResult(true)
            handleSuccess()
        } else {
            logRecoveryResult(false)
            handleError("Setting new PIN failed with result cancelled.")
        }
    }

    /** Starts the reset supervision PIN activity for the supervising user. */
    @RequiresPermission(
        anyOf = [Manifest.permission.CREATE_USERS, Manifest.permission.MANAGE_USERS]
    )
    private fun startResetPinActivity() {
        if (!resetSupervisionUser()) {
            handleError("Failed to reset supervision user.")
            logRecoveryResult(false)
            return
        }
        val intent =
            Intent(this, ChooseLockGeneric::class.java).apply {
                putExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
                )
                putExtra(Intent.EXTRA_USER_ID, supervisingUserHandle?.identifier)
            }
        setPinLauncher.launch(intent)
    }

    /** Helper method to handle errors consistently. */
    private fun handleError(errorMessage: String) {
        Log.e(SupervisionLog.TAG, errorMessage)
        setResult(RESULT_CANCELED)
        finish()
    }

    /** Helper method to handle success consistently. */
    private fun handleSuccess() {
        setResult(RESULT_OK)
        finish()
    }

    /** Logs the result of the PIN recovery process. */
    private fun logRecoveryResult(success: Boolean) {
        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
        metricsFeatureProvider.action(
            this,
            SettingsEnums.ACTION_SUPERVISION_PIN_RESET_SUCCEED,
            success,
        )
    }

    /**
     * Resets the supervision user by removing the existing one and creating a new one.
     *
     * This method first retrieves the current supervising user handle. If it exists, the user is
     * removed. Then, a new supervising user is created.
     *
     * @return True if the reset was successful, false otherwise.
     */
    @RequiresPermission(
        anyOf = [Manifest.permission.CREATE_USERS, Manifest.permission.MANAGE_USERS]
    )
    private fun resetSupervisionUser(): Boolean {
        val userManager = getSystemService(UserManager::class.java)
        supervisingUserHandle?.let { userManager.removeUserEvenWhenDisallowed(it.identifier) }
        val userInfo =
            userManager.createProfileForUserEvenWhenDisallowed(
                /* name= */ "Supervising",
                /* userType= */ USER_TYPE_PROFILE_SUPERVISING,
                /* flags= */ 0,
                /* userId= */ UserHandle.USER_NULL,
                /* disallowedPackages = */ null,
            )
        if (userInfo != null) {
            return true
        } else {
            Log.e(SupervisionLog.TAG, "Unable to create supervising profile.")
            return false
        }
    }

    companion object {
        // Action types for the PIN recovery activity.
        const val ACTION_SETUP = "android.app.supervision.action.SETUP_PIN_RECOVERY"
        const val ACTION_RECOVERY = "android.app.supervision.action.PERFORM_PIN_RECOVERY"
        const val ACTION_UPDATE = "android.app.supervision.action.UPDATE_PIN_RECOVERY"
        const val ACTION_SETUP_VERIFIED =
            "android.app.supervision.action.SETUP_VERIFIED_PIN_RECOVERY"
        const val ACTION_POST_SETUP_VERIFY =
            "android.app.supervision.action.POST_SETUP_VERIFY_PIN_RECOVERY"
    }
}
