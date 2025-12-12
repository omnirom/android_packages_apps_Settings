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

package com.android.settings.network.telephony;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.safetycenter.SafetyCenterManager;
import android.safetycenter.SafetySourceData;
import android.safetycenter.SafetySourceIssue;
import android.safetycenter.SafetySourceIssue.Action;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.widget.BannerMessagePreference;
import com.android.settingslib.widget.BannerMessagePreference.AttentionLevel;

/**
 * This controller for Cellular Security settings Screen, adds Cellular Security alerts for
 * Device ID Disclosure and Network encryption/unencryption to the screen.
 */
public class AdaptiveNetworkPreferenceController {
    private static final String TAG = "AdaptiveNetworkPreferenceController";
    private static final String CELLULAR_SECURITY_SAFETY_SOURCE_ID =
            "AndroidCellularNetworkSecurity";
    private static final String ACTION_LEARN_MORE_ID = "learn_more";
    private final Context mContext;
    private @Nullable TelephonyManager mTelephonyManager;

    /**
     * Class constructor of "Cellular Security banner" preference.
     *
     * @param context of settings
     */
    public AdaptiveNetworkPreferenceController(@NonNull Context context) {
        mContext = context;
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    /**
     * Adds the controlled preference to the provided preference screen.
     */
    public void addToScreen(@NonNull PreferenceScreen screen) {
        if (!areNotificationsEnabled()) {
            // Notification are not enabled for cellular security.
            return;
        }
        if (!isSafetyCenterSupported()) {
            // SafetyCenter is not supported on the device.
            return;
        }
        SafetyCenterManager safetyCenterManager = mContext.getSystemService(
                SafetyCenterManager.class);
        SafetySourceData data = safetyCenterManager.getSafetySourceData(
                CELLULAR_SECURITY_SAFETY_SOURCE_ID);
        if (data != null && !data.getIssues().isEmpty()) {
            for (SafetySourceIssue safetySourceIssue : data.getIssues()) {
                screen.addPreference(getBannerpreference(safetySourceIssue));
            }
        }
    }

    private BannerMessagePreference getBannerpreference(SafetySourceIssue safetySourceIssue) {
        BannerMessagePreference preference = new BannerMessagePreference(mContext);
        preference.setOrder(1);
        preference.setTitle(safetySourceIssue.getTitle());
        preference.setSummary(safetySourceIssue.getSummary());
        preference.setAttentionLevel(getBannerAttentionLevel(safetySourceIssue.getSeverityLevel()));
        if (SafetySourceData.SEVERITY_LEVEL_INFORMATION == safetySourceIssue.getSeverityLevel()) {
            preference.setIcon(R.drawable.ic_info_selector);
        }
        for (Action action : safetySourceIssue.getActions()) {
            if (ACTION_LEARN_MORE_ID.equals(action.getId())) {
                preference.setNegativeButtonText(action.getLabel());
                preference.setNegativeButtonOnClickListener(p ->
                        openLearnMoreLink(action.getPendingIntent()));
            }
        }
        return preference;
    }

    @VisibleForTesting
    protected boolean areNotificationsEnabled() {
        if (mTelephonyManager == null) {
            Log.w(TAG, "Telephony manager not yet initialized");
            return false;
        }
        try {
            return mTelephonyManager.isNullCipherNotificationsEnabled()
                    && mTelephonyManager.isCellularIdentifierDisclosureNotificationsEnabled();
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "Failed UnsupportedOperationException for isNullCipherNotificationsEnabled "
                    + "or isCellularIdentifierDisclosureNotificationsEnabled." + e);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed UnsupportedOperationException for isNullCipherNotificationsEnabled "
                    + "or isCellularIdentifierDisclosureNotificationsEnabled." + e);
        }
        return false;
    }

    @VisibleForTesting
    protected boolean isSafetyCenterSupported() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false;
        }
        SafetyCenterManager safetyCenterManager = mContext.getSystemService(
                SafetyCenterManager.class);
        if (safetyCenterManager == null) {
            return false;
        }
        return safetyCenterManager.isSafetyCenterEnabled();
    }

    private void openLearnMoreLink(PendingIntent intent) {
        if (intent != null) {
            mContext.startActivity(intent.getIntent());
        } else {
            Log.w(TAG, "LearnMoreIntent is null");
        }
    }

    private AttentionLevel getBannerAttentionLevel(int severityLevel) {
        switch (severityLevel) {
            case SafetySourceData.SEVERITY_LEVEL_INFORMATION:
                return AttentionLevel.LOW;
            case SafetySourceData.SEVERITY_LEVEL_RECOMMENDATION:
                return AttentionLevel.MEDIUM;
            case SafetySourceData.SEVERITY_LEVEL_CRITICAL_WARNING:
                return AttentionLevel.HIGH;
            default:
                return AttentionLevel.NORMAL;
        }
    }
}
