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

package com.android.settings.network.telephony.satellite

import android.content.Context
import android.telephony.TelephonyManager
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.network.telephony.satellite.SatelliteSettingAboutContentController.Companion.PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

class SatelliteSettingAboutContentControllerTest {
    private val mockTelephonyManager: TelephonyManager = mock<TelephonyManager> {
        on { getSimOperatorName(TEST_SUB_ID) } doReturn TEST_SIM_OPERATOR_NAME
    }

    private var context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
    }

    private val controller = SatelliteSettingAboutContentController(
        context = context,
        key = PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY,
    )

    private lateinit var screen: PreferenceScreen
    private lateinit var preference: TopIntroPreference

    @Before
    fun setUp() {
        preference =
            TopIntroPreference(context).apply { key = PREF_KEY_ABOUT_SATELLITE_CONNECTIVITY }
        screen = PreferenceManager(context).createPreferenceScreen(context)
        screen.addPreference(preference)
    }

    @Test
    fun displayPreference_preferenceTitle_hasSimOperatorName() {
        controller.init(TEST_SUB_ID)

        controller.displayPreference(screen)

        assertThat(preference.title).isEqualTo(
            "You can send and receive text messages and use some apps by satellite with an eligible Test Carrier account"
            )

    }

    private companion object {
        const val TEST_SUB_ID = 1
        const val TEST_SIM_OPERATOR_NAME = "Test Carrier"
    }
}