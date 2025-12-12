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

package com.android.settings.location

import android.content.ContextWrapper
import android.content.res.Resources
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class LocationServicesScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_OTHERS_25Q4

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    override val preferenceScreenCreator = LocationServicesScreen()

    @Test
    fun isAvailable_configIsTrue_returnsTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configIsFalse_returnsFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }
}
