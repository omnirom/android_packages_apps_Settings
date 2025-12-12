/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.ArrayList;
import java.util.List;

public class Enable2gToggleListController extends BasePreferenceController {

    private static final String LOG_TAG = "Enable2gToggleListController";
    private final SubscriptionManager mSubscriptionManager;
    private final List<Enable2gPreferenceController> mControllers = new ArrayList<>();

    public Enable2gToggleListController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        update(screen);
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        for (Enable2gPreferenceController controller : mControllers) {
            if (controller.getAvailabilityStatus() == AVAILABLE) {
                return AVAILABLE;
            }
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    private void update(PreferenceScreen screen) {
        PreferenceCategory preferenceCategory = screen.findPreference(getPreferenceKey());
        if (preferenceCategory == null) {
            Log.d(LOG_TAG, "preferenceCategory is null!");
            return;
        }

        final List<SubscriptionInfo> subInfos = mSubscriptionManager
                .getActiveSubscriptionInfoList();
        Log.d(LOG_TAG, "Number of active subscriptions is " + subInfos.size());
        for (SubscriptionInfo info : subInfos) {
            addTogglePreference(info.getSubscriptionId(), screen, preferenceCategory);
        }
    }

    private void addTogglePreference(int subId, PreferenceScreen screen,
                                     PreferenceCategory preferenceCategory) {
        RestrictedSwitchPreference pref =
                new RestrictedSwitchPreference(preferenceCategory.getContext());
        String title = mContext.getString(R.string.enable_2g_title);
        String key = title + subId;
        pref.setKey(key);
        pref.setTitle(title);
        preferenceCategory.addPreference(pref);
        Enable2gPreferenceController enable2gPreferenceController =
                new Enable2gPreferenceController(mContext, key);
        enable2gPreferenceController.init(subId, true);
        enable2gPreferenceController.displayPreference(screen);
        enable2gPreferenceController.updateState(pref);
        mControllers.add(enable2gPreferenceController);
    }
}
