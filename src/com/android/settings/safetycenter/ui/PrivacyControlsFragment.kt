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

package com.android.settings.safetycenter.ui;

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/**
 * Fragment that displays several privacy toggle controls.
 * This fragment is a sub-page of the main Safety Center UI.
 * It hosts preferences for privacy controls like camera, mic and clipboard access
 */
@SearchIndexable
class PrivacyControlsFragment : DashboardFragment() {

    private val TAG = "PrivacyControlsFragment"

    override fun getLogTag(): String = TAG

    override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_privacy_controls_settings
    }

    override fun getMetricsCategory(): Int = SettingsEnums.SAFETY_CENTER

    companion object {
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER =
            object : BaseSearchIndexProvider(R.xml.safety_center_privacy_controls_settings) {
                public override fun isPageSearchEnabled(context: Context): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }
            }
    }
}
