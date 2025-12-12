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

package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.preference.PreferenceScreen
import com.android.settingslib.bluetooth.hearingdevices.metrics.HearingDeviceStatsLogUtils
import com.android.settingslib.bluetooth.hearingdevices.metrics.HearingDeviceStatsLogUtils.HistoryType
import com.android.settingslib.widget.ButtonPreference

/**
 * Controller for managing the display and behavior of a feedback button preference specifically for
 * hearing devices. This class extends [FeedbackButtonPreferenceController] then sets the feedback
 * page ID based on the type of hearing aid that was most recently used (ASHA or LE Audio).
 */
class HearingDevicesFeedbackButtonPreferenceController(context: Context, prefKey: String) :
    FeedbackButtonPreferenceController(context, prefKey) {

    /**
     * Sets up the button preference and its click listener.
     *
     * When the button is clicked, it determines the appropriate feedback page ID based on the
     * hearing aid type and then launches the feedback activity.
     */
    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        screen.findPreference<ButtonPreference>(mPreferenceKey)?.apply {
            setOnClickListener { view ->
                // In order to get the latest result for the user's hearing device type, we need to
                // wait until the user clicks feedback button to have a new FeedbackManager.
                // Fallback to the parent's manager if the one with custom pageId is not available.
                val manager =
                    FeedbackManager(context, getPageIdForLatestUserType(context)).takeIf {
                        it.isAvailable
                    } ?: getFeedbackManager()
                view.context.startActivityForResult(
                    mPreferenceKey,
                    manager?.getFeedbackIntent(),
                    /* requestCode= */ 0,
                    /* options= */ null,
                )
            }
        }
    }

    /**
     * Gets the feedback page ID based on the latest hearing aid history type.
     *
     * This method checks the latest hearing aid connection history to determine whether the user
     * was using an ASHA or LE Audio hearing aid. It then returns the corresponding feedback page
     * ID.
     *
     * @param context The context.
     * @return The feedback page ID.
     */
    private fun getPageIdForLatestUserType(context: Context): Int {
        return when (HearingDeviceStatsLogUtils.getLatestHistoryType(context)) {
            HistoryType.TYPE_LE_HEARING_PAIRED,
            HistoryType.TYPE_LE_HEARING_CONNECTED ->
                SettingsEnums.ACCESSIBILITY_HEARING_DEVICES_HAP_FEEDBACK
            HistoryType.TYPE_HEARING_PAIRED,
            HistoryType.TYPE_HEARING_CONNECTED ->
                SettingsEnums.ACCESSIBILITY_HEARING_DEVICES_ASHA_FEEDBACK
            else -> SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS
        }
    }
}
