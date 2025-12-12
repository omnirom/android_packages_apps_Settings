/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.spa.network

import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.settings.R
import com.android.settings.network.telephony.MobileDataRepository
import com.android.settings.network.telephony.wificalling.CrossSimCallingViewModel
import com.android.settingslib.spa.framework.compose.HighlightBox
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.search.SearchablePage.SearchItem
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ID for AutomaticDataSwitchingPreference.
 *
 * Caution, has external dependencies on this:
 * - frameworks/opt/telephony
 * - frameworks/base/packages/SystemUI
 */
private const val AUTO_DATA_SWITCH_SETTING_R_ID = "auto_data_switch"

fun getAutomaticDataSwitchingSearchItem(context: Context) =
    SearchItem(
        highlightItemKey = AUTO_DATA_SWITCH_SETTING_R_ID,
        itemTitle = context.getString(R.string.primary_sim_automatic_data_title),
    )

@Composable
fun AutomaticDataSwitchingPreference(nonDds: Int) {
    if (!SubscriptionManager.isValidSubscriptionId(nonDds)) return

    val mobileDataRepository = rememberContext(::MobileDataRepository)
    val isAutoDataEnabled by
        remember(nonDds) {
                mobileDataRepository.isMobileDataPolicyEnabledFlow(
                    subId = nonDds,
                    policy = TelephonyManager.MOBILE_DATA_POLICY_AUTO_DATA_SWITCH,
                )
            }
            .collectAsStateWithLifecycle(initialValue = null)
    HighlightBox(AUTO_DATA_SWITCH_SETTING_R_ID) {
        AutomaticDataSwitchingPreference(
            isAutoDataEnabled = { isAutoDataEnabled },
            setAutoDataEnabled = { newEnabled ->
                mobileDataRepository.setAutoDataSwitch(nonDds, newEnabled)
            },
        )
    }
}

@Composable
fun AutomaticDataSwitchingPreference(
    isAutoDataEnabled: () -> Boolean?,
    setAutoDataEnabled: (newEnabled: Boolean) -> Unit,
) {
    val autoDataSummary = stringResource(id = R.string.primary_sim_automatic_data_msg)
    val coroutineScope = rememberCoroutineScope()
    // CrossSimCallingViewModel is responsible for maintaining the correct cross sim calling
    // settings (backup calling).
    viewModel<CrossSimCallingViewModel>()
    SwitchPreference(
        object : SwitchPreferenceModel {
            override val title = stringResource(id = R.string.primary_sim_automatic_data_title)
            override val summary = { autoDataSummary }
            override val checked = { isAutoDataEnabled() }
            override val onCheckedChange: (Boolean) -> Unit = { newEnabled ->
                coroutineScope.launch(Dispatchers.Default) {
                    setAutoDataEnabled(newEnabled)
                }
            }
        }
    )
}
