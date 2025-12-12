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

package com.android.settings.wifi.details2

import android.content.Context
import android.content.DialogInterface
import android.net.wifi.WifiConfiguration
import android.os.Bundle
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.core.TogglePreferenceController
import com.android.settings.wifi.WifiPickerTrackerHelper
import com.android.settings.wifi.details.WifiNetworkDetailsFragment
import com.android.wifitrackerlib.WifiEntry

class WifiSharedPreferenceController(
    context: Context,
    preferenceKey: String,
    private val wifiPickerTrackerHelper: WifiPickerTrackerHelper,
    private val wifiEntry: WifiEntry,
) : TogglePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int {
        return if (com.android.settings.connectivity.Flags.wifiMultiuser()) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun isChecked(): Boolean {
        return wifiEntry.isSharedWithOtherUsers()
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        val matchingWifiEntry: WifiEntry? = getMatchingWifiEntry(isChecked)
        val matchingWifiConfiguration: WifiConfiguration? =
            matchingWifiEntry?.getWifiConfiguration()
        if (matchingWifiEntry != null && matchingWifiConfiguration != null) {
            showAlertDialog(
                matchingWifiEntry.ssid ?: "",
                matchingWifiConfiguration.shared,
                matchingWifiEntry.getKey(),
            )
            return true
        }

        wifiEntry.setSharedWithOtherUsers(isChecked)
        return true
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_network
    }

    private fun showAlertDialog(ssid: String, shared: Boolean, key: String) {
        AlertDialog.Builder(mContext)
            .setTitle(
                mContext.getString(
                    R.string.wifi_conflict_dialog_title,
                    if (shared) mContext.getString(R.string.shared_message)
                    else mContext.getString(R.string.private_message),
                    ssid,
                )
            )
            .setMessage(
                mContext.getString(
                    R.string.wifi_conflict_dialog_message,
                    if (shared) mContext.getString(R.string.private_message)
                    else mContext.getString(R.string.shared_message),
                    if (shared) mContext.getString(R.string.private_message)
                    else mContext.getString(R.string.shared_message),
                    if (shared) mContext.getString(R.string.shared_message)
                    else mContext.getString(R.string.private_message),
                )
            )
            .setPositiveButton(mContext.getString(R.string.wifi_conflict_dialog_confirm)) {
                dialog: DialogInterface,
                which: Int ->
                launchNetworkDetailsPage(mContext, key, metricsCategory)
            }
            .setNegativeButton(mContext.getString(R.string.wifi_conflict_dialog_cancel)) {
                dialog: DialogInterface,
                which: Int ->
                dialog.dismiss()
            }
            .show()
    }

    private fun launchNetworkDetailsPage(context: Context, key: String, metricsCategory: Int) {
        val bundle: Bundle = Bundle()
        bundle.putString(WifiNetworkDetailsFragment.KEY_CHOSEN_WIFIENTRY_KEY, key)

        SubSettingLauncher(context)
            .setTitleText(context.getText(R.string.pref_title_network_details))
            .setDestination(WifiNetworkDetailsFragment::class.java.name)
            .setArguments(bundle)
            .setSourceMetricsCategory(metricsCategory)
            .launch()
    }

    private fun getMatchingWifiEntry(shared: Boolean): WifiEntry? {
        val wifiPickerTracker = wifiPickerTrackerHelper.getWifiPickerTracker()
        val matchingWifiEntry: WifiEntry? =
            wifiPickerTracker.wifiEntries
                .stream()
                .filter { entry: WifiEntry -> TextUtils.equals(wifiEntry.ssid, entry.ssid) }
                .filter { entry: WifiEntry -> entry.getWifiConfiguration()?.shared == shared }
                .findFirst()
                .orElse(null)

        return matchingWifiEntry
    }
}
