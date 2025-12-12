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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.HearingAidHelper
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference

class HearingDevicesTopIntroPreference(
    private val context: Context,
    private val helper: HearingAidHelper = HearingAidHelper(context),
) : PreferenceMetadata, PreferenceTitleProvider, PreferenceAvailabilityProvider, PreferenceBinding {
    override val key: String
        get() = KEY

    override val indexable
        get() = false

    override fun createWidget(context: Context) = TopIntroPreference(context)

    override fun getTitle(context: Context): CharSequence? {
        val isAshaProfileSupported: Boolean = helper.isAshaProfileSupported()
        val isHapClientProfileSupported: Boolean = helper.isHapClientProfileSupported()
        return if (isAshaProfileSupported && isHapClientProfileSupported) {
            context.getString(R.string.accessibility_hearingaid_intro)
        } else if (isAshaProfileSupported) {
            context.getString(R.string.accessibility_hearingaid_asha_only_intro)
        } else if (isHapClientProfileSupported) {
            context.getString(R.string.accessibility_hearingaid_hap_only_intro)
        } else {
            // Should not enter to this section, isAvailable() should handle visibility
            // for none-supported case.
            ""
        }
    }

    override fun isAvailable(context: Context): Boolean = helper.isHearingAidSupported

    companion object {
        const val KEY = "hearing_device_intro"
    }
}
