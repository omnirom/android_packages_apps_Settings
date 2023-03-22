/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.UserManager;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.network.telephony.CallsDefaultSubscriptionController;
import com.android.settings.network.telephony.NetworkProviderBackupCallingPreferenceController;
import com.android.settings.network.telephony.NetworkProviderWifiCallingPreferenceController;
import com.android.settings.network.telephony.SmsDefaultSubscriptionController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class NetworkProviderCallsSmsFragment extends DashboardFragment {
    @VisibleForTesting
    static final String LOG_TAG = "NetworkProviderCallsSmsFragment";
    @VisibleForTesting
    static final String KEY_PREFERENCE_CATEGORY_CALLING = "provider_model_calling_category";
    @VisibleForTesting
    static final String KEY_PREFERENCE_CATEGORY_BACKUP_CALLING =
            "provider_model_backup_calling_category";

    @VisibleForTesting
    static final String KEY_PREFERENCE_CALLS= "provider_model_calls_preference";
    @VisibleForTesting
    static final String KEY_PREFERENCE_SMS = "provider_model_sms_preference";

    private NetworkProviderWifiCallingPreferenceController
            mNetworkProviderWifiCallingPreferenceController;

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new CallsDefaultSubscriptionController(context, KEY_PREFERENCE_CALLS));
        controllers.add(new SmsDefaultSubscriptionController(context, KEY_PREFERENCE_SMS));
        mNetworkProviderWifiCallingPreferenceController =
                new NetworkProviderWifiCallingPreferenceController(context,
                        KEY_PREFERENCE_CATEGORY_CALLING);
        mNetworkProviderWifiCallingPreferenceController.init(getSettingsLifecycle());
        controllers.add(mNetworkProviderWifiCallingPreferenceController);

        NetworkProviderBackupCallingPreferenceController backupCallingPrefCtrl =
                new NetworkProviderBackupCallingPreferenceController(context,
                        KEY_PREFERENCE_CATEGORY_BACKUP_CALLING);
        backupCallingPrefCtrl.init(getSettingsLifecycle());
        controllers.add(backupCallingPrefCtrl);

        return controllers;
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferenceStates();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_provider_calls_sms;
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACTION_UNKNOWN;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.network_provider_calls_sms) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SubscriptionUtil.isSimHardwareVisible(context) &&
                            context.getSystemService(UserManager.class).isAdminUser();
                }
            };
}
