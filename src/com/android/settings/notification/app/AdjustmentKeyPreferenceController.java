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
package com.android.settings.notification.app;

import android.app.Flags;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.service.notification.Adjustment;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.core.SubSettingLauncher;
import com.android.settings.notification.BundlePreferenceFragment;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.SummarizationPreferenceFragment;
import com.android.settingslib.PrimarySwitchPreference;

/**
 * Used for the app-level preference screen to opt the app in or out of a provided Adjustment key.
 * E.g. to say an app can or cannot be classified by the NotificationAssistantService.
 */
public class AdjustmentKeyPreferenceController extends
        NotificationPreferenceController implements Preference.OnPreferenceChangeListener {
    private String mKey;

    public AdjustmentKeyPreferenceController(@NonNull Context context,
            @NonNull NotificationBackend backend, String key) {
        super(context, backend);
        mKey = key;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return mKey;
    }

    @Override
    public boolean isAvailable() {
        return mAppRow != null && isAvailable(mKey, mBackend, mAppRow.pkg, mAppRow.uid)
                && super.isAvailable();
    }

    static boolean isAvailable(String key, NotificationBackend backend, String pkg, int uid) {
        if (!(Flags.notificationClassificationUi() || Flags.nmSummarizationUi()
                || Flags.nmSummarization())) {
            return false;
        }
        boolean isBundlePref = Adjustment.KEY_TYPE.equals(key);
        boolean isSummarizePref = Adjustment.KEY_SUMMARIZATION.equals(key);
        if (!Flags.notificationClassificationUi() && isBundlePref) {
            return false;
        }
        if (!(Flags.nmSummarizationUi() || Flags.nmSummarization()) && isSummarizePref) {
            return false;
        }
        if (!isSummarizePref && !isBundlePref) {
            return false;
        }
        if (isSummarizePref && !(backend.hasSentValidMsg(pkg, uid)
                || backend.isInInvalidMsgState(pkg, uid))) {
            return false;
        }

        if (isSummarizePref && !backend.isNotificationSummarizationSupported()) {
            return false;
        }

        if (isBundlePref && !backend.isNotificationBundlingSupported()) {
            return false;
        }

        return backend.getAllowedAssistantAdjustments().contains(key);
    }

    @Override
    boolean isIncludedInFilter() {
        // not a channel-specific preference; only at the app level
        return false;
    }

    public void updateState(@NonNull Preference preference) {
        PrimarySwitchPreference pref = (PrimarySwitchPreference) preference;
        if (pref.getParent() != null) {
            pref.getParent().setVisible(true);
        }

        if (pref != null && mAppRow != null) {
            pref.setDisabledByAdmin(mAdmin);
            pref.setEnabled(!pref.isDisabledByAdmin());
            pref.setChecked(
                    mBackend.isAdjustmentSupportedForPackage(mAppRow.userId, mKey, mAppRow.pkg));
            pref.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, @NonNull Object newValue) {
        final boolean allowedForPkg = (Boolean) newValue;
        mBackend.setAdjustmentSupportedForPackage(mAppRow.userId, mKey, mAppRow.pkg, allowedForPkg);
        return true;
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        // only handle preference tree clicks for this controller's preference, as the dashboard
        // fragment will try all controllers to determine which one should handle the click
        if (!mKey.equals(preference.getKey())) {
            return false;
        }

        Class destination;
        if (Adjustment.KEY_TYPE.equals(mKey)) {
            destination = BundlePreferenceFragment.class;
        } else if (Adjustment.KEY_SUMMARIZATION.equals(mKey)) {
            destination = SummarizationPreferenceFragment.class;
        } else {
            // other keys not supported
            return false;
        }

        // Go to the settings page for this adjustment key type, noting that we came from the
        // notification app settings page
        new SubSettingLauncher(mContext)
                .setDestination(destination.getName())
                .setSourceMetricsCategory(SettingsEnums.NOTIFICATION_APP_NOTIFICATION).launch();
        return true;
    }
}
