/*
 * Copyright (C) 2012 Slimroms
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class PowerMenuStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;

    private static final String POWER_MENU_TEXT_COLOR =
        "pref_power_menu_text_color";
    private static final String POWER_MENU_ICON_COLOR =
        "pref_power_menu_icon_color";
    private static final String POWER_MENU_COLOR_MODE =
        "pref_power_menu_color_mode";

    private ColorPickerPreference mPowerMenuColor;
    private ColorPickerPreference mPowerMenuTextColor;
    private ListPreference mPowerMenuColorMode;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    public PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefSet = getPreferenceScreen();
        if (prefSet != null) {
            prefSet.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.power_menu_style);

        prefSet = getPreferenceScreen();

        mPowerMenuColor =
            (ColorPickerPreference) findPreference(POWER_MENU_ICON_COLOR);
        int intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.POWER_MENU_ICON_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                com.android.internal.R.color.power_menu_icon_default_color);
            mPowerMenuColor.setSummary(
                getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPowerMenuColor.setSummary(hexColor);
        }
        mPowerMenuColor.setNewPreviewColor(intColor);
        mPowerMenuColor.setOnPreferenceChangeListener(this);

        mPowerMenuTextColor =
            (ColorPickerPreference) findPreference(POWER_MENU_TEXT_COLOR);
        intColor = Settings.System.getInt(getContentResolver(),
                    Settings.System.POWER_MENU_TEXT_COLOR, -2);
        if (intColor == -2) {
            intColor = getResources().getColor(
                com.android.internal.R.color.power_menu_text_default_color);
            mPowerMenuTextColor.setSummary(
                getResources().getString(R.string.default_string));
        } else {
            String hexColor = String.format("#%08x", (0xffffffff & intColor));
            mPowerMenuTextColor.setSummary(hexColor);
        }
        mPowerMenuTextColor.setNewPreviewColor(intColor);
        mPowerMenuTextColor.setOnPreferenceChangeListener(this);

        mPowerMenuColorMode = (ListPreference) prefSet.findPreference(
                POWER_MENU_COLOR_MODE);
        mPowerMenuColorMode.setValue(String.valueOf(
                Settings.System.getIntForUser(getContentResolver(),
                Settings.System.POWER_MENU_ICON_COLOR_MODE, 0,
                UserHandle.USER_CURRENT_OR_SELF)));
        mPowerMenuColorMode.setSummary(mPowerMenuColorMode.getEntry());
        mPowerMenuColorMode.setOnPreferenceChangeListener(this);

        updateColorPreference();

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        return prefSet;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mPowerMenuColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_MENU_ICON_COLOR, intHex);
            return true;
        } else if (preference == mPowerMenuColorMode) {
            String val = (String) newValue;
            Settings.System.putInt(getContentResolver(),
                Settings.System.POWER_MENU_ICON_COLOR_MODE,
                Integer.valueOf(val));
            int index = mPowerMenuColorMode.findIndexOfValue(val);
            mPowerMenuColorMode.setSummary(
                mPowerMenuColorMode.getEntries()[index]);
            updateColorPreference();
            return true;
        } if (preference == mPowerMenuTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_MENU_TEXT_COLOR, intHex);
            return true;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void updateColorPreference() {
        int colorMode = Settings.System.getInt(getContentResolver(),
                Settings.System.POWER_MENU_ICON_COLOR_MODE, 0);
        mPowerMenuColor.setEnabled(colorMode != 3);
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

        PowerMenuStyle getOwner() {
            return (PowerMenuStyle) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_MENU_ICON_COLOR, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                   Settings.System.POWER_MENU_ICON_COLOR_MODE, 0);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_MENU_TEXT_COLOR, -2);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {

        }
    }

}
