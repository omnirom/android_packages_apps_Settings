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

package com.android.settings.notification;

import static android.provider.Settings.Secure.NOTIFICATION_BUNDLES_ALWAYS_EXPAND;

import static com.android.settings.notification.BundleCombinedPreferenceController.OFF;
import static com.android.settings.notification.BundleCombinedPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.os.RemoteException;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.service.notification.Adjustment;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreference;

import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(android.service.notification.Flags.FLAG_NOTIFICATION_CLASSIFICATION)
public class BundleCombinedPreferenceControllerTest {
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    private BundleCombinedPreferenceController mController;

    @Mock
    private NotificationBackend mBackend;

    @Mock
    private PreferenceCategory mPrefCategory;

    @Mock
    private PreferenceCategory mTypesPrefCategory;


    private MainSwitchPreference mGlobalSwitch;
    private SwitchPreference mWorkSwitch;
    private SwitchPreference mAlwaysExpandSwitch;
    private CheckBoxPreference mPromoCheckbox, mNewsCheckbox, mSocialCheckbox, mRecsCheckbox;
    private PreferenceCategory mExcludedAppsPrefCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();
        mController = new BundleCombinedPreferenceController(mContext, PREFERENCE_KEY, mBackend);

        // preference category/controller initiation
        mGlobalSwitch = new MainSwitchPreference(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.GLOBAL_KEY)).thenReturn(mGlobalSwitch);

        mWorkSwitch = new SwitchPreference(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.WORK_PREF_KEY)).thenReturn(mWorkSwitch);

        mAlwaysExpandSwitch = new SwitchPreference(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.ALWAYS_EXPAND_KEY)).thenReturn(
                mAlwaysExpandSwitch);

        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.TYPE_CATEGORY_KEY)).thenReturn(
                mTypesPrefCategory);
        mPromoCheckbox = new CheckBoxPreference(mContext);
        when(mTypesPrefCategory.findPreference(
                BundleCombinedPreferenceController.PROMO_KEY)).thenReturn(mPromoCheckbox);
        mNewsCheckbox = new CheckBoxPreference(mContext);
        when(mTypesPrefCategory.findPreference(
                BundleCombinedPreferenceController.NEWS_KEY)).thenReturn(mNewsCheckbox);
        mSocialCheckbox = new CheckBoxPreference(mContext);
        when(mTypesPrefCategory.findPreference(
                BundleCombinedPreferenceController.SOCIAL_KEY)).thenReturn(mSocialCheckbox);
        mRecsCheckbox = new CheckBoxPreference(mContext);
        when(mTypesPrefCategory.findPreference(
                BundleCombinedPreferenceController.RECS_KEY)).thenReturn(mRecsCheckbox);
        mExcludedAppsPrefCategory = new PreferenceCategory(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.EXCLUDED_APPS_CATEGORY_KEY)).thenReturn(
                mExcludedAppsPrefCategory);

        mController.updateState(mPrefCategory);
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void isAvailable_flagEnabledNasSupports_shouldReturnTrue() {
        when(mBackend.isNotificationBundlingSupported()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void isAvailable_flagEnabledNasDoesNotSupport_shouldReturnFalse()
            throws RemoteException {
        when(mBackend.isNotificationBundlingSupported()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void isAvailable_flagDisabledNasSupports_shouldReturnFalse() throws RemoteException {
        when(mBackend.isNotificationBundlingSupported()).thenReturn(true);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void updatePrefValues_reflectsSettings() {
        // bundling is enabled globally
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);

        // allowed key types are promos & news
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_PROMOTION,
                Adjustment.TYPE_NEWS));

        mController.updatePrefValues();
        assertThat(mGlobalSwitch.isChecked()).isTrue();
        assertThat(mPromoCheckbox.isChecked()).isTrue();
        assertThat(mNewsCheckbox.isChecked()).isTrue();
        assertThat(mRecsCheckbox.isChecked()).isFalse();
        assertThat(mSocialCheckbox.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void updateState_noManagedProfile_workSwitchNotVisible() {
        // bundling is enabled globally with non-zero types
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA));
        mController.setManagedProfile(null);

        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void updateState_hasManagedProfile_reflectsSettings() {
        // bundling is enabled globally with non-zero types
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA));

        // set up a work profile with userId 12345, with bundling enabled
        mController.setManagedProfile(new UserHandle(12345));
        when(mBackend.isNotificationBundlingEnabled(12345)).thenReturn(true);

        // need to re-update state and not just pref values so that the work pref is actually set
        // up correctly
        mController.updateState(mPrefCategory);
        assertThat(mWorkSwitch.isChecked()).isTrue();

        // now disabled
        when(mBackend.isNotificationBundlingEnabled(12345)).thenReturn(false);
        mController.updatePrefValues();
        assertThat(mWorkSwitch.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void updatePrefValues_otherPrefsGoneWhenGlobalOff() {
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(false);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_PROMOTION,
                Adjustment.TYPE_NEWS));

        Mockito.reset(mTypesPrefCategory);
        mController.updatePrefValues();
        assertThat(mGlobalSwitch.isChecked()).isFalse();
        assertThat(mWorkSwitch.isVisible()).isFalse();
        verify(mTypesPrefCategory).setVisible(false);
        assertThat(mAlwaysExpandSwitch.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOffGlobalSwitch_updatesBackendAndOtherSwitches() {
        // Initial state: global allowed + some types set. Work profile exists
        mController.setManagedProfile(new UserHandle(12345));
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_PROMOTION,
                Adjustment.TYPE_NEWS));
        mController.updateState(mPrefCategory);

        // Simulate the global switch turning off. This also requires telling the mock backend to
        // start returning false before the click listener updates pref values
        Mockito.reset(mTypesPrefCategory);
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(false);
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, false);
        verify(mBackend, times(1)).setNotificationBundlingEnabled(mContext.getUserId(), false);

        // All individual type checkboxes should now not be visible.
        assertThat(mWorkSwitch.isVisible()).isFalse();
        verify(mTypesPrefCategory).setVisible(false);
        assertThat(mExcludedAppsPrefCategory.isVisible()).isFalse();
        assertThat(mAlwaysExpandSwitch.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOnGlobalSwitch_updatesBackendAndTypeSwitches() {
        mController.setManagedProfile(new UserHandle(12345));
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(false);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_PROMOTION,
                Adjustment.TYPE_NEWS));
        mController.updateState(mPrefCategory);

        // simulate globally enabled but work profile setting still disabled
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.isNotificationBundlingEnabled(12345)).thenReturn(false);
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, true);
        verify(mBackend, times(1)).setNotificationBundlingEnabled(mContext.getUserId(), true);

        // type checkboxes should now exist & be checked accordingly to their state
        assertThat(mWorkSwitch.isChecked()).isFalse();
        assertThat(mPromoCheckbox.isChecked()).isTrue();
        assertThat(mNewsCheckbox.isChecked()).isTrue();
        assertThat(mRecsCheckbox.isChecked()).isFalse();
        assertThat(mSocialCheckbox.isChecked()).isFalse();
        assertThat(mExcludedAppsPrefCategory.isVisible()).isTrue();
        assertThat(mAlwaysExpandSwitch.isVisible()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void toggleWorkSwitch_updatesBackend() {
        // default setting: bundling is enabled, with some type enabled as well
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA));

        // work profile exists, user id 12345
        // re-update state to make sure the work profile switch is actually set up
        mController.setManagedProfile(new UserHandle(12345));
        mController.updateState(mPrefCategory);

        mWorkSwitch.getOnPreferenceChangeListener().onPreferenceChange(mWorkSwitch, true);
        verify(mBackend).setNotificationBundlingEnabled(12345, true);

        // no changes should be made to the main user switch
        verify(mBackend, never()).setNotificationBundlingEnabled(eq(mContext.getUserId()),
                anyBoolean());
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOnTypeBundle_updatesBackend_doesNotChangeGlobalSwitch() {
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA));
        mController.updatePrefValues();

        mRecsCheckbox.getOnPreferenceChangeListener().onPreferenceChange(mRecsCheckbox, true);

        // recs bundle setting should be updated in the backend, and global switch unchanged
        verify(mBackend).setBundleTypeState(Adjustment.TYPE_CONTENT_RECOMMENDATION, true);
        verify(mBackend, never()).setNotificationBundlingEnabled(anyInt(), anyBoolean());
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOffTypeBundle_lastOneChangesGlobalSwitch() {
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA,
                Adjustment.TYPE_CONTENT_RECOMMENDATION));
        mController.updatePrefValues();

        // Turning off one should update state, but not turn off the global setting
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA));
        mRecsCheckbox.getOnPreferenceChangeListener().onPreferenceChange(mRecsCheckbox, false);
        verify(mBackend).setBundleTypeState(Adjustment.TYPE_CONTENT_RECOMMENDATION, false);

        // Now turn off the second
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of());
        mSocialCheckbox.getOnPreferenceChangeListener().onPreferenceChange(mSocialCheckbox, false);
        verify(mBackend).setBundleTypeState(Adjustment.TYPE_SOCIAL_MEDIA, false);

        // This update should trigger a call to turn off the global switch
        verify(mBackend).setNotificationBundlingEnabled(mContext.getUserId(), false);
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void alwaysExpandSwitch_reflectsSettings() {
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA));
        mController.updatePrefValues();

        // Always expand setting is true
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                ON);
        mController.updatePrefValues();
        assertThat(mAlwaysExpandSwitch.isVisible()).isTrue();
        assertThat(mAlwaysExpandSwitch.isChecked()).isTrue();

        // Always expand setting is false
        Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                OFF);
        mController.updatePrefValues();
        assertThat(mAlwaysExpandSwitch.isVisible()).isTrue();
        assertThat(mAlwaysExpandSwitch.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void toggleAlwaysExpandSwitch_updatesSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(), NOTIFICATION_BUNDLES_ALWAYS_EXPAND,
                OFF);
        when(mBackend.isNotificationBundlingEnabled(anyInt())).thenReturn(true);
        mController.updatePrefValues();

        // Toggle on
        mAlwaysExpandSwitch.getOnPreferenceChangeListener().onPreferenceChange(
                mAlwaysExpandSwitch, true);
        // Check that the secure setting was updated
        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND, -1))
                .isEqualTo(ON);

        // Toggle off
        mAlwaysExpandSwitch.getOnPreferenceChangeListener().onPreferenceChange(
                mAlwaysExpandSwitch, false);
        // Check that the secure setting was updated
        assertThat(android.provider.Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_BUNDLES_ALWAYS_EXPAND, -1))
                .isEqualTo(OFF);
    }
}
