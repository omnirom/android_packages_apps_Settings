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

package com.android.settings.datausage

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.android.settingslib.utils.applications.PackageObservable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Preference screen for Apps -> Individual App Info -> Mobile data usage. */
@ProvidePreferenceScreen(DataUsageAppDetailScreen.KEY, parameterized = true)
open class DataUsageAppDetailScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin,
    PreferenceTitleProvider,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {

    private lateinit var keyedObserver: KeyedObserver<String>

    private val packageName = arguments.getString(KEY_APP_PACKAGE_NAME)!!

    private var appInfo = context.getAppInfo(packageName)

    override val key: String
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle: Int
        get() = R.string.data_usage_app_summary_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.APP_DATA_USAGE

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? =
        appInfo?.loadLabel(context.packageManager)

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun isAvailable(context: Context) = appInfo != null

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AppDataUsage::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, AppDataUsageActivity::class.java, arguments, metadata?.bindingKey)
            .apply { data = "package:$packageName".toUri() }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun onCreate(context: PreferenceLifecycleContext) {
        // observer to detect package changes (disabled/enabled/uninstall)
        val observer =
            KeyedObserver<String> { _, _ ->
                appInfo = context.getAppInfo(packageName)
                context.notifyPreferenceChange(bindingKey)
            }
        keyedObserver = observer
        val executor = HandlerExecutor.main
        if (isContainer(context)) {
            PackageObservable.get(context).addObserver(packageName, observer, executor)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            PackageObservable.get(context).removeObserver(packageName, keyedObserver)
        }
    }

    private fun Context.getAppInfo(packageName: String) =
        try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "App not found: $packageName", e)
            null
        }

    companion object {
        private const val TAG = "DataUsageAppDetailScreen"

        const val KEY = "app_data_usage_screen"
        const val KEY_APP_PACKAGE_NAME = "app"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val repo = AppListRepositoryImpl(context)
            repo.loadApps(context.userId).forEach { app ->
                emit(Bundle(1).apply { putString(KEY_APP_PACKAGE_NAME, app.packageName) })
            }
        }
    }
}
