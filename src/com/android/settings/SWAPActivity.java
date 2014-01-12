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

import com.android.settings.R;
import com.android.settings.Utils;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.preference.CheckBoxPreference;
import android.os.SystemProperties;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.*;

public class SWAPActivity extends PreferenceActivity implements
        Preference.OnPreferenceChangeListener {

    public static final String SWAP_MODE_PREF = "pref_swap_mode";

    public static final String SWAP_SIZE_PREF = "pref_swap_size";

    private static final String TAG = "SWAPSettings";

    private static final String SWAP_FILE = "/mnt/sdcard/.ShockGensMOD.swp";

    private static final String SWAP_ENABLED_PROP = "persist.sys.swap.enabled";

    private static final String SWAP_SIZE_PROP = "persist.sys.swap.size";

    private String DEFAULT_SWAP_SIZE = ""; // Set default SWAP VALUE... FOR THE MOMENT THIS VALUE IS EMPTY

    private Context myContext;

    private String mModeFormat;

    private String mSwapSize;

    private String mSizesFormat;

    private String command;

    private ListPreference mSwapPref;

    private ListPreference mSwapSizesPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = getApplicationContext();
        String temp;
        mModeFormat = getString(R.string.swap_mode_summary);
        mSizesFormat = getString(R.string.swap_sizes_summary);
        String[] availableModes = { "enabled", "disabled" };
        setTitle(R.string.swap_title);
        addPreferencesFromResource(R.xml.swap_settings);
        PreferenceScreen PrefScreen = getPreferenceScreen();
        mSwapPref = (ListPreference) PrefScreen.findPreference(SWAP_MODE_PREF);
        mSwapPref.setEntries(availableModes);
        mSwapPref.setEntryValues(availableModes);
        mSwapSizesPref = (ListPreference) PrefScreen.findPreference(SWAP_SIZE_PREF);
        mSwapSizesPref.setEntries(R.array.swap_sizes_values);
        mSwapSize = DEFAULT_SWAP_SIZE;
        temp = ((swapEnabled() == 1) ? "enabled" : "disabled");
        if (! android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            mSwapPref.setSummary("SD Card not found");
            mSwapPref.setEnabled(false);
            mSwapSizesPref.setEnabled(false);
        }
        else {
            mSwapPref.setValue(temp);
            mSwapPref.setSummary(String.format(mModeFormat, temp));
            mSwapPref.setOnPreferenceChangeListener(this);
            if (swapEnabled() == 0)
                mSwapSizesPref.setEnabled(false);
            else {
                temp = (SystemProperties.get(SWAP_SIZE_PROP).equals("") ? mSwapSize : SystemProperties.get(SWAP_SIZE_PROP));
                mSwapSizesPref.setEnabled(true);
                mSwapSizesPref.setValue(temp);
                mSwapSizesPref.setSummary(String.format(mSizesFormat, temp));
                mSwapSizesPref.setOnPreferenceChangeListener(this);
            }
        }
    }

    @Override
    public void onResume() {
        String temp;
        super.onResume();
        temp = ((swapEnabled() == 1) ? "enabled" : "disabled");
        if (! android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            mSwapPref.setSummary("SD Card not found");
            mSwapPref.setEnabled(false);
            mSwapSizesPref.setEnabled(false);
        }
        else {
            mSwapPref.setValue(temp);
            mSwapPref.setSummary(String.format(mModeFormat, temp));
            if (swapEnabled() == 0)
                mSwapSizesPref.setEnabled(false);
            else {
                temp = (SystemProperties.get(SWAP_SIZE_PROP).equals("") ? mSwapSize : SystemProperties.get(SWAP_SIZE_PROP));
                mSwapSizesPref.setEnabled(true);
                mSwapSizesPref.setValue(temp);
                mSwapSizesPref.setSummary(String.format(mSizesFormat, temp));
            }
        }
    }

    private class runShellCommand extends Thread {
        private String command = "";
        private String toastMessage = "";

        public runShellCommand(String command, String toastMessage) {
            this.command = command;
            this.toastMessage = toastMessage;
        }

        @Override
        public void run() {
        try {
            if (!command.equals("")) {
                Process process = Runtime.getRuntime().exec("su");
                Log.d(TAG, "Executing: " + command);
                DataOutputStream outputStream = new DataOutputStream(process.getOutputStream());
                DataInputStream inputStream = new DataInputStream(process.getInputStream());
                outputStream.writeBytes(command + "\n");
                outputStream.flush();
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                process.waitFor();
            }
            } catch (IOException e) {
                Log.e(TAG, "Thread IOException");
            }
            catch (InterruptedException e) {
                Log.e(TAG, "Thread InterruptedException");
            }
            Message messageToThread = new Message();
            Bundle messageData = new Bundle();
            messageToThread.what = 0;
            messageData.putString("toastMessage", toastMessage);
            messageToThread.setData(messageData);
            mrunShellCommandHandler.sendMessage(messageToThread);
        }
    };

    private runShellCommand mrunShellCommand;

    private Handler mrunShellCommandHandler = new Handler() {
        public void handleMessage(Message msg) {
            CharSequence text="";
            Bundle messageData = msg.getData();
            text = messageData.getString("toastMessage", "");
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(myContext, text, duration);
            toast.show();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (mrunShellCommand != null)
                mrunShellCommand.join();
        } catch (InterruptedException e) {
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue != null) {
            if (preference == mSwapPref) {
                setMode((String) newValue);
                return true;
            }
            else if (preference == mSwapSizesPref) {
                mSwapSize = (String) newValue;
                setMode((swapEnabled() == 1) ? "enabled" : "disabled");
                mSwapSizesPref.setSummary(String.format(mSizesFormat, (String) newValue));
                return true;
            }
        }
        return false;
    }

    public static int swapEnabled() {
        String swap_enabled = SystemProperties.get(SWAP_ENABLED_PROP);
        if (swap_enabled.equals("") || swap_enabled.equals("0"))
            return 0;
        else
            return 1;
    }

    public void setMode(String mode) {
        if (mode.equals("disabled")) {
            makeSwap(0, "0");
            SystemProperties.set(SWAP_ENABLED_PROP, "0");
            mSwapSizesPref.setEnabled(false);
        }
        else if (mode.equals("enabled")) {
            SystemProperties.set(SWAP_ENABLED_PROP, "0");
            mSwapSizesPref.setEnabled(false);
            makeSwap(1, mSwapSize);
            SystemProperties.set(SWAP_ENABLED_PROP, "1");
            SystemProperties.set(SWAP_SIZE_PROP, mSwapSize);
            mSwapSizesPref.setEnabled(true);
        }
        String temp = ((swapEnabled() == 1) ? "enabled" : "disabled");
        mSwapPref.setValue(temp);
        mSwapPref.setSummary(String.format(mModeFormat, temp));
    }

    public int makeSwap(int mode, String mSwapSize) {
        String command = "";
        String toastText = "";
        if (mode == 0) {//remove swap
            command = "swapoff " + SWAP_FILE + " && rm " + SWAP_FILE;
            toastText = getString(R.string.swap_toast_swap_disabled);
        }
        else if (mode == 1) {//remove previous swap, make new swapfile and swapon
            command = "swapoff " + SWAP_FILE + "; rm -f " + SWAP_FILE;
            command += "&& dd if=/dev/zero of=" + SWAP_FILE + " bs=1024 count=" + mSwapSize + "000 && mkswap " + SWAP_FILE + " && swapon " + SWAP_FILE;
            toastText = getString(R.string.swap_toast_swap_enabled);
        }
        mrunShellCommand = new runShellCommand(command, toastText);
        mrunShellCommand.start();
        return 0;
    }
}
