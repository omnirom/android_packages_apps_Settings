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
import android.provider.Settings.System.SCREEN_FLASH_NOTIFICATION
import android.provider.Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.ScreenFlashNotificationColor
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenFlashSwitchPreferenceTest {

    private val context : Context = ApplicationProvider.getApplicationContext()
    private val preference = ScreenFlashSwitchPreference()

    @Test
    fun performClick_switchOn_assertSwitchOff() {
        getStorage().setBoolean(SCREEN_FLASH_NOTIFICATION, true)
        val preferenceWidget = createScreenFlashWidget()
        assertThat(preferenceWidget.isChecked).isTrue()

        preferenceWidget.switch.performClick()

        assertThat(preferenceWidget.isChecked).isFalse()
        assertThat(getStorage().getBoolean(SCREEN_FLASH_NOTIFICATION)).isFalse()
    }

    @Test
    fun performClick_switchOff_assertSwitchOn() {
        getStorage().setBoolean(SCREEN_FLASH_NOTIFICATION, false)
        val preferenceWidget = createScreenFlashWidget()
        assertThat(preferenceWidget.isChecked).isFalse()

        preferenceWidget.switch.performClick()

        assertThat(preferenceWidget.isChecked).isTrue()
        assertThat(getStorage().getBoolean(SCREEN_FLASH_NOTIFICATION)).isTrue()
    }

    @Test
    fun setScreenColor_assertSummaryCorrect() {
        val testColor = ScreenFlashNotificationColor.BLUE
        val testColorString = context.getString(testColor.mStringRes)
        getStorage().setInt(SCREEN_FLASH_NOTIFICATION_COLOR, testColor.mColorInt)

        assertThat(preference.getSummary(context)).isEqualTo(testColorString)
    }

    private fun createScreenFlashWidget(): PrimarySwitchPreference =
        preference.createAndBindWidget<PrimarySwitchPreference>(context).apply {
            inflateViewHolder()
        }

    private fun getStorage(): KeyValueStore = preference.storage(context)
}