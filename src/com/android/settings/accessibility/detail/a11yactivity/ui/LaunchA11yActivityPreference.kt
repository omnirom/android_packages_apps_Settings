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

package com.android.settings.accessibility.detail.a11yactivity.ui

import android.accessibilityservice.AccessibilityShortcutInfo
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.ButtonPreference

class LaunchA11yActivityPreference(private val shortcutInfo: AccessibilityShortcutInfo) :
    PreferenceMetadata, PreferenceBinding, PreferenceTitleProvider {
    override val key: String
        get() = KEY

    override val indexable
        get() = false

    override fun getTitle(context: Context): CharSequence? {
        val activityLabel = shortcutInfo.activityInfo.loadLabel(context.packageManager)
        return context.getString(R.string.accessibility_service_primary_open_title, activityLabel)
    }

    override fun createWidget(context: Context): Preference {
        return ButtonPreference(context).apply {
            setButtonStyle(ButtonPreference.TYPE_TONAL, ButtonPreference.SIZE_EXTRA_LARGE)
            setOnClickListener { view ->
                val componentName = shortcutInfo.componentName
                AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(componentName, true)
                launchShortcutTargetActivity(view.context, view.context.displayId, componentName)
            }
        }
    }

    private fun launchShortcutTargetActivity(
        context: Context,
        displayId: Int,
        name: ComponentName,
    ) {
        val bundle = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle()
        val intent =
            Intent().apply {
                component = name
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            val userId = UserHandle.myUserId()
            context.startActivityAsUser(intent, bundle, UserHandle.of(userId))
        } catch (_: ActivityNotFoundException) {
            // ignore the exception
            Log.w(TAG, "Target activity not found.")
        }
    }

    companion object {
        const val KEY = "launch_preference"
        private const val TAG = "LaunchAccessibilityActivity"
    }
}
