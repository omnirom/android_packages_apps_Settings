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
package com.android.settings.applications.appinfo

import android.app.appfunctions.AppFunctionManager
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.Preference
import com.android.settings.appfunctions.AppFunctionAccessUtil
import com.android.settings.appfunctions.AppFunctionManagerWrapper
import com.android.settings.appfunctions.AppFunctionManagerWrapperImpl
import kotlinx.coroutines.launch

/** A PreferenceController handling the logic for Agent access settings. */
class ManageAgentAppFunctionAccessPreferenceController(context: Context, key: String) :
    AppInfoPreferenceControllerBase(context, key) {
    // Initialize as unavailable to prevent ANR; the status will be updated asynchronously.
    private var availabilityStatus = CONDITIONALLY_UNAVAILABLE

    override fun getAvailabilityStatus() = availabilityStatus

    override fun onViewCreated(viewLifecycleOwner: LifecycleOwner) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateAvailability(AppFunctionManagerWrapperImpl(mContext))
            }
        }
    }

    @VisibleForTesting
    suspend fun updateAvailability(appFunctionManagerWrapper: AppFunctionManagerWrapper) {
        availabilityStatus =
            if (
                AppFunctionAccessUtil.isAppFunctionAccessEnabled(mContext) &&
                    appFunctionManagerWrapper.isValidAgent(mAppEntry.info.packageName)
            ) {
                AVAILABLE
            } else {
                CONDITIONALLY_UNAVAILABLE
            }
        mPreference?.isVisible = availabilityStatus == AVAILABLE
    }

    override fun handlePreferenceTreeClick(preference: Preference?): Boolean {
        if ((preferenceKey == preference!!.key)) {
            startManageAppFunctionAccessActivity()
            return true
        }
        return false
    }

    private fun startManageAppFunctionAccessActivity() {
        val intent =
            Intent(AppFunctionManager.ACTION_MANAGE_AGENT_APP_FUNCTION_ACCESS)
                .putExtra(Intent.EXTRA_PACKAGE_NAME, mAppEntry.info.packageName)
        mContext.startActivityAsUser(intent, UserHandle.getUserHandleForUid(mAppEntry.info.uid))
    }
}
