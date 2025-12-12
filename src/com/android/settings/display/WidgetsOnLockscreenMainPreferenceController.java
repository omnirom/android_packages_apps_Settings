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

package com.android.settings.display;

import android.content.Context;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.SettingsMainSwitchPreference;

/** Controls the "widgets on lock screen" main toggle on the "widgets on lock screen" screen. */
public class WidgetsOnLockscreenMainPreferenceController extends TogglePreferenceController
        implements OnCheckedChangeListener {
    public WidgetsOnLockscreenMainPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        SettingsMainSwitchPreference pref = screen.findPreference(getPreferenceKey());
        pref.addOnSwitchChangeListener(this);
        pref.setChecked(WidgetsOnLockscreenPreferenceController.isEnabled(mContext));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked != isChecked()) {
            setChecked(isChecked);
        }
    }

    @Override
    public boolean isChecked() {
        return WidgetsOnLockscreenPreferenceController.isEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        WidgetsOnLockscreenPreferenceController.setEnabled(mContext, isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }
}
