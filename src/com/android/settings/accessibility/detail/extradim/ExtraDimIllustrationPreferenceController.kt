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

package com.android.settings.accessibility.detail.extradim

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.android.settings.R
import com.android.settings.accessibility.IllustrationPreferenceController

class ExtraDimIllustrationPreferenceController(context: Context, prefKey: String) :
    IllustrationPreferenceController(context, prefKey) {
    init {
        val imageUri =
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.packageName)
                .appendPath(R.raw.extra_dim_banner.toString())
                .build()
        val contentDescription =
            context.getString(
                R.string.accessibility_illustration_content_description,
                context.getText(R.string.reduce_bright_colors_preference_title),
            )
        initialize(imageUri, contentDescription)
    }
}
