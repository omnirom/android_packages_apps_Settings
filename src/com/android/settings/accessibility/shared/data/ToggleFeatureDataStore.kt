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

package com.android.settings.accessibility.shared.data

import android.content.ComponentName
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate

/**
 * A [KeyValueStoreDelegate] that logs the enabled state of an accessibility service.
 *
 * @param componentName The [ComponentName] of the accessibility service.
 * @param keyValueStoreDelegate The [KeyValueStore] to use for storing and retrieving the enabled
 *   state.
 */
class ToggleFeatureDataStore(
    private val componentName: ComponentName,
    override val keyValueStoreDelegate: KeyValueStore,
) : KeyValueStoreDelegate {

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        super.setValue(key, valueType, value)
        AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(
            componentName,
            (value as? Boolean) ?: false,
        )
    }
}
