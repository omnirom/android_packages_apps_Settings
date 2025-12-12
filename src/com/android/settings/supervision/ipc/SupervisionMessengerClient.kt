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
import android.content.Intent
import android.util.Log
import androidx.core.content.edit
import com.android.settings.supervision.PreferenceDataProvider
import com.android.settings.supervision.SupportedAppsProvider
import com.android.settings.supervision.systemSupervisionPackageName
import com.android.settingslib.ipc.MessengerServiceClient
import com.android.settingslib.supervision.SupervisionLog
import org.json.JSONException
import org.json.JSONObject

/**
 * A specialized [MessengerServiceClient] for interacting with the system supervision service.
 *
 * This class extends [MessengerServiceClient] to provide specific functionality for communicating
 * with the system supervision service. It defines the action for binding to the system supervision
 * service, and provides a method for retrieving preference data from the supervision app.
 *
 * @param context The Android Context used for binding to the service.
 */
class SupervisionMessengerClient(context: Context) :
    MessengerServiceClient(context), PreferenceDataProvider, SupportedAppsProvider {

    override val serviceIntentFactory = { Intent(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION) }

    override val packageName = context.systemSupervisionPackageName

    private val prefs by lazy {
        context.getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Retrieves preference data from the system supervision app.
     *
     * This suspend function sends a request to the supervision app for the specified preference
     * keys and returns a map of preference data. If an error occurs during the communication, an
     * empty map is returned and the error is logged.
     *
     * @param keys A list of preference keys to retrieve.
     * @return A map of preference data, where the keys are the preference keys and the values are
     *   [PreferenceData] objects, or an empty map if an error occurs.
     */
    override suspend fun getPreferenceData(keys: List<String>): Map<String, PreferenceData> {
        if (keys.isEmpty()) {
            return mapOf()
        }
        val cacheKey = generateCacheKeyForPreferences(keys)

        try {
            val targetPackageName = packageName ?: return mapOf()

            val cachedPreferenceDataMap = getCachedPreferenceData(keys)
            val freshPreferenceDataMap =
                invoke(targetPackageName, PreferenceDataApi(), PreferenceDataRequest(keys = keys))
                    .await()
            if (cachedPreferenceDataMap != freshPreferenceDataMap) {
                prefs.edit {
                    putString(cacheKey, serializePreferenceDataMap(freshPreferenceDataMap))
                }
            }
            return freshPreferenceDataMap
        } catch (e: Exception) {
            Log.e(SupervisionLog.TAG, "Error fetching Preference data from supervision app", e)
            return mapOf()
        }
    }

    /**
     * Retrieves cached preference data for the specified keys.
     *
     * Fetches cached preference data from local file system, the data is deserialized from JSON
     * formatted string. If no cached data is found or deserialization fails, an empty map is
     * returned.
     *
     * @param keys A list of strings representing the keys for which preference data is requested.
     * @return A map where the keys are the requested keys, and the values are the corresponding
     *   [PreferenceData] objects.
     */
    override fun getCachedPreferenceData(keys: List<String>): Map<String, PreferenceData> {
        val cacheKey = generateCacheKeyForPreferences(keys)
        return prefs.getString(cacheKey, null)?.let { deserializePreferenceDataMap(it) } ?: mapOf()
    }

    /**
     * Retrieves supported apps for the specified filter keys.
     *
     * This suspend function sends a request to the supervision app for the specified content filter
     * keys and returns a map of supported apps. If an error occurs during the communication, an
     * empty map is returned and the error is logged.
     *
     * @param keys A list of strings representing the keys for content filters.
     * @return A map where the keys are the requested keys, and the values are the corresponding
     *   list of supported apps.
     */
    override suspend fun getSupportedApps(keys: List<String>): Map<String, List<SupportedApp>> =
        try {
            val targetPackageName = packageName ?: return mapOf()

            invoke(targetPackageName, SupportedAppsApi(), SupportedAppsRequest(keys = keys)).await()
        } catch (e: Exception) {
            Log.e(SupervisionLog.TAG, "Error fetching supported apps from supervision app", e)
            mapOf()
        }

    /**
     * Generates a deterministic cache key from a list of preference keys. The keys are sorted to
     * ensure that the order of keys in the input list does not affect the resulting cache key.
     */
    private fun generateCacheKeyForPreferences(keys: List<String>): String {
        // Sort keys to ensure the cache key is consistent regardless of input order.
        val sortedKeys = keys.sorted()
        // Create a unique key by joining sorted keys. Add a prefix for clarity and to avoid
        // potential collisions with other keys in SharedPreferences.
        return "${PREFERENCE_DATA_CACHE_KEY_PREFIX}_${sortedKeys.joinToString("_")}"
    }

    private fun serializePreferenceDataMap(dataMap: Map<String, PreferenceData>): String? {
        return try {
            val jsonObject = JSONObject()
            for ((key, prefData) in dataMap) {
                jsonObject.put(key, prefData.toJsonObject())
            }
            jsonObject.toString()
        } catch (e: JSONException) {
            Log.e(SupervisionLog.TAG, "Error serializing preference data map: $e")
            null
        }
    }

    private fun deserializePreferenceDataMap(jsonString: String): Map<String, PreferenceData>? {
        return try {
            val jsonObject = JSONObject(jsonString)
            buildMap {
                for (key in jsonObject.keys()) {
                    val prefJson = jsonObject.getJSONObject(key)
                    put(key, PreferenceData(prefJson))
                }
            }
        } catch (e: JSONException) {
            Log.e(SupervisionLog.TAG, "Error deserializing preference data map: $e")
            null
        }
    }

    companion object {
        const val SUPERVISION_MESSENGER_SERVICE_BIND_ACTION =
            "android.app.supervision.action.SUPERVISION_MESSENGER_SERVICE"
        const val PREFERENCE_FILE_NAME = "supervision_messenger_cache"
        const val PREFERENCE_DATA_CACHE_KEY_PREFIX = "supervision_pref_data"
    }
}
