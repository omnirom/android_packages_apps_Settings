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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_UPDATE_RECOVERY
import android.app.supervision.SupervisionManager
import android.app.supervision.SupervisionRecoveryInfo.STATE_VERIFIED
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * update the PIN recovery email.
 */
class SupervisionUpdateRecoveryEmailPreference :
    PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceActionMetricsProvider,
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    override val key: String
        get() = KEY

    override val preferenceActionMetrics: Int
        get() = ACTION_SUPERVISION_UPDATE_RECOVERY

    override val title: Int
        get() = R.string.supervision_update_recovery_email_preference_title

    override fun dependencies(context: Context) = arrayOf(SupervisionSetupRecoveryPreference.KEY)

    override fun getSummary(context: Context): CharSequence? {
        val email =
            context
                .getSystemService(SupervisionManager::class.java)
                ?.getSupervisionRecoveryInfo()
                ?.accountName
                ?.asMaskedEmail()
        if (email == null) {
            return null
        }
        val spannableEmail = SpannableString.valueOf("$email %s")
        val indexIconStart = spannableEmail.indexOf("%s")
        val indexIconEnd = indexIconStart + 2
        val iconView = ImageView(context)
        iconView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check_24dp))
        val icon: Drawable = iconView.drawable!!.mutate()
        val tintColorList =
            ContextCompat.getColorStateList(
                context,
                com.android.settingslib.widget.theme.R.color
                    .settingslib_materialColorOnSurfaceVariant,
            )
        icon.setTintList(tintColorList)
        val imageSpan = ImageSpan(icon)
        imageSpan.contentDescription = ""
        icon.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        spannableEmail.setSpan(
            imageSpan,
            indexIconStart,
            indexIconEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return spannableEmail
    }

    override fun isAvailable(context: Context): Boolean {
        if (!Flags.enableSupervisionPinRecoveryScreen()) {
            return false
        }
        return context
            .getSystemService(SupervisionManager::class.java)
            ?.getSupervisionRecoveryInfo()
            ?.state == STATE_VERIFIED
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onActivityResult(
        context: PreferenceLifecycleContext,
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ): Boolean {
        if (requestCode != REQUEST_CODE_UPDATE_RECOVERY) {
            return false
        }
        if (resultCode == Activity.RESULT_OK) {
            context.notifyPreferenceChange(KEY)
        }
        return true
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val intent =
            Intent(lifeCycleContext, SupervisionPinRecoveryActivity::class.java)
                .setAction(SupervisionPinRecoveryActivity.ACTION_UPDATE)
        lifeCycleContext.startActivityForResult(intent, REQUEST_CODE_UPDATE_RECOVERY, null)
        return true
    }

    companion object {
        const val KEY = "supervision_update_recovery_email"
        const val REQUEST_CODE_UPDATE_RECOVERY = 2

        fun String.asMaskedEmail(): String? {
            val atIndex = this.indexOf('@')
            if (atIndex <= 0) {
                return null // Invalid email format
            }
            val username = this.substring(0, atIndex)
            val domain = this.substring(atIndex)

            return if (username.length <= 1) {
                username + domain
            } else if (username.length == 2) {
                "${username.first()}•$domain"
            } else {
                "${username.first()}" +
                    "•".repeat(username.length - 2) +
                    "${username.last()}$domain"
            }
        }
    }
}
