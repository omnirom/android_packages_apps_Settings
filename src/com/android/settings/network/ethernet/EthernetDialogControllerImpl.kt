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
import android.net.InetAddresses
import android.net.IpConfiguration
import android.net.IpConfiguration.IpAssignment
import android.net.IpConfiguration.ProxySettings
import android.net.LinkAddress
import android.net.ProxyInfo
import android.net.StaticIpConfiguration
import android.net.Uri
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.Spinner
import android.widget.TextView
import com.android.net.module.util.NetUtils
import com.android.net.module.util.ProxyUtils
import com.android.settings.ProxySelector
import com.android.settings.R
import java.net.Inet4Address
import java.net.InetAddress

class EthernetDialogControllerImpl(
    private val context: Context,
    private val ipConfiguration: IpConfiguration,
    private val parentView: View,
    private val dialog: EthernetDialog,
    private val preferenceId: String,
) : AdapterView.OnItemSelectedListener, EthernetDialogController, OnCheckedChangeListener {

    private val DHCP = 0
    private val STATIC_IP = 1

    private val PROXY_NONE = 0
    private val PROXY_MANUAL = 1
    private val PROXY_AUTO_CONFIG = 2

    private lateinit var mIpSettingsSpinner: Spinner

    private lateinit var mProxySettingsSpinner: Spinner

    private lateinit var mIpAddressView: TextView
    private lateinit var mGatewayView: TextView
    private lateinit var mNetworkPrefixLengthView: TextView
    private lateinit var mDns1View: TextView
    private lateinit var mDns2View: TextView

    private lateinit var mInterfaceNameView: TextView

    private lateinit var mProxyPacView: TextView
    private lateinit var mProxyHostView: TextView
    private lateinit var mProxyPortView: TextView
    private lateinit var mProxyExclusionListView: TextView

    private var mIpAssignment: IpAssignment = IpAssignment.UNASSIGNED
    private var mProxySettings: ProxySettings = ProxySettings.UNASSIGNED
    private var mHttpProxy: ProxyInfo? = null
    private var mStaticIpConfiguration: StaticIpConfiguration? = null
    private var mEthernetInterfaceName = ""

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("ethernet_preferences", Context.MODE_PRIVATE)

    init {
        initEthernetDialog()
    }

    private fun initEthernetDialog() {
        var showAdvancedFields = false

        initializeReferences()

        val interfaceName: String? = sharedPreferences.getString(preferenceId, null)
        if (interfaceName != null) {
            mInterfaceNameView.setText(interfaceName)
        }

        if (ipConfiguration.getIpAssignment() == IpAssignment.STATIC) {
            showAdvancedFields = true
            mIpSettingsSpinner.setSelection(STATIC_IP)

            val staticIp = ipConfiguration.getStaticIpConfiguration()
            mIpAddressView.setText(staticIp?.getIpAddress()?.getAddress()?.getHostAddress())
            mNetworkPrefixLengthView.setText(
                Integer.toString(staticIp?.getIpAddress()?.getPrefixLength() ?: 0)
            )

            if (staticIp?.getGateway() != null) {
                mGatewayView.setText(staticIp?.getGateway()?.getHostAddress())
            }

            val dnsServers = staticIp?.getDnsServers()
            if (dnsServers != null && dnsServers.size > 0) {
                mDns1View.setText(dnsServers.get(0).getHostAddress())
                if (dnsServers.size > 1) {
                    mDns2View.setText(dnsServers.get(1).getHostAddress())
                }
            }
        } else {
            mIpSettingsSpinner.setSelection(DHCP)
        }

        if (ipConfiguration.getProxySettings() == ProxySettings.STATIC) {
            showAdvancedFields = true
            mProxySettingsSpinner.setSelection(PROXY_MANUAL)
            val proxyProperties: ProxyInfo? = ipConfiguration.getHttpProxy()
            if (proxyProperties != null) {
                mProxyHostView.setText(proxyProperties.getHost())
                mProxyPortView.setText(Integer.toString(proxyProperties.getPort()))
                mProxyExclusionListView.setText(
                    ProxyUtils.exclusionListAsString(proxyProperties.getExclusionList())
                )
            }
        } else if (ipConfiguration.getProxySettings() == ProxySettings.PAC) {
            showAdvancedFields = true
            mProxySettingsSpinner.setSelection(PROXY_AUTO_CONFIG)
            val proxyInfo: ProxyInfo? = ipConfiguration.getHttpProxy()
            mProxyPacView?.setText(proxyInfo?.getPacFileUrl()?.toString())
        } else {
            mProxySettingsSpinner.setSelection(PROXY_NONE)
        }

        val advancedTogglebox: CheckBox =
            parentView.findViewById<CheckBox>(R.id.wifi_advanced_togglebox)
        if (!showAdvancedFields) {
            parentView.findViewById<View>(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE)
            advancedTogglebox.setOnCheckedChangeListener(this)
            advancedTogglebox.setChecked(showAdvancedFields)
        }
        parentView.findViewById<View>(R.id.ethernet_advanced_fields).visibility =
            if (showAdvancedFields) View.VISIBLE else View.GONE
    }

    private fun initializeReferences() {
        mIpSettingsSpinner = parentView.findViewById(R.id.ip_settings)
        mIpSettingsSpinner.setOnItemSelectedListener(this)

        mProxySettingsSpinner = parentView.findViewById(R.id.proxy_settings)
        mProxySettingsSpinner.setOnItemSelectedListener(this)

        mInterfaceNameView = parentView.findViewById<TextView>(R.id.ethernet_name_edit_text)
        mInterfaceNameView.addTextChangedListener(getTextFieldsWatcher(mInterfaceNameView))

        mIpAddressView = parentView.findViewById<TextView>(R.id.ipaddress)
        mIpAddressView.addTextChangedListener(getTextFieldsWatcher(mIpAddressView))

        mGatewayView = parentView.findViewById<TextView>(R.id.gateway)
        mGatewayView.addTextChangedListener(getTextFieldsWatcher(mGatewayView))

        mNetworkPrefixLengthView = parentView.findViewById<TextView>(R.id.network_prefix_length)
        mNetworkPrefixLengthView.addTextChangedListener(
            getTextFieldsWatcher(mNetworkPrefixLengthView)
        )

        mDns1View = parentView.findViewById<TextView>(R.id.dns1)
        mDns1View.addTextChangedListener(getTextFieldsWatcher(mDns1View))
        mDns2View = parentView.findViewById<TextView>(R.id.dns2)
        mDns2View.addTextChangedListener(getTextFieldsWatcher(mDns2View))

        mProxyPacView = parentView.findViewById<TextView>(R.id.proxy_pac)
        mProxyPacView?.addTextChangedListener(getTextFieldsWatcher(mProxyPacView))

        mProxyHostView = parentView.findViewById<TextView>(R.id.proxy_hostname)
        mProxyHostView.addTextChangedListener(getTextFieldsWatcher(mProxyHostView))

        mProxyPortView = parentView.findViewById<TextView>(R.id.proxy_port)
        mProxyPortView.addTextChangedListener(getTextFieldsWatcher(mProxyPortView))

        mProxyExclusionListView = parentView.findViewById<TextView>(R.id.proxy_exclusionlist)
        mProxyExclusionListView.addTextChangedListener(
            getTextFieldsWatcher(mProxyExclusionListView)
        )
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
            setVisibility(R.id.staticip, View.VISIBLE)
            mIpAssignment = IpAssignment.STATIC
        } else {
            setVisibility(R.id.staticip, View.GONE)
            mIpAssignment = IpAssignment.DHCP
        }

        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_MANUAL) {
            setVisibility(R.id.proxy_warning_limited_support, View.VISIBLE)
            setVisibility(R.id.proxy_fields, View.VISIBLE)
            setVisibility(R.id.proxy_pac_field, View.GONE)
            mProxySettings = ProxySettings.STATIC
        } else if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_AUTO_CONFIG) {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE)
            setVisibility(R.id.proxy_fields, View.GONE)
            setVisibility(R.id.proxy_pac_field, View.VISIBLE)
            mProxySettings = ProxySettings.PAC
        } else {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE)
            setVisibility(R.id.proxy_fields, View.GONE)
            setVisibility(R.id.proxy_pac_field, View.GONE)
            mProxySettings = ProxySettings.NONE
        }
    }

    private fun setVisibility(id: Int, visibility: Int) {
        val v: View? = parentView.findViewById(id)
        v?.setVisibility(visibility)
    }

    override fun onCheckedChanged(view: CompoundButton, isChecked: Boolean) {
        if (view.getId() == R.id.wifi_advanced_togglebox) {
            view.setVisibility(View.GONE)
            parentView.findViewById<View>(R.id.ethernet_advanced_fields).setVisibility(View.VISIBLE)
        }
    }

    fun getTextFieldsWatcher(view: TextView): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // work done in afterTextChanged
            }

            override fun onTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // work done in afterTextChanged
            }

            override fun afterTextChanged(s: Editable) {
                captureProxySettings()
                captureIpConfiguration()
                captureInterfaceName()
            }
        }
    }

    private fun captureInterfaceName() {
        mEthernetInterfaceName = mInterfaceNameView.text.toString()
    }

    private fun captureProxySettings() {
        val selectedPosition = mProxySettingsSpinner.getSelectedItemPosition()
        if (selectedPosition == PROXY_MANUAL) {
            val host = mProxyHostView.text.toString()
            val portStr = mProxyPortView.text.toString()
            val exclusionList = mProxyExclusionListView.text.toString()
            var port = 0
            try {
                port = Integer.parseInt(portStr)
                if (
                    ProxySelector.validate(host, portStr, exclusionList) == ProxyUtils.PROXY_VALID
                ) {
                    mHttpProxy = ProxyInfo.buildDirectProxy(host, port, exclusionList.split(","))
                }
            } catch (e: NumberFormatException) {
                return
            }
        } else if (selectedPosition == PROXY_AUTO_CONFIG) {
            val uriSequence: CharSequence = mProxyPacView.text
            if (uriSequence.isNullOrEmpty()) {
                return
            }
            val uri: Uri = Uri.parse(uriSequence.toString()) ?: return
            mHttpProxy = ProxyInfo.buildPacProxy(uri)
        }
    }

    private fun captureIpConfiguration() {
        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
            val ipAddr = mIpAddressView.text.toString()
            if (ipAddr.isEmpty()) return

            val inetAddr: Inet4Address? = getIPv4Address(ipAddr)
            if (inetAddr == null || inetAddr.toString().equals("0.0.0.0")) {
                return
            }

            val staticIPBuilder = StaticIpConfiguration.Builder()

            try {
                var networkPrefixLength = -1
                try {
                    networkPrefixLength = mNetworkPrefixLengthView.text.toString().toInt()
                    if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                        return
                    }
                    staticIPBuilder.setIpAddress(LinkAddress(inetAddr, networkPrefixLength))
                } catch (e: NumberFormatException) {
                    mNetworkPrefixLengthView.setText(
                        context.getString(R.string.ethernet_network_prefix_length_hint)
                    )
                } catch (e: IllegalArgumentException) {
                    return
                }

                val gateway = mGatewayView.text.toString()
                if (gateway.isNullOrEmpty()) {
                    try {
                        val netPart = NetUtils.getNetworkPart(inetAddr, networkPrefixLength)
                        val addr = netPart.address
                        addr[addr.size - 1] = 1
                        mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress())
                    } catch (ee: RuntimeException) {} catch (u: java.net.UnknownHostException) {}
                } else {
                    val gatewayAddr = getIPv4Address(gateway)
                    if (gatewayAddr == null || gatewayAddr.isMulticastAddress()) {
                        return
                    }
                    staticIPBuilder.setGateway(gatewayAddr)
                }

                var dns = mDns1View.text.toString()
                var dnsAddr: InetAddress? = null
                val dnsServers = ArrayList<InetAddress>()

                if (dns.isNullOrEmpty()) {
                    mDns1View.setText(context.getString(R.string.ethernet_dns1_hint))
                } else {
                    dnsAddr = getIPv4Address(dns)
                    if (dnsAddr == null) {
                        return
                    }
                    dnsServers.add(dnsAddr)
                }

                if (!mDns2View.text.toString().isNullOrEmpty()) {
                    dns = mDns2View.text.toString()
                    dnsAddr = getIPv4Address(dns)
                    if (dnsAddr == null) {
                        return
                    }
                    dnsServers.add(dnsAddr)
                }
                staticIPBuilder.setDnsServers(dnsServers)
            } finally {
                mStaticIpConfiguration = staticIPBuilder.build()
            }
        }
    }

    private fun getIPv4Address(text: String): Inet4Address? {
        return try {
            val address = InetAddresses.parseNumericAddress(text)
            if (address is Inet4Address) {
                address
            } else {
                null
            }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    override fun getConfig(): IpConfiguration {
        ipConfiguration.setStaticIpConfiguration(mStaticIpConfiguration)
        ipConfiguration.setIpAssignment(mIpAssignment)
        ipConfiguration.setProxySettings(mProxySettings)
        ipConfiguration.setHttpProxy(mHttpProxy)
        return ipConfiguration
    }

    override fun getInterfaceName() = mEthernetInterfaceName
}
