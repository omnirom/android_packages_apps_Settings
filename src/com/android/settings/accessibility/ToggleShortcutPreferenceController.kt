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

package com.android.settings.accessibility

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shared.utils.debounce
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.core.BasePreferenceController
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import kotlin.time.Duration.Companion.milliseconds

/**
 * A [com.android.settings.core.BasePreferenceController] that handles binding the shortcut
 * preference data to [ShortcutPreference] and navigating to edit shortcuts screen.
 */
open class ToggleShortcutPreferenceController(context: Context, key: String) :
    BasePreferenceController(context, key),
    DefaultLifecycleObserver,
    ShortcutPreference.OnClickCallback {
    private val settingsContentObserver =
        AccessibilitySettingsContentObserver(Looper.myLooper()?.run { Handler(/* async= */ true) })
    protected var shortcutPreference: ShortcutPreference? = null
    protected var componentName: ComponentName? = null
    protected var featureName: CharSequence? = null
    protected var sourceMetricsCategory: Int = 0
    protected var fragmentManager: FragmentManager? = null

    protected open val shortcutSettingsKey = ShortcutConstants.GENERAL_SHORTCUT_SETTINGS.toList()

    /** Initialize the [ComponentName] this shortcut toggle is attached to. */
    open fun initialize(
        componentName: ComponentName,
        fragmentManager: FragmentManager,
        featureName: CharSequence,
        sourceMetricsCategory: Int,
    ) {
        this.componentName = componentName
        this.fragmentManager = fragmentManager
        this.sourceMetricsCategory = sourceMetricsCategory
        this.featureName = featureName
    }

    // TODO(b/147990389): Delete this function after we migrated to MAGNIFICATION_COMPONENT_NAME.
    open fun getComponentNameAsString(): String {
        return componentName!!.flattenToString()
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        val debounceUpdateData =
            debounce(100.milliseconds, owner.lifecycleScope) {
                updateShortcutPreferenceData()
                updateState(shortcutPreference)
            }

        settingsContentObserver.registerKeysToObserverCallback(shortcutSettingsKey) { key: String?
            ->
            debounceUpdateData.invoke()
        }
        settingsContentObserver.register(mContext.contentResolver)
        // Always update the user preferred shortcuts when screen is shown,
        // because the user could change the shortcut types outside of this screen.
        updateShortcutPreferenceData()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        settingsContentObserver.unregister(mContext.contentResolver)
        shortcutPreference = null
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        shortcutPreference = screen?.findPreference<ShortcutPreference>(preferenceKey)
        shortcutPreference?.setOnClickCallback(this@ToggleShortcutPreferenceController)
    }

    override fun updateState(preference: Preference?) {
        if (preference as? ShortcutPreference != null && componentName != null) {
            preference.isChecked =
                AccessibilityUtil.getUserShortcutTypesFromSettings(mContext, componentName!!) !=
                    UserShortcutType.DEFAULT
        }
        refreshSummary(preference)
    }

    override fun getAvailabilityStatus(): Int {
        return AVAILABLE
    }

    override fun onSettingsClicked(preference: ShortcutPreference) {
        showEditShortcutsScreen(preference.title ?: "")
        // log here since calling super.onPreferenceTreeClick will be skipped
        featureFactory.metricsFeatureProvider.logClickedPreference(
            preference,
            sourceMetricsCategory,
        )
    }

    override fun onToggleClicked(preference: ShortcutPreference) {
        setChecked(preference, preference.isChecked)
        if (preference.isChecked) {
            componentName?.let { showShortcutsTutorial(getUserPreferredShortcutTypes(it)) }
        }
    }

    fun setChecked(preference: ShortcutPreference, checked: Boolean) {
        if (componentName == null) return
        val shortcutTypes = getUserPreferredShortcutTypes(componentName!!)
        mContext
            .getSystemService(AccessibilityManager::class.java)!!
            .enableShortcutsForTargets(
                checked,
                shortcutTypes,
                setOf(getComponentNameAsString()),
                mContext.userId,
            )
        preference.isChecked = checked
    }

    override fun getSummary(): CharSequence? {
        if (shortcutPreference == null) return null
        if (!shortcutPreference!!.isSettingsEditable) {
            return mContext.getText(R.string.accessibility_shortcut_edit_dialog_title_hardware)
        }

        if (!shortcutPreference!!.isChecked) {
            return mContext.getText(R.string.accessibility_shortcut_state_off)
        }

        val shortcutTypes =
            componentName?.let { getUserPreferredShortcutTypes(it) } ?: UserShortcutType.DEFAULT
        return AccessibilityUtil.getShortcutSummaryList(mContext, shortcutTypes)
    }

    /** Returns the user preferred shortcut types or the default shortcut types if not set */
    @UserShortcutType
    fun getUserPreferredShortcutTypes(componentName: ComponentName): Int {
        return PreferredShortcuts.retrieveUserShortcutType(
            mContext,
            getComponentNameAsString(),
            getDefaultShortcutTypes(),
        )
    }

    @UserShortcutType protected open fun getDefaultShortcutTypes(): Int = SOFTWARE

    protected fun updateShortcutPreferenceData() {
        if (componentName == null) {
            return
        }

        val shortcutTypes =
            AccessibilityUtil.getUserShortcutTypesFromSettings(mContext, componentName!!)
        if (shortcutTypes != UserShortcutType.DEFAULT) {
            val shortcut = PreferredShortcut(getComponentNameAsString(), shortcutTypes)
            PreferredShortcuts.saveUserShortcutType(mContext, shortcut)
        }
    }

    fun getContentObserverForTesting(): ContentObserver = settingsContentObserver

    fun showShortcutsTutorial(shortcutTypes: Int) {
        fragmentManager?.run {
            AccessibilityShortcutsTutorial.DialogFragment.showDialog(
                this,
                shortcutTypes,
                featureName!!,
                mContext.isInSetupWizard(),
            )
        }
    }

    fun showEditShortcutsScreen(screenTitle: CharSequence) {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
            mContext,
            sourceMetricsCategory,
            screenTitle,
            componentName!!,
            (mContext as? Activity)?.intent,
        )
    }
}
