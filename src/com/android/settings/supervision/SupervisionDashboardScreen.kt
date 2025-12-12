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
package com.android.settings.supervision

import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope

/**
 * Supervision settings landing page (Settings > Supervision).
 *
 * This screen typically includes three parts:
 * 1. Primary switch to toggle supervision on and off.
 * 2. List of supervision features. Individual features like website filters or bedtime schedules
 *    will be listed in a group and link out to their own respective settings pages. Features
 *    implemented by supervision client apps can also be dynamically injected into this group.
 * 3. Entry point to supervision PIN management settings page.
 */
@ProvidePreferenceScreen(SupervisionDashboardScreen.KEY)
open class SupervisionDashboardScreen : PreferenceScreenMixin, PreferenceLifecycleProvider {
    private var supervisionClient: SupervisionMessengerClient? = null
    private var supervisionManager: SupervisionManager? = null
    private var lifeCycleContext: PreferenceLifecycleContext? = null

    private val supervisionListener =
        object : SupervisionManager.SupervisionListener() {
            override fun onSupervisionEnabled(userId: Int) {
                refreshPreferences()
            }

            override fun onSupervisionDisabled(userId: Int) {
                refreshPreferences()
            }

            private fun refreshPreferences() {
                lifeCycleContext?.notifyPreferenceChange(KEY)
                lifeCycleContext?.notifyPreferenceChange(SupervisionMainSwitchPreference.KEY)
                lifeCycleContext?.notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
            }
        }

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            this.lifeCycleContext = context
            supervisionManager = context.getSystemService(SupervisionManager::class.java)
            supervisionManager?.registerSupervisionListener(supervisionListener)
        }
    }

    override fun isFlagEnabled(context: Context) = Flags.enableSupervisionSettingsScreen()

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_settings_title

    override val summary: Int
        get() = R.string.supervision_settings_summary

    override val icon: Int
        get() = R.drawable.ic_account_child_invert

    override val indexable
        get() = true

    override val keywords: Int
        get() = R.string.keywords_supervision_settings

    override fun fragmentClass(): Class<out Fragment>? = SupervisionDashboardFragment::class.java

    override fun getMetricsCategory() = SettingsEnums.SUPERVISION_DASHBOARD

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            supervisionClient?.close()
            supervisionManager?.unregisterSupervisionListener(supervisionListener)
            this.lifeCycleContext = null
            this.supervisionManager = null
        }
    }

    override fun hasCompleteHierarchy() = true

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val supervisionClient = getSupervisionClient(context)
            +SupervisionMainSwitchPreference(context, supervisionClient) order -200
            +UntitledPreferenceCategoryMetadata(SUPERVISION_DYNAMIC_GROUP_1) order -100 += {
                +SupervisionAppStoreFiltersScreen.KEY order 50
                +SupervisionWebContentFiltersScreen.KEY order 100
            }
            +UntitledPreferenceCategoryMetadata("pin_management_group") order 100 += {
                +SupervisionPinManagementScreen.KEY order 10
            }
            +UntitledPreferenceCategoryMetadata("footer_group") order 300 += {
                +SupervisionPromoFooterPreference(supervisionClient) order 30
                +SupervisionAocFooterPreference(supervisionClient) order 40
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SupervisionDashboardActivity::class.java, metadata?.key)

    private fun getSupervisionClient(context: Context) =
        supervisionClient ?: SupervisionMessengerClient(context).also { supervisionClient = it }

    companion object {
        const val KEY = "top_level_supervision"
        internal const val SUPERVISION_DYNAMIC_GROUP_1 = "supervision_features_group_1"
    }
}
