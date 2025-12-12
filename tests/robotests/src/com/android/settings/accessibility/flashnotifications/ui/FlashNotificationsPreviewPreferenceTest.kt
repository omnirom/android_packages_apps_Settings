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
import android.os.UserHandle.SYSTEM
import android.widget.Button
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.FlashNotificationsUtil
import com.android.settings.accessibility.FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_START_PREVIEW
import com.android.settings.accessibility.FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE
import com.android.settings.accessibility.FlashNotificationsUtil.TYPE_LONG_PREVIEW
import com.android.settings.accessibility.FlashNotificationsUtil.TYPE_SHORT_PREVIEW
import com.android.settings.accessibility.ShadowFlashNotificationsUtils
import com.android.settings.accessibility.ShadowFlashNotificationsUtils.setFlashNotificationsState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowFlashNotificationsUtils::class])
class FlashNotificationsPreviewPreferenceTest {

    private val context : Context = ApplicationProvider.getApplicationContext()
    private val preference = FlashNotificationsPreviewPreference()

    @Test
    fun isAvailable_cameraOff_screenOff_returnFalse() {
        setFlashNotificationsState(FlashNotificationsUtil.State.OFF)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_cameraOn_screenOff_returnTrue() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_cameraOff_screenOn_returnTrue() {
        setFlashNotificationsState(FlashNotificationsUtil.State.SCREEN)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_cameraOn_screenOn_returnTrue() {
        setFlashNotificationsState(FlashNotificationsUtil.State.CAMERA_SCREEN)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun performClick_assertResult() {
        val testContext = mock<Context>()
        val testButton = mock<Button> {
            on { context } doReturn testContext
        }
        preference.onClick(testButton)

        val intent = argumentCaptor {
            verify(testContext).sendBroadcastAsUser(
                capture(),
                eq(SYSTEM)
            )
        }.firstValue
        assertThat(intent.action).isEqualTo(ACTION_FLASH_NOTIFICATION_START_PREVIEW)
        assertThat(
            intent.getIntExtra(
                EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE,
                TYPE_LONG_PREVIEW
            )
        ).isEqualTo(TYPE_SHORT_PREVIEW)
    }
}
