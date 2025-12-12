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

package com.android.settings.dream;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.MainSwitchPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LowLightModePicker extends RadioButtonPickerFragment
        implements Preference.OnPreferenceChangeListener  {
    static final String LOW_LIGHT_DISPLAY_BEHAVIOR_NONE = "low_light_display_behavior_none";
    static final String LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF =
            "low_light_display_behavior_screen_off";
    static final String LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM =
            "low_light_display_behavior_low_light_clock_dream";
    static final String LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM = "low_light_display_behavior_no_dream";

    private Context mContext;
    private DreamBackend mBackend;
    @Nullable
    private MainSwitchPreference mMainSwitch;

    @Settings.Secure.LowLightDisplayBehavior
    static int getSettingFromPrefKey(String key) {
        switch (key) {
            case LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF:
                return Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF;
            case LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM:
                return Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM;
            case LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM:
                return Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM;
            case LOW_LIGHT_DISPLAY_BEHAVIOR_NONE:
            default:
                return Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NONE;
        }
    }

    static String getKeyFromSetting(@Settings.Secure.LowLightDisplayBehavior int setting) {
        switch (setting) {
            case Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF:
                return LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF;
            case Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM:
                return LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM;
            case Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM:
                return LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM;
            case Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NONE:
            default:
                return LOW_LIGHT_DISPLAY_BEHAVIOR_NONE;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
        mBackend = DreamBackend.getInstance(context);
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.low_light_mode_settings;
    }

    @Override
    protected String getDefaultKey() {
        return getKeyFromSetting(mBackend.getLowLightDisplayBehavior());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        mBackend.setLowLightDisplayBehavior(getSettingFromPrefKey(key));
        return true;
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        mMainSwitch = new MainSwitchPreference(mContext);
        mMainSwitch.setTitle(R.string.low_light_display_behavior_main_switch_title);
        mMainSwitch.setChecked(mBackend.getLowLightDisplayBehaviorEnabled());
        mMainSwitch.setOnPreferenceChangeListener(this);
        screen.addPreference(mMainSwitch);

        final Preference category = new PreferenceCategory(mContext);
        category.setTitle(R.string.low_light_display_behavior_subtitle);
        screen.addPreference(category);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mMainSwitch) {
            mBackend.setLowLightDisplayBehaviorEnabled((boolean) newValue);
            updateCandidates();
            return true;
        }

        return false;
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        super.onSelectionPerformed(success);

        getActivity().finish();
    }

    private String[] entries() {
        return getResources().getStringArray(R.array.low_light_display_behavior_entries);
    }

    private String[] keys() {
        return getResources().getStringArray(R.array.low_light_display_behavior_values);
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final String[] entries = entries();
        final String[] values = keys();
        final Set<String> allowedBehaviors = DreamUtils.getLowLightBehaviors(getResources());

        final List<LowLightModeCandidateInfo> candidates = new ArrayList<>();

        if (ArrayUtils.isEmpty(entries)) return candidates;
        if (ArrayUtils.size(values) != ArrayUtils.size(entries)) {
            throw new IllegalArgumentException("Entries and values must be of the same length.");
        }

        final boolean enabled = mBackend.getLowLightDisplayBehaviorEnabled();

        for (int i = 0; i < entries.length; i++) {
            if (!allowedBehaviors.contains(values[i])) {
                continue;
            }
            candidates.add(new LowLightModeCandidateInfo(entries[i], values[i], enabled));
        }

        return candidates;
    }

    private static final class LowLightModeCandidateInfo extends CandidateInfo {
        private final String mName;
        private final String mKey;

        LowLightModeCandidateInfo(String title, String value, boolean enabled) {
            super(enabled);

            mName = title;
            mKey = value;
        }

        @Override
        public CharSequence loadLabel() {
            return mName;
        }

        @Nullable
        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }
    }
}
