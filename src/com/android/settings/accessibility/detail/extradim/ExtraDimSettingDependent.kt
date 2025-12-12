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

package com.android.settings.accessibility.detail.extradim

import android.content.Context
import android.database.ContentObserver
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

const val EXTRA_DIM_SETTING = Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED
interface ExtraDimSettingDependent: DefaultLifecycleObserver {
    val context: Context
    val contentObserver: ContentObserver

    override fun onCreate(owner: LifecycleOwner) {
        context.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(EXTRA_DIM_SETTING),
            /* notifyForDescendants= */ false,
            contentObserver,
        )
    }

    override fun onDestroy(owner: LifecycleOwner) {
        context.contentResolver.unregisterContentObserver(contentObserver)
    }
}
