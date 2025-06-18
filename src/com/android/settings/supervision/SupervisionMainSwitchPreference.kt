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
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.MainSwitchPreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.MainSwitchPreferenceBinding
import com.android.settingslib.preference.forEachRecursively

/** Main toggle to enable or disable device supervision. */
class SupervisionMainSwitchPreference(context: Context) :
    MainSwitchPreference(KEY, R.string.device_supervision_switch_title),
    PreferenceSummaryProvider,
    MainSwitchPreferenceBinding,
    Preference.OnPreferenceChangeListener,
    PreferenceLifecycleProvider {

    private val supervisionMainSwitchStorage = SupervisionMainSwitchStorage(context)
    private lateinit var lifeCycleContext: PreferenceLifecycleContext

    // TODO(b/383568136): Make presence of summary conditional on whether PIN
    // has been set up before or not.
    override fun getSummary(context: Context): CharSequence? =
        context.getString(R.string.device_supervision_switch_no_pin_summary)

    override fun storage(context: Context): KeyValueStore = supervisionMainSwitchStorage

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override val sensitivityLevel: Int
        get() = SensitivityLevel.HIGH_SENSITIVITY

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        updateDependentPreferencesEnabledState(
            context.findPreference<Preference>(KEY),
            supervisionMainSwitchStorage.getBoolean(KEY)!!,
        )
    }

    override fun onActivityResult(
        context: PreferenceLifecycleContext,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (resultCode == Activity.RESULT_OK) {
            val mainSwitchPreference =
                context.requirePreference<com.android.settingslib.widget.MainSwitchPreference>(KEY)
            val newValue = !supervisionMainSwitchStorage.getBoolean(KEY)!!
            mainSwitchPreference.setChecked(newValue)
            updateDependentPreferencesEnabledState(mainSwitchPreference, newValue)
            context.notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
        }

        return true
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceChangeListener = this
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue !is Boolean) return true

        val intent = Intent(lifeCycleContext, ConfirmSupervisionCredentialsActivity::class.java)
        lifeCycleContext.startActivityForResult(
            intent,
            REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
            null,
        )
        return false
    }

    private fun updateDependentPreferencesEnabledState(
        preference: Preference?,
        isChecked: Boolean,
    ) {
        preference?.parent?.forEachRecursively {
            if (it.parent?.key == SupervisionDashboardScreen.SUPERVISION_DYNAMIC_GROUP_1) {
                it.isEnabled = isChecked
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private class SupervisionMainSwitchStorage(private val context: Context) :
        NoOpKeyedObservable<String>(), KeyValueStore {
        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            (context.getSystemService(SupervisionManager::class.java)?.isSupervisionEnabled() ==
                true)
                as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (key == KEY && value is Boolean) {
                val supervisionManager = context.getSystemService(SupervisionManager::class.java)
                supervisionManager?.setSupervisionEnabled(value)
            }
        }
    }

    companion object {
        const val KEY = "device_supervision_switch"
        const val REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS = 0
    }
}
