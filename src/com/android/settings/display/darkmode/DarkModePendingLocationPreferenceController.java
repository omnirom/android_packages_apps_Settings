/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.display.darkmode;

import static android.app.UiModeManager.MODE_NIGHT_AUTO;

import static com.google.common.base.Preconditions.checkNotNull;

import android.app.UiModeManager;
import android.content.Context;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.FooterPreference;

/** Controller for the Twilight location custom footer. */
public class DarkModePendingLocationPreferenceController extends BasePreferenceController {
    private final UiModeManager mUiModeManager;
    private final LocationManager mLocationManager;

    public DarkModePendingLocationPreferenceController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        mUiModeManager = context.getSystemService(UiModeManager.class);
        mLocationManager = context.getSystemService(LocationManager.class);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        FooterPreference footerPreference = checkNotNull(screen.findPreference(getPreferenceKey()));
        footerPreference.setIcon(R.drawable.ic_settings_location_filled);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setVisible(isActive());
    }

    public boolean isActive() {
        return mUiModeManager.getNightMode() == MODE_NIGHT_AUTO
                && mLocationManager.isLocationEnabled()
                && mLocationManager.getLastLocation() == null;
    }
}
