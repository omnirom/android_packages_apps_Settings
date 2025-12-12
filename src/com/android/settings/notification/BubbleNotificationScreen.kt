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

package com.android.settings.notification

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings.Secure.NOTIFICATION_BUBBLES
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.BubbleNotificationSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(BubbleNotificationScreen.KEY)
open class BubbleNotificationScreen :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.notification_bubbles_title

    override val screenTitle: Int
        get() = R.string.bubbles_app_toggle_title

    override fun isFlagEnabled(context: Context) = Flags.deeplinkNotifications25q4()

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun fragmentClass(): Class<out Fragment>? = BubbleNotificationSettings::class.java

    override fun hasCompleteHierarchy() = false

    override val highlightMenuKey
        get() = R.string.menu_key_notifications

    override fun getMetricsCategory(): Int = SettingsEnums.BUBBLE_SETTINGS

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, BubbleNotificationSettingsActivity::class.java, metadata?.key)

    override fun getSummary(context: Context): CharSequence? {
        val enabled =
            SettingsSecureStore.get(context).getInt(NOTIFICATION_BUBBLES)
                ?: BubbleHelper.SYSTEM_WIDE_ON
        return context.getString(
            if (enabled == BubbleHelper.SYSTEM_WIDE_ON)
                R.string.notifications_bubble_setting_on_summary
            else R.string.switch_off_text
        )
    }

    override fun isAvailable(context: Context): Boolean = BubbleHelper.isSupportedByDevice(context)

    companion object {
        const val KEY = "notification_bubbles"
    }
}
// LINT.ThenChange(BubbleNotificationSettings.java, BubbleSummaryNotificationPreferenceController.java)
