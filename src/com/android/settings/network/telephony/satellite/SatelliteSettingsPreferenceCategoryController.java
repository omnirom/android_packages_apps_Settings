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

import static android.telephony.CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC;
import static android.telephony.CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL;
import static android.telephony.CarrierConfigManager.KEY_SATELLITE_ESOS_SUPPORTED_BOOL;
import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_SMS;

import android.content.Context;
import android.os.OutcomeReceiver;
import android.os.PersistableBundle;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.satellite.NtnSignalStrength;
import android.telephony.satellite.SatelliteManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.flags.Flags;
import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.telephony.TelephonyBasePreferenceController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Preference controller for Satellite functions in mobile network settings. */
public class SatelliteSettingsPreferenceCategoryController extends
        TelephonyBasePreferenceController implements DefaultLifecycleObserver {
    private static final String TAG = "SatelliteSettingsPrefCategoryCon";

    @VisibleForTesting
    final CarrierRoamingNtnModeCallback mCarrierRoamingNtnModeCallback =
            new CarrierRoamingNtnModeCallback(this);

    private CarrierConfigCache mCarrierConfigCache;
    private SatelliteManager mSatelliteManager;
    private TelephonyManager mTelephonyManager;
    private PreferenceScreen mPreferenceScreen;
    @VisibleForTesting
    AtomicBoolean mIsSatelliteSupported = new AtomicBoolean(false);

    public SatelliteSettingsPreferenceCategoryController(Context context, String key) {
        super(context, key);
        mCarrierConfigCache = CarrierConfigCache.getInstance(mContext);
    }

    /**
     * Set subId for Satellite Settings category .
     *
     * @param subId subscription ID.
     */
    public void init(int subId) {
        Log.d(TAG, "init(), subId=" + subId);
        mSubId = subId;
        mSatelliteManager = mContext.getSystemService(SatelliteManager.class);
        mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        requestIsSatelliteSupported();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mPreferenceScreen != null) {
            mPreferenceScreen = screen;
        }
    }

    void displayPreference() {
        if (mPreferenceScreen != null) {
            displayPreference(mPreferenceScreen);
        }
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        if (!com.android.internal.telephony.flags.Flags.carrierEnabledSatelliteFlag()) {
            return UNSUPPORTED_ON_DEVICE;
        }
        final PersistableBundle carrierConfig = mCarrierConfigCache.getConfigForSubId(subId);

        boolean isSatelliteConnectedTypeIsAuto =
                CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC == carrierConfig.getInt(
                        KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
                        CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC);

        // SatelliteManager#requestIsSupported is only supported for manual connection type, so
        // if type is auto, this check shall be skipped.
        if (!isSatelliteConnectedTypeIsAuto && !mIsSatelliteSupported.get()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        boolean isSatelliteSosSupported = false;
        if (Flags.satelliteOemSettingsUxMigration()) {
            isSatelliteSosSupported = carrierConfig.getBoolean(KEY_SATELLITE_ESOS_SUPPORTED_BOOL);
        }

        if (!carrierConfig.getBoolean(KEY_SATELLITE_ATTACH_SUPPORTED_BOOL)) {
            return UNSUPPORTED_ON_DEVICE;
        }

        if (isSatelliteSosSupported) {
            return AVAILABLE_UNSEARCHABLE;
        }

        if (isSatelliteConnectedTypeIsAuto) {
            return AVAILABLE_UNSEARCHABLE;
        } else {
            return mCarrierRoamingNtnModeCallback.isSatelliteSmsAvailable()
                    ? AVAILABLE_UNSEARCHABLE
                    : CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        if (com.android.settings.flags.Flags.satelliteOemSettingsUxMigration()) {
            if (mTelephonyManager != null) {
                mTelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(),
                        mCarrierRoamingNtnModeCallback);
            }
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        if (com.android.settings.flags.Flags.satelliteOemSettingsUxMigration()) {
            if (mTelephonyManager != null) {
                mTelephonyManager.unregisterTelephonyCallback(mCarrierRoamingNtnModeCallback);
            }
        }
    }

    private void requestIsSatelliteSupported() {
        if (mSatelliteManager == null) {
            Log.d(TAG, "SatelliteManager is null");
            return;
        }
        mSatelliteManager.requestIsSupported(Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(Boolean result) {
                        mIsSatelliteSupported.set(result);
                        Log.d(TAG, "Satellite requestIsSupported onResult : " + result);
                        SatelliteSettingsPreferenceCategoryController.this.displayPreference();
                    }
                });
    }

    @VisibleForTesting
    static class CarrierRoamingNtnModeCallback extends TelephonyCallback implements
            TelephonyCallback.CarrierRoamingNtnListener {
        SatelliteSettingsPreferenceCategoryController mController;
        private boolean mIsSatelliteSmsAvailable = false;

        CarrierRoamingNtnModeCallback(SatelliteSettingsPreferenceCategoryController controller) {
            mController = controller;
        }

        boolean isSatelliteSmsAvailable() {
            return mIsSatelliteSmsAvailable;
        }

        @Override
        public void onCarrierRoamingNtnAvailableServicesChanged(@NonNull int[] availableServices) {
            CarrierRoamingNtnListener.super.onCarrierRoamingNtnAvailableServicesChanged(
                    availableServices);
            List<Integer> availableServicesList = Arrays.stream(availableServices).boxed().toList();
            mIsSatelliteSmsAvailable = availableServicesList.contains(SERVICE_TYPE_SMS);
            Log.d(TAG, "isSmsAvailable : " + mIsSatelliteSmsAvailable);
            mController.displayPreference();
        }

        @Override
        public void onCarrierRoamingNtnEligibleStateChanged(boolean eligible) {
            // Do nothing
        }

        @Override
        public void onCarrierRoamingNtnModeChanged(boolean active) {
            // Do nothing
        }

        @Override
        public void onCarrierRoamingNtnSignalStrengthChanged(
                @NonNull NtnSignalStrength ntnSignalStrength) {
            // Do nothing
        }
    }
}
