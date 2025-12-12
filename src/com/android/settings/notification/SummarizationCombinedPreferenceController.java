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

package com.android.settings.notification;

import android.app.Flags;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

/**
 * Preference controller controlling both global and work profile specific summarization preferences
 */
public class SummarizationCombinedPreferenceController extends BasePreferenceController {
    static final String GLOBAL_KEY = "global_pref";
    static final String WORK_PREF_KEY = "work_profile_pref";
    static final String EXCLUDED_APPS_CATEGORY_KEY =
            "notification_summarization_excluded_apps_list";

    @NonNull NotificationBackend mBackend;
    private @Nullable UserHandle mManagedProfile;
    private @Nullable TwoStatePreference mGlobalPref;
    private @Nullable TwoStatePreference mWorkPref;
    private @Nullable PreferenceCategory mExcludedAppsPrefCategory;

    public SummarizationCombinedPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey, @NonNull NotificationBackend backend) {
        super(context, preferenceKey);
        mBackend = backend;

        // will be null if no profile is present or enabled
        mManagedProfile = Utils.getManagedProfile(UserManager.get(mContext));
    }

    private boolean hasManagedProfile() {
        return mManagedProfile != null;
    }

    private int managedProfileId() {
        return mManagedProfile != null ? mManagedProfile.getIdentifier() : UserHandle.USER_NULL;
    }

    @VisibleForTesting
    void setManagedProfile(UserHandle profile) {
        mManagedProfile = profile;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        if ((Flags.nmSummarization() || Flags.nmSummarizationUi())
                && mBackend.isNotificationSummarizationSupported()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        PreferenceCategory category = (PreferenceCategory) preference;

        mGlobalPref = category.findPreference(GLOBAL_KEY);
        if (mGlobalPref != null) {
            mGlobalPref.setOnPreferenceChangeListener(mGlobalPrefListener);
        }

        mWorkPref = category.findPreference(WORK_PREF_KEY);
        if (mWorkPref != null) {
            mWorkPref.setVisible(hasManagedProfile());
            mWorkPref.setOnPreferenceChangeListener(mWorkPrefListener);
        }

        mExcludedAppsPrefCategory = category.findPreference(EXCLUDED_APPS_CATEGORY_KEY);

        updatePrefValues();
    }

    void updatePrefValues() {
        boolean isMainEnabled = mBackend.isNotificationSummarizationEnabled(mContext.getUserId());

        if (mGlobalPref != null) {
            mGlobalPref.setChecked(isMainEnabled);
        }

        if (mWorkPref != null && hasManagedProfile()) {
            mWorkPref.setVisible(isMainEnabled);
            if (isMainEnabled) {
                mWorkPref.setChecked(
                        mBackend.isNotificationSummarizationEnabled(managedProfileId()));
            }
        }

        // if global switch is off hide the whole category
        if (mExcludedAppsPrefCategory != null) {
            mExcludedAppsPrefCategory.setVisible(isMainEnabled);
        }
    }

    private Preference.OnPreferenceChangeListener mGlobalPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        mBackend.setNotificationSummarizationEnabled(mContext.getUserId(), checked);
        // update state to hide or show work switch if needed
        updatePrefValues();
        return true;
    };

    private Preference.OnPreferenceChangeListener mWorkPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        if (hasManagedProfile()) {
            mBackend.setNotificationSummarizationEnabled(managedProfileId(), checked);
        }
        return true;
    };
}
