/*
 * Copyright (C) 2014 AnimeROM
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

package com.android.settings;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;

import android.content.Context;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class PerfomanceHack extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String GMAPS_HACK_PREF = "pref_gmaps_hack";

    private static final String GMAPS_HACK_PERSIST_PROP = "persist.sys.gmaps_hack";

    private static final String GMAPS_HACK_DEFAULT = "0";

    private static final String GENERAL_CATEGORY = "general_category";

    private static final String JIT_PREF = "pref_jit_mode";

    private static final String JIT_ENABLED = "int:jit";

    private static final String JIT_DISABLED = "int:fast";

    private static final String JIT_PERSIST_PROP = "persist.sys.jit-mode";

    private static final String JIT_PROP = "dalvik.vm.execution-mode";

    private static final String HEAPSIZE_PREF = "pref_heapsize";

    private static final String HEAPSIZE_PROP = "dalvik.vm.heapsize";

    private static final String HEAPSIZE_PERSIST_PROP = "persist.sys.vm.heapsize";

    private static final String HEAPSIZE_DEFAULT = "48m";

    private static final String SDCARDCACHESIZE_PREF = "pref_sdcardcachesize";

    private static final String SDCARDCACHESIZE_PROP = "sys.sdcardcache.readsize";

    private static final String SDCARDCACHESIZE_PERSIST_PROP = "persist.sys.sdcardcachereadsize";

    private static final String SDCARDCACHESIZE_DEFAULT = "2048KB";

    private static final String DISABLE_BOOTANIMATION_PREF = "pref_disable_bootanimation";

    private static final String DISABLE_BOOTANIMATION_PERSIST_PROP = "persist.sys.nobootanimation";

    private static final String DISABLE_BOOTANIMATION_DEFAULT = "0";

    private CheckBoxPreference mGmapsHackPref;


    private CheckBoxPreference mJitPref;

    private CheckBoxPreference mDisableBootanimPref;

    private ListPreference mHeapsizePref;

    private ListPreference mSdcardcachesizePref;

    private int swapAvailable = 1;

    private int ksmAvailable = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {

            addPreferencesFromResource(R.xml.perfomance_hack);

            PreferenceScreen prefSet = getPreferenceScreen();

            String temp;

            mGmapsHackPref = (CheckBoxPreference) prefSet.findPreference(GMAPS_HACK_PREF);

            mJitPref = (CheckBoxPreference) prefSet.findPreference(JIT_PREF);
            String jitMode = SystemProperties.get(JIT_PERSIST_PROP,
                    SystemProperties.get(JIT_PROP, JIT_ENABLED));
            mJitPref.setChecked(JIT_ENABLED.equals(jitMode));

            mHeapsizePref = (ListPreference) prefSet.findPreference(HEAPSIZE_PREF);
            mHeapsizePref.setValue(SystemProperties.get(HEAPSIZE_PERSIST_PROP,
                    SystemProperties.get(HEAPSIZE_PROP, HEAPSIZE_DEFAULT)));
            mHeapsizePref.setOnPreferenceChangeListener(this);

	    mSdcardcachesizePref = (ListPreference) prefSet.findPreference(SDCARDCACHESIZE_PREF);
	    mSdcardcachesizePref.setValue(SystemProperties.get(SDCARDCACHESIZE_PERSIST_PROP,
                    SystemProperties.get(SDCARDCACHESIZE_PROP, SDCARDCACHESIZE_DEFAULT)));
            mSdcardcachesizePref.setOnPreferenceChangeListener(this);

            mDisableBootanimPref = (CheckBoxPreference) prefSet.findPreference(DISABLE_BOOTANIMATION_PREF);
            String disableBootanimation = SystemProperties.get(DISABLE_BOOTANIMATION_PERSIST_PROP, DISABLE_BOOTANIMATION_DEFAULT);
            mDisableBootanimPref.setChecked("1".equals(disableBootanimation));

            String gmapshack = SystemProperties.get(GMAPS_HACK_PERSIST_PROP, GMAPS_HACK_DEFAULT);
            mGmapsHackPref.setChecked("1".equals(gmapshack));

            }

        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mGmapsHackPref) {
            SystemProperties.set(GMAPS_HACK_PERSIST_PROP,
                    mGmapsHackPref.isChecked() ? "1" : "0");
            return true;
        }

        if (preference == mJitPref) {
            SystemProperties.set(JIT_PERSIST_PROP,
                    mJitPref.isChecked() ? JIT_ENABLED : JIT_DISABLED);
            return true;
        }

        if (preference == mDisableBootanimPref) {
            SystemProperties.set(DISABLE_BOOTANIMATION_PERSIST_PROP,
                    mDisableBootanimPref.isChecked() ? "1" : "0");
            return true;
        }

        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHeapsizePref) {
            if (newValue != null) {
                SystemProperties.set(HEAPSIZE_PERSIST_PROP, (String)newValue);
                return true;
            }
        }

        if (preference == mSdcardcachesizePref) {
            if (newValue != null) {
                SystemProperties.set(SDCARDCACHESIZE_PERSIST_PROP, (String)newValue);
                return true;
            }
        }

        return false;
    }

    private boolean isKsmAvailable() {
        if (ksmAvailable < 0) {
            ksmAvailable = new File(KSM_RUN_FILE).exists() ? 1 : 0;
        }
        return ksmAvailable > 0;
    }
}
