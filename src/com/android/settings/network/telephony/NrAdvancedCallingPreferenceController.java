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
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.telephony.util.ArrayUtils;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Preference controller for "Enhanced 4G LTE"
 */
public class NrAdvancedCallingPreferenceController extends TelephonyTogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final String TAG = "VoNrSettings";

    @VisibleForTesting
    Preference mPreference;
    private TelephonyManager mTelephonyManager;
    private PhoneCallStateTelephonyCallback mTelephonyCallback;
    private boolean mIsVonrVisibleFromCarrierConfig = false;
    private boolean mIsNrEnableFromCarrierConfig = false;
    private boolean mHas5gCapability = false;
    private Integer mCallState;

    public NrAdvancedCallingPreferenceController(Context context, String key) {
        super(context, key);
        mTelephonyManager = context.getSystemService(TelephonyManager.class);
    }

    /**
     * Initial this PreferenceController.
     * @param subId The subscription Id.
     * @return This PreferenceController.
     */
    public NrAdvancedCallingPreferenceController init(int subId) {
        Log.d(TAG, "init: ");
        if (mTelephonyCallback == null) {
            mTelephonyCallback = new PhoneCallStateTelephonyCallback();
        }

        mSubId = subId;

        if (mTelephonyManager == null) {
            mTelephonyManager = mContext.getSystemService(TelephonyManager.class);
        }
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
        }
        long supportedRadioBitmask = mTelephonyManager.getSupportedRadioAccessFamily();
        mHas5gCapability =
                (supportedRadioBitmask & TelephonyManager.NETWORK_TYPE_BITMASK_NR) > 0;

        PersistableBundle carrierConfig = getCarrierConfigForSubId(subId);
        if (carrierConfig == null) {
            return this;
        }
        mIsVonrVisibleFromCarrierConfig = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL);

        int[] nrAvailabilities = carrierConfig.getIntArray(
                CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY);
        mIsNrEnableFromCarrierConfig = !ArrayUtils.isEmpty(nrAvailabilities);

        Log.d(TAG, "mHas5gCapability: " + mHas5gCapability
                + ",mIsNrEnabledFromCarrierConfig: " + mIsNrEnableFromCarrierConfig
                + ",mIsVonrVisibleFromCarrierConfig: " + mIsVonrVisibleFromCarrierConfig);
        return this;
    }

    @Override
    public int getAvailabilityStatus(int subId) {
        init(subId);

        if (mHas5gCapability && mIsNrEnableFromCarrierConfig && mIsVonrVisibleFromCarrierConfig) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        if (mTelephonyCallback == null) {
            return;
        }
        mTelephonyCallback.register(mTelephonyManager);
    }

    @Override
    public void onStop() {
        if (mTelephonyCallback == null) {
            return;
        }
        mTelephonyCallback.unregister();
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        final SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setEnabled(isUserControlAllowed());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        Log.d(TAG, "setChecked: " + isChecked);
        int result = mTelephonyManager.setVoNrEnabled(isChecked);
        if (result == TelephonyManager.ENABLE_VONR_SUCCESS) {
            return true;
        }
        Log.d(TAG, "Fail to set VoNR result= " + result + ". subId=" + mSubId);
        return false;
    }

    @Override
    public boolean isChecked() {
        return mTelephonyManager.isVoNrEnabled();
    }

    @VisibleForTesting
    protected boolean isCallStateIdle() {
        return (mCallState != null) && (mCallState == TelephonyManager.CALL_STATE_IDLE);
    }

    private boolean isUserControlAllowed() {
        return isCallStateIdle();
    }

    private class PhoneCallStateTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {

        private TelephonyManager mLocalTelephonyManager;

        @Override
        public void onCallStateChanged(int state) {
            mCallState = state;
            updateState(mPreference);
        }

        public void register(TelephonyManager telephonyManager) {
            mLocalTelephonyManager = telephonyManager;

            // assign current call state so that it helps to show correct preference state even
            // before first onCallStateChanged() by initial registration.
            mCallState = mLocalTelephonyManager.getCallState();
            mLocalTelephonyManager.registerTelephonyCallback(
                    mContext.getMainExecutor(), mTelephonyCallback);
        }

        public void unregister() {
            mCallState = null;
            if (mLocalTelephonyManager != null) {
                mLocalTelephonyManager.unregisterTelephonyCallback(this);
            }
        }
    }
}
