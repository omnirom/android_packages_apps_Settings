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

package com.android.settings.accessibility.screenmagnification

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.accessibility.IllustrationPreferenceController
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import java.lang.String

/** Controls the accessibility screen magnification illustration preference. */
// LINT.IfChange
class MagnificationIllustrationPreferenceController(context: Context, prefKey: kotlin.String) :
    IllustrationPreferenceController(context, prefKey) {

    init {
        val imageUri =
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.packageName)
                .appendPath(
                    String.valueOf(
                        if (isExpressiveTheme(context)) {
                            R.raw.accessibility_magnification_banner_expressive
                        } else {
                            R.raw.accessibility_magnification_banner
                        }
                    )
                )
                .build()
        val contentDescription =
            context.getString(
                R.string.accessibility_illustration_content_description,
                context.getText(R.string.accessibility_screen_magnification_title),
            )
        initialize(imageUri, contentDescription)
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        val illustrationPref: IllustrationPreference? = screen?.findPreference(preferenceKey)
        illustrationPref?.applyDynamicColor()
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/MagnificationIllustrationPreference.kt)
