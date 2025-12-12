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

package com.android.settings.accessibility.shared

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import com.android.settings.accessibility.extensions.isInSetupWizard

class LaunchAppInfoPreferenceController(context: Context, prefKey: String) :
    LaunchIntentPreferenceController(context, prefKey) {

    fun initialize(componentName: ComponentName) {
        if (!mContext.packageManager.isPackageAvailable(componentName.packageName)) {
            return
        }

        val intent: Intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                setPackage(mContext.packageName)
                setData("package:${componentName.packageName}".toUri())
            }

        setIntent(intent)
    }

    override fun getAvailabilityStatus(): Int {
        return if (mContext.isInSetupWizard()) {
            CONDITIONALLY_UNAVAILABLE
        } else {
            super.getAvailabilityStatus()
        }
    }
}
