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

package com.android.settings.accessibility.colorinversion.ui

import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.IllustrationPreference

class ColorInversionIllustrationPreference : PreferenceMetadata, PreferenceBinding {
    override val key: String
        get() = KEY

    override val indexable
        get() = false

    override fun createWidget(context: Context) =
        IllustrationPreference(context).apply {
            isSelectable = false
            lottieAnimationResId = R.raw.accessibility_color_inversion_banner
            contentDescription =
                context.getString(
                    R.string.accessibility_illustration_content_description,
                    context.getText(R.string.accessibility_display_inversion_preference_title),
                )
        }

    companion object {
        const val KEY = "animated_image"
    }
}
