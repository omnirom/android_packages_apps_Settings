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

package com.android.settings.connecteddevice

import android.content.pm.PackageManager.FEATURE_NFC
import android.content.pm.PackageManager.FEATURE_NFC_HOST_CARD_EMULATION
import android.nfc.NfcAdapter
import com.android.settings.R
import com.android.settings.Settings.NfcSettingsActivity
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowNfcAdapter
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

class NfcAndPaymentScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_CONNECTED_DEVICES_25Q4

    override val preferenceScreenCreator = NfcAndPaymentScreen()

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(NfcAndPaymentScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(NfcSettingsActivity::class.java.getName())
    }

    @Test
    fun isAvailable_hasNfcFeatures_returnsAvailable() {
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC, true)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC_HOST_CARD_EMULATION, true)
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(true)
    }

    @Test
    fun isAvailable_noFeatures_returnsUnavailable() {
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(false)
    }

    @Test
    fun isAvailable_noNfcFeature_returnsUnavailable() {
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC, false)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC_HOST_CARD_EMULATION, true)
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(false)
    }

    @Test
    fun isAvailable_noNfcCardFeature_returnsUnavailable() {
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC, true)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC_HOST_CARD_EMULATION, false)
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(false)
    }

    @Test
    @Config(shadows = [ShadowNfcAdapter::class])
    fun getSummary_nfcOn_shouldProvideOnSummary() {
        shadowOf(NfcAdapter.getDefaultAdapter(appContext)).setEnabled(true)
        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getText(R.string.nfc_setting_on))
    }

    @Test
    @Config(shadows = [ShadowNfcAdapter::class])
    fun getSummary_nfcOff_shouldProvideOffSummary() {
        shadowOf(NfcAdapter.getDefaultAdapter(appContext)).setEnabled(false)
        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getText(R.string.nfc_setting_off))
    }

    @Test
    override fun migration() {
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC, true)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_NFC_HOST_CARD_EMULATION, true)
        super.migration()
    }
}
