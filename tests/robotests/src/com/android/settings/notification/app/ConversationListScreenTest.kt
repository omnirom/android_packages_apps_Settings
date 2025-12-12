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

import android.content.pm.ParceledListSlice
import android.service.notification.ConversationChannelWrapper
import com.android.settings.R
import com.android.settings.Settings.ConversationListSettingsActivity
import com.android.settings.flags.Flags
import com.android.settings.notification.NotificationBackend
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

class ConversationListScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NOTIFICATIONS_25Q4

    override val preferenceScreenCreator = ConversationListScreen()

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ConversationListScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(ConversationListSettingsActivity::class.java.getName())
    }

    @Test
    @Config(shadows = [ShadowNotificationBackend::class])
    fun getSummary_returnZeroAppsString() {
        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getString(R.string.priority_conversation_count_zero))
    }

    @Test
    @Config(
        shadows =
            [ShadowNotificationBackend::class, ShadowRecentConversationsPreferenceController::class]
    )
    override fun migration() {
        super.migration()
    }
}

@Implements(NotificationBackend::class)
class ShadowNotificationBackend {
    @Implementation
    fun getConversations(onlyImportant: Boolean): ParceledListSlice<ConversationChannelWrapper> =
        ParceledListSlice.emptyList()
}

@Implements(RecentConversationsPreferenceController::class)
class ShadowRecentConversationsPreferenceController {
    @Implementation fun updateList(): Boolean = true
}
