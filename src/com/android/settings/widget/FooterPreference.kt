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

package com.android.settings.widget

import android.content.Context
import androidx.preference.Preference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.getPreferenceTitle
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.preference.footer.R

/** Metadata of [FooterPreference]. */
interface FooterPreferenceMetadata : PreferenceMetadata {
    override val icon: Int
        get() = R.drawable.settingslib_ic_info_outline_24

    override fun isIndexable(context: Context) = false
}

/** Binding for [FooterPreferenceMetadata]. */
interface FooterPreferenceBinding : PreferenceBinding {
    override fun createWidget(context: Context) = FooterPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // In FooterPreference, setSummary is redirected to setTitle, and title is
        // reset unexpectedly. So rebind the title again.
        preference.title = metadata.getPreferenceTitle(preference.context)
        preference.isSelectable = false
    }

    companion object {
        @JvmStatic val INSTANCE = object : FooterPreferenceBinding {}
    }
}
