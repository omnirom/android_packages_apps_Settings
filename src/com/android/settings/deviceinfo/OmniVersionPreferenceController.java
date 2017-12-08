/*
 *  Copyright (C) 2017 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
*/
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.text.TextUtils;

import com.android.settingslib.core.AbstractPreferenceController;

public class OmniVersionPreferenceController extends AbstractPreferenceController {

    private static final String KEY_OMNI_VERSION = "mod_version";
    private static final String PROPERTY_OMNI_VERSION = "ro.omni.version";

    public OmniVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(SystemProperties.get(PROPERTY_OMNI_VERSION));
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(SystemProperties.get(PROPERTY_OMNI_VERSION));
    }

    @Override
    public String getPreferenceKey() {
        return KEY_OMNI_VERSION;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_OMNI_VERSION)) {
            try {
                final Intent intent = new Intent();
                ComponentName cn = new ComponentName("org.omnirom.games.eggs", "org.omnirom.games.eggs.StartingActivity");
                intent.setComponent(cn);
                mContext.startActivity(intent);
            } catch (Resources.NotFoundException | ActivityNotFoundException e) {
            }
        }
        return false;
    }
}
