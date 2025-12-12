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

package com.android.settings.applications.specialaccess

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.android.settings.applications.AppListScreen
import com.android.settings.applications.CatalystAppListFragment
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEmpty

/** Interface for Catalyst screens that display a list of apps with specific permission. */
abstract class SpecialAccessAppListScreen : AppListScreen() {

    abstract val appDetailScreenKey: String

    abstract fun appDetailParameters(context: Context, hierarchyType: Boolean): Flow<Bundle>

    override fun fragmentClass(): Class<out Fragment>? = CatalystAppListFragment::class.java

    override fun hasCompleteHierarchy() = true

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        generatePreferenceHierarchy(context, coroutineScope, DEFAULT_SHOW_SYSTEM)

    override fun generatePreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
        hierarchyType: Boolean,
    ) =
        preferenceHierarchy(context) {
            addAsync(coroutineScope, Dispatchers.Default) {
                val screenKey = appDetailScreenKey
                appDetailParameters(context, hierarchyType)
                    .onEmpty { +NoAppPreference() }
                    .collect { +(screenKey args it) }
            }
        }
}
