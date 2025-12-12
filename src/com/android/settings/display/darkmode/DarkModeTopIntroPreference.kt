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

package com.android.settings.display.darkmode

import android.content.Context
import android.view.accessibility.Flags
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference

// LINT.IfChange
internal class DarkModeTopIntroPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceTitleProvider {

    override val key: String
        get() = KEY

    override val indexable
        get() = false

    override fun createWidget(context: Context) = TopIntroPreference(context)

    override fun getTitle(context: Context): CharSequence? =
        context.getText(
            if (Flags.forceInvertColor()) {
                R.string.dark_ui_text_force_invert
            } else {
                R.string.dark_ui_text
            }
        )

    companion object {
        const val KEY = "dark_ui_top_intro"
    }
}
// LINT.ThenChange(DarkModeTopIntroPreferenceController.java)
