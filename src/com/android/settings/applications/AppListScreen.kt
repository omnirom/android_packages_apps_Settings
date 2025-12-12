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

package com.android.settings.applications

import android.content.Context
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceHierarchyGenerator
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.utils.applications.PackageObservable
import com.android.settingslib.widget.ZeroStatePreference

/** Interface for all Catalyst screens showing an app list. */
abstract class AppListScreen :
    PreferenceScreenMixin, PreferenceHierarchyGenerator<Boolean>, PreferenceLifecycleProvider {

    private lateinit var observer: KeyedObserver<String?>

    override val highlightMenuKey
        get() = R.string.menu_key_apps

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            observer = KeyedObserver { _, _ -> context.regeneratePreferenceHierarchy() }
            PackageObservable.get(context).addObserver(observer, HandlerExecutor.main)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            PackageObservable.get(context).removeObserver(observer)
        }
    }

    class NoAppPreference : PreferenceMetadata, PreferenceBinding {
        override val key
            get() = "no_app"

        override val title
            get() = R.string.no_applications

        override val icon
            get() = R.drawable.ic_apps_alt

        override fun createWidget(context: Context) = ZeroStatePreference(context)
    }
}
