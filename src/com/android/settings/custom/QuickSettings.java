/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.custom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.wimax.WimaxHelper;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.telephony.Phone;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class QuickSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "QuickSettings";
    private static final String SEPARATOR = "OV=I=XseparatorX=I=VO";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_settings_panel_settings);
    }

    private static final String TILE_MODES_CATEGORY = "pref_tile_modes";
    private static final String SELECT_TILE_KEY_PREFIX = "pref_tile_";
    private static final String EXP_RING_MODE = "pref_ring_mode";

    private static final String DYNAMIC_ALARM = "dynamic_alarm";
    private static final String DYNAMIC_BUGREPORT = "dynamic_bugreport";
    private static final String DYNAMIC_IME = "dynamic_ime";
    private static final String DYNAMIC_WIFI = "dynamic_wifi";

    MultiSelectListPreference mRingMode;
    CheckBoxPreference mDynamicAlarm;
    CheckBoxPreference mDynamicBugReport;
    CheckBoxPreference mDynamicWifi;
    CheckBoxPreference mDynamicIme;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        PreferenceScreen prefSet = getPreferenceScreen();
        PackageManager pm = getPackageManager();
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();

        // Add the dynamic tiles checkboxes
        mDynamicAlarm = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_ALARM);
        mDynamicAlarm.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1);
        mDynamicBugReport = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_BUGREPORT);
        mDynamicBugReport.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT, 1) == 1);
        mDynamicIme = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_IME);
        mDynamicIme.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1);
        mDynamicWifi = (CheckBoxPreference) prefSet.findPreference(DYNAMIC_WIFI);
        mDynamicWifi.setChecked(Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_WIFI, 1) == 1);

        // Add the ring mode
        mRingMode = (MultiSelectListPreference) prefSet.findPreference(EXP_RING_MODE);
        String storedRingMode = Settings.System.getString(getActivity()
                .getApplicationContext().getContentResolver(),
                Settings.System.EXPANDED_RING_MODE);
        if (storedRingMode != null) {
            String[] ringModeArray = TextUtils.split(storedRingMode, SEPARATOR);
            mRingMode.setValues(new HashSet<String>(Arrays.asList(ringModeArray)));
            updateSummary(storedRingMode, mRingMode, R.string.pref_ring_mode_summary);
        }
        mRingMode.setOnPreferenceChangeListener(this);

        // Add the available mode tiles, incase they need to be removed later
        PreferenceCategory prefTileModes = (PreferenceCategory) prefSet
                .findPreference(TILE_MODES_CATEGORY);

        // get our list of Tiles
        ArrayList<String> buttonList = QuickSettingsUtil.getTileListFromString(QuickSettingsUtil
                .getCurrentTiles(getActivity().getApplicationContext()));

        // Don't show mobile data options if not supported
        boolean isMobileData = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        if (!isMobileData) {
            QuickSettingsUtil.TILES.remove(QuickSettingsUtil.TILE_MOBILEDATA);
        }
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        ContentResolver resolver = getActivity().getApplicationContext().getContentResolver();
        if (preference == mDynamicAlarm) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_ALARM,
                    mDynamicAlarm.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mDynamicBugReport) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_BUGREPORT,
                    mDynamicBugReport.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mDynamicIme) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_IME,
                    mDynamicIme.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mDynamicWifi) {
            Settings.System.putInt(resolver, Settings.System.QS_DYNAMIC_WIFI,
                    mDynamicWifi.isChecked() ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private class MultiSelectListPreferenceComparator implements Comparator<String> {
        private MultiSelectListPreference pref;

        MultiSelectListPreferenceComparator(MultiSelectListPreference p) {
            pref = p;
        }

        @Override
        public int compare(String lhs, String rhs) {
            return Integer.compare(pref.findIndexOfValue(lhs),
                    pref.findIndexOfValue(rhs));
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mRingMode) {
            ArrayList<String> arrValue = new ArrayList<String>((Set<String>) newValue);
            Collections.sort(arrValue, new MultiSelectListPreferenceComparator(mRingMode));
            Settings.System.putString(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.EXPANDED_RING_MODE, TextUtils.join(SEPARATOR, arrValue));
            updateSummary(TextUtils.join(SEPARATOR, arrValue), mRingMode, R.string.pref_ring_mode_summary);
        }
        return true;
    }

    private void updateSummary(String val, MultiSelectListPreference pref, int defSummary) {
        // Update summary message with current values
        final String[] values = parseStoredValue(val);
        if (values != null) {
            final int length = values.length;
            final CharSequence[] entries = pref.getEntries();
            StringBuilder summary = new StringBuilder();
            for (int i = 0; i < (length); i++) {
                CharSequence entry = entries[Integer.parseInt(values[i])];
                if ((length - i) > 2) {
                    summary.append(entry).append(", ");
                } else if ((length - i) == 2) {
                    summary.append(entry).append(" & ");
                } else if ((length - i) == 1) {
                    summary.append(entry);
                }
            }
            pref.setSummary(summary);
        } else {
            pref.setSummary(defSummary);
        }
    }

    public static String[] parseStoredValue(CharSequence val) {
        if (TextUtils.isEmpty(val)) {
            return null;
        } else {
            return val.toString().split(SEPARATOR);
        }
    }

}
