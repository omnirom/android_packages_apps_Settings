/*
 * Copyright (C) 2014 Slimroms
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

package com.android.settings.slim;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PieTriggerSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final int DLG_WARNING = 0;

    // This equals EdgeGesturePosition.LEFT.FLAG
    private static final int DEFAULT_POSITION = 1 << 0;

    private static final String PREF_PIE_DISABLE_IME_TRIGGERS = "pie_disable_ime_triggers";

    private static final String[] TRIGGER = {
        "pie_control_trigger_left",
        "pie_control_trigger_bottom",
        "pie_control_trigger_right",
        "pie_control_trigger_top"
    };

    private CheckBoxPreference[] mTrigger = new CheckBoxPreference[4];
    private CheckBoxPreference mDisableImeTriggers;

    private ContentObserver mPieTriggerObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updatePieTriggers();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pie_trigger);

        PreferenceScreen prefSet = getPreferenceScreen();

        for (int i = 0; i < TRIGGER.length; i++) {
            mTrigger[i] = (CheckBoxPreference) prefSet.findPreference(TRIGGER[i]);
            mTrigger[i].setOnPreferenceChangeListener(this);
        }

        mDisableImeTriggers = (CheckBoxPreference) findPreference(PREF_PIE_DISABLE_IME_TRIGGERS);
        mDisableImeTriggers.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        int triggerSlots = 0;
        int counter = 0;
        if (preference == mDisableImeTriggers) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_IME_CONTROL,
                    (Boolean) newValue ? 1 : 0);
        } else {
        for (int i = 0; i < mTrigger.length; i++) {
            boolean checked = preference == mTrigger[i]
                    ? (Boolean) newValue : mTrigger[i].isChecked();
            if (checked) {
                if (!TRIGGER[i].equals("pie_control_trigger_top")) {
                    counter++;
                }
                triggerSlots |= 1 << i;
            }
        }
        if (counter == 0) {
            showDialogInner(DLG_WARNING);
            return true;
        }
        Settings.System.putInt(getContentResolver(),
                Settings.System.PIE_GRAVITY, triggerSlots);
        }
        updatePieTriggers();
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.PIE_GRAVITY), true,
                mPieTriggerObserver);

        updatePieTriggers();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mPieTriggerObserver);
    }

    private void updatePieTriggers() {
        int triggerSlots = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_GRAVITY, DEFAULT_POSITION);

        for (int i = 0; i < mTrigger.length; i++) {
            if ((triggerSlots & (0x01 << i)) != 0) {
                mTrigger[i].setChecked(true);
            } else {
                mTrigger[i].setChecked(false);
            }
        }

        mDisableImeTriggers.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_IME_CONTROL, 1) == 1);
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        PieTriggerSettings getOwner() {
            return (PieTriggerSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_WARNING:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(R.string.pie_trigger_warning)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().updatePieTriggers();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_WARNING:
                    getOwner().updatePieTriggers();
                    break;
            }
        }
    }
}
