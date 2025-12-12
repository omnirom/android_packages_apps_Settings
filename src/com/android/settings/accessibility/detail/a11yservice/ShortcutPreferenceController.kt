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

package com.android.settings.accessibility.detail.a11yservice

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.ToggleShortcutPreferenceController
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.isServiceWarningRequired
import com.android.settings.accessibility.extensions.targetSdkIsAtLeast
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment.Companion.RESULT_STATUS_ALLOW
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory

class ShortcutPreferenceController(context: Context, prefKey: String) :
    ToggleShortcutPreferenceController(context, prefKey) {
    private var shortcutTitle: CharSequence? = null
    private var serviceInfo: AccessibilityServiceInfo? = null

    fun initialize(
        serviceInfo: AccessibilityServiceInfo,
        fragmentManager: FragmentManager,
        featureName: CharSequence,
        sourceMetricsCategory: Int,
    ) {
        super.initialize(
            serviceInfo.componentName,
            fragmentManager,
            featureName,
            sourceMetricsCategory,
        )
        this.serviceInfo = serviceInfo
        shortcutTitle =
            mContext.getString(
                R.string.accessibility_shortcut_title,
                serviceInfo.getFeatureName(mContext),
            )
        setAllowedPreferredShortcutType()
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        shortcutTitle?.let { screen?.findPreference<Preference>(preferenceKey)?.title = it }
        shortcutPreference?.isSettingsEditable =
            serviceInfo?.targetSdkIsAtLeast(Build.VERSION_CODES.R) == true
    }

    override fun getDefaultShortcutTypes(): Int {
        val hasQsTile = serviceInfo?.tileServiceName?.isNotEmpty() == true
        val isAccessibilityTool = serviceInfo?.isAccessibilityTool == true
        return if (serviceInfo?.targetSdkIsAtLeast(Build.VERSION_CODES.R) == true) {
            if (isAccessibilityTool && hasQsTile) {
                QUICK_SETTINGS
            } else {
                super.getDefaultShortcutTypes()
            }
        } else {
            HARDWARE
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        fragmentManager?.setFragmentResultListener(
            /* requestKey= */ SERVICE_WARNING_DIALOG_REQUEST_CODE,
            /* lifecycleOwner= */ owner,
        ) { requestKey, result ->
            if (requestKey == SERVICE_WARNING_DIALOG_REQUEST_CODE) {
                val allow: Boolean =
                    AccessibilityServiceWarningDialogFragment.getResultStatus(result) ==
                        RESULT_STATUS_ALLOW
                val dialogEnum =
                    AccessibilityServiceWarningDialogFragment.getResultDialogContext(result)
                handleServiceWarningResponse(allow, dialogEnum)
            }
        }
    }

    override fun onToggleClicked(preference: ShortcutPreference) {
        val isChecked = preference.isChecked
        if (!isChecked) {
            setChecked(preference, checked = false)
        } else {
            val serviceWarningRequired =
                serviceInfo?.isServiceWarningRequired(preference.context) != false
            if (serviceWarningRequired) {
                showServiceWarning(ENABLE_WARNING_FROM_SHORTCUT_TOGGLE)
            } else {
                handleServiceWarningResponse(
                    allow = true,
                    dialogEnum = ENABLE_WARNING_FROM_SHORTCUT_TOGGLE,
                )
            }
        }
    }

    override fun onSettingsClicked(preference: ShortcutPreference) {
        val serviceWarningRequired =
            serviceInfo?.isServiceWarningRequired(preference.context) != false
        if (serviceWarningRequired) {
            showServiceWarning(ENABLE_WARNING_FROM_SHORTCUT)
        } else {
            handleServiceWarningResponse(allow = true, dialogEnum = ENABLE_WARNING_FROM_SHORTCUT)
        }

        // log here since calling super.onPreferenceTreeClick will be skipped
        featureFactory.metricsFeatureProvider.logClickedPreference(
            preference,
            sourceMetricsCategory,
        )
    }

    private fun setAllowedPreferredShortcutType() {
        serviceInfo?.let {
            if (it.targetSdkIsAtLeast(Build.VERSION_CODES.R) != true) {
                PreferredShortcuts.saveUserShortcutType(
                    mContext,
                    PreferredShortcut(it.componentName.flattenToString(), HARDWARE),
                )
            }
        }
    }

    private fun showServiceWarning(dialogEnum: Int) {
        AccessibilityServiceWarningDialogFragment.showDialog(
            fragmentManager = requireNotNull(fragmentManager),
            componentName = requireNotNull(serviceInfo).componentName,
            source = dialogEnum,
            requestKey = SERVICE_WARNING_DIALOG_REQUEST_CODE,
        )
    }

    private fun handleServiceWarningResponse(allow: Boolean, dialogEnum: Int) {
        if (allow) {
            when (dialogEnum) {
                ENABLE_WARNING_FROM_SHORTCUT -> {
                    showEditShortcutsScreen(shortcutPreference?.title ?: "")
                }

                ENABLE_WARNING_FROM_SHORTCUT_TOGGLE -> {
                    shortcutPreference?.run {
                        setChecked(this, checked = true)
                        componentName?.let {
                            showShortcutsTutorial(getUserPreferredShortcutTypes(it))
                        }
                    }
                }
            }
        } else {
            shortcutPreference?.run { setChecked(this, checked = false) }
        }
    }

    companion object {
        private const val SERVICE_WARNING_DIALOG_REQUEST_CODE =
            "serviceWarningRequestFromA11yServiceShortcut"
    }
}
