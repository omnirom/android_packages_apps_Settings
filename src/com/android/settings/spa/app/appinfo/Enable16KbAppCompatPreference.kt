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

package com.android.settings.spa.app.appinfo

import android.app.AlertDialog
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ApplicationInfo
import android.os.UserManager
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settings.development.Enable16kUtils
import com.android.settingslib.RestrictedLockUtils
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.flow.flow

@Composable
fun Enable16KbAppCompatPreference(
    app: ApplicationInfo,
    packageInfoPresenter: PackageInfoPresenter
) {
    val context = LocalContext.current
    val presenter = remember(app) { Enable16KbAppCompatSwitchPresenter(context, app, packageInfoPresenter) }
    if (!presenter.isAvailable()) return

    val isCheckedState = presenter.isCheckedFlow.collectAsStateWithLifecycle(initialValue = null)
    SwitchPreference(remember {
        object : SwitchPreferenceModel {
            override val title =
                context.getString(R.string.enable_16k_app_compat_title)

            override val summary = {
                context.getString(R.string.enable_16k_app_compat_details)
            }

            override val checked = {
                isCheckedState.value
            }

            override val onCheckedChange = presenter::onCheckedChange
        }
    })
}


private class Enable16KbAppCompatSwitchPresenter(private val context: Context, private val app: ApplicationInfo,
                                                 private val packageInfoPresenter: PackageInfoPresenter) {
    private val packageManager = context.packageManager
    fun isAvailable(): Boolean {
        return Enable16kUtils.isUsing16kbPages()
    }

    private val isChecked = OverridableFlow(flow {
        emit(packageManager.isPageSizeCompatEnabled(app.packageName))
    })

    val isCheckedFlow = isChecked.flow

    fun onCheckedChange(newChecked: Boolean) {
        try {
            getForceStopRestriction(app)?.let { admin ->
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(context, admin)
                return
            }
            showDialog(newChecked)
        } catch (e: RuntimeException) {
            Log.e("Enable16KbAppCompat", "Failed to set" +
                    "setPageSizeAppCompatModeSettingsOverride", e);
        }
    }

    fun updatePageSizeCompat(newChecked: Boolean) {
        packageManager.setPageSizeAppCompatFlagsSettingsOverride(app.packageName, newChecked)
        isChecked.override(newChecked)
        packageInfoPresenter.stopPackage()
    }

    fun getForceStopRestriction(app: ApplicationInfo): RestrictedLockUtils.EnforcedAdmin? = when {
        packageManager.isPackageStateProtected(app.packageName, app.userId) -> {
            RestrictedLockUtilsInternal.getDeviceOwner(context) ?: RestrictedLockUtils.EnforcedAdmin()
        }

        else -> RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                context, UserManager.DISALLOW_APPS_CONTROL, app.userId
        )
    }

    fun showDialog(newChecked: Boolean) {
        // Uses the same string from 'Force Stop' action button.
        val builder =
                AlertDialog.Builder(context)
                        .setTitle(R.string.stop_app_dlg_title)
                        .setMessage(R.string.stop_app_dlg_text)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.okay,  DialogInterface.OnClickListener { dialog, _ ->
                                updatePageSizeCompat(newChecked)
                                dialog.dismiss()
                            })
                        .create();
        builder.show();
    }
}
