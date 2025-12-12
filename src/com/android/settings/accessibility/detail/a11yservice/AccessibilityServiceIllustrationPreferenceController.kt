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

package com.android.settings.accessibility.detail.a11yservice

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.android.settings.R
import com.android.settings.accessibility.IllustrationPreferenceController
import com.android.settings.accessibility.extensions.getFeatureName

class AccessibilityServiceIllustrationPreferenceController(context: Context, prefKey: String) :
    IllustrationPreferenceController(context, prefKey) {

    fun initialize(serviceInfo: AccessibilityServiceInfo) {
        val componentName = serviceInfo.componentName
        val imageRes = serviceInfo.animatedImageRes
        val imageUri =
            if (imageRes > 0) {
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(componentName.packageName)
                    .appendPath("$imageRes")
                    .build()
            } else {
                null
            }

        val contentDescription =
            mContext.getString(
                R.string.accessibility_illustration_content_description,
                serviceInfo.getFeatureName(mContext),
            )

        initialize(imageUri, contentDescription)
    }
}
