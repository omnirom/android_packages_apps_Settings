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

package com.android.settings.accessibility.shared.ui

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata

/**
 * Preference that launches the app info page for a given package name.
 *
 * @param key The key of the preference.
 * @param title The title of the preference. Defaults to "App info".
 * @param packageName The package name of the app to launch the info page for.
 */
class LaunchAppInfoPreference(
    override val key: String,
    override val title: Int = R.string.application_info_label,
    private val packageName: String,
) : PreferenceMetadata, PreferenceAvailabilityProvider {

    override val indexable
        get() = false

    override fun intent(context: Context): Intent? {
        return if (context.packageManager.isPackageAvailable(packageName)) {
            Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
                setData("package:$packageName".toUri())
            }
        } else {
            null
        }
    }

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard() && intent(context) != null
    }
}
