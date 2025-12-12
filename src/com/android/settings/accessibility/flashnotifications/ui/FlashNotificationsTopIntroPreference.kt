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

import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.FlashNotificationsUtil
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.TopIntroPreference

// LINT.IfChange
class FlashNotificationsTopIntroPreference :
    PreferenceMetadata, PreferenceTitleProvider, PreferenceBinding {
    override val key: String
        get() = "flash_notifications_intro"

    override val indexable
        get() = false

    override fun createWidget(context: Context) = TopIntroPreference(context)

    override fun getTitle(context: Context): CharSequence? {
        return if (FlashNotificationsUtil.isTorchAvailable(context)) {
            context.getString(R.string.flash_notifications_intro)
        } else {
            context.getString(R.string.flash_notifications_intro_without_camera_flash)
        }
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/FlashNotificationsIntroPreferenceController.java)
