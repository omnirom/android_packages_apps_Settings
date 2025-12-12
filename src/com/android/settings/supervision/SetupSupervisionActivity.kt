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

import android.Manifest.permission.CREATE_USERS
import android.Manifest.permission.INTERACT_ACROSS_USERS
import android.Manifest.permission.INTERACT_ACROSS_USERS_FULL
import android.Manifest.permission.MANAGE_USERS
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.os.UserManager
import android.os.UserManager.USER_TYPE_PROFILE_SUPERVISING
import android.util.Log
import android.view.MenuItem
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.password.ChooseLockGeneric
import com.android.settingslib.collapsingtoolbar.R.drawable.settingslib_expressive_icon_back as EXPRESSIVE_BACK_ICON
import com.android.settingslib.supervision.SupervisionLog
import com.android.settingslib.widget.SettingsThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This activity starts the flow for setting up device supervision.
 *
 * The flow involves three essential steps:
 * 1. Creating a dedicated supervising user profile.
 * 2. Setting up a secure lock (e.g., PIN, password) for this profile.
 * 3. Enabling device supervision using `SupervisionManager.isSupervisionEnabled()`.
 *
 * After completing these steps, the activity prompts the user to set up PIN recovery. The result of
 * the PIN recovery setup does not affect the activity's overall result.
 *
 * The activity returns:
 * - `Activity.RESULT_OK` if steps 1-3 are successful.
 * - `Activity.RESULT_CANCELED` if any of steps 1-3 fail, or if the user cancels lock setup.
 *
 * Usage:
 * 1. Start this activity using `startActivityForResult()`.
 * 2. Handle the result in `onActivityResult()`.
 */
class SetupSupervisionActivity : FragmentActivity() {

    private val chooseLockLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleSetLockResult(result)
        }

    private val setupRecoveryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handlePinRecoveryResult(result)
        }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isSupervisingCredentialSet) {
            setResult(RESULT_CANCELED)
            finish()
        }
        if (savedInstanceState == null) {
            // Set up loading screen before enabling supervision
            setContentView(R.layout.supervision_dashboard_loading_screen)
            enableSupervision()
        }
    }

    public override fun onResume() {
        super.onResume()

        getActionBar()?.let {
            it.elevation = 0f
            it.setDisplayHomeAsUpEnabled(true)
            if (SettingsThemeHelper.isExpressiveTheme(this)) {
                it.setHomeAsUpIndicator(EXPRESSIVE_BACK_ICON)
            }
        }

        if (isSupervisingCredentialSet) {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            setResult(RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @RequiresPermission(anyOf = [CREATE_USERS, MANAGE_USERS])
    private fun enableSupervision() {
        // Create user profile in the background to avoid blocking the loading indicator UI
        lifecycleScope.launch(Dispatchers.IO) {
            val supervisingUser: UserHandle? = setupSupervisingUser()

            // Update UI on the main thread
            lifecycleScope.launch(Dispatchers.Main) {
                if (supervisingUser != null) {
                    startChooseLockActivity(supervisingUser)
                } else {
                    showGenericErrorScreen()
                }
            }
        }
    }

    /** Displays the generic error screen */
    private fun showGenericErrorScreen() {
        startActivity(Intent(this, SupervisionErrorActivity::class.java))
        setResult(RESULT_CANCELED)
        finish()
    }

    @RequiresPermission(anyOf = [CREATE_USERS, MANAGE_USERS])
    private fun setupSupervisingUser(): UserHandle? {
        val userManager = getSystemService(UserManager::class.java)
        var userHandle = userManager.supervisingUserHandle
        // If a supervising profile does not already exist on the device, create one
        if (userHandle == null) {
            val userInfo =
                userManager?.createProfileForUserEvenWhenDisallowed(
                    "Supervising",
                    USER_TYPE_PROFILE_SUPERVISING,
                    /* flags= */ 0,
                    UserHandle.USER_NULL,
                    /* disallowedPackages= */ null,
                )
            if (userInfo == null) {
                Log.w(SupervisionLog.TAG, "Could not create supervising profile.")
                return null
            }

            userHandle = userInfo.userHandle
        }

        return userHandle
    }

    @RequiresPermission(anyOf = [INTERACT_ACROSS_USERS_FULL, INTERACT_ACROSS_USERS])
    private fun startChooseLockActivity(userHandle: UserHandle) {
        val intent =
            Intent(this, ChooseLockGeneric::class.java).apply {
                putExtra(
                    LockPatternUtils.PASSWORD_TYPE_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
                )
                putExtra(Intent.EXTRA_USER_ID, userHandle.identifier)
            }
        chooseLockLauncher.launch(intent)
    }

    private fun handleSetLockResult(result: ActivityResult) {
        val supervisingUser = supervisingUserHandle
        if (supervisingUser == null) {
            Log.w(SupervisionLog.TAG, "No supervising user handle found after lock setup.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        val keyguardManager = getSystemService(KeyguardManager::class.java)
        if (!keyguardManager.isDeviceSecure(supervisingUser.identifier)) {
            Log.w(SupervisionLog.TAG, "Lock for supervising user not set up.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Enable device supervision
        val supervisionManager = getSystemService(SupervisionManager::class.java)
        supervisionManager?.setSupervisionEnabled(true)

        // Start PIN recovery setup
        startPinRecoveryActivity()
    }

    private fun handlePinRecoveryResult(result: ActivityResult) {
        if (result.resultCode == RESULT_CANCELED) {
            Log.i(SupervisionLog.TAG, "PIN recovery setup was skipped by the user.")
        }
        setResult(RESULT_OK)
        finish()
    }

    private fun startPinRecoveryActivity() {
        // prompt user to setup pin recovery if flag is enabled
        if (!Flags.enableSupervisionPinRecoveryScreen()) {
            Log.d(SupervisionLog.TAG, "PIN recovery setup is not enabled.")
            setResult(RESULT_OK)
            finish()
        }

        val intent =
            Intent(this, SupervisionPinRecoveryActivity::class.java).apply {
                action = SupervisionPinRecoveryActivity.ACTION_SETUP
            }
        setupRecoveryLauncher.launch(intent)
    }
}
