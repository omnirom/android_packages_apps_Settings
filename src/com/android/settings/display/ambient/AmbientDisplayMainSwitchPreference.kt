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
package com.android.settings.display.ambient

import android.content.Context
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import com.android.systemui.shared.Flags.ambientAod

class AmbientDisplayMainSwitchPreference : BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = if (ambientAod()) R.string.doze_always_on_title2 else R.string.doze_always_on_title

    override fun storage(context: Context): KeyValueStore = AmbientDisplayStorage(context)

    companion object {
        val KEY = "ambient_display_always_on_key"
    }
}
