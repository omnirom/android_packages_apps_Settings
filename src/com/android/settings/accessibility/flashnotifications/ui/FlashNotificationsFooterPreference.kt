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

package com.android.settings.accessibility.flashnotifications.ui

import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.widget.FooterPreference

class FlashNotificationsFooterPreference : FooterPreferenceMetadata, FooterPreferenceBinding {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.flash_notifications_note

    override val indexable
        get() = false

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)

        val footerPreference = preference as FooterPreference
        footerPreference.isSelectable = false

        val aboutTitle = preference.context.getString(R.string.flash_notifications_about_title)
        footerPreference.contentDescription = "$aboutTitle\n${footerPreference.title}"
    }

    companion object {
        const val KEY = "flash_notifications_footer"
    }
}
