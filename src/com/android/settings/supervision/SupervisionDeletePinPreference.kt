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
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_DELETE_PIN
import android.app.settings.SettingsEnums.SUPERVISION_MANAGE_PIN_SCREEN
import android.app.supervision.SupervisionManager
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.network.getActivity
import com.android.settingslib.HelpUtils
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * delete the device PIN.
 */
class SupervisionDeletePinPreference() :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private lateinit var confirmPinLauncher: ActivityResultLauncher<Intent>

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_delete_pin_preference_title

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context

        confirmPinLauncher =
            context.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
                onPinConfirmed(result.resultCode)
            }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        FeatureFactory.featureFactory.metricsFeatureProvider.clicked(
            SUPERVISION_MANAGE_PIN_SCREEN,
            KEY,
        )
        showDeletionDialog(preference.context)
        return true
    }

    @VisibleForTesting
    fun showDeletionDialog(context: Context) {
        val builder = AlertDialog.Builder(context)

        val supervisionManager = context.getSystemService(SupervisionManager::class.java)
        val userManager = context.getSystemService(UserManager::class.java)

        if (supervisionManager == null || userManager == null) {
            // TODO(b/415995161): Improve error handling
            builder
                .setTitle(R.string.supervision_delete_pin_error_header)
                .setMessage(R.string.supervision_delete_pin_error_message)
                .setPositiveButton(R.string.okay, null)
        } else if (context.areAnyUsersExceptCurrentSupervised(supervisionManager, userManager)) {
            builder
                .setTitle(R.string.supervision_delete_pin_supervision_enabled_header)
                .setMessage(R.string.supervision_delete_pin_supervision_enabled_message)
                .setPositiveButton(R.string.okay, null)
                .setNegativeButton(R.string.learn_more, { _, _ -> onLearnMore() })
        } else {
            builder
                .setTitle(R.string.supervision_delete_pin_confirm_header)
                .setMessage(R.string.supervision_delete_pin_confirm_message)
                .setPositiveButton(R.string.delete) { _, _ -> onConfirmDeleteClick() }
                .setNegativeButton(R.string.cancel, null)
        }
        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    @VisibleForTesting
    fun onLearnMore() {
        val intent =
            HelpUtils.getHelpIntent(
                lifeCycleContext,
                lifeCycleContext.getString(R.string.supervision_pin_learn_more_link),
                lifeCycleContext::class.java.name,
            )
        if (intent != null) {
            lifeCycleContext.startActivity(intent)
        } else {
            Log.w(TAG, "HelpIntent is null")
        }
    }

    private fun showErrorDialog(context: Context) {
        // TODO(b/415995161): Improve error handling
        AlertDialog.Builder(context)
            .setTitle(R.string.supervision_delete_pin_error_header)
            .setMessage(R.string.supervision_delete_pin_error_message)
            .setPositiveButton(R.string.okay, null)
            .create()
            .show()
    }

    @VisibleForTesting
    fun onConfirmDeleteClick() {
        val intent =
            Intent(lifeCycleContext, ConfirmSupervisionCredentialsActivity::class.java).apply {
                putExtra(ConfirmSupervisionCredentialsActivity.EXTRA_FORCE_CONFIRMATION, true)
            }
        confirmPinLauncher.launch(intent)
    }

    private fun onPinConfirmed(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            if (lifeCycleContext.deleteSupervisionData()) {
                lifeCycleContext.notifyPreferenceChange(KEY)
                FeatureFactory.featureFactory.metricsFeatureProvider.action(
                    lifeCycleContext,
                    ACTION_SUPERVISION_DELETE_PIN,
                )
                // Programmatically trigger back press to properly return to the supervision
                // dashboard with a correct back stack.
                val activity =
                    (lifeCycleContext.baseContext.getActivity()
                        as? androidx.activity.ComponentActivity)
                activity?.onBackPressedDispatcher?.onBackPressed()
            } else {
                Log.e(TAG, "Can't delete supervision data; unable to delete supervising profile.")
                showErrorDialog(lifeCycleContext)
            }
        }
    }

    companion object {
        const val KEY = "supervision_delete_pin"
    }
}
