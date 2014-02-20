package com.android.settings;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class AmraSettings extends SettingsPreferenceFragment {
    private static final String TAG = "AmraSettings";

 @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.amra_settings);
        ContentResolver resolver = getContentResolver();
        }

    @Override
    public void onResume() {
        super.onResume();
    }
}
