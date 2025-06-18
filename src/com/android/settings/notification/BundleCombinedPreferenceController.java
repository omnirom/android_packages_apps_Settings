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
import android.service.notification.Adjustment;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.BasePreferenceController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Preference controller governing both the global and individual type-based bundle preferences.
 */
public class BundleCombinedPreferenceController extends BasePreferenceController {

    static final String GLOBAL_KEY = "global_pref";
    static final String PROMO_KEY = "promotions";
    static final String NEWS_KEY = "news";
    static final String SOCIAL_KEY = "social";
    static final String RECS_KEY = "recs";

    static final List<String> ALL_PREF_TYPES = List.of(PROMO_KEY, NEWS_KEY, SOCIAL_KEY, RECS_KEY);

    @NonNull NotificationBackend mBackend;

    private @Nullable TwoStatePreference mGlobalPref;
    private Map<String, TwoStatePreference> mTypePrefs = new ArrayMap<>();

    public BundleCombinedPreferenceController(@NonNull Context context, @NonNull String prefKey,
            @NonNull NotificationBackend backend) {
        super(context, prefKey);
        mBackend = backend;
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
        for (String key : ALL_PREF_TYPES) {
            TwoStatePreference typePref = category.findPreference(key);
            if (typePref != null) {
                mTypePrefs.put(key, typePref);
                typePref.setOnPreferenceChangeListener(getListenerForType(key));
            }
        }

        updatePrefValues();
    }

    void updatePrefValues() {
        boolean isBundlingEnabled = mBackend.isNotificationBundlingEnabled(mContext);
        Set<Integer> allowedTypes = mBackend.getAllowedBundleTypes();

        // State check: if bundling is globally enabled, but there are no allowed bundle types,
        // disable the global bundling state from here before proceeding.
        if (isBundlingEnabled && allowedTypes.size() == 0) {
            mBackend.setNotificationBundlingEnabled(false);
            isBundlingEnabled = false;
        }

        if (mGlobalPref != null) {
            mGlobalPref.setChecked(isBundlingEnabled);
        }

        for (String key : mTypePrefs.keySet()) {
            TwoStatePreference typePref = mTypePrefs.get(key);
            // checkboxes for individual types should only be active if the global switch is on
            typePref.setVisible(isBundlingEnabled);
            if (isBundlingEnabled) {
                typePref.setChecked(allowedTypes.contains(getBundleTypeForKey(key)));
            }
        }
    }

    private Preference.OnPreferenceChangeListener mGlobalPrefListener = (p, val) -> {
        boolean checked = (boolean) val;
        mBackend.setNotificationBundlingEnabled(checked);
        // update state to hide or show preferences for individual types
        updatePrefValues();
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
