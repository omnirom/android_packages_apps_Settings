/*
 * Copyright (C) 2017 ABC rom
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.AbstractPreferenceController;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_AMBIENT_DISPLAY;

public class DozeAutoBrightnessPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin,
        Preference.OnPreferenceChangeListener {

    private static final String KEY_AMBIENT_DOZE_AUTO_BRIGHTNESS = "ambient_doze_auto_brightness";

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final AmbientDisplayConfiguration mConfig;

    public DozeAutoBrightnessPreferenceController(Context context,
            AmbientDisplayConfiguration config, MetricsFeatureProvider metricsFeatureProvider) {
        super(context);
        mMetricsFeatureProvider = metricsFeatureProvider;
        // AmbientDisplayConfiguration
        mConfig = config;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_AMBIENT_DOZE_AUTO_BRIGHTNESS;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_AMBIENT_DOZE_AUTO_BRIGHTNESS.equals(preference.getKey())) {
            mMetricsFeatureProvider.action(mContext, ACTION_AMBIENT_DISPLAY);
        }
        return false;
    }

    @Override
    public void updateState(Preference preference) {
        SwitchPreference mAmbientDozeAutoBrightness =
                (SwitchPreference) preference;
        boolean defaultAmbientDozeAutoBrighthness = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_allowAutoBrightnessWhileDozing);
        boolean isAmbientDozeAutoBrighthness = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_DOZE_AUTO_BRIGHTNESS, defaultAmbientDozeAutoBrighthness ? 1 : 0,
                UserHandle.myUserId()) == 1;
        mAmbientDozeAutoBrightness.setChecked(isAmbientDozeAutoBrighthness);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean value = (Boolean) newValue;
        Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_DOZE_AUTO_BRIGHTNESS, value ? 1 : 0, UserHandle.myUserId());
        return true;
    }

    @Override
    public boolean isAvailable() {
        return mConfig.available();
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                AmbientDisplaySettings.class.getName(), KEY_AMBIENT_DOZE_AUTO_BRIGHTNESS,
                mContext.getString(R.string.ambient_display_screen_title));

        return new InlineSwitchPayload(Settings.Secure.DOZE_ENABLED,
                ResultPayload.SettingsSource.SECURE, 1 /* onValue */, intent, isAvailable(),
                1 /* defaultValue */);
    }
}
