/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.spa.search

import android.app.settings.SettingsEnums
import android.os.Bundle
import com.android.settings.core.SubSettingLauncher
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.password.PasswordUtils
import com.android.settings.spa.SpaDestination
import com.android.settingslib.spa.search.SpaSearchLandingActivity

class SettingsSpaSearchLandingActivity : SpaSearchLandingActivity() {
    override fun isValidCall(): Boolean {
        val callingAppPackageName = PasswordUtils.getCallingAppPackageName(activityToken)
        if (callingAppPackageName == packageName) {
            // SettingsIntelligence sometimes starts SearchResultTrampoline first, in this case,
            // SearchResultTrampoline checks if the call is valid, then SearchResultTrampoline will
            // start this activity, allow this use case.
            return true
        }
        return callingAppPackageName ==
            featureFactory.searchFeatureProvider.getSettingsIntelligencePkgName(this)
    }

    override fun startSpaPage(destination: String, highlightItemKey: String) {
        SpaDestination(destination = destination, highlightItemKey = highlightItemKey)
            .startFromSearch(this)
    }

    override fun startFragment(fragmentName: String, arguments: Bundle) {
        SubSettingLauncher(this)
            .setDestination(fragmentName)
            .setArguments(arguments)
            .setSourceMetricsCategory(SettingsEnums.PAGE_UNKNOWN)
            .launch()
    }
}
