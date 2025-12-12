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

import android.content.Context
import android.content.SharedPreferences
import android.net.EthernetManager
import android.net.EthernetNetworkUpdateRequest
import android.net.IpConfiguration
import android.net.LinkProperties
import android.net.StaticIpConfiguration
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.widget.EntityHeaderController
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.widget.LayoutPreference

class EthernetInterfaceDetailsController(
    context: Context,
    private val fragment: PreferenceFragmentCompat,
    private val preferenceId: String,
    private val lifecycle: Lifecycle,
) :
    AbstractPreferenceController(context),
    EthernetDialog.EthernetDialogListener,
    EthernetInterface.EthernetInterfaceStateListener,
    LifecycleEventObserver {
    private val KEY_HEADER = "ethernet_details"

    private val ethernetManager = context.getSystemService(EthernetManager::class.java)
    private val ethernetInterface =
        EthernetTrackerImpl.getInstance(context).getInterface(preferenceId)

    private lateinit var entityHeaderController: EntityHeaderController

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ethernet_preferences", Context.MODE_PRIVATE)

    private var ipAddressPref: Preference? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun isAvailable(): Boolean {
        return true
    }

    override fun getPreferenceKey(): String? {
        return KEY_HEADER
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_START -> {
                ethernetInterface?.registerListener(this)
            }

            Lifecycle.Event.ON_STOP -> {
                ethernetInterface?.unregisterListener(this)
            }

            else -> {}
        }
    }

    override fun onSubmit(dialog: EthernetDialog) {
        val ipConfiguration = dialog.getController().getConfig()

        val staticIp = ipConfiguration.getStaticIpConfiguration()

        val updateRequest: EthernetNetworkUpdateRequest =
            EthernetNetworkUpdateRequest.Builder().setIpConfiguration(ipConfiguration).build()

        ethernetManager.updateConfiguration(
            preferenceId,
            updateRequest,
            /* executor */ null,
            /* callback */ null,
        )

        val interfaceName = dialog.getController().getInterfaceName()
        if (interfaceName != null && !interfaceName.isEmpty()) {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString(preferenceId, interfaceName)
            editor.apply()
            entityHeaderController?.setLabel(interfaceName)?.done(true)
        }
    }

    override fun displayPreference(screen: PreferenceScreen) {
        val headerPref: LayoutPreference? = screen.findPreference(KEY_HEADER)

        entityHeaderController =
            EntityHeaderController.newInstance(
                fragment.getActivity(),
                fragment,
                headerPref?.findViewById(R.id.entity_header),
            )

        val iconView: ImageView? = headerPref?.findViewById(R.id.entity_header_icon)

        iconView?.setScaleType(ImageView.ScaleType.CENTER_INSIDE)

        if (entityHeaderController != null) {
            var interfaceName: String? = sharedPreferences.getString(preferenceId, null)
            if (interfaceName == null) {
                interfaceName = "Ethernet"
            }
            entityHeaderController
                .setLabel(interfaceName)
                .setSummary(
                    if (ethernetInterface?.getInterfaceState() == EthernetManager.STATE_LINK_UP) {
                        mContext.getString(R.string.network_connected)
                    } else {
                        mContext.getString(R.string.network_disconnected)
                    }
                )
                .setSecondSummary("")
                .setIcon(mContext.getDrawable(R.drawable.ic_settings_ethernet))
                .done(true /* rebind */)
        }

        ipAddressPref = screen.findPreference<Preference>("ethernet_ip_address")

        if (ethernetInterface?.getInterfaceState() == EthernetManager.STATE_LINK_UP) {
            initializeIpDetails()
        }
    }

    override fun interfaceUpdated() {
        entityHeaderController?.setSummary(
            if (ethernetInterface?.getInterfaceState() == EthernetManager.STATE_LINK_UP) {
                mContext.getString(R.string.network_connected)
            } else {
                mContext.getString(R.string.network_disconnected)
            }
        )
        initializeIpDetails()
    }

    private fun initializeIpDetails() {
        val ipConfiguration: IpConfiguration? = ethernetInterface?.getConfiguration()
        val linkProperties: LinkProperties? = ethernetInterface?.getLinkProperties()

        if (ipConfiguration?.getIpAssignment() == IpConfiguration.IpAssignment.STATIC) {
            val staticIp: StaticIpConfiguration? = ipConfiguration?.getStaticIpConfiguration()
            ipAddressPref?.setSummary(staticIp?.getIpAddress()?.getAddress()?.getHostAddress())
        } else {
            val addresses = linkProperties?.getAddresses()
            if (addresses != null && addresses.size > 0) {
                ipAddressPref?.setSummary(addresses?.first()?.getHostAddress())
            } else {
                ipAddressPref?.setSummary("")
            }
        }
    }
}
