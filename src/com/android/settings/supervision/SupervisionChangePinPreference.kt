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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_CHANGE_PIN
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.internal.widget.LockPatternUtils
import com.android.settings.R
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.password.ChooseLockGeneric
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * update the existing device supervision PIN.
 */
class SupervisionChangePinPreference : PreferenceMetadata, PreferenceActionMetricsProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_change_pin_preference_title

    override val preferenceActionMetrics: Int
        get() = ACTION_SUPERVISION_CHANGE_PIN

    override fun intent(context: Context): Intent? {
        if (!context.isSupervisingCredentialSet) {
            Log.w(TAG, "Supervising credential not set")
            return null
        }
        return Intent(context, ChooseLockGeneric::class.java).apply {
            // To go directly to setting up a PIN
            putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC,
            )
            putExtra(Intent.EXTRA_USER_ID, context.supervisingUserHandle?.identifier)
        }
    }

    companion object {
        const val KEY = "supervision_change_pin"
    }
}
