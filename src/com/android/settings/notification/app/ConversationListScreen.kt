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

package com.android.settings.notification.app

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ConversationListSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.notification.NotificationBackend
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.utils.StringUtil
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(ConversationListScreen.KEY)
open class ConversationListScreen : PreferenceScreenMixin, PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.zen_mode_conversations_title

    override val title: Int
        get() = R.string.conversations_category_title

    private val backend = NotificationBackend()

    override fun isFlagEnabled(context: Context) = Flags.deeplinkNotifications25q4()

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun fragmentClass(): Class<out Fragment>? = ConversationListSettings::class.java

    override fun hasCompleteHierarchy() = false

    override val highlightMenuKey
        get() = R.string.menu_key_notifications

    override fun getMetricsCategory(): Int = SettingsEnums.NOTIFICATION_CONVERSATION_LIST_SETTINGS

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ConversationListSettingsActivity::class.java, metadata?.key)

    override fun getSummary(context: Context): CharSequence? {
        val count: Int = backend.getConversations(true).getList().size
        if (count == 0) {
            return context.getText(R.string.priority_conversation_count_zero)
        }
        return StringUtil.getIcuPluralsString(context, count, R.string.priority_conversation_count)
    }

    companion object {
        const val KEY = "conversations"
    }
}
// LINT.ThenChange(ConversationListSettings.java, ../ConversationListSummaryPreferenceController.java)
