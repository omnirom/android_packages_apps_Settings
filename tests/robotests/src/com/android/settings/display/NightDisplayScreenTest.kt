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

package com.android.settings.display

import android.content.ContextWrapper
import android.content.res.Resources
import android.hardware.display.ColorDisplayManager
import com.android.internal.R
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class NightDisplayScreenTest : SettingsCatalystTestCase() {
    private val mockResources = mock<Resources>()
    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    val colorDM: ColorDisplayManager = context.getSystemService(ColorDisplayManager::class.java)

    override val preferenceScreenCreator = NightDisplayScreen(context)

    override val flagName: String
        get() = Flags.FLAG_CATALYST_NIGHT_DISPLAY

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(NightDisplayScreen.KEY)
    }

    @Test
    fun configuredAvailable() {
        mockResources.stub { on { getBoolean(R.bool.config_nightDisplayAvailable) } doReturn true }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun configuredUnavailable() {
        mockResources.stub { on { getBoolean(R.bool.config_nightDisplayAvailable) } doReturn false }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun toggleOn() {
        mockResources.stub { on { getBoolean(R.bool.config_nightDisplayAvailable) } doReturn true }

        colorDM.isNightDisplayActivated = false
        assertThat(colorDM.isNightDisplayActivated).isFalse()

        preferenceScreenCreator.storage(context).setBoolean(NightDisplayScreen.KEY, true)
        assertThat(colorDM.isNightDisplayActivated).isTrue()
    }

    @Test
    fun toggleOff() {
        mockResources.stub { on { getBoolean(R.bool.config_nightDisplayAvailable) } doReturn true }

        colorDM.isNightDisplayActivated = true
        assertThat(colorDM.isNightDisplayActivated).isTrue()

        preferenceScreenCreator.storage(context).setBoolean(NightDisplayScreen.KEY, false)
        assertThat(colorDM.isNightDisplayActivated).isFalse()
    }

    override fun migration() {}
}
