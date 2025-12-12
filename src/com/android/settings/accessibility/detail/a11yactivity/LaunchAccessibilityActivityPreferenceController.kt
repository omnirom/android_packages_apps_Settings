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

package com.android.settings.accessibility.detail.a11yactivity

import android.accessibilityservice.AccessibilityShortcutInfo
import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log
import android.view.View
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.widget.ButtonPreference

// LINT.IfChange
class LaunchAccessibilityActivityPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey) {
    private val TAG = "LaunchAccessibilityActivity"
    private var shortcutInfo: AccessibilityShortcutInfo? = null

    fun initialize(shortcutInfo: AccessibilityShortcutInfo) {
        this.shortcutInfo = shortcutInfo
    }

    override fun getAvailabilityStatus(): Int = AVAILABLE_UNSEARCHABLE

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        if (shortcutInfo == null) return

        val preference: ButtonPreference? = screen?.findPreference(preferenceKey)
        val componentName = shortcutInfo?.componentName

        preference?.apply {
            title =
                mContext.getString(
                    R.string.accessibility_service_primary_open_title,
                    shortcutInfo?.activityInfo?.loadLabel(mContext.packageManager),
                )
            setOnClickListener(
                object : View.OnClickListener {
                    override fun onClick(view: View?) {
                        if (view == null) return
                        AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(
                            componentName,
                            true,
                        )
                        launchShortcutTargetActivity(
                            view.context,
                            view.context.displayId,
                            componentName,
                        )
                    }
                }
            )
        }
    }

    private fun launchShortcutTargetActivity(
        context: Context,
        displayId: Int,
        name: ComponentName?,
    ) {
        val intent = Intent()
        val bundle = ActivityOptions.makeBasic().setLaunchDisplayId(displayId).toBundle()

        intent.setComponent(name)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            val userId = UserHandle.myUserId()
            context.startActivityAsUser(intent, bundle, UserHandle.of(userId))
        } catch (ignore: ActivityNotFoundException) {
            // ignore the exception
            Log.w(TAG, "Target activity not found.")
        }
    }
}
// LINT.ThenChange(ui/LaunchA11yActivityPreference.kt)
