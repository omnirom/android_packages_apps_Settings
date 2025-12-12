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

package com.android.settings.deviceinfo.storage

import android.app.settings.SettingsEnums
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.UserManager
import android.os.storage.StorageManager
import android.util.DataUnit
import android.util.SparseArray
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.deviceinfo.StorageDashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.applications.StorageStatsSource
import com.android.settingslib.deviceinfo.PrivateStorageInfo
import com.android.settingslib.deviceinfo.StorageManagerVolumeProvider
import com.android.settingslib.metadata.PreferenceHierarchyGenerator
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(StoragePreferenceScreen.KEY)
open class StoragePreferenceScreen(private val context: Context) :
    PreferenceScreenMixin, PreferenceHierarchyGenerator<Int> {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.storage_settings

    override val highlightMenuKey: Int
        get() = R.string.menu_key_storage

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_STORAGE_CATEGORY

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.INTERNAL_STORAGE_SETTINGS").apply {
            setPackage(context.packageName)
            highlightPreference(metadata?.key)
        }

    override fun isFlagEnabled(context: Context) = Flags.catalystSystemStorage()

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        generatePreferenceHierarchy(context, coroutineScope, context.userId)

    override fun generatePreferenceHierarchy(
        context: Context,
        coroutineScope: CoroutineScope,
        type: Int, // userId
    ) =
        preferenceHierarchy(context) {
            val cache = getStorageCache(context, type)

            // Storage Used
            val usedSummary =
                StorageUtils.getStorageSummary(
                    context,
                    R.string.storage_usage_summary,
                    cache.totalUsedSize,
                )
            +StoragePreference(KEY_SUMMARY_USED, 0, { null }, { usedSummary }, { usedSummary })

            // Storage Total
            val totalSummary =
                StorageUtils.getStorageSummary(
                    context,
                    R.string.storage_total_summary,
                    cache.totalSize,
                )
            +StoragePreference(KEY_SUMMARY_TOTAL, 0, { null }, { totalSummary }, { totalSummary })

            // Free up space
            +StoragePreference(
                KEY_FREE_UP_SPACE,
                R.string.storage_free_up_space_title,
                { c ->
                    Intent(StorageManager.ACTION_MANAGE_STORAGE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                },
                { c -> c.getString(R.string.storage_free_up_space_summary) },
            )

            // Apps
            +StoragePreference(
                KEY_PREF_APPS,
                R.string.storage_apps,
                { c ->
                    Intent("android.intent.action.MANAGE_PACKAGE_STORAGE").setPackage(c.packageName)
                },
                { StorageUtils.getStorageSizeLabel(context, cache.allAppsExceptGamesSize) },
            )

            // Trash
            +StoragePreference(
                KEY_PREF_TRASH,
                R.string.storage_trash,
                { c ->
                    val intent = Intent("android.settings.VIEW_TRASH")
                    if (c.packageManager.resolveActivityAsUser(intent, 0, type) == null) {
                        // Settings' trash handling doesn't have a dedicated intent.
                        // Must be handled from the Storage page itself
                        getLaunchIntent(c, null)
                    } else {
                        intent
                    }
                },
                { StorageUtils.getStorageSizeLabel(context, cache.trashSize) },
            )

            // Images
            +StoragePreference(
                KEY_PREF_IMAGES,
                R.string.storage_images,
                { c ->
                    Intent(Intent.ACTION_VIEW).apply {
                        data = c.getString(R.string.config_images_storage_category_uri).toUri()
                    }
                },
                { StorageUtils.getStorageSizeLabel(context, cache.imagesSize) },
            )

            // Games
            +StoragePreference(
                KEY_PREF_GAMES,
                R.string.storage_games,
                {
                    // TODO no intent for games storage exposed
                    null
                },
                { StorageUtils.getStorageSizeLabel(context, cache.gamesSize) },
            )

            // Documents
            +StoragePreference(
                KEY_PREF_DOCUMENTS,
                R.string.storage_documents,
                { c ->
                    Intent(Intent.ACTION_VIEW).apply {
                        data = c.getString(R.string.config_documents_storage_category_uri).toUri()
                    }
                },
                { StorageUtils.getStorageSizeLabel(context, cache.documentsSize) },
            )

            // Other
            +StoragePreference(
                KEY_PREF_OTHER,
                R.string.storage_other,
                { c ->
                    Intent(Intent.ACTION_VIEW).apply {
                        data = c.getString(R.string.config_other_storage_category_uri).toUri()
                    }
                },
                { StorageUtils.getStorageSizeLabel(context, cache.otherSize) },
            )

            // Audio
            +StoragePreference(
                KEY_PREF_AUDIO,
                R.string.storage_audio,
                { c ->
                    Intent(Intent.ACTION_VIEW).apply {
                        data = c.getString(R.string.config_audio_storage_category_uri).toUri()
                    }
                },
                { StorageUtils.getStorageSizeLabel(context, cache.audioSize) },
            )

            // Video
            +StoragePreference(
                KEY_PREF_VIDEOS,
                R.string.storage_videos,
                { c ->
                    Intent(Intent.ACTION_VIEW).apply {
                        data = c.getString(R.string.config_videos_storage_category_uri).toUri()
                    }
                },
                { StorageUtils.getStorageSizeLabel(context, cache.videosSize) },
            )

            // System - OS
            +StoragePreference(
                KEY_PREF_SYSTEM,
                0,
                { null },
                { StorageUtils.getStorageSizeLabel(context, cache.systemSize) },
                { c ->
                    c.getString(R.string.storage_os_name, Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY)
                },
            )

            // System - Temp
            +StoragePreference(
                KEY_PREF_TEMP,
                R.string.storage_temporary_files,
                { null },
                { StorageUtils.getStorageSizeLabel(context, cache.temporaryFilesSize) },
            )
        }

    override fun fragmentClass(): Class<out Fragment>? = StorageDashboardFragment::class.java

    private fun getStorageCache(context: Context, userId: Int): StorageCacheHelper.StorageCache {
        val cacheHelper = StorageCacheHelper(context, userId)
        // use cache if available; otherwise retrieve new data
        return if (cacheHelper.hasCachedSizeInfo()) {
            cacheHelper.retrieveCachedSize()
        } else {
            val storageEntry = StorageEntry.getDefaultInternalStorageEntry(context)
            val volumeSizesLoader =
                VolumeSizesLoader(
                    context,
                    StorageManagerVolumeProvider(
                        context.getSystemService(StorageManager::class.java)
                    ),
                    context.getSystemService(StorageStatsManager::class.java),
                    storageEntry.volumeInfo,
                )
            val privateStorageInfo = volumeSizesLoader.loadInBackground()
            val loader =
                StorageAsyncLoader(
                    context,
                    context.getSystemService(UserManager::class.java),
                    storageEntry.getFsUuid(),
                    StorageStatsSource(context),
                    context.packageManager,
                )
            val storageResults = loader.loadInBackground()
            populateStorageCache(userId, cacheHelper, storageResults, privateStorageInfo)
        }
    }

    private fun populateStorageCache(
        userId: Int,
        cacheHelper: StorageCacheHelper,
        resultArray: SparseArray<StorageAsyncLoader.StorageResult>?,
        privateStorageInfo: PrivateStorageInfo?,
    ): StorageCacheHelper.StorageCache {
        val cache = StorageCacheHelper.StorageCache()
        if (privateStorageInfo != null) {
            with(cache) {
                totalSize = privateStorageInfo.totalBytes
                totalUsedSize = privateStorageInfo.totalBytes - privateStorageInfo.freeBytes
            }
            cacheHelper.cacheTotalSizeAndTotalUsedSize(cache.totalSize, cache.totalUsedSize)
        }
        // logic taken from StorageItemPreferenceController#getSizeInfo
        if (resultArray != null) {
            val result = resultArray[userId]
            if (result != null) {
                with(cache) {
                    imagesSize = result.imagesSize
                    videosSize = result.videosSize
                    audioSize = result.audioSize
                    allAppsExceptGamesSize = result.allAppsExceptGamesSize
                    gamesSize = result.gamesSize
                    documentsSize = result.documentsSize
                    otherSize = result.otherSize
                    trashSize = result.trashSize
                    systemSize = result.systemSize

                    var otherData = 0L
                    for (i in 0 until resultArray.size()) {
                        val attr = resultArray.valueAt(i)
                        otherData += attr.gamesSize
                        +attr.audioSize
                        +attr.videosSize
                        +attr.imagesSize
                        +attr.documentsSize
                        +attr.otherSize
                        +attr.trashSize
                        +attr.allAppsExceptGamesSize
                        otherData -= attr.duplicateCodeSize
                    }
                    otherData += result.systemSize
                    temporaryFilesSize =
                        kotlin.math.max(DataUnit.GIBIBYTES.toBytes(1), totalUsedSize - otherData)
                }
                cacheHelper.cacheSizeInfo(cache)
            }
        }
        return cache
    }

    companion object {
        const val KEY = "storage_dashboard_fragment"

        const val KEY_SUMMARY_USED = "storage_summary_used"
        const val KEY_SUMMARY_TOTAL = "storage_summary_total"
        const val KEY_FREE_UP_SPACE = "free_up_space"
        const val KEY_PREF_APPS = "pref_apps"
        const val KEY_PREF_GAMES = "pref_games"
        const val KEY_PREF_DOCUMENTS = "pref_documents"
        const val KEY_PREF_VIDEOS = "pref_videos"
        const val KEY_PREF_AUDIO = "pref_audio"
        const val KEY_PREF_IMAGES = "pref_images"
        const val KEY_PREF_TRASH = "pref_trash"
        const val KEY_PREF_OTHER = "pref_other"
        const val KEY_PREF_SYSTEM = "pref_system"
        const val KEY_PREF_TEMP = "temporary_files"
    }
}
