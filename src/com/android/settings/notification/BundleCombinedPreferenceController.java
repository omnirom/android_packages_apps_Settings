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

import static android.provider.Settings.Secure.NOTIFICATION_BUNDLES_ALWAYS_EXPAND;

import android.app.Flags;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.Adjustment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;

import java.util.List;
import java.util.Set;

/**
 * Preference controller governing both the global and individual type-based bundle preferences.
 */
public class BundleCombinedPreferenceController extends BasePreferenceController {

    static final String GLOBAL_KEY = "global_pref";
    static final String WORK_PREF_KEY = "work_profile_pref";
    static final String TYPE_CATEGORY_KEY = "enabled_classification_types";
    static final String EXCLUDED_APPS_CATEGORY_KEY = "notification_bundle_excluded_apps_list";
    static final String PROMO_KEY = "promotions";
    static final String NEWS_KEY = "news";
    static final String SOCIAL_KEY = "social";
    static final String RECS_KEY = "recs";
    static final String ALWAYS_EXPAND_KEY = "always_expand_pref";

    static final List<String> ALL_PREF_TYPES = List.of(PROMO_KEY, NEWS_KEY, SOCIAL_KEY, RECS_KEY);

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    @NonNull NotificationBackend mBackend;
    private @Nullable UserHandle mManagedProfile;

    private @Nullable TwoStatePreference mGlobalPref;
    private @Nullable TwoStatePreference mWorkPref;
    private @Nullable TwoStatePreference mAlwaysExpandPref;
    private @Nullable PreferenceCategory mTypesPrefCategory;
    private @Nullable PreferenceCategory mExcludedAppsPrefCategory;

    public BundleCombinedPreferenceController(@NonNull Context context, @NonNull String prefKey,
            @NonNull NotificationBackend backend) {
        super(context, prefKey);
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
        if (Flags.notificationClassificationUi() && mBackend.isNotificationBundlingSupported()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        PreferenceCategory category = (PreferenceCategory) preference;

        // Find and cache relevant preferences for later updates, then set values
        mGlobalPref = category.findPreference(GLOBAL_KEY);
        if (mGlobalPref != null) {
            mGlobalPref.setOnPreferenceChangeListener(mGlobalPrefListener);
        }

        mWorkPref = category.findPreference(WORK_PREF_KEY);
        if (mWorkPref != null) {
            mWorkPref.setVisible(hasManagedProfile());
            mWorkPref.setOnPreferenceChangeListener(mWorkPrefListener);
        }

        mAlwaysExpandPref = category.findPreference(ALWAYS_EXPAND_KEY);
        if (mAlwaysExpandPref != null) {
            mAlwaysExpandPref.setOnPreferenceChangeListener(mAlwaysExpandPrefListener);
        }

        mTypesPrefCategory = category.findPreference(TYPE_CATEGORY_KEY);
        if (mTypesPrefCategory != null) {
            for (String key : ALL_PREF_TYPES) {
                TwoStatePreference typePref = mTypesPrefCategory.findPreference(key);
                if (typePref != null) {
                    typePref.setOnPreferenceChangeListener(getListenerForType(key));
                }
            }
        }

        mExcludedAppsPrefCategory = category.findPreference(EXCLUDED_APPS_CATEGORY_KEY);

        updatePrefValues();
    }

    void updatePrefValues() {
        boolean isBundlingEnabled = mBackend.isNotificationBundlingEnabled(mContext.getUserId());
        Set<Integer> allowedTypes = mBackend.getAllowedBundleTypes();

        // State check: if bundling is globally enabled, but there are no allowed bundle types,
        // disable the global bundling state from here before proceeding.
        if (isBundlingEnabled && allowedTypes.size() == 0) {
            mBackend.setNotificationBundlingEnabled(mContext.getUserId(), false);
            isBundlingEnabled = false;
        }

        if (mGlobalPref != null) {
            mGlobalPref.setChecked(isBundlingEnabled);
        }

        if (mWorkPref != null && hasManagedProfile()) {
            // profile preference should only be active if the global switch is on
            mWorkPref.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                mWorkPref.setChecked(mBackend.isNotificationBundlingEnabled(managedProfileId()));
            }
        }

        // if global switch is off hide the whole category
        if (mTypesPrefCategory != null) {
            mTypesPrefCategory.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                // checkboxes for individual types should only be active if the global switch is on
                for (String key : ALL_PREF_TYPES) {
                    TwoStatePreference typePref = mTypesPrefCategory.findPreference(key);
                    typePref.setChecked(allowedTypes.contains(getBundleTypeForKey(key)));
                }
            }
        }

        // if global switch is off hide the whole category
        if (mExcludedAppsPrefCategory != null) {
            mExcludedAppsPrefCategory.setVisible(isBundlingEnabled);
        }

        if (mAlwaysExpandPref != null) {
            mAlwaysExpandPref.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                mAlwaysExpandPref.setChecked(Settings.Secure.getInt(mContext.getContentResolver(),
                        NOTIFICATION_BUNDLES_ALWAYS_EXPAND, OFF) == ON);
            }
        }
    }

    private Preference.OnPreferenceChangeListener mGlobalPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        mBackend.setNotificationBundlingEnabled(mContext.getUserId(), checked);
        // update state to hide or show preferences for individual types
        updatePrefValues();
        return true;
    };

    private Preference.OnPreferenceChangeListener mWorkPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        if (hasManagedProfile()) {
            mBackend.setNotificationBundlingEnabled(managedProfileId(), checked);
        }
        return true;
    };

    private Preference.OnPreferenceChangeListener mAlwaysExpandPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                checked ? ON : OFF);
        return true;
    };

    // Returns a preference listener for the given pref key that:
    //   * sets the backend state for whether that type is enabled
    //   * if it is disabled, trigger a new update sync global switch if needed
    private Preference.OnPreferenceChangeListener getListenerForType(String prefKey) {
        return (p, val) -> {
            boolean checked = (boolean) val;
            mBackend.setBundleTypeState(getBundleTypeForKey(prefKey), checked);
            if (!checked) {
                // goes from checked to un-checked; update state in case this was the last enabled
                // individual category
                updatePrefValues();
            }
            return true;
        };
    }

    static @Adjustment.Types int getBundleTypeForKey(String preferenceKey) {
        if (PROMO_KEY.equals(preferenceKey)) {
            return Adjustment.TYPE_PROMOTION;
        } else if (NEWS_KEY.equals(preferenceKey)) {
            return Adjustment.TYPE_NEWS;
        } else if (SOCIAL_KEY.equals(preferenceKey)) {
            return Adjustment.TYPE_SOCIAL_MEDIA;
        } else if (RECS_KEY.equals(preferenceKey)) {
            return Adjustment.TYPE_CONTENT_RECOMMENDATION;
        }
        return Adjustment.TYPE_OTHER;
    }

}
