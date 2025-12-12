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

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED;
import static android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.PersistableBundle;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.network.telephony.TelephonyBasePreferenceController;
import com.android.settingslib.Utils;

import java.util.List;

/** A controller to show some of apps info which supported on Satellite service. */
public class SatelliteAppListCategoryController extends TelephonyBasePreferenceController {
    private static final String TAG = "SatelliteAppListCategoryController";
    @VisibleForTesting
    static final int MAXIMUM_OF_PREFERENCE_AMOUNT = 3;

    private List<String> mPackageNameList;
    private boolean mIsSmsAvailable;
    private boolean mIsDataAvailable;
    private boolean mIsSatelliteEligible;
    private int mDataMode = SATELLITE_DATA_SUPPORT_ONLY_RESTRICTED;
    private PersistableBundle mConfigBundle = new PersistableBundle();

    public SatelliteAppListCategoryController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    /** Initialize the necessary applications' data */
    public void init(int subId, @NonNull PersistableBundle configBundle) {
        mSubId = subId;
        mConfigBundle = configBundle;
        mPackageNameList = getSatelliteDataOptimizedApps();
    }

    void setCarrierRoamingNtnAvailability(boolean isSmsAvailable, boolean isDataAvailable,
            int dataMode) {
        mIsSmsAvailable = isSmsAvailable;
        mIsDataAvailable = isDataAvailable;
        mDataMode = dataMode;
        mIsSatelliteEligible = isSatelliteEligible();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceCategory preferenceCategory = screen.findPreference(getPreferenceKey());
        for (int i = 0; i < mPackageNameList.size() && i < MAXIMUM_OF_PREFERENCE_AMOUNT; i++) {
            String packageName = mPackageNameList.get(i);
            ApplicationInfo appInfo = getApplicationInfo(mContext, packageName);
            if (appInfo != null) {
                Drawable icon = Utils.getBadgedIcon(mContext, appInfo);
                CharSequence name = appInfo.loadLabel(mContext.getPackageManager());
                Preference pref = new Preference(mContext);
                pref.setIcon(icon);
                pref.setTitle(name);
                preferenceCategory.addPreference(pref);
            }
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        // Only when carrier support entitlement check, it shall check account eligible or not.
        if (mConfigBundle.getBoolean(KEY_SATELLITE_ENTITLEMENT_SUPPORTED_BOOL)
                && !mIsSatelliteEligible) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        Log.d(TAG, "Supported apps have " + mPackageNameList.size());

        return mIsDataAvailable
                && mDataMode == SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED
                && !mPackageNameList.isEmpty() ? AVAILABLE_UNSEARCHABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @VisibleForTesting
    protected List<String> getSatelliteDataOptimizedApps() {
        SatelliteManager satelliteManager = mContext.getSystemService(SatelliteManager.class);
        if (satelliteManager == null) {
            return List.of();
        }
        try {
            return satelliteManager.getSatelliteDataOptimizedApps();
        } catch (IllegalStateException e) {
            Log.d(TAG, "getSatelliteDataOptimizedApps failed due to " + e);
        }
        return List.of();
    }

    @VisibleForTesting
    protected boolean isSatelliteEligible() {
        if (mConfigBundle.getInt(KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT)
                == CARRIER_ROAMING_NTN_CONNECT_MANUAL) {
            return mIsSmsAvailable;
        }
        return SatelliteCarrierSettingUtils.isSatelliteAccountEligible(mContext, mSubId);
    }

    static ApplicationInfo getApplicationInfo(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationInfoAsUser(packageName, /* flags= */ 0, context.getUserId());
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}
