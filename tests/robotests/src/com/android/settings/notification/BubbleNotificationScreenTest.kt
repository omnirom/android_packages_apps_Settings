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

import android.app.ActivityManager
import android.provider.Settings.Secure.NOTIFICATION_BUBBLES
import com.android.settings.R
import com.android.settings.Settings.BubbleNotificationSettingsActivity
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowActivityManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class BubbleNotificationScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NOTIFICATIONS_25Q4

    override val preferenceScreenCreator = BubbleNotificationScreen()

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(BubbleNotificationScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(BubbleNotificationSettingsActivity::class.java.getName())
    }

    @Test
    fun getSummary_modeIsOff_showOff() {
        SettingsSecureStore.get(appContext)
            .setInt(NOTIFICATION_BUBBLES, BubbleHelper.SYSTEM_WIDE_OFF)
        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getString(R.string.switch_off_text))
    }

    @Test
    fun getSummary_modeIsOn_showOn() {
        SettingsSecureStore.get(appContext)
            .setInt(NOTIFICATION_BUBBLES, BubbleHelper.SYSTEM_WIDE_ON)
        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getString(R.string.notifications_bubble_setting_on_summary))
    }

    @Test
    @Config(shadows = [ShadowActivityManager::class, SettingsShadowResources::class])
    fun isAvailable_lowRam_returnsFalse() {
        val activityManager = shadowOf(appContext.getSystemService(ActivityManager::class.java))
        activityManager.setIsLowRamDevice(true)
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_supportsBubble,
            true,
        )
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(false)
    }

    @Test
    @Config(shadows = [ShadowActivityManager::class, SettingsShadowResources::class])
    fun isAvailable_notLowRamBubbleNotSupported_returnsFalse() {
        val activityManager = shadowOf(appContext.getSystemService(ActivityManager::class.java))
        activityManager.setIsLowRamDevice(false)
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_supportsBubble,
            false,
        )
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(false)
    }

    @Test
    @Config(shadows = [ShadowActivityManager::class, SettingsShadowResources::class])
    fun isAvailable_notLowRamBubbleSupported_returnsTrue() {
        val activityManager = shadowOf(appContext.getSystemService(ActivityManager::class.java))
        activityManager.setIsLowRamDevice(false)
        SettingsShadowResources.overrideResource(
            com.android.internal.R.bool.config_supportsBubble,
            true,
        )
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(true)
    }
}
