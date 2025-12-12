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

import static android.provider.Settings.Secure.GLANCEABLE_HUB_ENABLED;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.systemui.Flags;

/** Controls the "widgets on lock screen" preferences (under "Display & touch > Lock screen"). */
public class WidgetsOnLockscreenPreferenceController extends TogglePreferenceController {
    private static final String TAG = "WidgetsOnLockscreen";

    public WidgetsOnLockscreenPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        disablePreferenceIfManaged(preference);
    }

    @Override
    public boolean isChecked() {
        return isEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        setEnabled(mContext, isChecked);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isMainUser()) {
            return UNSUPPORTED_ON_DEVICE;
        }

        // This config should only be true on tablet.
        if (mContext.getResources().getBoolean(R.bool.config_show_glanceable_hub_toggle_setting)) {
            return AVAILABLE;
        }

        if (Flags.glanceableHubV2()
                && mContext.getResources().getBoolean(
                R.bool.config_show_glanceable_hub_toggle_setting_mobile)) {
            return AVAILABLE;
        }

        return UNSUPPORTED_ON_DEVICE;
    }

    static void setEnabled(Context context, boolean isChecked) {
        Settings.Secure.putInt(
                context.getContentResolver(), GLANCEABLE_HUB_ENABLED, isChecked ? 1 : 0);
    }

    static boolean isEnabled(Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(),
                GLANCEABLE_HUB_ENABLED,
                getEnabledDefault(context)) == 1;
    }

    static int getEnabledDefault(Context context) {
        if (!Flags.glanceableHubV2()) {
            // The default was just "1" (enabled) pre-hub_v2).
            return 1;
        }

        return Flags.glanceableHubEnabledByDefault()
                || context.getResources().getBoolean(
                com.android.internal.R.bool.config_glanceableHubEnabledByDefault) ? 1 : 0;
    }

    private void disablePreferenceIfManaged(Preference pref) {
        final RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                        mContext,
                        DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL,
                        UserHandle.myUserId());
        if (admin != null) {
            if (pref instanceof RestrictedSwitchPreference) {
                ((RestrictedSwitchPreference) pref).setDisabledByAdmin(admin);
                ((RestrictedSwitchPreference) pref).setChecked(false);
            } else if (pref instanceof PrimarySwitchPreference) {
                ((PrimarySwitchPreference) pref).setDisabledByAdmin(admin);
                ((PrimarySwitchPreference) pref).setChecked(false);
            } else {
                Log.e(TAG, "Unknown pref type passed to disablePreferenceIfManaged: " + pref);
            }
        }
    }

    private boolean isMainUser() {
        final UserManager userManager = mContext.getSystemService(UserManager.class);
        return userManager.getUserInfo(UserHandle.myUserId()).isMain();
    }
}
