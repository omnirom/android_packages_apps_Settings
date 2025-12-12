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
import android.provider.Settings.System.CAMERA_FLASH_NOTIFICATION
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.ShadowFlashNotificationsUtils
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowFlashNotificationsUtils::class])
class CameraFlashSwitchPreferenceTest {

    private val context : Context = ApplicationProvider.getApplicationContext()
    private val preference = CameraFlashSwitchPreference()

    @After
    fun tearDown() {
        ShadowFlashNotificationsUtils.reset()
    }

    @Test
    fun isAvailable_noValidCamera_returnFalse() {
        ShadowFlashNotificationsUtils.setIsTorchAvailable(false)
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noValidCamera_returnTrue() {
        ShadowFlashNotificationsUtils.setIsTorchAvailable(true)
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun performClick_switchOn_assertSwitchOff() {
        getStorage().setBoolean(CAMERA_FLASH_NOTIFICATION, true)
        val preferenceWidget = createCameraFlashWidget()
        assertThat(preferenceWidget.isChecked).isTrue()

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isFalse()
        assertThat(getStorage().getBoolean(CAMERA_FLASH_NOTIFICATION)).isFalse()
    }

    @Test
    fun performClick_switchOff_assertSwitchOn() {
        getStorage().setBoolean(CAMERA_FLASH_NOTIFICATION, false)
        val preferenceWidget = createCameraFlashWidget()
        assertThat(preferenceWidget.isChecked).isFalse()

        preferenceWidget.performClick()

        assertThat(preferenceWidget.isChecked).isTrue()
        assertThat(getStorage().getBoolean(CAMERA_FLASH_NOTIFICATION)).isTrue()
    }

    private fun createCameraFlashWidget(): SwitchPreferenceCompat =
        preference.createAndBindWidget<SwitchPreferenceCompat>(context).apply {
            inflateViewHolder()
        }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}