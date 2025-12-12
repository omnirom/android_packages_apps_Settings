/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_REMOVE_USER;
import static android.os.UserManager.SWITCHABILITY_STATUS_OK;
import static android.os.UserManager.SWITCHABILITY_STATUS_USER_IN_CALL;
import static android.os.UserManager.SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED;
import static android.app.admin.flags.Flags.FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED;

import static com.android.settings.flags.Flags.FLAG_SHOW_USER_DETAILS_SETTINGS_FOR_SELF;
import static com.android.settings.testutils.DevicePolicyUtils.DPC_ADMIN;
import static com.android.settings.testutils.DevicePolicyUtils.SYSTEM_ADMIN;
import static com.android.settings.users.UserDetailsSettings.REQUEST_CONFIRM_REMOVE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.robolectric.Shadows.shadowOf;

import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.multiuser.Flags;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowIntent;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowUserManager.class,
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowDevicePolicyManager.class,
        ShadowLockPatternUtils.class
})
public class UserDetailsSettingsTest {

    private static final String KEY_GRANT_ADMIN = "user_grant_admin";
    private static final String KEY_SWITCH_USER = "switch_user";
    private static final String KEY_ENABLE_TELEPHONY = "enable_calling";
    private static final String KEY_REMOVE_USER = "remove_user";
    private static final String KEY_APP_AND_CONTENT_ACCESS = "app_and_content_access";
    private static final String KEY_APP_COPYING = "app_copying";

    private static final int DIALOG_CONFIRM_REMOVE = 1;
    private static final int DIALOG_CONFIRM_RESET_GUEST = 4;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private Resources mResources;

    private ShadowUserManager mUserManager;

    @Mock
    private RestrictedPreference mSwitchUserPref;
    @Mock
    private SwitchPreference mPhonePref;
    @Mock
    private SwitchPreference mGrantAdminPref;
    @Mock
    private Preference mRemoveUserPref;
    @Mock
    private Preference mAppAndContentAccessPref;
    @Mock
    private Preference mAppCopyingPref;

    private FragmentActivity mActivity;
    private Context mContext;
    private UserCapabilities mUserCapabilities;
    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private UserDetailsSettings mFragment;
    private Bundle mArguments;
    private UserInfo mUserInfo;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mActivity = spy(ActivityController.of(new FragmentActivity()).get());
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mResources).when(mContext).getResources();
        mUserCapabilities = UserCapabilities.create(mContext);
        mUserCapabilities.mUserSwitcherEnabled = true;
        mFragment = spy(new UserDetailsSettings());
        mArguments = new Bundle();

        UserManager userManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mUserManager = Shadow.extract(userManager);

        doReturn(mTelephonyManager).when(mActivity).getSystemService(Context.TELEPHONY_SERVICE);

        ReflectionHelpers.setField(mFragment, "mUserManager", userManager);
        ReflectionHelpers.setField(mFragment, "mUserCaps", mUserCapabilities);
        ReflectionHelpers.setField(mFragment, "mMetricsFeatureProvider", mMetricsFeatureProvider);
        doReturn(mActivity).when(mFragment).getActivity();
        doReturn(mActivity).when(mFragment).getContext();

        doReturn(mock(PreferenceScreen.class)).when(mFragment).getPreferenceScreen();

        doReturn(mSwitchUserPref).when(mFragment).findPreference(KEY_SWITCH_USER);
        doReturn(mGrantAdminPref).when(mFragment).findPreference(KEY_GRANT_ADMIN);
        doReturn(mPhonePref).when(mFragment).findPreference(KEY_ENABLE_TELEPHONY);
        doReturn(mRemoveUserPref).when(mFragment).findPreference(KEY_REMOVE_USER);
        doReturn(mAppAndContentAccessPref).when(mFragment).findPreference(
                KEY_APP_AND_CONTENT_ACCESS);
        doReturn(mAppCopyingPref).when(mFragment).findPreference(KEY_APP_COPYING);
    }

    @After
    public void tearDown() {
        ShadowUserManager.reset();
    }

    @Test(expected = IllegalStateException.class)
    public void initialize_nullArguments_shouldThrowException() {
        mFragment.initialize(mActivity, null);
    }

    @Test(expected = IllegalStateException.class)
    public void initialize_emptyArguments_shouldThrowException() {
        mFragment.initialize(mActivity, new Bundle());
    }

    @Test
    public void initialize_userSelected_shouldSetupSwitchPref() {
        setupSelectedUser();
        doReturn("Switch to " + mUserInfo.name).when(mActivity).getString(anyInt(), anyString());

        mFragment.initialize(mActivity, mArguments);

        verify(mActivity).getString(com.android.settingslib.R.string.user_switch_to_user,
                mUserInfo.name);
        verify(mSwitchUserPref).setTitle("Switch to " + mUserInfo.name);
        verify(mSwitchUserPref).setOnPreferenceClickListener(mFragment);
        verify(mFragment, never()).removePreference(KEY_SWITCH_USER);
    }

    @Test
    public void initialize_guestSelected_shouldSetupSwitchPref() {
        setupSelectedGuest();
        doReturn("Switch to " + mUserInfo.name).when(mActivity).getString(anyInt(), anyString());

        mFragment.initialize(mActivity, mArguments);

        verify(mActivity).getString(com.android.settingslib.R.string.user_switch_to_user,
                mUserInfo.name);
        verify(mSwitchUserPref).setTitle("Switch to " + mUserInfo.name);
        verify(mSwitchUserPref).setOnPreferenceClickListener(mFragment);
        verify(mFragment, never()).removePreference(KEY_SWITCH_USER);
    }

    @Test
    public void initialize_userSelected_shouldNotShowAppAndContentPref() {
        setupSelectedUser();

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_APP_AND_CONTENT_ACCESS);
    }

    @Test
    public void initialize_guestSelected_shouldNotShowAppAndContentPref() {
        setupSelectedGuest();

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_APP_AND_CONTENT_ACCESS);
    }

    @Test
    public void onResume_canSwitch_shouldEnableSwitchPref() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_OK);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(true);
    }

    @Test
    public void onResume_userInCall_shouldDisableSwitchPref() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_USER_IN_CALL);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(false);
    }

    @Test
    public void onResume_UserSwitcherDisabled_shouldDisableSwitchPref() {
        setupSelectedUser();
        mUserCapabilities.mUserSwitcherEnabled = false;
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(false);
    }

    @Test
    public void onResume_switchDisallowed_shouldDisableSwitchPref() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(false);
    }

    @Test
    public void onResume_systemUserLocked_shouldDisableSwitchPref() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(UserManager.SWITCHABILITY_STATUS_SYSTEM_USER_LOCKED);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mSwitchUserPref).setEnabled(false);
    }

    @Test
    public void initialize_adminWithTelephonyVoiceCapable_shouldShowPhonePreference() {
        setupSelectedUser();
        doReturn(true).when(mTelephonyManager).isDeviceVoiceCapable();
        doReturn(true).when(mResources).getBoolean(
                com.android.settings.R.bool.config_show_sim_info);
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment, never()).removePreference(KEY_ENABLE_TELEPHONY);
        verify(mPhonePref).setOnPreferenceChangeListener(mFragment);
    }

    @Test
    public void initialize_adminNoVoiceCapable_shouldNotShowPhonePreference() {
        setupSelectedUser();
        doReturn(false).when(mTelephonyManager).isDeviceVoiceCapable();
        doReturn(true).when(mResources).getBoolean(
                com.android.settings.R.bool.config_show_sim_info);
        mUserManager.setIsAdminUser(true);
        doReturn(null).when(mActivity).getSystemService(Context.TELEPHONY_SERVICE);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_ENABLE_TELEPHONY);
    }

    @Test
    public void initialize_adminNoTelephony_shouldNotShowPhonePreference() {
        setupSelectedUser();
        doReturn(true).when(mTelephonyManager).isDeviceVoiceCapable();
        doReturn(false).when(mResources).getBoolean(
                com.android.settings.R.bool.config_show_sim_info);
        mUserManager.setIsAdminUser(true);
        doReturn(null).when(mActivity).getSystemService(Context.TELEPHONY_SERVICE);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_ENABLE_TELEPHONY);
    }

    @Test
    public void initialize_nonAdminWithTelephonyVoiceCapable_shouldNotShowPhonePreference() {
        setupSelectedUser();
        doReturn(true).when(mTelephonyManager).isDeviceVoiceCapable();
        doReturn(true).when(mResources).getBoolean(
                com.android.settings.R.bool.config_show_sim_info);
        mUserManager.setIsAdminUser(false);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_ENABLE_TELEPHONY);
    }

    @Test
    public void initialize_nonAdmin_shouldNotShowAppAndContentPref() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(false);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_APP_AND_CONTENT_ACCESS);
    }

    @Test
    public void initialize_adminSelectsSecondaryUser_shouldShowRemovePreference() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mRemoveUserPref).setOnPreferenceClickListener(mFragment);
        verify(mRemoveUserPref).setTitle(R.string.user_remove_user);
        verify(mFragment, never()).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_adminSelectsNewRestrictedUser_shouldOpenAppContentScreen() {
        setupSelectedRestrictedUser();
        mUserManager.setIsAdminUser(true);
        mArguments.putBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, true);

        mFragment.initialize(mActivity, mArguments);

        Intent startedIntent = shadowOf(mActivity).getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);
        assertThat(shadowIntent.getIntentClass()).isEqualTo(SubSettings.class);
        assertThat(startedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                AppRestrictionsFragment.class.getName());
        Bundle arguments = startedIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(arguments).isNotNull();
        assertThat(arguments.getInt(AppRestrictionsFragment.EXTRA_USER_ID, 0)).isEqualTo(
                mUserInfo.id);
        assertThat(arguments.getBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, false)).isEqualTo(
                true);
    }

    @Test
    public void initialize_adminSelectsRestrictedUser_shouldSetupPreferences() {
        setupSelectedRestrictedUser();
        mUserManager.setIsAdminUser(true);
        doReturn(true).when(mTelephonyManager).isDeviceVoiceCapable();
        doReturn(true).when(mResources).getBoolean(
                com.android.settings.R.bool.config_show_sim_info);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment, never()).removePreference(KEY_REMOVE_USER);
        verify(mFragment, never()).removePreference(KEY_SWITCH_USER);
        verify(mFragment, never()).removePreference(KEY_APP_AND_CONTENT_ACCESS);
        verify(mFragment).removePreference(KEY_ENABLE_TELEPHONY);
        verify(mSwitchUserPref).setTitle("Switch to " + mUserInfo.name);
        verify(mAppAndContentAccessPref).setOnPreferenceClickListener(mFragment);
        verify(mSwitchUserPref).setOnPreferenceClickListener(mFragment);
        verify(mRemoveUserPref).setOnPreferenceClickListener(mFragment);
    }

    @Test
    public void initialize_adminSelectsExistingRestrictedUser_shouldNotStartAppAndContentAccess() {
        setupSelectedRestrictedUser();
        mUserManager.setIsAdminUser(true);
        mArguments.putBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, false);

        mFragment.initialize(mActivity, mArguments);

        verify(mActivity, never()).startActivity(any(Intent.class));
    }

    @Test
    public void initialize_adminSelectsGuest_shouldShowRemovePreference() {
        setupSelectedGuest();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mRemoveUserPref).setOnPreferenceClickListener(mFragment);
        verify(mRemoveUserPref).setTitle(com.android.settingslib.R.string.guest_exit_guest);
        verify(mFragment, never()).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_nonAdmin_shouldNotShowRemovePreference() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(false);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_onMainUser_shouldNotShowRemovePreference() {
        setupSelectedMainUser();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_REMOVE_USER);
    }

    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void initialize_disallowRemoveUserRestriction_refactoringEnabled_shouldNotShowRemovePreference() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        ShadowDevicePolicyManager.getShadow().setPolicyEnforcementInfoForUserRestriction(
                DISALLOW_REMOVE_USER, new PolicyEnforcementInfo(List.of(SYSTEM_ADMIN)));

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_REMOVE_USER);
    }

    @DisableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void initialize_disallowRemoveUserRestriction_refactoringDisabled_shouldNotShowRemovePreference() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mUserManager.addBaseUserRestriction(UserManager.DISALLOW_REMOVE_USER);

        mFragment.initialize(mActivity, mArguments);

        verify(mFragment).removePreference(KEY_REMOVE_USER);
    }

    @Test
    public void initialize_userHasCallRestriction_shouldSetPhoneSwitchUnChecked() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mUserManager.setUserRestriction(mUserInfo.getUserHandle(),
                UserManager.DISALLOW_OUTGOING_CALLS, true);

        mFragment.initialize(mActivity, mArguments);

        verify(mPhonePref).setChecked(false);
    }

    @Test
    public void initialize_noCallRestriction_shouldSetPhoneSwitchChecked() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);

        mFragment.initialize(mActivity, mArguments);

        verify(mPhonePref).setChecked(true);
    }

    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void initialize_switchUserDisallowed_refactoringEnabled_shouldSetAdminDisabledOnSwitchPreference() {
        setupSelectedUser();
        mUserCapabilities.mDisallowSwitchUser = true;
        mUserCapabilities.mDisallowSwitchUserRestrictionEnforcementInfo = new PolicyEnforcementInfo(
                List.of(DPC_ADMIN));

        mFragment.initialize(mActivity, mArguments);

        verify(mSwitchUserPref).setDisabledByAdmin(DPC_ADMIN);
    }

    @DisableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void initialize_switchUserDisallowed_refactoringDisabled_shouldSetAdminDisabledOnSwitchPreference() {
        setupSelectedUser();
        mUserCapabilities.mDisallowSwitchUser = true;
        DevicePolicyManager devicePolicyManager = mock(DevicePolicyManager.class);
        doReturn(devicePolicyManager).when(mActivity).getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        doReturn(mock(ComponentName.class)).when(
                devicePolicyManager).getDeviceOwnerComponentOnAnyUser();

        mFragment.initialize(mActivity, mArguments);

        verify(mSwitchUserPref).setDisabledByAdmin(any(RestrictedLockUtils.EnforcedAdmin.class));
    }

    @Test
    public void initialize_switchUserAllowed_shouldSetSwitchPreferenceEnabled() {
        setupSelectedUser();
        mUserCapabilities.mDisallowSwitchUser = false;

        mFragment.initialize(mActivity, mArguments);

        verify(mSwitchUserPref).setDisabledByAdmin((EnforcingAdmin) null);
        verify(mSwitchUserPref).setSelectable(true);
        verify(mSwitchUserPref).setOnPreferenceClickListener(mFragment);
    }

    @Test
    public void onPreferenceClick_switchClicked_canSwitch_shouldSwitch() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_OK);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        mFragment.mUserInfo = mUserInfo;

        mFragment.onPreferenceClick(mSwitchUserPref);

        verify(mFragment).switchUser();
        verify(mMetricsFeatureProvider).action(any(), eq(SettingsEnums.ACTION_SWITCH_TO_USER));
    }

    @Test
    public void onPreferenceClick_switchToRestrictedClicked_canSwitch_shouldSwitch() {
        setupSelectedRestrictedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_OK);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        mFragment.mUserInfo = mUserInfo;

        mFragment.onPreferenceClick(mSwitchUserPref);

        verify(mFragment).switchUser();
        verify(mMetricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_SWITCH_TO_RESTRICTED_USER));
    }

    @Test
    public void onPreferenceClick_switchToGuestClicked_canSwitch_shouldSwitch() {
        setupSelectedGuest();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_OK);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        mFragment.mUserInfo = mUserInfo;

        mFragment.onPreferenceClick(mSwitchUserPref);

        verify(mFragment).switchUser();
        verify(mMetricsFeatureProvider).action(any(), eq(SettingsEnums.ACTION_SWITCH_TO_GUEST));
    }

    @Test
    public void onPreferenceClick_switchClicked_canNotSwitch_doNothing() {
        setupSelectedUser();
        mUserManager.setSwitchabilityStatus(SWITCHABILITY_STATUS_USER_SWITCH_DISALLOWED);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        mFragment.mUserInfo = mUserInfo;

        mFragment.onPreferenceClick(mSwitchUserPref);

        verify(mFragment, never()).switchUser();
    }

    @Test
    public void onPreferenceClick_removeGuestClicked_canDelete_shouldShowDialog() {
        setupSelectedGuest();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(true);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceClick(mRemoveUserPref);

        verify(mMetricsFeatureProvider).action(any(), eq(SettingsEnums.ACTION_REMOVE_GUEST_USER));
        verify(mFragment).canDeleteUser();
        verify(mFragment).showDialog(DIALOG_CONFIRM_RESET_GUEST);
    }

    @Test
    public void onPreferenceClick_removeRestrictedClicked_canDelete_shouldShowDialog() {
        setupSelectedRestrictedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(true);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceClick(mRemoveUserPref);

        verify(mMetricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_REMOVE_RESTRICTED_USER));
        verify(mFragment).canDeleteUser();
        verify(mFragment).showDialog(DIALOG_CONFIRM_REMOVE);
    }

    @Test
    public void onPreferenceClick_removeClicked_canDelete_shouldShowDialog() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(true);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceClick(mRemoveUserPref);

        verify(mMetricsFeatureProvider).action(any(), eq(SettingsEnums.ACTION_REMOVE_USER));
        verify(mFragment).canDeleteUser();
        verify(mFragment).showDialog(DIALOG_CONFIRM_REMOVE);
    }

    @Test
    public void runKeyguardConfirmation_userHasScreenLock_shouldLaunchAuthenticationActivity() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(true);

        mUserManager.addProfile(new UserInfo(UserHandle.myUserId(), "Bob", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_MAIN));

        ShadowLockPatternUtils.setKeyguardStoredPasswordQuality(
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        doNothing().when(mFragment).startActivityForResult(any(), anyInt(), any());
        ShadowUserManager.getShadow().setProfileIdsWithDisabled(new int[]{UserHandle.myUserId()});

        assertThat(mFragment.runUserRemovalKeyguardConfirmation()).isTrue();
        verify(mFragment).startActivityForResult(any(Intent.class), eq(REQUEST_CONFIRM_REMOVE),
                any());
    }

    @Test
    public void runKeyguardConfirmation_userHasNoScreenLock_shouldNotLaunchAuthentication() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(true);

        mUserManager.addProfile(new UserInfo(UserHandle.myUserId(), "Bob", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_MAIN));

        ShadowUserManager.getShadow().setProfileIdsWithDisabled(new int[]{UserHandle.myUserId()});
        assertThat(mFragment.runUserRemovalKeyguardConfirmation()).isFalse();
        verify(mFragment, never()).startActivityForResult(any(Intent.class),
                eq(REQUEST_CONFIRM_REMOVE), any());
    }

    @Test
    public void onPreferenceClick_removeClicked_canNotDelete_doNothing() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(false);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceClick(mRemoveUserPref);

        verify(mFragment).canDeleteUser();
        verify(mFragment, never()).showDialog(DIALOG_CONFIRM_REMOVE);
    }

    @Test
    public void onPreferenceClick_selectRestrictedUser_appAndContentAccessClicked_startActivity() {
        setupSelectedRestrictedUser();
        mFragment.mUserInfo = mUserInfo;
        mUserManager.setIsAdminUser(true);
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;

        mFragment.onPreferenceClick(mAppAndContentAccessPref);

        Intent startedIntent = shadowOf(mActivity).getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);
        assertThat(shadowIntent.getIntentClass()).isEqualTo(SubSettings.class);
        assertThat(startedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                AppRestrictionsFragment.class.getName());
        Bundle arguments = startedIntent.getBundleExtra(
                SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(arguments.getInt(AppRestrictionsFragment.EXTRA_USER_ID, 0)).isEqualTo(
                mUserInfo.id);
        assertThat(arguments.getBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, true)).isEqualTo(
                false);
    }

    @Test
    public void onPreferenceClick_unknownPreferenceClicked_doNothing() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mFragment.mSwitchUserPref = mSwitchUserPref;
        mFragment.mRemoveUserPref = mRemoveUserPref;
        mFragment.mAppAndContentAccessPref = mAppAndContentAccessPref;

        mFragment.onPreferenceClick(mock(UserPreference.class));

        verify(mFragment).onPreferenceClick(any());
        verifyNoMoreInteractions(mFragment);
    }

    @Test
    public void canDeleteUser_nonAdminUser_shouldReturnFalse() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(false);

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isFalse();
    }

    @Test
    public void canDeleteUser_onMainUser_shouldReturnFalse() {
        setupSelectedMainUser();
        mUserManager.setIsAdminUser(true);
        mFragment.mUserInfo = mUserInfo;

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isFalse();
    }

    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void canDeleteUser_adminSelectsUser_noRestrictions_refactoringEnabled_shouldReturnTrue() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mFragment.mUserInfo = mUserInfo;

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isTrue();
    }

    @DisableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void canDeleteUser_adminSelectsUser_noRestrictions_refactoringDisabled_shouldReturnTrue() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mFragment.mUserInfo = mUserInfo;

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isTrue();
    }

    @EnableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void canDeleteUser_adminSelectsUser_hasRemoveRestriction_refactoringEnabled_shouldReturnFalse() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mFragment.mUserInfo = mUserInfo;
        ShadowDevicePolicyManager.getShadow().setPolicyEnforcementInfoForUserRestriction(
                DISALLOW_REMOVE_USER, new PolicyEnforcementInfo(List.of(DPC_ADMIN)));

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isFalse();
    }

    @DisableFlags(FLAG_POLICY_TRANSPARENCY_REFACTOR_ENABLED)
    @Test
    public void canDeleteUser_adminSelectsUser_hasRemoveRestriction_refactoringDisabled_shouldReturnFalse() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        mFragment.mUserInfo = mUserInfo;
        ComponentName componentName = new ComponentName("test", "test");
        ShadowDevicePolicyManager.getShadow().setDeviceOwnerComponentOnAnyUser(componentName);
        ShadowDevicePolicyManager.getShadow().setDeviceOwnerUserId(UserHandle.myUserId());
        List<UserManager.EnforcingUser> enforcingUsers = new ArrayList<>();
        enforcingUsers.add(new UserManager.EnforcingUser(UserHandle.myUserId(),
                UserManager.RESTRICTION_SOURCE_DEVICE_OWNER));
        mUserManager.setUserRestrictionSources(UserManager.DISALLOW_REMOVE_USER,
                UserHandle.of(UserHandle.myUserId()), enforcingUsers);

        boolean result = mFragment.canDeleteUser();

        assertThat(result).isFalse();
    }

    @Test
    public void initialize_userSelected_shouldShowGrantAdminPref_MultipleAdminEnabled() {
        setupSelectedUser();
        mUserManager.setIsAdminUser(true);
        ShadowUserManager.setIsMultipleAdminEnabled(true);
        mFragment.initialize(mActivity, mArguments);
        verify(mFragment, never()).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    public void initialize_userSelected_shouldNotShowGrantAdminPref() {
        setupSelectedUser();
        mFragment.initialize(mActivity, mArguments);
        verify(mFragment).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    public void initialize_restrictUserSelected_shouldNotShowGrantAdminPref_MultipleAdminEnabled() {
        setupSelectedUser();
        ShadowUserManager.setIsMultipleAdminEnabled(true);
        // target user has DISALLOW_GRANT_ADMIN restriction
        mUserManager.setUserRestriction(mUserInfo.getUserHandle(), UserManager.DISALLOW_GRANT_ADMIN,
                true);
        mFragment.initialize(mActivity, mArguments);
        verify(mFragment).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_UNICORN_MODE_REFACTORING_FOR_HSUM_READ_ONLY)
    public void initialize_currentUserRestrict_shouldNotShowGrantAdminPref_MultipleAdminEnabled() {
        setupSelectedUser();
        ShadowUserManager.setIsMultipleAdminEnabled(true);
        // current user has DISALLOW_GRANT_ADMIN restriction
        mUserManager.setUserRestriction(mContext.getUser(), UserManager.DISALLOW_GRANT_ADMIN, true);
        mFragment.initialize(mActivity, mArguments);
        verify(mFragment).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    public void initialize_mainUserSelected_shouldShowGrantAdminPref_MultipleAdminEnabled() {
        setupSelectedMainUser();
        ShadowUserManager.setIsMultipleAdminEnabled(true);
        mFragment.initialize(mActivity, mArguments);
        verify(mFragment).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    public void initialize_guestSelected_shouldNotShowGrantAdminPref_MultipleAdminEnabled() {
        setupSelectedGuest();
        ShadowUserManager.setIsMultipleAdminEnabled(true);
        mFragment.initialize(mActivity, mArguments);
        verify(mFragment).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    @EnableFlags(FLAG_SHOW_USER_DETAILS_SETTINGS_FOR_SELF)
    public void initialize_nonAdminCurrentUserSelected_shouldShowRemovePref() {
        setupSelectedCurrentUser();
        mFragment.initialize(mActivity, mArguments);

        verify(mRemoveUserPref).setOnPreferenceClickListener(mFragment);
        verify(mRemoveUserPref).setTitle(R.string.user_remove_user);
        verify(mFragment, never()).removePreference(KEY_REMOVE_USER);
        verify(mFragment).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    @EnableFlags(FLAG_SHOW_USER_DETAILS_SETTINGS_FOR_SELF)
    public void initialize_nonAdminCurrentUserSelected_shouldHideSwitchPref() {
        setupSelectedCurrentUser();
        mFragment.initialize(mActivity, mArguments);

        verify(mSwitchUserPref).setVisible(false);
    }

    @Test
    @EnableFlags(FLAG_SHOW_USER_DETAILS_SETTINGS_FOR_SELF)
    public void initialize_adminCurrentUserSelected_shouldShowRemoveAndGrantAdminPref() {
        setupSelectedCurrentUser();
        mUserManager.setIsAdminUser(true);
        ShadowUserManager.setIsMultipleAdminEnabled(true);
        mFragment.initialize(mActivity, mArguments);

        verify(mRemoveUserPref).setOnPreferenceClickListener(mFragment);
        verify(mRemoveUserPref).setTitle(R.string.user_remove_user);
        verify(mFragment, never()).removePreference(KEY_REMOVE_USER);
        verify(mFragment, never()).removePreference(KEY_GRANT_ADMIN);
    }

    @Test
    public void onPreferenceChange_grantAdminClicked_isNotAdmin_shouldLogGrantAdmin() {
        setupSelectedUser();
        mFragment.mUserInfo = mUserInfo;
        mFragment.mGrantAdminPref = mGrantAdminPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceChange(mGrantAdminPref, true);

        verify(mMetricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_GRANT_ADMIN_FROM_SETTINGS));
    }

    @Test
    public void onPreferenceChange_grantAdminClicked_isAdmin_shouldLogRevokeAdmin() {
        setupSelectedAdminUser();
        mFragment.mUserInfo = mUserInfo;
        mFragment.mGrantAdminPref = mGrantAdminPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceChange(mGrantAdminPref, false);

        verify(mMetricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_REVOKE_ADMIN_FROM_SETTINGS));
    }

    @Test
    @EnableFlags(FLAG_SHOW_USER_DETAILS_SETTINGS_FOR_SELF)
    public void onPreferenceChange_revokeOwnAdmin_shouldHideAdminToggle() {
        setupSelectedCurrentUser();
        mUserManager.setIsAdminUser(true);
        ShadowUserManager.setIsMultipleAdminEnabled(true);
        mFragment.mGrantAdminPref = mGrantAdminPref;
        doNothing().when(mFragment).showDialog(anyInt());

        mFragment.onPreferenceChange(mGrantAdminPref, false);

        verify(mMetricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_REVOKE_ADMIN_FROM_SETTINGS));
        assertThat(mGrantAdminPref.isVisible()).isFalse();
    }

    private void setupSelectedUser() {
        mArguments.putInt("user_id", 1);
        mUserInfo = new UserInfo(1, "Tom", null, UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED,
                UserManager.USER_TYPE_FULL_SECONDARY);
        mFragment.mUserInfo = mUserInfo;
        mUserManager.addProfile(mUserInfo);
    }

    private void setupSelectedMainUser() {
        mArguments.putInt("user_id", 11);
        mUserInfo = new UserInfo(11, "Jerry", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_MAIN,
                UserManager.USER_TYPE_FULL_SECONDARY);
        mFragment.mUserInfo = mUserInfo;
        mUserManager.addProfile(mUserInfo);
    }

    private void setupSelectedAdminUser() {
        mArguments.putInt("user_id", 12);
        mUserInfo = new UserInfo(12, "Andy", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_ADMIN,
                UserManager.USER_TYPE_FULL_SECONDARY);
        mFragment.mUserInfo = mUserInfo;
        mUserManager.addProfile(mUserInfo);
    }

    private void setupSelectedGuest() {
        mArguments.putInt("user_id", 23);
        mUserInfo = new UserInfo(23, "Guest", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_GUEST,
                UserManager.USER_TYPE_FULL_GUEST);
        mFragment.mUserInfo = mUserInfo;
        mUserManager.addProfile(mUserInfo);
    }

    private void setupSelectedRestrictedUser() {
        mArguments.putInt("user_id", 21);
        mUserInfo = new UserInfo(21, "Bob", null,
                UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_RESTRICTED,
                UserManager.USER_TYPE_FULL_RESTRICTED);
        mFragment.mUserInfo = mUserInfo;
        mUserManager.addProfile(mUserInfo);
    }

    private void setupSelectedCurrentUser() {
        mArguments.putInt("user_id", 0);
        mUserInfo = new UserInfo(0, "Me", null, UserInfo.FLAG_FULL | UserInfo.FLAG_INITIALIZED,
                UserManager.USER_TYPE_FULL_SECONDARY);
        mFragment.mUserInfo = mUserInfo;
        mUserManager.addProfile(mUserInfo);
    }
}
