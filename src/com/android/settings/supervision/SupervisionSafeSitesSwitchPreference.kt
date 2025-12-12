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
package com.android.settings.supervision

import android.app.Activity
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_ALLOW_ALL_SITES
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_BLOCK_EXPLICIT_SITES
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding
import com.android.settingslib.supervision.SupervisionIntentProvider
import com.android.settingslib.supervision.SupervisionLog.TAG

/** Web content filters browser filter preference. */
class SupervisionSafeSitesSwitchPreference(protected val dataStore: SupervisionSafeSitesDataStore) :
    SwitchPreference(KEY),
    SwitchPreferenceBinding,
    Preference.OnPreferenceChangeListener,
    PreferenceLifecycleProvider {
    private lateinit var lifeCycleContext: PreferenceLifecycleContext

    private lateinit var supervisionCredentialLauncher: ActivityResultLauncher<Intent>

    override val title
        get() = R.string.supervision_web_content_filters_browser_filter_title

    override fun storage(context: Context) = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        supervisionCredentialLauncher =
            context.registerForActivityResult(StartActivityForResult(), ::onConfirmCredentials)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue !is Boolean) return true

        val intent =
            SupervisionIntentProvider.getConfirmSupervisionCredentialsIntent(lifeCycleContext)
        if (intent != null) {
            supervisionCredentialLauncher.launch(intent)
        }
        return false
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onConfirmCredentials(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val preference = lifeCycleContext.requirePreference<SwitchPreferenceCompat>(key)
            val isChecked = preference.isChecked
            preference.isChecked = !isChecked
            logMetrics(preference)
            Log.i(TAG, "Browser filter has changed.")
        }
    }

    private fun logMetrics(preference: SwitchPreferenceCompat) {
        val isChecked = preference.isChecked
        val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
        val action =
            if (isChecked) ACTION_SUPERVISION_BLOCK_EXPLICIT_SITES
            else ACTION_SUPERVISION_ALLOW_ALL_SITES
        metricsFeatureProvider.action(preference.context, action)
    }

    companion object {
        const val KEY = "web_content_filters_browser_filter"
    }
}
