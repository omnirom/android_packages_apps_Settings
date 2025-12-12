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

package com.android.settings.network.telephony

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class ConvertToEsimPreferenceControllerTest {
    private var context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            doNothing().whenever(mock).startActivity(any())
        }

    private val preference = Preference(context).apply { key = PREF_KEY }
    private val preferenceScreen =
        PreferenceManager(context).createPreferenceScreen(context).apply {
            addPreference(preference)
        }
    private var controller = ConvertToEsimPreferenceController(context, PREF_KEY)

    @Test
    fun updateState_isAirplaneModeOn_disabled() {
        controller.notifyAirplaneModeChanged(true)
        controller.displayPreference(preferenceScreen)

        controller.updateState(preference)

        Truth.assertThat(preference.isEnabled).isFalse()
    }

    @Test
    fun updateState_isAirplaneModeOff_enabled() {
        controller.notifyAirplaneModeChanged(false)
        controller.displayPreference(preferenceScreen)

        controller.updateState(preference)

        Truth.assertThat(preference.isEnabled).isTrue()
    }

    private companion object {
        const val PREF_KEY = "ConvertToEsimPreference"
    }
}
