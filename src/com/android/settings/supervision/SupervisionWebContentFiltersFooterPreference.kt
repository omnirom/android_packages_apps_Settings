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

import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.HelpUtils
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.widget.FooterPreference

class SupervisionWebContentFiltersFooterPreference :
    FooterPreferenceMetadata, FooterPreferenceBinding {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_web_content_filters_footer_title

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val footerPreference = preference as FooterPreference
        val context = preference.context

        footerPreference.setLearnMoreAction {
            val intent =
                HelpUtils.getHelpIntent(
                    context,
                    context.getString(R.string.supervision_web_content_filters_learn_more_link),
                    context::class.java.name,
                )
            if (intent != null) {
                context.startActivity(intent)
            } else {
                Log.w(TAG, "HelpIntent is null")
            }
        }
    }

    companion object {
        private const val TAG = "WebContentFilters"
        const val KEY = "web_content_filters_footer"
    }
}
