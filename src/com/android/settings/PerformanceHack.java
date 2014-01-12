/*
 * Copyright (C) 2014 ShockGensMOD
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

    public static final String KSM_RUN_FILE = "/sys/kernel/mm/ksm/run";

    public static final String KSM_PREF = "pref_ksm";

    public static final String KSM_PREF_DISABLED = "0";

    public static final String KSM_PREF_ENABLED = "1";

    public static final String KSM_SLEEP_RUN_FILE = "/sys/kernel/mm/ksm/sleep_millisecs";

    public static final String KSM_SLEEP_PREF = "pref_ksm_sleep";

    private static final String KSM_SLEEP_PROP = "ksm_sleep_time";

    public static final String KSM_SLEEP_PREF_DEFAULT = "2000";

    public static final String KSM_SCAN_RUN_FILE = "/sys/kernel/mm/ksm/pages_to_scan";

    public static final String KSM_SCAN_PREF = "pref_ksm_scan";

    private static final String KSM_SCAN_PROP = "ksm_scan_time";

    public static final String KSM_SCAN_PREF_DEFAULT = "128";

    public static final String LOWMEMKILL_RUN_FILE = "/sys/module/lowmemorykiller/parameters/minfree";

    public static final String LOWMEMKILL_PREF = "pref_lowmemkill";

    private static final String LOWMEMKILL_PROP = "lowmemkill";

    public static final String LOWMEMKILL_PREF_DEFAULT = "6400,8960,19200,23040,38400,64000";

    public static final String ADJ_RUN_FILE = "/sys/module/lowmemorykiller/parameters/adj";

    public static final String ADJ_PREF = "pref_adj";

    private static final String ADJ_PROP = "adj";

    public static final String ADJ_PREF_DEFAULT = "0,1,2,3,4,10";

    public static final String VSYNC_RUN_FILE = "/d/clk/mdp_vsync_clk/enable";

    public static final String VSYNC_PREF = "pref_vsync";

    private static final String VSYNC_PROP = "vsync";

    public static final String VSYNC_PREF_DEFAULT = "1";

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

    private CheckBoxPreference mKSMPref;

    private ListPreference mKSMSleepPref;

    private ListPreference mKSMScanPref;

    private ListPreference mLowMemKillPref;

    private ListPreference mADJKillPref;

    private ListPreference mVSYNCPref;

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
            mKSMPref = (CheckBoxPreference) prefSet.findPreference(KSM_PREF);
            mKSMSleepPref = (ListPreference) prefSet.findPreference(KSM_SLEEP_PREF);
            mKSMScanPref = (ListPreference) prefSet.findPreference(KSM_SCAN_PREF);
            mLowMemKillPref = (ListPreference) prefSet.findPreference(LOWMEMKILL_PREF);
            mADJKillPref = (ListPreference) prefSet.findPreference(ADJ_PREF);
            mVSYNCPref = (ListPreference) prefSet.findPreference(VSYNC_PREF);

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

            if (isKsmAvailable()) {
                mKSMPref.setChecked(KSM_PREF_ENABLED.equals(Utils.fileReadOneLine(KSM_RUN_FILE)));
            } else {
                prefSet.removePreference(mKSMPref);
            }

            if (isKsmAvailable()) {
                temp = Utils.fileReadOneLine(KSM_SLEEP_RUN_FILE);
                mKSMSleepPref.setValue(temp);
                mKSMSleepPref.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mKSMSleepPref);
            }

            if (isKsmAvailable()) {
                temp = Utils.fileReadOneLine(KSM_SCAN_RUN_FILE);
                mKSMScanPref.setValue(temp);
                mKSMScanPref.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mKSMScanPref);
            }

            temp = Utils.fileReadOneLine(LOWMEMKILL_RUN_FILE);

            mLowMemKillPref.setValue(temp);
            mLowMemKillPref.setOnPreferenceChangeListener(this);

            if (temp == null) {
                prefSet.removePreference(mLowMemKillPref);
            }

            temp = Utils.fileReadOneLine(ADJ_RUN_FILE);

            mADJKillPref.setValue(temp);
            mADJKillPref.setOnPreferenceChangeListener(this);

            if (temp == null) {
                prefSet.removePreference(mADJKillPref);
            }

            mVSYNCPref = (ListPreference) prefSet.findPreference(VSYNC_PREF);

            temp = Utils.fileReadOneLine(VSYNC_RUN_FILE);

            mVSYNCPref.setValue(temp);
            mVSYNCPref.setOnPreferenceChangeListener(this);

            if (temp == null) {
                prefSet.removePreference(mVSYNCPref);
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

        if (preference == mKSMPref) {
            Utils.fileWriteOneLine(KSM_RUN_FILE, mKSMPref.isChecked() ? "1" : "0");
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
        if (preference == mKSMSleepPref) {
            if (newValue != null) {
                SystemProperties.set(KSM_SLEEP_PROP, (String)newValue);
                Utils.fileWriteOneLine(KSM_SLEEP_RUN_FILE, (String)newValue);
                return true;
            }
        }

        if (preference == mKSMScanPref) {
            if (newValue != null) {
                SystemProperties.set(KSM_SCAN_PROP, (String)newValue);
                Utils.fileWriteOneLine(KSM_SCAN_RUN_FILE, (String)newValue);
                return true;
            }
        }

        if (preference == mLowMemKillPref) {
            if (newValue != null) {
                SystemProperties.set(LOWMEMKILL_PROP, (String)newValue);
                Utils.fileWriteOneLine(LOWMEMKILL_RUN_FILE, (String)newValue);
                return true;
            }
        }

        if (preference == mADJKillPref) {
            if (newValue != null) {
                SystemProperties.set(ADJ_PROP, (String)newValue);
                Utils.fileWriteOneLine(ADJ_RUN_FILE, (String)newValue);
                return true;
            }
        }

        if (preference == mVSYNCPref) {
            if (newValue != null) {
                SystemProperties.set(VSYNC_PROP, (String)newValue);
                Utils.fileWriteOneLine(VSYNC_RUN_FILE, (String)newValue);
                return true;
            }
        }

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
