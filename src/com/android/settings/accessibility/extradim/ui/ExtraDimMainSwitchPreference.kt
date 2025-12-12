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

package com.android.settings.accessibility.extradim.ui

import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.extradim.data.ExtraDimDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.widget.MainSwitchPreferenceBinding

class ExtraDimMainSwitchPreference(
    context: Context,
    private val storage: ExtraDimDataStore = ExtraDimDataStore(context),
) :
    SwitchPreference(key = KEY, title = R.string.reduce_bright_colors_switch_title),
    MainSwitchPreferenceBinding {

    override fun storage(context: Context): KeyValueStore = storage

    override fun getReadPermissions(context: Context) = ExtraDimDataStore.getReadPermissions()

    override fun getReadPermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = ExtraDimDataStore.getWritePermissions()

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    companion object {
        const val KEY = "reduce_bright_colors_switch"
    }
}
