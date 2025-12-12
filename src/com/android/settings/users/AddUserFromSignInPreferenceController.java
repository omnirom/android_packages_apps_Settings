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

import static com.android.settings.flags.Flags.showAddUsersFromSigninToggle;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class AddUserFromSignInPreferenceController extends TogglePreferenceController {

    private final UserCapabilities mUserCaps;
    private UserManager mUserManager;

    public AddUserFromSignInPreferenceController(Context context, String key) {
        super(context, key);
        mUserCaps = UserCapabilities.create(context);
        mUserManager = context.getSystemService(UserManager.class);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mUserCaps.updateAddUserCapabilities(mContext);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!showAddUsersFromSigninToggle()) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (!mContext.getResources()
                .getBoolean(
                        com.android.internal.R.bool.config_userSwitchingMustGoThroughLoginScreen)) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (!mUserCaps.isAdmin()) {
            return DISABLED_FOR_USER;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public boolean isChecked() {
        return !mUserManager.hasUserRestriction(UserManager.DISALLOW_ADD_USER, UserHandle.SYSTEM);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mUserManager.setUserRestriction(
                UserManager.DISALLOW_ADD_USER, !isChecked, UserHandle.SYSTEM);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }
}
