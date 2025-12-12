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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings.ACTION_MANAGE_CROSS_PROFILE_ACCESS
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesDetails
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesSettings
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ProvidePreferenceScreen(InteractAcrossProfilesAppDetailScreen.KEY, parameterized = true)
open class InteractAcrossProfilesAppDetailScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceTitleProvider {

    private val packageName = arguments.getString("app")!!

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    private val storage: KeyValueStore =
        InteractAcrossProfilesStorage(context, appInfo, packageName)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.interact_across_profiles_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence =
        appInfo.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence =
        context.getString(
            when (storage.getBoolean(InteractAcrossProfilesMainSwitch.KEY)) {
                true -> R.string.interact_across_profiles_summary_allowed
                else -> R.string.interact_across_profiles_summary_not_allowed
            }
        )

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_MANAGE_CROSS_PROFILE_ACCESS).apply {
            data = "package:${appInfo.packageName}".toUri()
            // Only one switch so no need to highlight it with [IntentUtils.highlightPreference].
        }

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun extras(context: Context): Bundle? =
        Bundle(1).apply { putString(KEY_EXTRA_PACKAGE_NAME, arguments.getString("app")) }

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +InteractAcrossProfilesMainSwitch(storage) }

    companion object {
        const val KEY = "special_access_interact_across_profiles_app_detail"

        const val KEY_EXTRA_PACKAGE_NAME = "package_name"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val packageManager = context.packageManager
            val userManager = context.getSystemService(UserManager::class.java)
            val crossProfileApps = context.getSystemService(CrossProfileApps::class.java)

            InteractAcrossProfilesSettings.collectConfigurableApps(
                    packageManager,
                    userManager,
                    crossProfileApps,
                )
                .forEach { appUser ->
                    emit(Bundle(1).apply { putString("app", appUser.first.packageName) })
                }
        }
    }
}

private class InteractAcrossProfilesMainSwitch(private val storage: KeyValueStore) :
    BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.interact_across_profiles_title

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_interact_across_profiles_settings_switch"
    }
}

private class InteractAcrossProfilesStorage(
    private val context: Context,
    private val appInfo: ApplicationInfo,
    private val packageName: String,
) : NoOpKeyedObservable<String>(), KeyValueStore {

    override fun contains(key: String): Boolean {
        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T {
        return InteractAcrossProfilesDetails.isInteractAcrossProfilesEnabled(context, packageName)
            as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}
}
