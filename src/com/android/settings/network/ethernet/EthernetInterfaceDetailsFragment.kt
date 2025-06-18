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

package com.android.settings.network.ethernet

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settingslib.core.AbstractPreferenceController

class EthernetInterfaceDetailsFragment : DashboardFragment() {
    private val TAG = "EthernetInterfaceDetailsFragment"
    private val ETHERNET_INTERFACE_KEY = "EthernetInterfaceKey"
    private var preferenceId: String? = null

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        preferenceId = bundle?.getString(ETHERNET_INTERFACE_KEY)
    }

    override public fun getPreferenceScreenResId(): Int {
        return R.xml.ethernet_interface_details
    }

    @VisibleForTesting
    override fun getMetricsCategory(): Int {
        return SettingsEnums.ETHERNET_SETTINGS
    }

    override public fun getLogTag(): String {
        return TAG
    }

    override public fun createPreferenceControllers(
        context: Context
    ): List<AbstractPreferenceController> {
        return listOf(
            EthernetInterfaceDetailsController(
                context,
                this,
                preferenceId ?: "",
                getSettingsLifecycle(),
            )
        )
    }
}
