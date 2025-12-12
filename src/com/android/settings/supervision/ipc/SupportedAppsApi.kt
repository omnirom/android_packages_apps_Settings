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
 * Message API between UI (Settings App) and data provider (i.e. System Supervision role holder).
 *
 * Request: a list of filter keys to get supported apps for. Response: a map of filter key to a list
 * of supported apps.
 */
class SupportedAppsApi : ApiDescriptor<SupportedAppsRequest, Map<String, List<SupportedApp>>> {
    override val id: Int
        get() = 2

    override val requestCodec: MessageCodec<SupportedAppsRequest> =
        object : MessageCodec<SupportedAppsRequest> {
            override fun encode(data: SupportedAppsRequest) = data.toBundle()

            override fun decode(data: Bundle) = SupportedAppsRequest(data)
        }

    override val responseCodec: MessageCodec<Map<String, List<SupportedApp>>> =
        object : MessageCodec<Map<String, List<SupportedApp>>> {
            override fun encode(data: Map<String, List<SupportedApp>>) =
                Bundle().apply {
                    for ((key, supportedApps) in data) {
                        putParcelableArrayList(key, ArrayList(supportedApps.map { it.toBundle() }))
                    }
                }

            override fun decode(data: Bundle): Map<String, List<SupportedApp>> {
                return buildMap<String, List<SupportedApp>> {
                    for (key in data.keySet()) {
                        data.getParcelableArrayList<Bundle>(key)?.let { supportedApps ->
                            put(key, supportedApps.map { SupportedApp(it) })
                        }
                    }
                }
            }
        }
}
