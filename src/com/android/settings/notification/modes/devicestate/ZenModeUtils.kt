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

package com.android.settings.notification.modes.devicestate

import android.app.AutomaticZenRule.TYPE_BEDTIME
import android.content.Context
import android.service.notification.ZenModeConfig.MANUAL_RULE_ID
import com.android.settings.R
import com.android.settings.Utils.createAccessibleSequence
import com.android.settingslib.notification.modes.ZenMode
import com.android.settingslib.notification.modes.ZenModeDescriptions
import com.android.settingslib.notification.modes.ZenModesBackend

internal fun Context.getBackend() = ZenModesBackend.getInstance(this)

internal fun Context.hasDndMode() = (getBackend().getMode(MANUAL_RULE_ID) != null)

internal fun Context.getDndMode(): ZenMode? = getBackend().getMode(MANUAL_RULE_ID)

internal fun Context.hasBedtimeMode() = getBackend().modes.any { it.type == TYPE_BEDTIME }

internal fun Context.getBedtimeMode(): ZenMode? =
    getBackend().modes.find { it.type == TYPE_BEDTIME }

internal fun Context.getZenModeScreenSummary(zenMode: ZenMode?): String {
    if (zenMode == null) return ""
    val status = zenMode.status
    val descriptions = ZenModeDescriptions(this)
    val statusText = getStatusText(status, descriptions.getTriggerDescription(zenMode))
    val triggerDescriptionForA11y = descriptions.getTriggerDescriptionForAccessibility(zenMode)
    return if (triggerDescriptionForA11y != null) {
        createAccessibleSequence(statusText, getStatusText(status, triggerDescriptionForA11y))
            .toString()
    } else {
        statusText
    }
}

private fun Context.getStatusText(status: ZenMode.Status, triggerDescription: String?): String =
    when (status) {
        ZenMode.Status.ENABLED_AND_ACTIVE ->
            if (triggerDescription.isNullOrEmpty()) {
                getString(R.string.zen_mode_active_text)
            } else {
                getString(
                    R.string.zen_mode_format_status_and_trigger,
                    getString(R.string.zen_mode_active_text),
                    triggerDescription,
                )
            }
        ZenMode.Status.ENABLED -> triggerDescription ?: ""
        ZenMode.Status.DISABLED_BY_USER -> getString(R.string.zen_mode_disabled_by_user)
        ZenMode.Status.DISABLED_BY_OTHER -> getString(R.string.zen_mode_disabled_needs_setup)
    }
