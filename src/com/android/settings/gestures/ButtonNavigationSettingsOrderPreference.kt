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

package com.android.settings.gestures

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import com.android.settingslib.preference.forEachRecursively

sealed class ButtonNavigationSettingsOrderPreference(
    val store: ButtonNavigationSettingsOrderStore
) :
    BooleanValuePreference,
    BooleanValuePreferenceBinding,
    ButtonNavigationSettingsOrderRadioButton.OnClickListener {
    abstract val icons: List<Int>

    abstract val labels: List<Int>

    override fun createWidget(context: Context) =
        ButtonNavigationSettingsOrderRadioButton(context, icons, labels)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.isPersistent = false
        (preference as ButtonNavigationSettingsOrderRadioButton).also {
            it.isChecked = store.getBoolean(key) == true
            it.listener = this
        }
        preference.isPersistent = true
    }

    override fun storage(context: Context): KeyValueStore = store

    override fun onRadioButtonClicked(source: ButtonNavigationSettingsOrderRadioButton) {
        source.parent?.forEachRecursively {
            if (it is ButtonNavigationSettingsOrderRadioButton) {
                it.isChecked = it == source
            }
        }
    }

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getReadPermissions(context: Context): Permissions? =
        ButtonNavigationSettingsOrderStore.readPermissions

    override fun getWritePermissions(context: Context): Permissions? =
        ButtonNavigationSettingsOrderStore.readPermissions

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY
}

class DefaultButtonNavigationSettingsOrderPreference(store: ButtonNavigationSettingsOrderStore) :
    ButtonNavigationSettingsOrderPreference(store) {
    override val key
        get() = KEY

    override val icons
        get() =
            listOf(
                R.drawable.ic_sysbar_back,
                R.drawable.ic_sysbar_home,
                R.drawable.ic_sysbar_recents,
            )

    override val labels
        get() =
            listOf(
                R.string.navbar_back_button,
                R.string.navbar_home_button,
                R.string.navbar_recent_button,
            )

    companion object {
        const val KEY = "navbar_order_preference_default"
    }
}

class ReverseButtonNavigationSettingsOrderPreference(store: ButtonNavigationSettingsOrderStore) :
    ButtonNavigationSettingsOrderPreference(store) {
    override val key
        get() = KEY

    override val icons
        get() =
            listOf(
                R.drawable.ic_sysbar_recents,
                R.drawable.ic_sysbar_home,
                R.drawable.ic_sysbar_back,
            )

    override val labels
        get() =
            listOf(
                R.string.navbar_recent_button,
                R.string.navbar_home_button,
                R.string.navbar_back_button,
            )

    companion object {
        const val KEY = "navbar_order_preference_reverse"
    }
}
