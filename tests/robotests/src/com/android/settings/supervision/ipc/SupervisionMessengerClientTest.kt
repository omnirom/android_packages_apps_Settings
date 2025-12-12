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

package com.android.settings.supervision.ipc

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.ipc.SupervisionMessengerClient.Companion.PREFERENCE_DATA_CACHE_KEY_PREFIX
import com.android.settings.supervision.ipc.SupervisionMessengerClient.Companion.PREFERENCE_FILE_NAME
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupervisionMessengerClientTest {
    private lateinit var context: Context
    private lateinit var client: SupervisionMessengerClient

    private val prefKey1 = "pref_key_1"
    private val prefKey2 = "pref_key_2"
    private val preferenceData1 = PreferenceData(JSONObject().apply { put("title", "Title 1") })
    private val preferenceData2 = PreferenceData(JSONObject().apply { put("summary", "Summary 2") })

    private val prefs by lazy {
        context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        client = SupervisionMessengerClient(context)

        prefs.edit { clear() }
    }

    @Test
    fun getCachedPreferenceData_cacheIsEmpty_returnsEmptyMap() {
        val keys = listOf(prefKey1, prefKey2)
        val cachedData = client.getCachedPreferenceData(keys)
        assertThat(cachedData).isEmpty()
    }

    @Test
    fun getCachedPreferenceData_cacheIsPopulated_returnsCorrectData() {
        val keys = listOf(prefKey1, prefKey2)
        val expectedDataMap = mapOf(prefKey1 to preferenceData1, prefKey2 to preferenceData2)
        val jsonString =
            JSONObject()
                .put(prefKey1, preferenceData1.toJsonObject())
                .put(prefKey2, preferenceData2.toJsonObject())
                .toString()
        val cacheKey = "${PREFERENCE_DATA_CACHE_KEY_PREFIX}_${prefKey1}_${prefKey2}"

        prefs.edit { putString(cacheKey, jsonString) }
        val cachedData = client.getCachedPreferenceData(keys)

        assertThat(cachedData).isEqualTo(expectedDataMap)
    }

    @Test
    fun getCachedPreferenceData_cacheIsMalformed_returnsEmptyMap() {
        val keys = listOf(prefKey1)
        val cacheKey = "${PREFERENCE_DATA_CACHE_KEY_PREFIX}_${prefKey1}"
        val malformedJson = "this is not a valid json string"

        prefs.edit { putString(cacheKey, malformedJson) }

        val cachedData = client.getCachedPreferenceData(keys)

        assertThat(cachedData).isEmpty()
    }

    @Test
    fun generateCacheKey_isDeterministicAndSorted() {
        val keys1 = listOf(prefKey1, prefKey2)
        val keys2 = listOf(prefKey2, prefKey1)
        val jsonString =
            JSONObject()
                .put(prefKey1, preferenceData1.toJsonObject())
                .put(prefKey2, preferenceData2.toJsonObject())
                .toString()
        val expectedKey = "${PREFERENCE_DATA_CACHE_KEY_PREFIX}_${prefKey1}_${prefKey2}"
        prefs.edit { putString(expectedKey, jsonString) }

        // Use both key lists to retrieve data; they should both hit the same cache entry.
        val result1 = client.getCachedPreferenceData(keys1)
        val result2 = client.getCachedPreferenceData(keys2)

        assertThat(result1).isNotNull()
        assertThat(result2).isNotNull()
        assertThat(result1).isEqualTo(result2)
    }

    @Test
    fun getCachedPreferenceData_withDifferentKeySet_returnsEmptyMap() {
        val jsonString =
            JSONObject()
                .put(prefKey1, preferenceData1.toJsonObject())
                .put(prefKey2, preferenceData2.toJsonObject())
                .toString()
        val cacheKey = "${PREFERENCE_DATA_CACHE_KEY_PREFIX}_${prefKey1}_${prefKey2}"

        prefs.edit { putString(cacheKey, jsonString) }

        val differentKeys = listOf("different_key")
        val cachedData = client.getCachedPreferenceData(differentKeys)

        assertThat(cachedData).isEmpty()
    }
}
