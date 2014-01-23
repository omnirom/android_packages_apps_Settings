/*
 * Copyright (C) 2014 AnimeROM
 * Miguel Angel Sánchez Bravo
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

package com.android.settings.animerom;

import com.android.settings.R;

import java.io.File;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import com.android.settings.SettingsPreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;


public class PerformanceHack extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String COMPCACHE_PREF = "pref_compcache_size";

    private static final String COMPCACHE_PERSIST_PROP = "persist.service.compcache";

    private static final String COMPCACHE_DEFAULT = SystemProperties.get("ro.compcache.default");

    private static final String PURGEABLE_ASSETS_PREF = "pref_purgeable_assets";

    private static final String PURGEABLE_ASSETS_PERSIST_PROP = "persist.sys.purgeable_assets";

    private static final String PURGEABLE_ASSETS_DEFAULT = "0";

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

    public static final String LOWMEMKILL_PREF_DEFAULT = "2560,4096,6144,11264,11776,14336";

    private ListPreference mCompcachePref;

    private CheckBoxPreference mPurgeableAssetsPref;

    private CheckBoxPreference mGmapsHackPref;

    private CheckBoxPreference mKSMPref;

    private ListPreference mKSMSleepPref;

    private ListPreference mKSMScanPref;

    private ListPreference mLowMemKillPref;

    private int swapAvailable = -1;

    private int ksmAvailable = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getPreferenceManager() != null) {

            addPreferencesFromResource(R.xml.performance_hack);

            PreferenceScreen prefSet = getPreferenceScreen();

            String temp;

            mCompcachePref = (ListPreference) prefSet.findPreference(COMPCACHE_PREF);
            mPurgeableAssetsPref = (CheckBoxPreference) prefSet.findPreference(PURGEABLE_ASSETS_PREF);
            mGmapsHackPref = (CheckBoxPreference) prefSet.findPreference(GMAPS_HACK_PREF);
            mKSMPref = (CheckBoxPreference) prefSet.findPreference(KSM_PREF);
            mKSMSleepPref = (ListPreference) prefSet.findPreference(KSM_SLEEP_PREF);
            mKSMScanPref = (ListPreference) prefSet.findPreference(KSM_SCAN_PREF);
            mLowMemKillPref = (ListPreference) prefSet.findPreference(LOWMEMKILL_PREF);

            if (isSwapAvailable()) {
                if (SystemProperties.get(COMPCACHE_PERSIST_PROP) == "1")
                    SystemProperties.set(COMPCACHE_PERSIST_PROP, COMPCACHE_DEFAULT);
                mCompcachePref.setValue(SystemProperties.get(COMPCACHE_PERSIST_PROP, COMPCACHE_DEFAULT));
                mCompcachePref.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mCompcachePref);
            }

            String purgeableAssets = SystemProperties.get(PURGEABLE_ASSETS_PERSIST_PROP,
                    PURGEABLE_ASSETS_DEFAULT);
            mPurgeableAssetsPref.setChecked("1".equals(purgeableAssets));

            String gmapshack = SystemProperties.get(GMAPS_HACK_PERSIST_PROP, GMAPS_HACK_DEFAULT);
            mGmapsHackPref.setChecked("1".equals(gmapshack));

            if (isKsmAvailable()) {
                mKSMPref.setChecked(KSM_PREF_ENABLED.equals(CPUActivity.readOneLine(KSM_RUN_FILE)));
            } else {
                prefSet.removePreference(mKSMPref);
            }

            if (isKsmAvailable()) {
                temp = CPUActivity.readOneLine(KSM_SLEEP_RUN_FILE);
                mKSMSleepPref.setValue(temp);
                mKSMSleepPref.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mKSMSleepPref);
            }

            if (isKsmAvailable()) {
                temp = CPUActivity.readOneLine(KSM_SCAN_RUN_FILE);
                mKSMScanPref.setValue(temp);
                mKSMScanPref.setOnPreferenceChangeListener(this);
            } else {
                prefSet.removePreference(mKSMScanPref);
            }

            temp = CPUActivity.readOneLine(LOWMEMKILL_RUN_FILE);

            mLowMemKillPref.setValue(temp);
            mLowMemKillPref.setOnPreferenceChangeListener(this);

            if (temp == null) {
                prefSet.removePreference(mLowMemKillPref);
            }
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mPurgeableAssetsPref) {
            SystemProperties.set(PURGEABLE_ASSETS_PERSIST_PROP,
                    mPurgeableAssetsPref.isChecked() ? "1" : "0");
            return true;
        }

        if (preference == mGmapsHackPref) {
            SystemProperties.set(GMAPS_HACK_PERSIST_PROP,
                    mGmapsHackPref.isChecked() ? "1" : "0");
            return true;
        }

        if (preference == mKSMPref) {
            CPUActivity.writeOneLine(KSM_RUN_FILE, mKSMPref.isChecked() ? "1" : "0");
            return true;
        }

        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCompcachePref) {
            if (newValue != null) {
                SystemProperties.set(COMPCACHE_PERSIST_PROP, (String) newValue);
                return true;
            }
        }

        if (preference == mKSMSleepPref) {
            if (newValue != null) {
                SystemProperties.set(KSM_SLEEP_PROP, (String)newValue);
                CPUActivity.writeOneLine(KSM_SLEEP_RUN_FILE, (String)newValue);
                return true;
            }
        }

        if (preference == mKSMScanPref) {
            if (newValue != null) {
                SystemProperties.set(KSM_SCAN_PROP, (String)newValue);
                CPUActivity.writeOneLine(KSM_SCAN_RUN_FILE, (String)newValue);
                return true;
            }
        }

        if (preference == mLowMemKillPref) {
            if (newValue != null) {
                SystemProperties.set(LOWMEMKILL_PROP, (String)newValue);
                CPUActivity.writeOneLine(LOWMEMKILL_RUN_FILE, (String)newValue);
                return true;
            }
        }

        return false;
    }

    /**
     * Check if swap support is available on the system
     */
    private boolean isSwapAvailable() {
        if (swapAvailable < 0) {
            swapAvailable = new File("/proc/swaps").exists() ? 1 : 0;
        }
        return swapAvailable > 0;
    }

    /**
     * Check if KSM support is available on the system
     */
    private boolean isKsmAvailable() {
        if (ksmAvailable < 0) {
            ksmAvailable = new File(KSM_RUN_FILE).exists() ? 1 : 0;
        }
        return ksmAvailable > 0;
    }
}
