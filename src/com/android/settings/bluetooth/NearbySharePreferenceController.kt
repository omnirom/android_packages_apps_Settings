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

package com.android.settings.bluetooth

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.widget.LayoutPreference

/** Preference controller for Nearby Share. */
class NearbySharePreferenceController(private val context: Context, key: String) :
    BasePreferenceController(context, key) {
    private lateinit var intent: Intent
    private var nearbyComponentName: ComponentName? = null
    private var nearbyLabel: CharSequence? = null

    fun init(sendIntent: Intent) {
        this.intent = sendIntent
        val componentString =
            Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.NEARBY_SHARING_COMPONENT,
            )
        if (TextUtils.isEmpty(componentString)) {
            return
        }
        nearbyComponentName = ComponentName.unflattenFromString(componentString)?.also {
            intent.setComponent(it)
            intent.putExtra(EXTRA_REDIRECT_FROM_SETTINGS, true)
            nearbyLabel = getNearbyLabel(it)
        }
    }

    override fun getAvailabilityStatus(): Int {
        if (nearbyLabel == null) {
            return CONDITIONALLY_UNAVAILABLE
        }
        return AVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val preference: LayoutPreference = screen.findPreference(preferenceKey) ?: return

        preference.findViewById<TextView>(R.id.nearby_sharing_suggestion_title).text =
            context.getString(R.string.bluetooth_try_nearby_share_title, nearbyLabel)
        preference.findViewById<TextView>(R.id.nearby_sharing_suggestion_summary).text =
            context.getString(R.string.bluetooth_try_nearby_share_summary, nearbyLabel)
        FeatureFactory.featureFactory.metricsFeatureProvider.action(
            SettingsEnums.PAGE_UNKNOWN,
            SettingsEnums.ACTION_NEARBY_SHARE_ENTRYPOINT_SHOWN,
            SettingsEnums.BLUETOOTH_DEVICE_PICKER,
            "",
            0
        )
        preference.findViewById<View>(R.id.card_container).setOnClickListener {
            FeatureFactory.featureFactory.metricsFeatureProvider.clicked(
                SettingsEnums.BLUETOOTH_DEVICE_PICKER,
                preferenceKey
            )
            context.startActivity(intent)
            true
        }
    }

    private fun getNearbyLabel(componentName: ComponentName): CharSequence? =
        try {
            context.packageManager
                .getActivityInfo(componentName, PackageManager.GET_META_DATA)
                .loadLabel(context.packageManager)
        } catch(_: NameNotFoundException) {
            null
        }

    companion object {
        private const val EXTRA_REDIRECT_FROM_SETTINGS = "android.intent.extra.REDIRECTED_FROM_BLUETOOTH_SHARE"
    }
}
