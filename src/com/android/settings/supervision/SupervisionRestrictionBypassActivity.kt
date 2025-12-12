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

import android.app.admin.DevicePolicyManager
import android.app.supervision.SupervisionManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.android.settingslib.supervision.SupervisionLog.TAG

class SupervisionRestrictionBypassActivity : FragmentActivity() {

    private lateinit var restriction: String

    private val contract = ActivityResultContracts.StartActivityForResult()

    private val confirmCredentialsLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract) { result -> onCredentialConfirmed(result.resultCode) }

    private fun onCredentialConfirmed(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            Log.i(TAG, "Credentials confirmed. Clearing restriction: $restriction")
            val dpm = getSystemService(DevicePolicyManager::class.java)
            if (dpm != null) {
                dpm.clearUserRestriction(
                    SupervisionManager.SUPERVISION_SYSTEM_ENTITY,
                    restriction,
                    applicationContext.userId,
                )
                Log.i(
                    TAG,
                    "User restriction '$restriction' cleared for user $applicationContext.userId",
                )
            } else {
                Log.e(TAG, "DevicePolicyManager not found.")
            }
        } else {
            Log.w(TAG, "Credential confirmation failed or cancelled.")
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        restriction =
            intent.getStringExtra(Settings.EXTRA_SUPERVISION_RESTRICTION)?.takeIf {
                it.isNotBlank()
            }
                ?: run {
                    Log.e(TAG, "No valid restriction found.")
                    finish()
                    return
                }
        val confirmIntent = Intent(this, ConfirmSupervisionCredentialsActivity::class.java)
        confirmCredentialsLauncher.launch(confirmIntent)
    }
}
