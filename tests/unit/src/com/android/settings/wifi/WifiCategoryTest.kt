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

package com.android.settings.wifi

import androidx.preference.PreferenceCategory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.network.NetworkProviderSettings
import com.android.settingslib.RestrictedSwitchPreference
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class WifiCategoryTest {

    private val mockFragment = mock<NetworkProviderSettings>()
    private val mockCategory = mock<PreferenceCategory>()
    private val mockSwitchPreference = mock<RestrictedSwitchPreference>()
    private val mockWifiEntryPreference = mock<WifiEntryPreference>()

    private val wifiCategory = WifiCategory(mockFragment)

    @Before
    fun setUp() {
        mockFragment.stub {
            on { findPreference<PreferenceCategory>(WifiCategory.KEY) } doReturn mockCategory
        }
    }

    @Test
    fun removeWifiEntryPreferences_onlySwitchPreference_doNotRemoveSwitchPreference() {
        mockCategory.stub {
            on { preferenceCount } doReturn 1
            on { getPreference(0) } doReturn mockSwitchPreference
        }

        wifiCategory.removeWifiEntryPreferences()

        verify(mockCategory, never()).removePreference(mockSwitchPreference)
    }

    @Test
    fun removeWifiEntryPreferences_containsWifiEntryPreference_removeWifiEntryPreference() {
        mockCategory.stub {
            on { preferenceCount } doReturn 2
            on { getPreference(0) } doReturn mockSwitchPreference
            on { getPreference(1) } doReturn mockWifiEntryPreference
        }

        wifiCategory.removeWifiEntryPreferences()

        verify(mockCategory, never()).removePreference(mockSwitchPreference)
        verify(mockCategory).removePreference(mockWifiEntryPreference)
    }
}