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

package com.android.settings.users;

import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.PolicyEnforcementInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;

/**
 * Controls the preference on the user settings screen which determines whether the guest user
 * should have access to telephony or not.
 */
public class GuestTelephonyPreferenceController extends TogglePreferenceController {

    private final UserManager mUserManager;
    private final UserCapabilities mUserCaps;

    public GuestTelephonyPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mUserManager = context.getSystemService(UserManager.class);
        mUserCaps = UserCapabilities.create(context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
                || UserManager.isHeadlessSystemUserMode() || !mUserCaps.isAdmin()) {
            return DISABLED_FOR_USER;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return !mUserManager.getDefaultGuestRestrictions()
                .getBoolean(UserManager.DISALLOW_OUTGOING_CALLS, false);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Bundle guestRestrictions = mUserManager.getDefaultGuestRestrictions();
        guestRestrictions.putBoolean(UserManager.DISALLOW_SMS, true);
        guestRestrictions.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS, !isChecked);
        mUserManager.setDefaultGuestRestrictions(guestRestrictions);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_system;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mUserCaps.updateAddUserCapabilities(mContext);
        final RestrictedSwitchPreference restrictedSwitchPreference =
                (RestrictedSwitchPreference) preference;
        restrictedSwitchPreference.setChecked(isChecked());
        if (!isAvailable()) {
            restrictedSwitchPreference.setVisible(false);
        } else {
            restrictedSwitchPreference.setVisible(true);
            if (!handleRemoveUserRestrictionByAdmin(restrictedSwitchPreference)) {
                // We only need to check add user restrictions if removing user is not restricted.
                handleAddUserRestriction(restrictedSwitchPreference);
            }
        }
    }

    /**
     * Sets the preference as disabled by admin if the add user is restricted by admin.
     *
     * @return true if the preference is disabled by admin, false otherwise.
     */
    private boolean handleRemoveUserRestrictionByAdmin(RestrictedSwitchPreference preference) {
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            DevicePolicyManager dpm = mContext.getSystemService(DevicePolicyManager.class);
            if (dpm == null) {
                return false;
            }
            final PolicyEnforcementInfo policyEnforcementInfo = dpm.getEnforcingAdminsForPolicy(
                    DevicePolicyIdentifiers.getIdentifierForUserRestriction(
                            UserManager.DISALLOW_REMOVE_USER), UserHandle.myUserId());
            boolean disallowRemoveUser = !policyEnforcementInfo.getAllAdmins().isEmpty()
                    && !policyEnforcementInfo.isOnlyEnforcedBySystem();
            if (disallowRemoveUser) {
                preference.setDisabledByAdmin(
                        policyEnforcementInfo.getMostImportantEnforcingAdmin());
            }
            return disallowRemoveUser;
        } else {
            final RestrictedLockUtils.EnforcedAdmin disallowRemoveUserAdmin =
                    RestrictedLockUtilsInternal.checkIfRestrictionEnforced(mContext,
                            UserManager.DISALLOW_REMOVE_USER, UserHandle.myUserId());
            if (disallowRemoveUserAdmin != null) {
                preference.setDisabledByAdmin(disallowRemoveUserAdmin);
            }
            return disallowRemoveUserAdmin != null;
        }

    }

    private void handleAddUserRestriction(RestrictedSwitchPreference restrictedSwitchPreference) {
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            // Do nothing if adding user is allowed.
            if (!mUserCaps.mDisallowAddUser) {
                return;
            }
            if (mUserCaps.mDisallowAddUserSetByAdmin) {
                restrictedSwitchPreference.setDisabledByAdmin(
                        mUserCaps.mDisallowAddUserRestrictionEnforcementInfo
                                .getMostImportantEnforcingAdmin());
            } else {
                // Adding user is restricted by system.
                restrictedSwitchPreference.setVisible(false);
            }
        } else {
            if (mUserCaps.mDisallowAddUserSetByAdmin) {
                restrictedSwitchPreference.setDisabledByAdmin(mUserCaps.mEnforcedAdmin);
            } else if (mUserCaps.mDisallowAddUser) {
                // Adding user is restricted by system
                restrictedSwitchPreference.setVisible(false);
            }
        }
    }
}
