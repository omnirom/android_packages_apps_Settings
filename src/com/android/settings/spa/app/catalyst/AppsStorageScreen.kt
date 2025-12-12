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

package com.android.settings.spa.app.catalyst

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.settings.R
import com.android.settings.Settings.StorageUseActivity
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.spa.app.storage.StorageType
import com.android.settingslib.metadata.PreferenceHierarchyGenerator
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@ProvidePreferenceScreen(AppStorageAppListScreen.KEY)
open class AppStorageAppListScreen : PreferenceScreenMixin, PreferenceHierarchyGenerator<Boolean> {

    override val key: String
        get() = KEY

    override val title: Int
        get() = StorageType.Apps.titleResource

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = Flags.catalystAppList()

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        when (metadata) {
            null -> Intent(context, StorageUseActivity::class.java)
            else -> null // not yet support highlight on specific app
        }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        generatePreferenceHierarchy(context, coroutineScope, false)

    override fun generatePreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
        type: Boolean, // whether to include system apps
    ) = preferenceHierarchy(context) {
        addAsync(coroutineScope, Dispatchers.Default) {
            AppListRepositoryImpl(context).loadAndMaybeExcludeSystemApps(context.userId, type)
                .forEach { app ->
                    if (StorageType.Apps.filter(app)) {
                        val arguments = Bundle(1).apply { putString("app", app.packageName) }
                        +(AppInfoStorageScreen.KEY args arguments)
                    }
                }
        }
    }

    companion object {
        const val KEY = "device_state_apps_storage"
    }
}
