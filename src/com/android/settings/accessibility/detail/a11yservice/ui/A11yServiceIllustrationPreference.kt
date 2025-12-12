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

package com.android.settings.accessibility.detail.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.android.settings.R
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.shared.ui.ImageUriPreference

class A11yServiceIllustrationPreference(private val serviceInfo: AccessibilityServiceInfo) :
    ImageUriPreference() {

    override fun getImageUri(context: Context): Uri? {
        return serviceInfo.run {
            if (animatedImageRes > 0) {
                Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(componentName.packageName)
                    .appendPath(animatedImageRes.toString())
                    .build()
            } else {
                null
            }
        }
    }

    override fun getContentDescription(context: Context): CharSequence? {
        return context.getString(
            R.string.accessibility_illustration_content_description,
            serviceInfo.getFeatureName(context),
        )
    }
}
