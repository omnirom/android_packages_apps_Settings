/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL;
import static android.provider.Settings.Secure.GLANCEABLE_HUB_ENABLED;

import static com.android.systemui.Flags.FLAG_GLANCEABLE_HUB_ENABLED_BY_DEFAULT;
import static com.android.systemui.Flags.FLAG_GLANCEABLE_HUB_V2;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class,
        WidgetsOnLockscreenPreferenceControllerTest.ShadowRestrictedLockUtilsInternal.class,
})
public class WidgetsOnLockscreenPreferenceControllerTest {
    private static final String PREFERENCE_KEY = "preference_key";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    public PrimarySwitchPreference mPreference;

    @Mock
    private DevicePolicyManager mDevicePolicyManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private UserInfo mUserInfo;

    private WidgetsOnLockscreenPreferenceController mController;

    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());

        when(mContext.getSystemService(eq(Context.DEVICE_POLICY_SERVICE)))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.getKeyguardDisabledFeatures(any(), anyInt())).thenReturn(0);

        when(mContext.getSystemService(eq(UserManager.class))).thenReturn(mUserManager);
        when(mUserManager.getUserInfo(UserHandle.myUserId())).thenReturn(mUserInfo);
        when(mUserInfo.isMain()).thenReturn(true);

        mController = new WidgetsOnLockscreenPreferenceController(mContext, PREFERENCE_KEY);
    }

    @After
    public void tearDown() {
        ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    public void getAvailabilityStatus_featureEnabled_available() {
        setShowGlanceableHubToggleSettingEnabled(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_glanceableHubSettingEnabledOnMobile_available() {
        // The show_glanceable_hub_toggle_setting config needs to be false in order for
        // getAvailabilityStatus to fall through to the glanceable_hub config check.
        setShowGlanceableHubToggleSettingEnabled(false);
        setShowGlanceableHubToggleSettingOnMobileEnabled(true);
        setGlanceableHubEnabled(true);
        mSetFlagsRule.enableFlags(FLAG_GLANCEABLE_HUB_V2);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_glanceableHubSettingConfigsDisabled_unsupported() {
        setShowGlanceableHubToggleSettingEnabled(false);
        setShowGlanceableHubToggleSettingOnMobileEnabled(false);
        setGlanceableHubEnabled(true);
        mSetFlagsRule.enableFlags(FLAG_GLANCEABLE_HUB_V2);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_glanceableV2FlagDisabled_unsupported() {
        // The show_glanceable_hub_toggle_setting config needs to be false in order for
        // getAvailabilityStatus to fall through to the glanceable_hub config check.
        setShowGlanceableHubToggleSettingEnabled(false);
        setShowGlanceableHubToggleSettingOnMobileEnabled(true);
        setGlanceableHubEnabled(true);
        mSetFlagsRule.disableFlags(FLAG_GLANCEABLE_HUB_V2);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_forSecondaryUser_unsupported() {
        setShowGlanceableHubToggleSettingEnabled(true);

        when(mUserInfo.isMain()).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_keyguardWidgetsDisabled_prefDisabledByAdmin() {
        setShowGlanceableHubToggleSettingEnabled(true);
        ShadowRestrictedLockUtilsInternal.setKeyguardDisabledFeatures(KEYGUARD_DISABLE_WIDGETS_ALL);

        mController.updateState(mPreference);

        verify(mPreference).setDisabledByAdmin(any(RestrictedLockUtils.EnforcedAdmin.class));
        verify(mPreference).setChecked(false);
    }

    @Test
    public void setChecked_checked_updateSettings() throws Settings.SettingNotFoundException {
        // set the initial state to off
        updateSecureSetting(0);

        assertThat(readSecureSetting()).isEqualTo(0);

        mController.setChecked(true);

        assertThat(readSecureSetting()).isEqualTo(1);
    }

    @Test
    public void setChecked_unchecked_updateSettings() throws Settings.SettingNotFoundException {
        // set the initial state to on
        updateSecureSetting(1);

        assertThat(readSecureSetting()).isEqualTo(1);

        mController.setChecked(false);

        assertThat(readSecureSetting()).isEqualTo(0);
    }

    @EnableFlags(FLAG_GLANCEABLE_HUB_V2)
    @DisableFlags(FLAG_GLANCEABLE_HUB_ENABLED_BY_DEFAULT)
    @Test
    public void hubEnabledByUser_defaultsToConfigValue_true() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_glanceableHubEnabledByDefault, true);

        assertThat(mController.isChecked()).isTrue();
    }

    @EnableFlags(FLAG_GLANCEABLE_HUB_V2)
    @DisableFlags(FLAG_GLANCEABLE_HUB_ENABLED_BY_DEFAULT)
    @Test
    public void hubEnabledByUser_defaultsToConfigValue_false() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_glanceableHubEnabledByDefault, false);

        assertThat(mController.isChecked()).isFalse();
    }

    @EnableFlags({FLAG_GLANCEABLE_HUB_V2, FLAG_GLANCEABLE_HUB_ENABLED_BY_DEFAULT})
    @Test
    public void hubEnabledByUser_defaultsToFlagValue_true() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_glanceableHubEnabledByDefault, false);

        assertThat(mController.isChecked()).isTrue();
    }

    private void updateSecureSetting(int value) {
        Settings.Secure.putInt(mContext.getContentResolver(), GLANCEABLE_HUB_ENABLED, value);
    }

    private int readSecureSetting() throws Settings.SettingNotFoundException {
        return Settings.Secure.getInt(mContext.getContentResolver(), GLANCEABLE_HUB_ENABLED);
    }

    private void setShowGlanceableHubToggleSettingEnabled(boolean enabled) {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_glanceable_hub_toggle_setting, enabled);
    }

    private void setGlanceableHubEnabled(boolean enabled) {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_glanceableHubEnabled, enabled);
    }

    private void setShowGlanceableHubToggleSettingOnMobileEnabled(boolean enabled) {
        SettingsShadowResources.overrideResource(
                R.bool.config_show_glanceable_hub_toggle_setting_mobile, enabled);
    }

    @Implements(RestrictedLockUtilsInternal.class)
    public static class ShadowRestrictedLockUtilsInternal {
        private static int sKeyguardDisabledFeatures;

        /** Reset state. */
        @Resetter
        public static void reset() {
            sKeyguardDisabledFeatures = 0;
        }

        @Implementation
        protected static EnforcedAdmin checkIfKeyguardFeaturesDisabled(
                Context context, int features, @UserIdInt final int userId) {
            return (sKeyguardDisabledFeatures & features) == 0 ? null : new EnforcedAdmin();
        }

        public static void setKeyguardDisabledFeatures(int features) {
            sKeyguardDisabledFeatures = features;
        }
    }
}
