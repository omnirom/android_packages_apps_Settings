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

package com.android.settings.network.telephony

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.LifecycleOwner
import com.android.settingslib.spa.framework.util.collectLatestWithLifecycle
import com.android.settingslib.spaprivileged.settingsprovider.settingsGlobalBooleanFlow
import kotlinx.coroutines.flow.Flow

/** To notify the preferences when airplane mode changed. */
interface AirplaneModeChangedCallback {
    fun notifyAirplaneModeChanged(isAirplaneModeOn: Boolean) {}
}

class AirplaneModeRepository(private val context: Context) {
    fun airplaneModeChangedFlow(): Flow<Boolean> {
        return context.settingsGlobalBooleanFlow(Settings.Global.AIRPLANE_MODE_ON)
    }

    /** TODO: Move this to UI layer, when UI layer migrated to Kotlin. */
    fun collectAirplaneModeChanged(lifecycleOwner: LifecycleOwner, action: (Boolean) -> Unit) {
        airplaneModeChangedFlow().collectLatestWithLifecycle(lifecycleOwner, action = action)
    }
}
