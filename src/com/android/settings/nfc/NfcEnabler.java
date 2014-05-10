/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.R;

/**
 * NfcEnabler is a helper to manage the Nfc polling list preference. It is
 * turns on/off Nfc and ensures the summary of the preference reflects the
 * current state.
 */
public class NfcEnabler implements Preference.OnPreferenceChangeListener {
    private static final String KEY_NFC_POLLING = "nfc_polling";
    static final int POLLING_OFF = 0;
    static final int POLLING_SCREEN_ON_LOCKED = 2;

    private final Context mContext;
    private final ListPreference mList;
    private final PreferenceScreen mAndroidBeam;
    private final NfcAdapter mNfcAdapter;
    private final IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
        }
    };

    public NfcEnabler(Context context, ListPreference listPreference,
            PreferenceScreen androidBeam) {
        mContext = context;
        mList = listPreference;
        mAndroidBeam = androidBeam;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);

        if (mNfcAdapter == null) {
            // NFC is not supported
            mList.setEnabled(false);
            mAndroidBeam.setEnabled(false);
            mIntentFilter = null;
            return;
        }
        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
    }

    public void resume() {
        if (mNfcAdapter == null) {
            return;
        }
        handleNfcStateChanged(mNfcAdapter.getAdapterState());
        mContext.registerReceiver(mReceiver, mIntentFilter);
        final int currentPollingMode = Settings.System.getInt(mContext.getContentResolver(), KEY_NFC_POLLING,
                POLLING_SCREEN_ON_LOCKED);
        mList.setValue(String.valueOf(currentPollingMode));
        mList.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        if (mNfcAdapter == null) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        mList.setOnPreferenceChangeListener(null);
    }

    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn NFC on/off

        final int pollingMode = Integer.parseInt((String)value);
        Settings.System.putInt(mContext.getContentResolver(), KEY_NFC_POLLING, pollingMode);
        mList.setValue(String.valueOf(pollingMode));

        mNfcAdapter.disable();
        if (pollingMode != POLLING_OFF) {
            mNfcAdapter.enable();
        }

        return false;
    }

    private void handleNfcStateChanged(int newState) {
        switch (newState) {
        case NfcAdapter.STATE_OFF:
            mList.setEnabled(true);
            mAndroidBeam.setEnabled(false);
            mAndroidBeam.setSummary(R.string.android_beam_disabled_summary);
            break;
        case NfcAdapter.STATE_ON:
            mList.setEnabled(true);
            mAndroidBeam.setEnabled(true);
            if (mNfcAdapter.isNdefPushEnabled()) {
                mAndroidBeam.setSummary(R.string.android_beam_on_summary);
            } else {
                mAndroidBeam.setSummary(R.string.android_beam_off_summary);
            }
            break;
        case NfcAdapter.STATE_TURNING_ON:
            mList.setEnabled(false);
            mAndroidBeam.setEnabled(false);
            break;
        case NfcAdapter.STATE_TURNING_OFF:
            mList.setEnabled(false);
            mAndroidBeam.setEnabled(false);
            break;
        }
    }
}
