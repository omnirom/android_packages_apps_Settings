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

package com.android.settings.connecteddevice.display

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for [ConnectedDisplayInjector].
 *
 * This class focuses on testing the logic within the injector, particularly its interaction with
 * the Android DisplayManager service.
 */
@RunWith(AndroidJUnit4::class)
class ConnectedDisplayInjectorTest {

    @Spy private val context: Context = ApplicationProvider.getApplicationContext()
    @Mock private lateinit var mockDisplayManager: DisplayManager

    private lateinit var injector: ConnectedDisplayInjector

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        injector = ConnectedDisplayInjector(context)
        doReturn(mockDisplayManager).whenever(context).getSystemService(DisplayManager::class.java)
    }

    @Test
    fun getDisplayConnectionPreference_whenIdExists_returnsPreferenceFromManager() {
        val uniqueId = "test_display_id_1"
        val expectedPreference = 1

        whenever(mockDisplayManager.getExternalDisplayConnectionPreference(uniqueId))
            .thenReturn(expectedPreference)

        val result = injector.getDisplayConnectionPreference(uniqueId)

        assertEquals(
            "Should return the preference value from DisplayManager",
            expectedPreference,
            result,
        )
    }

    @Test
    fun getDisplayConnectionPreference_whenDisplayManagerIsNull_returnsDefaultPreference() {
        val injectorWithNullContext = ConnectedDisplayInjector(null)
        val uniqueId = "any_id"

        val result = injectorWithNullContext.getDisplayConnectionPreference(uniqueId)

        verify(mockDisplayManager, never()).getExternalDisplayConnectionPreference(uniqueId)
        assertEquals(
            "Should return default 'ASK' preference when DisplayManager is not available",
            DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_ASK,
            result,
        )
    }

    @Test
    fun updateDisplayConnectionPreference_withValidId_callsDisplayManager() {
        val uniqueId = "test_display_id_2"
        val newPreference = 2

        injector.updateDisplayConnectionPreference(uniqueId, newPreference)

        verify(mockDisplayManager).setExternalDisplayConnectionPreference(uniqueId, newPreference)
    }

    @Test
    fun updateDisplayConnectionPreference_whenDisplayManagerIsNull_doesNotCrash() {
        val injectorWithNullContext = ConnectedDisplayInjector(null)
        val uniqueId = "any_id"
        val newPreference = 1

        injectorWithNullContext.updateDisplayConnectionPreference(uniqueId, newPreference)

        verify(mockDisplayManager, never())
            .setExternalDisplayConnectionPreference(uniqueId, newPreference)
    }
}
