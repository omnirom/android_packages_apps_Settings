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

package com.android.settings.accessibility.shared.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityShortcutsTutorial
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shared.data.AccessibilityShortcutDataStore
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBinding

/**
 * An interface for providing the name of the feature associated with the accessibility shortcut.
 * This is used, for example, to display the feature name in the accessibility shortcut tutorial.
 */
interface ShortcutFeatureNameProvider {
    fun getFeatureName(context: Context): CharSequence
}

/**
 * Metadata of Accessibility shortcuts.
 *
 * This class displays a [ShortcutPreference] on the screen, which has a toggle switch. It handles
 * the data storage for the shortcut's on/off state via an [AccessibilityShortcutDataStore] and
 * manages user interactions.
 */
open class AccessibilityShortcutPreference(
    context: Context,
    override val key: String,
    @StringRes override val title: Int = 0,
    val componentName: ComponentName,
    @StringRes val featureName: Int = 0,
    val metricsCategory: Int = METRICS_CATEGORY_UNKNOWN,
) :
    BooleanValuePreference,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {

    protected open val dataStore: AccessibilityShortcutDataStore by lazy {
        AccessibilityShortcutDataStore(context, componentName)
    }

    override fun createWidget(context: Context): Preference = ShortcutPreference(context, null)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is ShortcutPreference) {
            preference.apply {
                isChecked = dataStore.getBoolean(key) ?: false
                isSettingsEditable = getSettingsEditable(context)
            }
        }
    }

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when (value) {
            true -> ReadWritePermit.DISALLOW
            else -> ReadWritePermit.ALLOW
        }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        val shortcutPreference = context.requirePreference<ShortcutPreference>(key)
        shortcutPreference.setOnClickCallback(
            object : ShortcutPreference.OnClickCallback {

                override fun onSettingsClicked(preference: ShortcutPreference?) {
                    if (preference == null) return
                    onSettingsClicked(preference, context)
                }

                override fun onToggleClicked(preference: ShortcutPreference?) {
                    if (preference == null) return
                    onToggleClicked(preference, context)
                }
            }
        )
    }

    protected open fun onSettingsClicked(
        preference: ShortcutPreference,
        context: PreferenceLifecycleContext,
    ) {
        showEditShortcutsScreen(preference.context, preference.title ?: "")
        featureFactory.metricsFeatureProvider.logClickedPreference(preference, metricsCategory)
    }

    protected open fun onToggleClicked(
        preference: ShortcutPreference,
        context: PreferenceLifecycleContext,
    ) {
        if (preference.isChecked) {
            showShortcutsTutorial(
                context,
                context.childFragmentManager,
                dataStore.getUserShortcutTypes(),
                preference.context.isInSetupWizard(),
            )
        }
        dataStore.setBoolean(key, preference.isChecked)
    }

    override fun getSummary(context: Context): CharSequence? {
        if (!getSettingsEditable(context)) {
            return context.getText(R.string.accessibility_shortcut_edit_dialog_title_hardware)
        }

        if (dataStore.getBoolean(key) != true) {
            return context.getText(R.string.accessibility_shortcut_state_off)
        }

        return AccessibilityUtil.getShortcutSummaryList(context, dataStore.getUserShortcutTypes())
    }

    protected open fun getSettingsEditable(context: Context): Boolean = true

    protected fun showShortcutsTutorial(
        context: Context,
        fragmentManager: FragmentManager,
        shortcutTypes: Int,
        isInSetupWizard: Boolean,
    ) {
        val featureLabel: CharSequence =
            when {
                title != 0 -> context.getText(title)
                this is ShortcutFeatureNameProvider -> getFeatureName(context)
                else -> ""
            }

        AccessibilityShortcutsTutorial.DialogFragment.showDialog(
            fragmentManager,
            shortcutTypes,
            featureLabel,
            isInSetupWizard,
        )
    }

    protected fun showEditShortcutsScreen(context: Context, screenTitle: CharSequence) {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
            context,
            metricsCategory,
            screenTitle,
            componentName,
            (context as? Activity)?.intent,
        )
    }
}
