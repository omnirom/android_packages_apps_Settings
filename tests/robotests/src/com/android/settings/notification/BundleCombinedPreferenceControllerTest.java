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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.content.Context;
import android.os.RemoteException;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.Adjustment;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;

import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

    private MainSwitchPreference mGlobalSwitch;
    private CheckBoxPreference mPromoCheckbox, mNewsCheckbox, mSocialCheckbox, mRecsCheckbox;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.getApplication();
        mController = new BundleCombinedPreferenceController(mContext, PREFERENCE_KEY, mBackend);

        // preference category/controller initiation
        mGlobalSwitch = new MainSwitchPreference(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.GLOBAL_KEY)).thenReturn(mGlobalSwitch);
        mPromoCheckbox = new CheckBoxPreference(mContext);
        when(mPrefCategory.findPreference(BundleCombinedPreferenceController.PROMO_KEY)).thenReturn(
                mPromoCheckbox);
        mNewsCheckbox = new CheckBoxPreference(mContext);
        when(mPrefCategory.findPreference(BundleCombinedPreferenceController.NEWS_KEY)).thenReturn(
                mNewsCheckbox);
        mSocialCheckbox = new CheckBoxPreference(mContext);
        when(mPrefCategory.findPreference(
                BundleCombinedPreferenceController.SOCIAL_KEY)).thenReturn(mSocialCheckbox);
        mRecsCheckbox = new CheckBoxPreference(mContext);
        when(mPrefCategory.findPreference(BundleCombinedPreferenceController.RECS_KEY)).thenReturn(
                mRecsCheckbox);

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
        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(true);

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
    public void updatePrefValues_typesGoneWhenGlobalOff() {
        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(false);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_PROMOTION,
                Adjustment.TYPE_NEWS));

        mController.updatePrefValues();
        assertThat(mGlobalSwitch.isChecked()).isFalse();
        assertThat(mPromoCheckbox.isVisible()).isFalse();
        assertThat(mNewsCheckbox.isVisible()).isFalse();
        assertThat(mRecsCheckbox.isVisible()).isFalse();
        assertThat(mSocialCheckbox.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOffGlobalSwitch_updatesBackendAndTypeSwitches() {
        // Initial state: global allowed + some types set
        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_PROMOTION,
                Adjustment.TYPE_NEWS));
        mController.updatePrefValues();

        // Simulate the global switch turning off. This also requires telling the mock backend to
        // start returning false before the click listener updates pref values
        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(false);
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, false);
        verify(mBackend, times(1)).setNotificationBundlingEnabled(false);

        // All individual type checkboxes should now not be visible.
        assertThat(mPromoCheckbox.isVisible()).isFalse();
        assertThat(mNewsCheckbox.isVisible()).isFalse();
        assertThat(mRecsCheckbox.isVisible()).isFalse();
        assertThat(mSocialCheckbox.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOnGlobalSwitch_updatesBackendAndTypeSwitches() {
        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(false);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_PROMOTION,
                Adjustment.TYPE_NEWS));
        mController.updatePrefValues();

        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(true);
        mGlobalSwitch.getOnPreferenceChangeListener().onPreferenceChange(mGlobalSwitch, true);
        verify(mBackend, times(1)).setNotificationBundlingEnabled(true);

        // type checkboxes should now exist & be checked accordingly to their state
        assertThat(mPromoCheckbox.isChecked()).isTrue();
        assertThat(mNewsCheckbox.isChecked()).isTrue();
        assertThat(mRecsCheckbox.isChecked()).isFalse();
        assertThat(mSocialCheckbox.isChecked()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOnTypeBundle_updatesBackend_doesNotChangeGlobalSwitch() {
        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(true);
        when(mBackend.getAllowedBundleTypes()).thenReturn(Set.of(Adjustment.TYPE_SOCIAL_MEDIA));
        mController.updatePrefValues();

        mRecsCheckbox.getOnPreferenceChangeListener().onPreferenceChange(mRecsCheckbox, true);

        // recs bundle setting should be updated in the backend, and global switch unchanged
        verify(mBackend).setBundleTypeState(Adjustment.TYPE_CONTENT_RECOMMENDATION, true);
        verify(mBackend, never()).setNotificationBundlingEnabled(anyBoolean());
    }

    @Test
    @EnableFlags(Flags.FLAG_NOTIFICATION_CLASSIFICATION_UI)
    public void turnOffTypeBundle_lastOneChangesGlobalSwitch() {
        when(mBackend.isNotificationBundlingEnabled(any())).thenReturn(true);
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
        verify(mBackend).setNotificationBundlingEnabled(false);
    }
}
