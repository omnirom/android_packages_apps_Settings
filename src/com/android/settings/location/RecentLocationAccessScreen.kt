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
package com.android.settings.location

import android.Manifest
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.icu.text.RelativeDateTimeFormatter
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.dashboard.profileselector.ProfileSelectFragment
import com.android.settings.flags.Flags
import com.android.settingslib.applications.RecentAppOpsAccess
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.utils.StringUtil
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(RecentLocationAccessScreen.KEY)
open class RecentLocationAccessScreen: PreferenceScreenMixin, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.location_category_recent_location_access

    override val highlightMenuKey: Int
        get() = R.string.menu_key_location

    override fun getMetricsCategory() = SettingsEnums.LOCATION_RECENT_ACCESS_ALL

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = Flags.catalystLocationSettings()

    override fun isAvailable(context: Context) = LocationEnabler(context, null, null).isEnabled(
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.LOCATION_MODE,
            Settings.Secure.LOCATION_MODE_OFF
        )
    )

    override fun fragmentClass(): Class<out Fragment>? =
        RecentLocationAccessSeeAllFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            // not showing system app access for now
            val userManager = UserManager.get(context)
            val accessList = RecentAppOpsAccess.createForLocation(context)
                .getAppListSorted(false)
                .filter { access ->
                    RecentLocationAccessPreferenceController.isRequestMatchesProfileType(
                        userManager,
                        access,
                        ProfileSelectFragment.ProfileType.ALL
                    )
                }
            for (i in 0 until accessList.size) {
                +LocationAccessAppPreference(accessList[i], i)
            }
        }

    companion object {
        const val KEY = "device_state_all_recent_location_access"
    }
}

private class LocationAccessAppPreference(
    private val access: RecentAppOpsAccess.Access,
    private val index: Int
): PreferenceMetadata, PreferenceTitleProvider, PreferenceSummaryProvider {

    override val key: String
        get() = "recent_app_location_access_$index"

    override fun getTitle(context: Context): CharSequence? = access.label

    override fun getSummary(context: Context): CharSequence? = StringUtil.formatRelativeTime(
        context,
        (System.currentTimeMillis() - access.accessFinishTime).toDouble(),
        false,
        RelativeDateTimeFormatter.Style.LONG
    )

    override fun intent(context: Context) =
        Intent(Intent.ACTION_MANAGE_APP_PERMISSION).apply {
            `package` = context.packageManager.permissionControllerPackageName
            putExtra(Intent.EXTRA_PERMISSION_GROUP_NAME, Manifest.permission_group.LOCATION)
            putExtra(Intent.EXTRA_PACKAGE_NAME, access.packageName)
            putExtra(Intent.EXTRA_USER, context.user)
        }

    override fun extras(context: Context): Bundle? {
        return Bundle(1).apply {
            putString(Intent.EXTRA_PACKAGE_NAME, access.packageName)
        }
    }
}
