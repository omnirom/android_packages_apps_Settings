/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class BuildUserPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_BUILD_USER = "build_user";
    
    private static final String PROPERTY_BUILD_USER = "ro.build.user";
    
    private static final String PROPERTY_BUILD_HOST = "ro.build.host";

    private static final String PROPERTY_CUST_BUILD_USER = "ro.omni.builder";

    public BuildUserPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        
        super.updateState(preference);
        
        String cust_build_user = SystemProperties.get(PROPERTY_CUST_BUILD_USER);
        String build_user = SystemProperties.get(PROPERTY_BUILD_USER);
        String build_host = SystemProperties.get(PROPERTY_BUILD_HOST);
        String build_user_host = build_user + "@" + build_host;
        
        if (cust_build_user != null && cust_build_user.length() > 0) {

            preference.setSummary(cust_build_user);

        } else {

            preference.setSummary(build_user_host);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BUILD_USER;
    }
}
