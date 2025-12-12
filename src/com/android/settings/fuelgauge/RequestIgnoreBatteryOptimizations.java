/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.fuelgauge;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

import static com.android.settings.fuelgauge.BatteryOptimizeUtils.MODE_UNRESTRICTED;

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SettingsThemeHelper;

public class RequestIgnoreBatteryOptimizations extends FragmentActivity
        implements DialogInterface.OnClickListener {
    private static final String TAG = "RequestIgnoreBatteryOptimizations";
    private static final boolean DEBUG = false;

    @VisibleForTesting static BatteryOptimizeUtils sTestBatteryOptimizeUtils = null;

    @VisibleForTesting ApplicationInfo mApplicationInfo;
    @VisibleForTesting MetricsFeatureProvider mMetricsFeatureProvider;

    /** Request status of ignore battery optimizations dialog. */
    @VisibleForTesting
    enum RequestStatus {
        UNKNOWN(0),
        ALREADY_PROMPTED(1),
        NO_PERMISSION(2),
        PACKAGE_NOT_EXIST(3);

        public final int value;

        RequestStatus(int value) {
            this.value = value;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        getWindow()
                .addSystemFlags(
                        android.view.WindowManager.LayoutParams
                                .SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        if (SettingsThemeHelper.isExpressiveTheme(this)) {
            setTheme(R.style.Transparent_Expressive);
        }

        Uri data = getIntent().getData();
        final String packageName = data == null ? null : data.getSchemeSpecificPart();
        if (TextUtils.isEmpty(packageName)) {
            debugLog(
                    "No data supplied for IGNORE_BATTERY_OPTIMIZATION_SETTINGS in: " + getIntent());
            logDialogAction(
                    SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                    RequestStatus.PACKAGE_NOT_EXIST.value);
            finish();
            return;
        }

        // Package in Unrestricted mode already ignoring the battery optimizations.
        PowerManager power = getSystemService(PowerManager.class);
        if (power.isIgnoringBatteryOptimizations(packageName)) {
            debugLog("Not should prompt, already ignoring optimizations: " + packageName);
            logDialogAction(
                    SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                    RequestStatus.ALREADY_PROMPTED.value);
            finish();
            return;
        }

        if (getPackageManager()
                        .checkPermission(
                                Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                packageName)
                != PackageManager.PERMISSION_GRANTED) {
            debugLog(
                    "Requested package "
                            + packageName
                            + " does not hold permission "
                            + Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            logDialogAction(
                    SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                    RequestStatus.NO_PERMISSION.value);
            finish();
            return;
        }

        try {
            mApplicationInfo = getPackageManager().getApplicationInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            debugLog("Requested package doesn't exist: " + packageName);
            logDialogAction(
                    SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_FAIL_SHOW,
                    RequestStatus.PACKAGE_NOT_EXIST.value);
            finish();
            return;
        }

        final CharSequence appLabel =
                mApplicationInfo.loadSafeLabel(
                        getPackageManager(),
                        PackageItemInfo.DEFAULT_MAX_LABEL_SIZE_PX,
                        PackageItemInfo.SAFE_LABEL_FLAG_TRIM
                                | PackageItemInfo.SAFE_LABEL_FLAG_FIRST_LINE);
        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setTitle(R.string.high_power_prompt_title)
                        .setMessage(getString(R.string.high_power_prompt_body, appLabel))
                        .setPositiveButton(R.string.allow, this)
                        .setNegativeButton(R.string.deny, this)
                        .setOnDismissListener(this::onDismissDialog)
                        .create();
        logDialogAction(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_SHOW, mApplicationInfo.uid);
        dialog.show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                BatteryOptimizeUtils batteryOptimizeUtils =
                        sTestBatteryOptimizeUtils != null
                                ? sTestBatteryOptimizeUtils
                                : new BatteryOptimizeUtils(
                                        getApplicationContext(),
                                        mApplicationInfo.uid,
                                        mApplicationInfo.packageName);
                batteryOptimizeUtils.setAppUsageState(
                        MODE_UNRESTRICTED, Action.APPLY, /* forceMode= */ true);
                logDialogAction(
                        SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_ALLOW,
                        mApplicationInfo.uid);
                dialog.dismiss();
                break;
            case BUTTON_NEGATIVE:
                logDialogAction(
                        SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_DENY,
                        mApplicationInfo.uid);
                dialog.dismiss();
                break;
        }
    }

    private void onDismissDialog(DialogInterface dialog) {
        logDialogAction(
                SettingsEnums.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZE_DISMISS, mApplicationInfo.uid);
        finish();
    }

    private void logDialogAction(int action, int value) {
        mMetricsFeatureProvider.action(
                /* attribution= */ SettingsEnums.DIALOG_REQUEST_IGNORE_BATTERY_OPTIMIZE,
                /* action= */ action,
                /* pageId= */ SettingsEnums.DIALOG_REQUEST_IGNORE_BATTERY_OPTIMIZE,
                /* key= */ TAG,
                /* value= */ value);
    }

    private static void debugLog(String debugContent) {
        if (DEBUG) Log.w(TAG, debugContent);
    }
}
