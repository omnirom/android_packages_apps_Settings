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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_FOR_USER;
import static com.android.settings.testutils.DevicePolicyUtils.DPC_ADMIN;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.admin.PolicyEnforcementInfo;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.RestrictedLockUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUserManager.class, ShadowDevicePolicyManager.class})
public class MultiUserMainSwitchPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private ShadowUserManager mUserManager;
    private SettingsMainSwitchPreference mPreference;
    private static final String KEY_USER_SWITCH_TOGGLE = "multiple_users_main_switch";

    private PreferenceScreen mScreen;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mScreen = mock(PreferenceScreen.class);
        mPreference = mock(SettingsMainSwitchPreference.class);

        mUserManager = ShadowUserManager.getShadow();
        mUserManager.setSupportsMultipleUsers(true);

        doReturn(mPreference).when(mScreen).findPreference(KEY_USER_SWITCH_TOGGLE);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test
    @DisableFlags({android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED})
    public void displayPreference_disallowUserSwitchByAdmin_shouldSetDisabledByAdminUnchecked() {
        int userId = UserHandle.myUserId();
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(userId,
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        // Ensure that RestrictedLockUtils.checkIfRestrictionEnforced doesn't return null.
        ShadowUserManager.getShadow().setUserRestrictionSources(
                UserManager.DISALLOW_USER_SWITCH,
                UserHandle.of(userId),
                enforcingUsers);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);
        multiUserMainSwitchPreferenceController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
        verify(mPreference).setDisabledByAdmin(any(RestrictedLockUtils.EnforcedAdmin.class));
    }

    @Test
    @EnableFlags({android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED})
    public void
            displayPreference_disallowUserSwitchByAdmin_dpmFlagEnabled_shouldSetDisabledByAdminUnchecked() {
        ShadowDevicePolicyManager.getShadow().setPolicyEnforcementInfoForUserRestriction(
                UserManager.DISALLOW_USER_SWITCH, new PolicyEnforcementInfo(List.of(DPC_ADMIN)));

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);
        multiUserMainSwitchPreferenceController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
        verify(mPreference).setDisabledByAdmin(DPC_ADMIN);
    }

    @Test
    public void displayPreference_disallowUserSwitch_userNotMain_shouldSetDisabledUnchecked() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, true);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);
        multiUserMainSwitchPreferenceController.displayPreference(mScreen);

        verify(mPreference).setChecked(false);
        verify(mPreference).setSwitchBarEnabled(false);
        verify(mPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
    }

    @Test
    public void displayPreference_allowUserSwitch_notMainUser_shouldSetDisabled() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);
        mUserManager.addUser(10, "Test", UserInfo.FLAG_ADMIN);
        mUserManager.switchUser(10);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);
        multiUserMainSwitchPreferenceController.displayPreference(mScreen);

        verify(mPreference).setSwitchBarEnabled(false);
    }

    @Test
    public void displayPreference_allowUserSwitch_shouldNotSetDisabledByAdmin() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);
        multiUserMainSwitchPreferenceController.displayPreference(mScreen);

        assertEquals(AVAILABLE, multiUserMainSwitchPreferenceController.getAvailabilityStatus());
        verify(mPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
    }

    @Test
    public void displayPreference_userIsNotMain_shouldNotBeEnabled() {

        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);
        mUserManager.addUser(10, "Test", UserInfo.FLAG_ADMIN);
        mUserManager.switchUser(10);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);
        multiUserMainSwitchPreferenceController.displayPreference(mScreen);

        assertEquals(AVAILABLE, multiUserMainSwitchPreferenceController.getAvailabilityStatus());
        verify(mPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mPreference).setSwitchBarEnabled(false);
    }

    @Test
    public void displayPreference_userIsMain_shouldBeEnabled() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);
        multiUserMainSwitchPreferenceController.displayPreference(mScreen);

        verify(mPreference, never()).setDisabledByAdmin(
                any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mPreference).setSwitchBarEnabled(true);
    }

    @Test
    public void userIsGuest_shouldBeHidden() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);
        mUserManager.addUser(10, "Test", UserInfo.FLAG_GUEST);
        mUserManager.switchUser(10);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);

        assertEquals(DISABLED_FOR_USER,
                multiUserMainSwitchPreferenceController.getAvailabilityStatus());
    }

    @Test
    public void userIsMain_shouldBeHidden() {
        mUserManager.setUserRestriction(UserHandle.of(UserHandle.myUserId()),
                UserManager.DISALLOW_USER_SWITCH, false);
        mUserManager.setIsAdminUser(true);

        MultiUserMainSwitchPreferenceController multiUserMainSwitchPreferenceController =
                new MultiUserMainSwitchPreferenceController(mContext, KEY_USER_SWITCH_TOGGLE);

        assertEquals(AVAILABLE,
                multiUserMainSwitchPreferenceController.getAvailabilityStatus());
    }
}
