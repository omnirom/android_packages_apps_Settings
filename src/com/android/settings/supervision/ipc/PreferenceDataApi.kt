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

import android.os.Bundle
import com.android.settingslib.ipc.ApiDescriptor
import com.android.settingslib.ipc.MessageCodec

/**
 * Message API between UI(Settings App) and data provider(i.e. System Supervision role holder)
 *
 * Request: a list of preference keys to get new preference data of. Response: a map of preference
 * key to preference data.
 *
 * All fields in [PreferenceData] are nullable, null fields will be ignored by UI.
 */
class PreferenceDataApi : ApiDescriptor<PreferenceDataRequest, Map<String, PreferenceData>> {
    override val id: Int
        get() = 1

    override val requestCodec: MessageCodec<PreferenceDataRequest> =
        object : MessageCodec<PreferenceDataRequest> {
            override fun encode(data: PreferenceDataRequest) = data.toBundle()

            override fun decode(data: Bundle) = PreferenceDataRequest(data)
        }

    override val responseCodec: MessageCodec<Map<String, PreferenceData>> =
        object : MessageCodec<Map<String, PreferenceData>> {
            override fun encode(data: Map<String, PreferenceData>) =
                Bundle().apply {
                    for ((key, preferenceData) in data) {
                        putBundle(key, preferenceData.toBundle())
                    }
                }

            override fun decode(data: Bundle): Map<String, PreferenceData> {
                val resultMap = mutableMapOf<String, PreferenceData>()
                for (key in data.keySet()) {
                    data.getBundle(key)?.let { resultMap[key] = PreferenceData(it) }
                }
                return resultMap
            }
        }
}
