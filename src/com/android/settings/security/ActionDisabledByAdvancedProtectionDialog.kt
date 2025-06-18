/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.security

import android.content.Intent
import android.content.pm.PackageManager
import android.security.advancedprotection.AdvancedProtectionManager
import android.security.advancedprotection.AdvancedProtectionManager.EXTRA_SUPPORT_DIALOG_FEATURE
import android.security.advancedprotection.AdvancedProtectionManager.EXTRA_SUPPORT_DIALOG_TYPE
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_CELLULAR_2G
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_WEP
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_ENABLE_MTE
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_DISABLED_SETTING
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_UNKNOWN
import android.util.Log
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.android.settings.R
import com.android.settingslib.spa.SpaDialogWindowTypeActivity
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.SettingsAlertDialogContent
import com.android.settingslib.wifi.WifiUtils.Companion.DIALOG_WINDOW_TYPE

class ActionDisabledByAdvancedProtectionDialog : SpaDialogWindowTypeActivity() {

    @Composable
    override fun Content() {
        SettingsAlertDialogContent(
            confirmButton = AlertDialogButton(getString(R.string.okay)) {
                finish()
                logDialogShown(learnMoreClicked = false)
            },
            dismissButton = getSupportButtonIfExists(),
            title = getString(R.string.disabled_by_advanced_protection_title),
            icon = {
                Icon(
                    painter = painterResource(R.drawable.ic_settings_safety_center),
                    contentDescription = null
                )
            },
            text = { Text(getDialogMessage()) })
    }

    private fun getDialogMessage(): String {
        val featureId = getIntentFeatureId()
        val type = getIntentDialogueType()
        val messageId = when (type) {
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING -> {
                if (featureIdsWithSettingOn.contains(featureId)) {
                    R.string.disabled_by_advanced_protection_setting_is_on_message
                } else if (featureIdsWithSettingOff.contains(featureId)) {
                    R.string.disabled_by_advanced_protection_setting_is_off_message
                } else {
                    defaultMessageId
                }
            }
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION -> {
                if (featureId == FEATURE_ID_DISALLOW_WEP) {
                    R.string.disabled_by_advanced_protection_wep_action_message
                } else {
                    R.string.disabled_by_advanced_protection_action_message
                }
            }
            else -> defaultMessageId
        }
        return getString(messageId)
    }

    @VisibleForTesting
    fun getSupportButtonIfExists(): AlertDialogButton? {
        try {
            val helpIntentUri = getString(
                com.android.internal.R.string.config_help_url_action_disabled_by_advanced_protection
            )
            val helpIntent = Intent.parseUri(helpIntentUri, Intent.URI_INTENT_SCHEME)
            if (helpIntent == null) return null
            helpIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val helpActivityInfo = packageManager.resolveActivity(helpIntent, /* flags */ 0)
                ?.activityInfo
            if (helpActivityInfo == null) return null
            return AlertDialogButton(
                getString(R.string.disabled_by_advanced_protection_help_button_title)
            ) {
                startActivity(helpIntent)
                finish()
                logDialogShown(learnMoreClicked = true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tried to set up help button, but this exception was thrown: ${e.message}")
        }
        return null
    }

    private fun logDialogShown(learnMoreClicked: Boolean) {
        // We should always have this permission, but just in case we don't, we should not log.
        if (checkSelfPermission(android.Manifest.permission.MANAGE_ADVANCED_PROTECTION_MODE)
                != PackageManager.PERMISSION_GRANTED) {
            return
        }

        this.getSystemService(AdvancedProtectionManager::class.java)
                .logDialogShown(getIntentFeatureId(), getIntentDialogueType(), learnMoreClicked)
    }

    override fun getDialogWindowType(): Int? = if (intent.hasExtra(DIALOG_WINDOW_TYPE)) {
        intent.getIntExtra(DIALOG_WINDOW_TYPE, WindowManager.LayoutParams.TYPE_APPLICATION)
    } else null

    private fun getIntentFeatureId(): Int {
        return intent.getIntExtra(EXTRA_SUPPORT_DIALOG_FEATURE, -1)
    }

    private fun getIntentDialogueType(): Int {
        return intent.getIntExtra(EXTRA_SUPPORT_DIALOG_TYPE, SUPPORT_DIALOG_TYPE_UNKNOWN)
    }

    private companion object {
        const val TAG = "AdvancedProtectionDlg"
        val defaultMessageId = R.string.disabled_by_advanced_protection_action_message
        val featureIdsWithSettingOn = setOf(FEATURE_ID_DISALLOW_CELLULAR_2G, FEATURE_ID_ENABLE_MTE)
        val featureIdsWithSettingOff =
            setOf(FEATURE_ID_DISALLOW_WEP, FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES)
    }
}
