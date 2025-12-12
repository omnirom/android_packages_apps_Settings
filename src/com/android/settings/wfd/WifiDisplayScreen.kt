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

package com.android.settings.wfd

import android.app.settings.SettingsEnums
import android.content.Context
import android.media.MediaRouter
import android.text.TextUtils
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.WifiDisplaySettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** The app detail catalyst screen for connections to remote displays */
// LINT.IfChange
@ProvidePreferenceScreen(WifiDisplayScreen.KEY)
open class WifiDisplayScreen :
    PreferenceScreenMixin,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {

    private var router: MediaRouter? = null
    private lateinit var lifeCycleContext: PreferenceLifecycleContext

    private val routerCallback =
        object : MediaRouter.SimpleCallback() {
            override fun onRouteSelected(
                router: MediaRouter,
                type: Int,
                info: MediaRouter.RouteInfo,
            ) {
                lifeCycleContext.notifyPreferenceChange(KEY)
            }

            override fun onRouteUnselected(
                router: MediaRouter,
                type: Int,
                info: MediaRouter.RouteInfo,
            ) {
                lifeCycleContext.notifyPreferenceChange(KEY)
            }

            override fun onRouteAdded(router: MediaRouter, info: MediaRouter.RouteInfo) {
                lifeCycleContext.notifyPreferenceChange(KEY)
            }

            override fun onRouteRemoved(router: MediaRouter, info: MediaRouter.RouteInfo) {
                lifeCycleContext.notifyPreferenceChange(KEY)
            }

            override fun onRouteChanged(router: MediaRouter, info: MediaRouter.RouteInfo) {
                lifeCycleContext.notifyPreferenceChange(KEY)
            }
        }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.wifi_display_settings_title

    override val icon: Int
        get() = R.drawable.ic_cast_24dp

    override val highlightMenuKey: Int
        get() = R.string.menu_key_connected_devices

    override val keywords: Int
        get() = R.string.keywords_display_cast_screen

    override fun fragmentClass(): Class<out Fragment>? = WifiDisplaySettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun isFlagEnabled(context: Context) = Flags.deeplinkConnectedDevices25q4()

    override fun getMetricsCategory() = SettingsEnums.WFD_WIFI_DISPLAY

    override fun getSummary(context: Context): CharSequence? {
        var summary: CharSequence? = context.getString(R.string.disconnected)

        val router = router ?: return summary

        val routeCount: Int = router.routeCount
        for (i in 0..<routeCount) {
            val route: MediaRouter.RouteInfo = router.getRouteAt(i)
            if (
                route.matchesTypes(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY) &&
                    route.isSelected &&
                    !route.isConnecting
            ) {
                val status = route.status
                summary =
                    if (!TextUtils.isEmpty(status)) {
                        status
                    } else {
                        context.getString(R.string.wifi_display_status_connected)
                    }
                break
            }
        }
        return summary
    }

    override fun isAvailable(context: Context) = WifiDisplaySettings.isAvailable(context)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, WifiDisplaySettingsActivity::class.java, metadata?.key)

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        router = context.getSystemService(MediaRouter::class.java)
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        router?.addCallback(MediaRouter.ROUTE_TYPE_REMOTE_DISPLAY, routerCallback)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        router?.removeCallback(routerCallback)
    }

    companion object {
        const val KEY = "wifi_display_settings"
    }
}
// LINT.ThenChange(WifiDisplayPreferenceController.java, WifiDisplaySettings.java)
