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

package com.android.settings.network.telephony.satellite;

import static com.android.settings.network.telephony.satellite.SatelliteAppListCategoryController.getApplicationInfo;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.network.SatelliteRepository;
import com.android.settingslib.Utils;
import com.android.settingslib.core.AbstractPreferenceController;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.stream.Collectors;

/** Shows all applications which support satellite service. */
public class SatelliteAppListFragment extends RestrictedDashboardFragment {
    public SatelliteAppListFragment() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(SatelliteAppListPreferenceController.class).init();
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        SatelliteAppListPreferenceController satelliteAppListPreferenceController =
                new SatelliteAppListPreferenceController(getContext());
        return List.of(satelliteAppListPreferenceController);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.satellite_settings_apps_list;
    }

    @Override
    protected String getLogTag() {
        return "SatelliteAppListFragment";
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SATELLITE_APPS_LIST;
    }

    @VisibleForTesting
    static class SatelliteAppListPreferenceController extends BasePreferenceController {
        private static final String TAG = "SatelliteAppListPreferenceController";
        private static final String KEY = "key_satellite_app_list";

        private List<ApplicationInfo> mApplicationInfoList = List.of();

        SatelliteAppListPreferenceController(@NonNull Context context) {
            super(context, KEY);
        }

        public void init() {
            SatelliteRepository satelliteRepository = new SatelliteRepository(mContext);
            init(satelliteRepository);
        }

        void init(@NonNull SatelliteRepository satelliteRepository) {
            mApplicationInfoList =
                    satelliteRepository.getSatelliteDataOptimizedApps()
                            .stream()
                            .map(name -> getApplicationInfo(mContext, name))
                            .collect(Collectors.toList());
        }

        @Override
        public void displayPreference(PreferenceScreen screen) {
            super.displayPreference(screen);
            if (mApplicationInfoList.isEmpty()) {
                return;
            }
            mApplicationInfoList.forEach(appInfo -> {
                if (appInfo != null) {
                    Log.i(TAG, "Add preference to UI : " + appInfo.packageName);
                    Drawable icon = Utils.getBadgedIcon(mContext, appInfo);
                    CharSequence name = appInfo.loadLabel(mContext.getPackageManager());
                    Preference pref = new Preference(mContext);
                    pref.setIcon(icon);
                    pref.setTitle(name);
                    screen.addPreference(pref);
                }
            });
        }

        @Override
        public int getAvailabilityStatus() {
            return AVAILABLE;
        }
    }
}
