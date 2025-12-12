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

import android.app.Dialog
import android.app.settings.SettingsEnums
import android.content.Context
import android.net.IpConfiguration
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settingslib.core.AbstractPreferenceController

class EthernetInterfaceDetailsFragment :
    DashboardFragment(), EthernetDialog.EthernetDialogListener {
    private val TAG = "EthernetInterfaceDetailsFragment"
    private val ETHERNET_INTERFACE_KEY = "EthernetInterfaceKey"
    private val ETHERNET_DIALOG_ID = 1

    private lateinit var controller: EthernetInterfaceDetailsController

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        val item: MenuItem = menu.add(0, Menu.FIRST, 0, R.string.ethernet_modify)
        item.setIcon(com.android.internal.R.drawable.ic_mode_edit)
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            Menu.FIRST -> {
                showDialog(ETHERNET_DIALOG_ID)
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun getDialogMetricsCategory(dialogId: Int): Int {
        if (dialogId == ETHERNET_DIALOG_ID) {
            return SettingsEnums.ETHERNET_SETTINGS
        }
        return 0
    }

    @VisibleForTesting
    override public fun getPreferenceScreenResId(): Int {
        return R.xml.ethernet_interface_details
    }

    override fun onCreateDialog(dialogId: Int): Dialog {
        val preferenceId = getArguments()?.getString(ETHERNET_INTERFACE_KEY)
        val ethernetTracker = EthernetTrackerImpl.getInstance(requireContext())
        val ethernetInterface = ethernetTracker.getInterface(preferenceId ?: "")
        return EthernetDialogImpl(
            requireContext(),
            this,
            ethernetInterface?.getConfiguration() ?: IpConfiguration(),
            preferenceId ?: "",
        )
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.ETHERNET_SETTINGS
    }

    override fun onSubmit(dialog: EthernetDialog) {
        controller.onSubmit(dialog)
    }

    @VisibleForTesting
    override public fun getLogTag(): String {
        return TAG
    }

    override public fun createPreferenceControllers(
        context: Context
    ): List<AbstractPreferenceController> {
        val preferenceId = getArguments()?.getString(ETHERNET_INTERFACE_KEY)
        controller =
            EthernetInterfaceDetailsController(
                context,
                this,
                preferenceId ?: "",
                getSettingsLifecycle(),
            )
        return listOf(controller)
    }
}
