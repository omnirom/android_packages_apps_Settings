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

package com.android.settings.communal

import android.content.Context
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.widget.IllustrationPreference

/**
 * Preference controller for the illustration that appears on the "widgets on lock screen" settings
 * screen.
 */
class WidgetsIllustrationPreferenceController
@JvmOverloads
constructor(private val context: Context, key: String) : BasePreferenceController(context, key) {

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)

        val illustrationPreference = screen.findPreference<IllustrationPreference>(preferenceKey)
        illustrationPreference?.imageDrawable = context.getDrawable(R.drawable.widgets_illustration)
        illustrationPreference?.isSelectable = false
    }

    override fun getAvailabilityStatus() = AVAILABLE
}
