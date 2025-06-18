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
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.EthernetManager
import android.net.EthernetManager.STATE_ABSENT
import android.net.EthernetNetworkManagementException
import android.net.EthernetNetworkUpdateRequest
import android.net.IpConfiguration
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.OutcomeReceiver
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.common.annotations.VisibleForTesting

class EthernetInterface(private val context: Context, private val id: String) :
    EthernetManager.InterfaceStateListener {
    interface EthernetInterfaceStateListener {
        fun interfaceUpdated()
    }

    private val ethernetManager: EthernetManager? =
        context.getSystemService(EthernetManager::class.java)
    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(ConnectivityManager::class.java)
    private val executor = ContextCompat.getMainExecutor(context)
    private val interfaceListeners = mutableListOf<EthernetInterfaceStateListener>()

    private val TAG = "EthernetInterface"

    private val networkRequest: NetworkRequest =
        NetworkRequest.Builder()
            .clearCapabilities()
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

    private var interfaceState = STATE_ABSENT
    private var ipConfiguration = IpConfiguration()
    private var linkProperties = LinkProperties()

    fun getInterfaceState() = interfaceState

    fun getId() = id

    fun getConfiguration() = ipConfiguration

    fun getLinkProperties() = linkProperties

    fun registerListener(listener: EthernetInterfaceStateListener) {
        if (interfaceListeners.isEmpty()) {
            ethernetManager?.addInterfaceStateListener(ContextCompat.getMainExecutor(context), this)
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
        }
        interfaceListeners.add(listener)
    }

    fun unregisterListener(listener: EthernetInterfaceStateListener) {
        interfaceListeners.remove(listener)
        if (interfaceListeners.isEmpty()) {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
            ethernetManager?.removeInterfaceStateListener(this)
        }
    }

    fun setConfiguration(ipConfiguration: IpConfiguration) {
        val request =
            EthernetNetworkUpdateRequest.Builder().setIpConfiguration(ipConfiguration).build()
        ethernetManager?.updateConfiguration(
            id,
            request,
            executor,
            object : OutcomeReceiver<String, EthernetNetworkManagementException> {
                override fun onError(e: EthernetNetworkManagementException) {
                    Log.e(TAG, "Failed to updateConfiguration: ", e)
                }

                override fun onResult(id: String) {
                    Log.d(TAG, "Successfully updated configuration: " + id)
                }
            },
        )
    }

    private fun notifyListeners() {
        for (listener in interfaceListeners) {
            listener.interfaceUpdated()
        }
    }

    override fun onInterfaceStateChanged(id: String, state: Int, role: Int, cfg: IpConfiguration?) {
        if (id == this.id) {
            ipConfiguration = cfg ?: IpConfiguration()
            interfaceState = state
            notifyListeners()
        }
    }

    @VisibleForTesting
    val networkCallback =
        object : NetworkCallback() {
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) {
                if (lp.getInterfaceName().equals(id)) {
                    linkProperties = lp
                    notifyListeners()
                }
            }
        }
}
