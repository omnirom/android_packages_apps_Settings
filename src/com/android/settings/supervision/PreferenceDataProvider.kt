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
package com.android.settings.supervision

import com.android.settings.supervision.ipc.PreferenceData

/**
 * Interface for providing preference data.
 *
 * This interface defines a method for retrieving preference data based on a list of keys.
 * Implementations of this interface are responsible for fetching and returning a map of
 * [PreferenceData] objects, where the keys in the map correspond to the requested keys.
 */
interface PreferenceDataProvider {

    /** Package of preference data provider. */
    val packageName: String?

    /**
     * Retrieves preference data for the specified keys.
     *
     * This suspend function fetches preference data asynchronously.
     *
     * @param keys A list of strings representing the keys for which preference data is requested.
     * @return A map where the keys are the requested keys, and the values are the corresponding
     *   [PreferenceData] objects.
     */
    suspend fun getPreferenceData(keys: List<String>): Map<String, PreferenceData>

    /**
     * Retrieves cached preference data for the specified keys.
     *
     * This function fetches cached preference data from local file system.
     *
     * @param keys A list of strings representing the keys for which preference data is requested.
     * @return A map where the keys are the requested keys, and the values are the corresponding
     *   [PreferenceData] objects.
     */
    fun getCachedPreferenceData(keys: List<String>): Map<String, PreferenceData>
}
