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

import android.util.FeatureFlagUtils
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.FlashNotificationsUtil
import com.android.settings.accessibility.ShadowFlashNotificationsUtils
import com.android.settings.accessibility.ShadowFlashNotificationsUtils.setFlashNotificationsState
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.annotation.Config

@Config(shadows = [ShadowFlashNotificationsUtils::class])
class FlashNotificationsScreenTest : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = FlashNotificationsScreen()
    override val flagName: String = Flags.FLAG_CATALYST_FLASH_NOTIFICATIONS

    @Test override fun migration() {}

    @Test
    fun isAvailable_flashNotificationNotSupported_returnFalse() {
        FeatureFlagUtils.setEnabled(
            appContext,
            FeatureFlagUtils.SETTINGS_FLASH_NOTIFICATIONS,
            false,
        )

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isFalse()
    }

    @Test
    fun isAvailable_flashNotificationSupported_returnTrue() {
        FeatureFlagUtils.setEnabled(appContext, FeatureFlagUtils.SETTINGS_FLASH_NOTIFICATIONS, true)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isTrue()
    }

    @Test
    fun getSummary_stateOff_assertOff() {
        setFlashNotificationsState(FlashNotificationsUtil.State.OFF)

        assertThat(preferenceScreenCreator.getSummary(appContext).toString())
            .isEqualTo(appContext.getString(R.string.flash_notifications_summary_off))
    }

    @Test
    fun getSummary_stateCamera_assertOn() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA)

        assertThat(preferenceScreenCreator.getSummary(appContext).toString())
            .isEqualTo(appContext.getString(R.string.flash_notifications_summary_on))
    }

    @Test
    fun getSummary_stateScreen_assertOn() {
        setFlashNotificationsState(FlashNotificationsUtil.State.SCREEN)

        assertThat(preferenceScreenCreator.getSummary(appContext).toString())
            .isEqualTo(appContext.getString(R.string.flash_notifications_summary_on))
    }

    @Test
    fun getSummary_stateCameraScreen_assertOn() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA_SCREEN)

        assertThat(preferenceScreenCreator.getSummary(appContext).toString())
            .isEqualTo(appContext.getString(R.string.flash_notifications_summary_on))
    }
}
