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
import android.os.UserHandle
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.applications.AppStorageSettings
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.spa.app.storage.StorageType
import com.android.settings.utils.highlightPreference
import com.android.settingslib.applications.StorageStatsSource
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.spaprivileged.model.app.AppStorageRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ProvidePreferenceScreen(AppInfoStorageScreen.KEY, parameterized = true)
open class AppInfoStorageScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceTitleProvider {

    private val appInfo = context.packageManager.getApplicationInfo(arguments.getString("app")!!, 0)

    private val repo = AppStorageRepositoryImpl(context)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.storage_label

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.APPLICATIONS_APP_STORAGE

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? =
        appInfo.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence? = repo.formatSize(appInfo)

    override fun isFlagEnabled(context: Context) = Flags.catalystAppList()

    override fun extras(context: Context): Bundle? =
        Bundle(1).apply { putString(KEY_EXTRA_PACKAGE_NAME, arguments.getString("app")) }

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AppStorageSettings::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("com.android.settings.APP_STORAGE_SETTINGS").apply {
            data = "package:${appInfo.packageName}".toUri()
            highlightPreference(arguments, metadata?.key)
        }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val stats = context.getStatsForPackage() ?: return@preferenceHierarchy
            +AppSizePreference(stats, repo)
            +AppUserDataSizePreference(stats, repo)
            +AppCacheSizePreference(stats, repo)
            +AppTotalSizePreference(stats, repo)
        }

    private fun Context.getStatsForPackage() =
        try {
            StorageStatsSource(this)
                .getStatsForPackage(appInfo.volumeUuid, appInfo.packageName, UserHandle.of(userId))
        } catch (_: Exception) {
            // error during lookup, return no result
            null
        }

    companion object {
        const val KEY = "device_state_app_info_storage"

        const val KEY_EXTRA_PACKAGE_NAME = "package_name"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            AppListRepositoryImpl(context).loadApps(context.userId).forEach { app ->
                if (StorageType.Apps.filter(app)) {
                    emit(Bundle(1).apply { putString("app", app.packageName) })
                }
            }
        }
    }
}

private class AppSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "app_size"

    override val title: Int
        get() = R.string.application_size_label

    override fun getSummary(context: Context): CharSequence? = repo.formatSizeBytes(stats.codeBytes)
}

private class AppUserDataSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "data_size"

    override val title: Int
        get() = R.string.data_size_label

    override fun getSummary(context: Context): CharSequence? =
        repo.formatSizeBytes(stats.dataBytes - stats.cacheBytes)
}

private class AppCacheSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "cache_size"

    override val title: Int
        get() = R.string.cache_size_label

    override fun getSummary(context: Context): CharSequence? =
        repo.formatSizeBytes(stats.cacheBytes)
}

private class AppTotalSizePreference(
    private val stats: StorageStatsSource.AppStorageStats,
    private val repo: AppStorageRepositoryImpl,
) : PreferenceMetadata, PreferenceSummaryProvider {

    override val key: String
        get() = "total_size"

    override val title: Int
        get() = R.string.total_size_label

    override fun getSummary(context: Context): CharSequence? =
        repo.formatSizeBytes(stats.totalBytes)
}
