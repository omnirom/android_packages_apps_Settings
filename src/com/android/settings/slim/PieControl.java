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
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.util.slim.SlimActions;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class PieControl extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String PIE_CONTROL = "pie_control";
    private static final String PIE_MENU = "pie_menu";

    private static final int DLG_NAVIGATION_WARNING = 0;

    private CheckBoxPreference mPieControl;
    private ListPreference mPieMenuDisplay;

    private SettingsObserver mSettingsObserver = new SettingsObserver(new Handler());
    private final class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = getActivity().getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PIE_CONTROLS), false, this,
                    UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateSettings();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pie_control);

        PreferenceScreen prefSet = getPreferenceScreen();

        mPieControl = (CheckBoxPreference) prefSet.findPreference(PIE_CONTROL);
        mPieControl.setOnPreferenceChangeListener(this);

        mPieMenuDisplay = (ListPreference) prefSet.findPreference(PIE_MENU);
        mPieMenuDisplay.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mPieControl) {
            if (!((Boolean) newValue) && !SlimActions.isNavBarEnabled(getActivity())
                    && SlimActions.isNavBarDefault(getActivity())) {
                showDialogInner(DLG_NAVIGATION_WARNING);
                return true;
            }
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_CONTROLS, (Boolean) newValue ? 1 : 0);
        } else if (preference == mPieMenuDisplay) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.PIE_MENU, Integer.parseInt((String) newValue));
        }
        return true;
    }

    private void updateSettings() {
        mPieMenuDisplay.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_MENU,
                2) + "");
        mPieControl.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_CONTROLS, 0) == 1);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSettings();
        mSettingsObserver.observe();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mSettingsObserver);
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

        PieControl getOwner() {
            return (PieControl) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_NAVIGATION_WARNING:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(R.string.pie_warning_no_navigation_present)
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.PIE_CONTROLS, 0);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.NAVIGATION_BAR_SHOW, 1);

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
                case DLG_NAVIGATION_WARNING:
                    getOwner().mPieControl.setChecked(true);
                    break;
            }
        }
    }
}
