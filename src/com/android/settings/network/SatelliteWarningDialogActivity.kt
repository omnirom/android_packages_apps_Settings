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

package com.android.settings.network

import android.os.Bundle
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.android.settings.R
import com.android.settingslib.spa.SpaDialogWindowTypeActivity
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.SettingsAlertDialogContent
import com.android.settingslib.wifi.WifiUtils

/** A dialog to show the warning message when device is under satellite mode. */
class SatelliteWarningDialogActivity : SpaDialogWindowTypeActivity() {
    private var warningType = TYPE_IS_UNKNOWN
    private var customizedContent: HashMap<Int, String> = HashMap<Int, String>()
    private var isCustomizedContent = false
    override fun onCreate(savedInstanceState: Bundle?) {
        isCustomizedContent = intent.hasExtra(EXTRA_TYPE_OF_SATELLITE_CUSTOMIZED_CONTENT)
        if (isCustomizedContent) {
            customizedContent =
                intent.getSerializableExtra(EXTRA_TYPE_OF_SATELLITE_CUSTOMIZED_CONTENT)
                        as HashMap<Int, String>
        } else {
            warningType =
                intent.getIntExtra(EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG, TYPE_IS_UNKNOWN)
            if (warningType == TYPE_IS_UNKNOWN) {
                finish()
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun getDialogWindowType(): Int {
        return intent.getIntExtra(
            WifiUtils.DIALOG_WINDOW_TYPE,
            WindowManager.LayoutParams.LAST_APPLICATION_WINDOW
        )
    }

    fun getBodyString(): String {
        if (isCustomizedContent) {
            // For customized content
            return customizedContent.get(CUSTOM_CONTENT_DESCRIPTION).toString()
        } else {
            // For wifi, bluetooth, airplane mode
            return String.format(
                getString(R.string.satellite_warning_dialog_content),
                getTypeString(warningType)
            )
        }
    }

    fun getButtonString(): String {
        if (isCustomizedContent)
            // For customized content
            return customizedContent.get(CUSTOM_CONTENT_BUTTON_NAME).toString()
        else
            // For wifi, bluetooth, airplane mode
            return getString(com.android.settingslib.R.string.okay)
    }

    fun getTitleString(): String {
        if (isCustomizedContent)
            // For customized content
            return customizedContent.get(CUSTOM_CONTENT_TITLE).toString()
        else
            // For wifi, bluetooth, airplane mode
            return String.format(
                getString(R.string.satellite_warning_dialog_title),
                getTypeString(warningType)
            )

    }

    @Composable
    override fun Content() {
        SettingsAlertDialogContent(
            dismissButton = null,
            confirmButton = AlertDialogButton(
                getButtonString()
            ) { finish() },
            title = getTitleString(),
            text = {
                Text(
                    getBodyString(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            })
    }

    private fun getTypeString(num: Int): String {
        return when (num) {
            TYPE_IS_WIFI -> getString(R.string.wifi)
            TYPE_IS_BLUETOOTH -> getString(R.string.bluetooth)
            TYPE_IS_AIRPLANE_MODE -> getString(R.string.airplane_mode)
            else -> ""
        }
    }

    companion object {
        const val EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG: String =
            "extra_type_of_satellite_warning_dialog"
        const val EXTRA_TYPE_OF_SATELLITE_CUSTOMIZED_CONTENT: String =
            "extra_type_of_satellite_customized_content"
        const val TYPE_IS_UNKNOWN = -1
        const val TYPE_IS_WIFI = 0
        const val TYPE_IS_BLUETOOTH = 1
        const val TYPE_IS_AIRPLANE_MODE = 2
        const val CUSTOM_CONTENT_TITLE = 0
        const val CUSTOM_CONTENT_DESCRIPTION = 1
        const val CUSTOM_CONTENT_BUTTON_NAME = 2
    }
}