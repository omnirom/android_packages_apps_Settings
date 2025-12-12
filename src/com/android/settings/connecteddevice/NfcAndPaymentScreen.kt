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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.UserManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.NfcSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(NfcAndPaymentScreen.KEY)
open class NfcAndPaymentScreen :
    PreferenceScreenMixin,
    PreferenceRestrictionMixin,
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.nfc_quick_toggle_title

    override val icon: Int
        get() = R.drawable.ic_nfc

    override val highlightMenuKey
        get() = R.string.menu_key_connected_devices

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_NEAR_FIELD_COMMUNICATION_RADIO)

    override val useAdminDisabledSummary
        get() = true

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun isFlagEnabled(context: Context) = Flags.deeplinkConnectedDevices25q4()

    override fun getMetricsCategory() = SettingsEnums.CONNECTION_DEVICE_ADVANCED_NFC

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = NfcAndPaymentFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, NfcSettingsActivity::class.java, metadata?.key)

    override fun getSummary(context: Context): CharSequence? {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return null
        return context.getString(
            if (adapter.isEnabled) R.string.nfc_setting_on else R.string.nfc_setting_off
        )
    }

    override fun isAvailable(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)

    companion object {
        const val KEY = "nfc_and_payment_settings"
    }
}
// LINT.ThenChange(NfcAndPaymentFragment.java, NfcAndPaymentFragmentController.java)
