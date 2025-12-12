/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dream;

import static android.service.dreams.Flags.allowDreamWhenPostured;
import static android.service.dreams.Flags.dreamsV2;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.flags.Flags;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

public class WhenToDreamPicker extends RadioButtonPickerFragment {

    private static final String TAG = "WhenToDreamPicker";

    private Context mContext;
    private DreamBackend mBackend;
    private boolean mDreamsSupportedOnBattery;
    private boolean mShowRestrictToWirelessCharging;
    private RadioButtonPickerExtraSwitchController mRestrictToWirelessChargingController;

    private final RadioButtonPickerExtraSwitchController.PreferenceAccessor
            mWirelessChargingPreferenceAccessor =
            new RadioButtonPickerExtraSwitchController.PreferenceAccessor() {
                @Override
                public void setValue(boolean value) {
                    mBackend.setRestrictToWirelessCharging(value);
                }

                @Override
                public boolean getValue() {
                    return mBackend.getRestrictToWirelessCharging();
                }
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
        mBackend = DreamBackend.getInstance(context);
        mDreamsSupportedOnBattery = getResources().getBoolean(
                com.android.internal.R.bool.config_dreamsEnabledOnBattery);
        mShowRestrictToWirelessCharging =
                getResources().getBoolean(R.bool.config_show_restrict_to_wireless_charging);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.when_to_dream_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_WHEN_TO_DREAM;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final String[] entries = entries();
        final String[] values = keys();
        final List<WhenToDreamCandidateInfo> candidates = new ArrayList<>();

        if (entries == null || entries.length <= 0) return null;
        if (values == null || values.length != entries.length) {
            throw new IllegalArgumentException("Entries and values must be of the same length.");
        }

        final boolean supportDreamWhilePostured = allowDreamWhenPostured()
                && getResources().getBoolean(R.bool.config_posturing_supported);
        for (int i = 0; i < entries.length; i++) {
            final String key = values[i];
            if (DreamSettings.WHILE_POSTURED_ONLY.equals(key) && !supportDreamWhilePostured) {
                continue;
            }
            candidates.add(new WhenToDreamCandidateInfo(entries[i], key));
        }

        return candidates;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        if (!dreamsV2()) {
            return;
        }

        if (mShowRestrictToWirelessCharging && mRestrictToWirelessChargingController == null) {
            mRestrictToWirelessChargingController =
                    new RadioButtonPickerExtraSwitchController(
                            mContext,
                            R.string.screensaver_restrict_to_wireless_charging_title,
                            mWirelessChargingPreferenceAccessor);
            mRestrictToWirelessChargingController.addToScreen(screen);
        }
    }

    private String[] entries() {
        if (Flags.resolveMissingWhenToDream()) {
            return DreamUtils.getWhenToDreamEntries(getResources());
        }

        if (mDreamsSupportedOnBattery) {
            return getResources().getStringArray(R.array.when_to_start_screensaver_entries);
        }
        return getResources().getStringArray(R.array.when_to_start_screensaver_entries_no_battery);
    }

    private String[] keys() {
        if (Flags.resolveMissingWhenToDream()) {
            return DreamUtils.getWhenToDreamKeys(getResources());
        }

        if (mDreamsSupportedOnBattery) {
            return getResources().getStringArray(R.array.when_to_start_screensaver_values);
        }
        return getResources().getStringArray(R.array.when_to_start_screensaver_values_no_battery);
    }

    @Override
    protected String getDefaultKey() {
        return DreamSettings.getKeyFromSetting(mBackend.getWhenToDreamSetting());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        mBackend.setWhenToDream(DreamSettings.getSettingFromPrefKey(key));
        return true;
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        super.onSelectionPerformed(success);

        if (!dreamsV2()) {
            getActivity().finish();
        }
    }

    private final class WhenToDreamCandidateInfo extends CandidateInfo {
        private final String name;
        private final String key;

        WhenToDreamCandidateInfo(String title, String value) {
            super(true);

            name = title;
            key = value;
        }

        @Override
        public CharSequence loadLabel() {
            return name;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return key;
        }
    }
}
