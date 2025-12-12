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

package com.android.settings.users;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.PreferenceScreen;


import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.Objects;

public class MultiUserMainSwitchPreferenceController extends TogglePreferenceController
        implements OnCheckedChangeListener {

    private static final String TAG = MultiUserMainSwitchPreferenceController.class.getSimpleName();

    @Nullable
    private SettingsMainSwitchPreference mPreference;
    private final UserCapabilities mUserCaps;

    MultiUserMainSwitchPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUserCaps = UserCapabilities.create(context);
    }

    @Override
    public boolean isChecked() {
        return mUserCaps.mUserSwitcherEnabled;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Log.d(TAG, "Setting ALLOW_USER_SWITCH to " + isChecked);
        return Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.USER_SWITCHER_ENABLED, isChecked ? 1 : 0);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return  R.string.menu_key_system;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mUserCaps.mIsGuest) {
            return AVAILABLE;
        } else {
            return DISABLED_FOR_USER;
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.addOnSwitchChangeListener(this);
            updateState();
        }
    }

    void updateState() {
        if (mPreference != null) {
            mUserCaps.updateAddUserCapabilities(mContext);
            mPreference.setChecked(isChecked());
            if (!handleUserSwitchRestrictedByAdmin()) {
                mPreference.setSwitchBarEnabled(
                        mUserCaps.mIsMain && !mUserCaps.mDisallowSwitchUser);
            }
        }
    }

    private boolean handleUserSwitchRestrictedByAdmin() {
        Objects.requireNonNull(mPreference);
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            if (!mUserCaps.mDisallowSwitchUserRestrictionEnforcementInfo.getAllAdmins().isEmpty()
                    && !mUserCaps.mDisallowSwitchUserRestrictionEnforcementInfo
                    .isOnlyEnforcedBySystem()) {
                mPreference.setDisabledByAdmin(
                        mUserCaps.mDisallowSwitchUserRestrictionEnforcementInfo
                                .getMostImportantEnforcingAdmin());
                return true;
            }
            return false;
        } else {
            RestrictedLockUtils.EnforcedAdmin enforcedAdmin =
                    RestrictedLockUtilsInternal.checkIfRestrictionEnforced(mContext,
                            UserManager.DISALLOW_USER_SWITCH, UserHandle.myUserId());
            if (enforcedAdmin != null) {
                mPreference.setDisabledByAdmin(enforcedAdmin);
            }
            return enforcedAdmin != null;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setChecked(isChecked);
    }
}
