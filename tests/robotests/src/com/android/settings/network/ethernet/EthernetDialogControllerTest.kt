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
import android.net.InetAddresses
import android.net.IpConfiguration
import android.net.LinkAddress
import android.net.ProxyInfo
import android.net.StaticIpConfiguration
import android.net.Uri
import android.text.Editable
import android.view.View
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever

@RunWith(AndroidJUnit4::class)
class EthernetDialogControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockView: View = mock(View::class.java)
    private val ethernetDialog: EthernetDialog = mock(EthernetDialog::class.java)

    private val ipSettingsSpinner = mock(Spinner::class.java)
    private val proxySettingsSpinner = mock(Spinner::class.java)

    private val ipAddressView = mock(TextView::class.java)
    private val gatewayView = mock(TextView::class.java)
    private val networkPrefixLengthView = mock(TextView::class.java)
    private val dns1View = mock(TextView::class.java)
    private val dns2View = mock(TextView::class.java)

    private val interfaceNameView = mock(TextView::class.java)

    private val proxyPacView = mock(TextView::class.java)
    private val proxyHostView = mock(TextView::class.java)
    private val proxyPortView = mock(TextView::class.java)
    private val proxyExclusionListView = mock(TextView::class.java)

    private val proxyWarning = mock(View::class.java)
    private val proxyFields = mock(View::class.java)
    private val proxyPacField = mock(View::class.java)

    private val advancedTogglebox = mock(CheckBox::class.java)
    private val advancedToggle = mock(View::class.java)
    private val advancedFields = mock(View::class.java)

    private val staticIpView = mock(View::class.java)

    private val DHCP = 0
    private val STATIC_IP = 1

    private val NONE = 0
    private val MANUAL = 1
    private val AUTO_CONFIG = 2

    @Before
    fun setUp() {
        whenever(mockView.findViewById<Spinner>(R.id.ip_settings)).thenReturn(ipSettingsSpinner)
        whenever(mockView.findViewById<Spinner>(R.id.proxy_settings))
            .thenReturn(proxySettingsSpinner)

        whenever(mockView.findViewById<TextView>(R.id.ipaddress)).thenReturn(ipAddressView)
        whenever(mockView.findViewById<TextView>(R.id.gateway)).thenReturn(gatewayView)
        whenever(mockView.findViewById<TextView>(R.id.network_prefix_length))
            .thenReturn(networkPrefixLengthView)
        whenever(mockView.findViewById<TextView>(R.id.dns1)).thenReturn(dns1View)
        whenever(mockView.findViewById<TextView>(R.id.dns2)).thenReturn(dns2View)

        whenever(mockView.findViewById<TextView>(R.id.ethernet_name_edit_text))
            .thenReturn(interfaceNameView)

        whenever(mockView.findViewById<TextView>(R.id.proxy_pac)).thenReturn(proxyPacView)
        whenever(mockView.findViewById<TextView>(R.id.proxy_hostname)).thenReturn(proxyHostView)
        whenever(mockView.findViewById<TextView>(R.id.proxy_port)).thenReturn(proxyPortView)
        whenever(mockView.findViewById<TextView>(R.id.proxy_exclusionlist))
            .thenReturn(proxyExclusionListView)
        whenever(mockView.findViewById<View>(R.id.proxy_warning_limited_support))
            .thenReturn(proxyWarning)
        whenever(mockView.findViewById<View>(R.id.proxy_fields)).thenReturn(proxyFields)
        whenever(mockView.findViewById<View>(R.id.proxy_pac_field)).thenReturn(proxyPacField)

        whenever(mockView.findViewById<CheckBox>(R.id.wifi_advanced_togglebox))
            .thenReturn(advancedTogglebox)
        whenever(advancedTogglebox.getId()).thenReturn(R.id.wifi_advanced_togglebox)
        whenever(mockView.findViewById<View>(R.id.wifi_advanced_toggle)).thenReturn(advancedToggle)
        whenever(mockView.findViewById<View>(R.id.ethernet_advanced_fields))
            .thenReturn(advancedFields)

        whenever(mockView.findViewById<View>(R.id.staticip)).thenReturn(staticIpView)
    }

    @Test
    fun initializeDHCPIp() {
        val ipConfig = IpConfiguration()
        ipConfig.setIpAssignment(IpConfiguration.IpAssignment.DHCP)

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        verify(ipSettingsSpinner).setSelection(DHCP)
        verify(advancedToggle).setVisibility(View.VISIBLE)
        verify(advancedFields).setVisibility(View.GONE)
        verify(advancedTogglebox).setChecked(false)
    }

    @Test
    fun initializeStaticIp() {
        val ipConfig = IpConfiguration()
        ipConfig.setIpAssignment(IpConfiguration.IpAssignment.STATIC)

        val dnsServers = ArrayList<InetAddress>()
        dnsServers.add(InetAddresses.parseNumericAddress("8.8.8.8"))
        dnsServers.add(InetAddresses.parseNumericAddress("8.8.8.9"))

        val networkPrefixLength = 24

        val staticIPBuilder = StaticIpConfiguration.Builder()
        staticIPBuilder.setIpAddress(
            LinkAddress(InetAddresses.parseNumericAddress("192.168.5.4"), networkPrefixLength)
        )
        staticIPBuilder.setGateway(InetAddresses.parseNumericAddress("192.168.5.1"))
        staticIPBuilder.setDnsServers(dnsServers)
        ipConfig.setStaticIpConfiguration(staticIPBuilder.build())

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        verify(ipAddressView).setText("192.168.5.4")
        verify(gatewayView).setText("192.168.5.1")
        verify(dns1View).setText("8.8.8.8")
        verify(dns2View).setText("8.8.8.9")

        verify(ipSettingsSpinner).setSelection(STATIC_IP)
        verify(advancedToggle, times(0)).setVisibility(View.VISIBLE)
        verify(advancedFields).setVisibility(View.VISIBLE)
        verify(advancedTogglebox, times(0)).setChecked(false)
    }

    @Test
    fun initializeManualProxy() {
        val ipConfig = IpConfiguration()
        ipConfig.setProxySettings(IpConfiguration.ProxySettings.STATIC)

        val exclusionList = ArrayList<String>()
        exclusionList.add("http://excluone.com")
        exclusionList.add("http://exclutwo.com")

        ipConfig.setHttpProxy(ProxyInfo.buildDirectProxy("http://proxy.com", 5330, exclusionList))

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        verify(proxySettingsSpinner).setSelection(MANUAL)
        verify(proxyHostView).setText("http://proxy.com")
        verify(proxyPortView).setText("5330")
        verify(proxyExclusionListView).setText("http://excluone.com,http://exclutwo.com")

        verify(advancedToggle, times(0)).setVisibility(View.VISIBLE)
        verify(advancedFields).setVisibility(View.VISIBLE)
        verify(advancedTogglebox, times(0)).setChecked(false)
    }

    @Test
    fun initializeAutoProxy() {
        val ipConfig = IpConfiguration()
        ipConfig.setProxySettings(IpConfiguration.ProxySettings.PAC)
        ipConfig.setHttpProxy(
            ProxyInfo.buildPacProxy(Uri.parse("http://pac.example.com/proxy.pac"))
        )

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        verify(proxySettingsSpinner).setSelection(AUTO_CONFIG)
        verify(proxyPacView).setText("http://pac.example.com/proxy.pac")

        verify(advancedToggle, times(0)).setVisibility(View.VISIBLE)
        verify(advancedFields).setVisibility(View.VISIBLE)
        verify(advancedTogglebox, times(0)).setChecked(false)
    }

    @Test
    fun initializeNoneProxy() {
        val ipConfig = IpConfiguration()
        ipConfig.setProxySettings(IpConfiguration.ProxySettings.NONE)

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        verify(proxySettingsSpinner).setSelection(NONE)
        verify(advancedToggle).setVisibility(View.VISIBLE)
        verify(advancedFields).setVisibility(View.GONE)
        verify(advancedTogglebox).setChecked(false)
    }

    @Test
    fun ipSettingsSpinner_selectedDHCP() {
        val ipConfig = IpConfiguration()
        ipConfig.setIpAssignment(IpConfiguration.IpAssignment.DHCP)

        whenever(ipSettingsSpinner.getSelectedItemPosition()).thenReturn(DHCP)

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        controller.onItemSelected(null, mockView, 0, 0)

        verify(staticIpView).setVisibility(View.GONE)
    }

    @Test
    fun ipSettingsSpinner_selectedStatic() {
        val ipConfig = IpConfiguration()
        ipConfig.setIpAssignment(IpConfiguration.IpAssignment.DHCP)

        whenever(ipSettingsSpinner.getSelectedItemPosition()).thenReturn(STATIC_IP)

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        controller.onItemSelected(null, mockView, 0, 0)

        verify(staticIpView).setVisibility(View.VISIBLE)
    }

    @Test
    fun proxySettingsSpinner_selectedAuto() {
        val ipConfig = IpConfiguration()
        ipConfig.setProxySettings(IpConfiguration.ProxySettings.PAC)

        whenever(proxySettingsSpinner.getSelectedItemPosition()).thenReturn(AUTO_CONFIG)

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        controller.onItemSelected(null, mockView, 0, 0)

        verify(proxyWarning).setVisibility(View.GONE)
        verify(proxyFields).setVisibility(View.GONE)
        verify(proxyPacField).setVisibility(View.VISIBLE)
    }

    @Test
    fun proxySettingsSpinner_selectedManual() {
        val ipConfig = IpConfiguration()
        ipConfig.setProxySettings(IpConfiguration.ProxySettings.STATIC)

        whenever(proxySettingsSpinner.getSelectedItemPosition()).thenReturn(MANUAL)

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        controller.onItemSelected(null, mockView, 0, 0)

        verify(proxyWarning).setVisibility(View.VISIBLE)
        verify(proxyFields).setVisibility(View.VISIBLE)
        verify(proxyPacField).setVisibility(View.GONE)
    }

    @Test
    fun proxySettingsSpinner_selectedNone() {
        val ipConfig = IpConfiguration()
        ipConfig.setProxySettings(IpConfiguration.ProxySettings.NONE)

        whenever(proxySettingsSpinner.getSelectedItemPosition()).thenReturn(NONE)

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        controller.onItemSelected(null, mockView, 0, 0)

        verify(proxyWarning).setVisibility(View.GONE)
        verify(proxyFields).setVisibility(View.GONE)
        verify(proxyPacField).setVisibility(View.GONE)
    }

    @Test
    fun advancedToggleChecked() {
        val ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")

        controller.onCheckedChanged(advancedTogglebox, true)

        verify(advancedTogglebox).setVisibility(View.GONE)
        verify(advancedFields).setVisibility(View.VISIBLE)
    }

    @Test
    fun captureInterfaceName() {
        val ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")
        whenever(interfaceNameView.getText()).thenReturn("TestInterfaceName")

        val textWatcher = controller.getTextFieldsWatcher(interfaceNameView)

        textWatcher.afterTextChanged(mock(Editable::class.java))

        assertEquals(controller.getInterfaceName(), "TestInterfaceName")
    }

    @Test
    fun captureProxySettings_ManualWithValidInput() {
        var ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")
        whenever(proxySettingsSpinner.getSelectedItemPosition()).thenReturn(MANUAL)
        whenever(interfaceNameView.getText()).thenReturn("TestInterfaceName")
        whenever(proxyHostView.getText()).thenReturn("proxy.com")
        whenever(proxyPortView.getText()).thenReturn("5220")
        whenever(proxyExclusionListView.getText()).thenReturn("exclone.com,excltwo.com")

        val textWatcher = controller.getTextFieldsWatcher(proxyHostView)

        textWatcher.afterTextChanged(mock(Editable::class.java))

        ipConfig = controller.getConfig()
        val httpProxy = ipConfig.getHttpProxy()
        val exclList: ArrayList<String> =
            httpProxy?.getExclusionList()?.let { array -> ArrayList(array.toList()) } ?: ArrayList()
        assertEquals(httpProxy?.getHost(), "proxy.com")
        assertEquals(httpProxy?.getPort(), 5220)
        assertEquals(exclList[0], "exclone.com")
        assertEquals(exclList[1], "excltwo.com")
    }

    @Test
    fun captureProxySettings_ManualWithInvalidInput() {
        var ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")
        whenever(proxySettingsSpinner.getSelectedItemPosition()).thenReturn(MANUAL)
        whenever(interfaceNameView.getText()).thenReturn("TestInterfaceName")
        whenever(proxyHostView.getText()).thenReturn("proxy.com")
        whenever(proxyPortView.getText()).thenReturn("5a0")
        whenever(proxyExclusionListView.getText()).thenReturn("exclone.com,excltwo.com")

        val textWatcher = controller.getTextFieldsWatcher(proxyHostView)

        textWatcher.afterTextChanged(mock(Editable::class.java))

        ipConfig = controller.getConfig()
        val httpProxy = ipConfig.getHttpProxy()
        val exclList: ArrayList<String> =
            httpProxy?.getExclusionList()?.let { array -> ArrayList(array.toList()) } ?: ArrayList()
        assertEquals(httpProxy?.getHost(), null)
        assertEquals(httpProxy?.getPort(), null)
    }

    @Test
    fun captureProxySettings_AutoWithValidInput() {
        var ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")
        whenever(proxySettingsSpinner.getSelectedItemPosition()).thenReturn(AUTO_CONFIG)
        whenever(interfaceNameView.getText()).thenReturn("TestInterfaceName")
        whenever(proxyPacView.getText()).thenReturn("http://pac.example.com/proxy.pac")

        val textWatcher = controller.getTextFieldsWatcher(proxyHostView)

        textWatcher.afterTextChanged(mock(Editable::class.java))

        ipConfig = controller.getConfig()
        val httpProxy = ipConfig.getHttpProxy()

        assertEquals(httpProxy?.getPacFileUrl()?.toString(), "http://pac.example.com/proxy.pac")
    }

    @Test
    fun captureProxySettings_AutoWithInvalidInput() {
        var ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")
        whenever(proxySettingsSpinner.getSelectedItemPosition()).thenReturn(AUTO_CONFIG)
        whenever(interfaceNameView.getText()).thenReturn("TestInterfaceName")
        whenever(proxyPacView.getText()).thenReturn("")

        val textWatcher = controller.getTextFieldsWatcher(proxyHostView)

        textWatcher.afterTextChanged(mock(Editable::class.java))

        ipConfig = controller.getConfig()
        val httpProxy = ipConfig.getHttpProxy()

        assertEquals(httpProxy, null)
    }

    @Test
    fun captureIp_DHCP() {
        var ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")
        whenever(ipSettingsSpinner.getSelectedItemPosition()).thenReturn(DHCP)
        whenever(interfaceNameView.getText()).thenReturn("TestInterfaceName")

        val textWatcher = controller.getTextFieldsWatcher(ipAddressView)

        textWatcher.afterTextChanged(mock(Editable::class.java))

        ipConfig = controller.getConfig()

        assertEquals(ipConfig.getStaticIpConfiguration(), null)
    }

    @Test
    fun captureIp_STATICWithValidInput() {
        var ipConfig = IpConfiguration()

        val controller =
            EthernetDialogControllerImpl(context, ipConfig, mockView, ethernetDialog, "eth0")
        whenever(ipSettingsSpinner.getSelectedItemPosition()).thenReturn(STATIC_IP)
        whenever(interfaceNameView.getText()).thenReturn("TestInterfaceName")
        whenever(ipAddressView.getText()).thenReturn("192.168.5.4")
        whenever(networkPrefixLengthView.getText()).thenReturn("24")
        whenever(gatewayView.getText()).thenReturn("192.168.5.1")
        whenever(dns1View.getText()).thenReturn("8.8.8.8")
        whenever(dns2View.getText()).thenReturn("8.8.8.9")

        val textWatcher = controller.getTextFieldsWatcher(ipAddressView)

        textWatcher.afterTextChanged(mock(Editable::class.java))

        ipConfig = controller.getConfig()

        assertEquals(
            ipConfig.getStaticIpConfiguration()?.getIpAddress()?.getAddress()?.getHostAddress(),
            "192.168.5.4",
        )
        assertEquals(
            ipConfig.getStaticIpConfiguration()?.getGateway()?.getHostAddress(),
            "192.168.5.1",
        )

        val dnsServers: MutableList<InetAddress>? =
            ipConfig.getStaticIpConfiguration()?.getDnsServers()
        assertEquals(dnsServers?.get(0)?.getHostAddress(), "8.8.8.8")
        assertEquals(dnsServers?.get(1)?.getHostAddress(), "8.8.8.9")
    }
}
