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

package com.android.settings.communal;

import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_CHARGING;
import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_CHARGING_UPRIGHT;
import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_DOCKED;
import static android.provider.Settings.Secure.GLANCEABLE_HUB_START_NEVER;
import static android.provider.Settings.Secure.WHEN_TO_START_GLANCEABLE_HUB;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dream.RadioButtonPickerExtraSwitchController;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment that provides radio buttons to allow the user to choose when the hub should auto-start.
 */
public class WhenToStartHubPicker extends RadioButtonPickerFragment {
    private static final String TAG = "WhenToStartHubPicker";
    private static final String SHOW_WHILE_CHARGING = "while_charging";
    private static final String SHOW_WHILE_DOCKED = "while_docked";
    private static final String SHOW_WHILE_CHARGING_AND_UPRIGHT = "while_charging_and_upright";
    private static final String SHOW_NEVER = "never";

    private Context mContext;
    private boolean mShowRestrictToWirelessCharging;

    @Nullable
    private RadioButtonPickerExtraSwitchController mRestrictToWirelessChargingController = null;

    private final RadioButtonPickerExtraSwitchController.PreferenceAccessor
            mWirelessChargingPreferenceAccessor =
            new RadioButtonPickerExtraSwitchController.PreferenceAccessor() {
                @Override
                public void setValue(boolean value) {
                    Settings.Secure.putInt(
                            mContext.getContentResolver(),
                            Settings.Secure.GLANCEABLE_HUB_RESTRICT_TO_WIRELESS_CHARGING,
                            value ? 1 : 0);
                }

                @Override
                public boolean getValue() {
                    final int defaultValue = mContext.getResources().getBoolean(
                            com.android.internal.R.bool
                                    .config_onlyShowGlanceableHubWhenWirelessChargingDefault)
                            ? 1 : 0;
                    return Settings.Secure.getInt(
                            mContext.getContentResolver(),
                            Settings.Secure.GLANCEABLE_HUB_RESTRICT_TO_WIRELESS_CHARGING,
                            defaultValue) == 1;
                }
            };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        mContext = context;
        mShowRestrictToWirelessCharging =
                getResources().getBoolean(R.bool.config_show_restrict_to_wireless_charging);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.when_to_start_hubmode_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WHEN_TO_SHOW_WIDGETS_ON_LOCKSCREEN;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final List<WhenToStartHubCandidateInfo> candidates = new ArrayList<>();

        final String[] entries = entries();
        final String[] values = keys();

        if (entries == null || entries.length <= 0) return candidates;
        if (values == null || values.length != entries.length) {
            throw new IllegalArgumentException("Entries and values must be of the same length.");
        }

        final boolean supportAutoShowWhilePostured =
                getResources().getBoolean(R.bool.config_posturing_supported);
        for (int i = 0; i < entries.length; i++) {
            final String key = values[i];
            if (key.equals(SHOW_WHILE_CHARGING_AND_UPRIGHT) && !supportAutoShowWhilePostured) {
                continue;
            }

            candidates.add(new WhenToStartHubCandidateInfo(entries[i], values[i]));
        }

        return candidates;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        if (mShowRestrictToWirelessCharging && mRestrictToWirelessChargingController == null) {
            mRestrictToWirelessChargingController =
                    new RadioButtonPickerExtraSwitchController(
                            mContext,
                            R.string.hub_restrict_to_wireless_charging_title,
                            mWirelessChargingPreferenceAccessor);
            mRestrictToWirelessChargingController.addToScreen(screen);
        }
    }

    private String[] entries() {
        return getResources().getStringArray(R.array.when_to_start_hubmode_entries);
    }

    private String[] keys() {
        return getResources().getStringArray(R.array.when_to_start_hubmode_values);
    }

    @Override
    protected String getDefaultKey() {
        final int defaultValue = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_whenToStartHubModeDefault);
        final int setting = Settings.Secure.getInt(
                mContext.getContentResolver(), WHEN_TO_START_GLANCEABLE_HUB, defaultValue);
        return getKeyFromSetting(setting);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                WHEN_TO_START_GLANCEABLE_HUB,
                getSettingFromPrefKey(key));

        return true;
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        super.onSelectionPerformed(success);

        getActivity().finish();
    }


    @Settings.Secure.WhenToStartGlanceableHub
    private static int getSettingFromPrefKey(String key) {
        switch (key) {
            case SHOW_WHILE_CHARGING:
                return GLANCEABLE_HUB_START_CHARGING;
            case SHOW_WHILE_DOCKED:
                return GLANCEABLE_HUB_START_DOCKED;
            case SHOW_WHILE_CHARGING_AND_UPRIGHT:
                return GLANCEABLE_HUB_START_CHARGING_UPRIGHT;
            case SHOW_NEVER:
            default:
                return GLANCEABLE_HUB_START_NEVER;
        }
    }

    private static String getKeyFromSetting(@Settings.Secure.WhenToStartGlanceableHub int setting) {
        switch (setting) {
            case GLANCEABLE_HUB_START_CHARGING:
                return SHOW_WHILE_CHARGING;
            case GLANCEABLE_HUB_START_DOCKED:
                return SHOW_WHILE_DOCKED;
            case GLANCEABLE_HUB_START_CHARGING_UPRIGHT:
                return SHOW_WHILE_CHARGING_AND_UPRIGHT;
            case GLANCEABLE_HUB_START_NEVER:
            default:
                return SHOW_NEVER;
        }
    }

    private static final class WhenToStartHubCandidateInfo extends CandidateInfo {
        private final String mName;
        private final String mKey;

        WhenToStartHubCandidateInfo(String title, String value) {
            super(true);

            mName = title;
            mKey = value;
        }

        @Override
        public CharSequence loadLabel() {
            return mName;
        }

        @Override
        @Nullable
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }
}
