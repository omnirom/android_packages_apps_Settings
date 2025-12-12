/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.location;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Html;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {SettingsShadowResources.class})
public class LocationSettingsFooterPreferenceControllerTest {

    private static final int TEST_RES_ID = 1234;
    private static final String TEST_TEXT = "text";
    private static final String PREFERENCE_KEY = "location_footer";

    private Context mContext;
    private LocationSettingsFooterPreferenceController mController;
    private Lifecycle mLifecycle;

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private FooterPreference mFooterPreference;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private Resources mResources;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        // Telephony features are enabled by default.
        mockTelephonyService();
        when(mTelephonyManager.isDeviceSmsCapable()).thenReturn(true);
        when(mTelephonyManager.isDeviceVoiceCapable()).thenReturn(true);
        setConfigShowSimInfo(true);

        LifecycleOwner lifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(lifecycleOwner);
        LocationSettings locationSettings = spy(new LocationSettings());
        when(locationSettings.getSettingsLifecycle()).thenReturn(mLifecycle);

        mController = spy(new LocationSettingsFooterPreferenceController(mContext, PREFERENCE_KEY));
        mController.init(locationSettings);

        when(mPreferenceScreen.findPreference(PREFERENCE_KEY)).thenReturn(mFooterPreference);
        when(mPackageManager.getResourcesForApplication(any(ApplicationInfo.class)))
                .thenReturn(mResources);
        when(mResources.getString(TEST_RES_ID)).thenReturn(TEST_TEXT);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void isAvailable_hasValidFooter_returnsTrue() {
        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        assertThat(mController.isAvailable()).isTrue();
    }

    /**
     * Display the footer even without the injected string.
     */
    @Test
    public void isAvailable_noSystemApp_returnsTrue() {
        setUpFooterPreference(/*isSystemApp*/ false, /*hasRequiredMetaData*/ true);
        assertThat(mController.isAvailable()).isTrue();
    }

    /**
     * Display the footer even without the injected string.
     */
    @Test
    public void isAvailable_noRequiredMetadata_returnsTrue() {
        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ false);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_setTitle() {
        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        mController.updateState(mFooterPreference);

        ArgumentCaptor<CharSequence> title = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference).setTitle(title.capture());
        assertThat(title.getValue()).isNotNull();
    }

    @Test
    public void onLocationModeChanged_off_telephonyEnabled_setTitle() {
        // Calling and SMS messaging capabilities are enabled by default in test setup.
        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        setUpLocationModeChanged(/*locationEnabled*/ false, /*shouldFooterTitleChange*/ true);
        assertLocationFooter(R.string.location_settings_footer_location_off_with_telephony);
    }

    /**
     * Test that the correct footer string shows when location is off and telephony is disabled by
     * the config boolean.
     */
    @Test
    public void onLocationModeChanged_off_telephonyDisabled_setTitle() {
        setConfigShowSimInfo(false);

        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        setUpLocationModeChanged(/*locationEnabled*/ false, /*shouldFooterTitleChange*/ true);
        assertLocationFooter(R.string.location_settings_footer_location_off_no_telephony);
    }

    @Test
    public void onLocationModeChanged_off_noSmsMessaging_setTitle() {
        when(mTelephonyManager.isDeviceSmsCapable()).thenReturn(false);

        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        setUpLocationModeChanged(/*locationEnabled*/ false, /*shouldFooterTitleChange*/ true);
        assertLocationFooter(R.string.location_settings_footer_location_off_with_telephony);
    }

    @Test
    public void onLocationModeChanged_off_noCalling_setTitle() {
        when(mTelephonyManager.isDeviceVoiceCapable()).thenReturn(false);

        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        setUpLocationModeChanged(/*locationEnabled*/ false, /*shouldFooterTitleChange*/ true);
        assertLocationFooter(R.string.location_settings_footer_location_off_with_telephony);
    }

    @Test
    public void onLocationModeChanged_off_noCallingOrMessaging_setTitle() {
        when(mTelephonyManager.isDeviceSmsCapable()).thenReturn(false);
        when(mTelephonyManager.isDeviceVoiceCapable()).thenReturn(false);

        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        setUpLocationModeChanged(/*locationEnabled*/ false, /*shouldFooterTitleChange*/ true);
        assertLocationFooter(R.string.location_settings_footer_location_off_no_telephony);
    }

    @Test
    public void onLocationModeChanged_on_setTitle() {
        setUpFooterPreference(/*isSystemApp*/ true, /*hasRequiredMetaData*/ true);
        setUpLocationModeChanged(/*locationEnabled*/ true, /*shouldFooterTitleChange*/ true);

        ArgumentCaptor<CharSequence> title = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference, times(2)).setTitle(title.capture());

        assertThat(title.getValue().toString()).doesNotContain(
                Html.fromHtml(mContext.getString(
                        R.string.location_settings_footer_location_off_with_telephony)).toString());
        assertThat(title.getValue().toString()).doesNotContain(
                Html.fromHtml(mContext.getString(
                        R.string.location_settings_footer_location_off_no_telephony)).toString());
    }

    @Test
    public void onLocationModeChanged_on_withoutInjectedString_setTitle() {
        setUpFooterPreference(/*isSystemApp*/ false, /*hasRequiredMetaData*/ true);
        setUpLocationModeChanged(/*locationEnabled*/ true, /*shouldFooterTitleChange*/ false);

        ArgumentCaptor<CharSequence> title = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference, times(1)).setTitle(title.capture());
        assertThat(title.getValue().toString())
                .isEqualTo(
                        Html.fromHtml(mContext.getString(R.string.location_settings_footer_general))
                                .toString());
    }

    @Test
    public void updateState_notSystemApp_ignore() {
        setUpFooterPreference(/*isSystemApp*/ false, /*hasRequiredMetaData*/ true);
        mController.updateState(mFooterPreference);
        verify(mFooterPreference, never()).setTitle(anyChar());
    }

    /**
     * Returns a ResolveInfo object for testing
     * @param isSystemApp If true, the application is a system app.
     * @param hasRequiredMetaData If true, the broadcast receiver has a valid value for
     *                            {@link LocationManager#METADATA_SETTINGS_FOOTER_STRING}
     */
    private ResolveInfo getTestResolveInfo(boolean isSystemApp, boolean hasRequiredMetaData) {
        ResolveInfo testResolveInfo = new ResolveInfo();
        ApplicationInfo testAppInfo = new ApplicationInfo();
        if (isSystemApp) {
            testAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        }
        ActivityInfo testActivityInfo = new ActivityInfo();
        testActivityInfo.name = "TestActivityName";
        testActivityInfo.packageName = "TestPackageName";
        testActivityInfo.applicationInfo = testAppInfo;
        if (hasRequiredMetaData) {
            testActivityInfo.metaData = new Bundle();
            testActivityInfo.metaData.putInt(
                    LocationManager.METADATA_SETTINGS_FOOTER_STRING, TEST_RES_ID);
        }
        testResolveInfo.activityInfo = testActivityInfo;
        return testResolveInfo;
    }

    /**
     * Sets up the footer preference.
     *
     * @param isSystemApp If true, the application is a system app.
     * @param hasRequiredMetaData If true, the broadcast receiver has a valid value for
     *                            {@link LocationManager#METADATA_SETTINGS_FOOTER_STRING}
     */
    private void setUpFooterPreference(boolean isSystemApp, boolean hasRequiredMetaData) {
        final List<ResolveInfo> testResolveInfos = new ArrayList<>();
        testResolveInfos.add(getTestResolveInfo(isSystemApp, hasRequiredMetaData));
        when(mPackageManager.queryBroadcastReceivers(any(Intent.class), anyInt()))
                .thenReturn(testResolveInfos);
    }

    /**
     * Sets up the location mode to the given status.
     *
     * @param locationEnabled Whether the location mode is on or off.
     * @param shouldFooterTitleChange Whether the footer title should change.
     */
    private void setUpLocationModeChanged(
            boolean locationEnabled, boolean shouldFooterTitleChange) {
        assertThat(mController.isAvailable()).isTrue();
        mController.updateState(mFooterPreference);
        if (shouldFooterTitleChange) {
            verify(mFooterPreference).setTitle(any());
        }
        mController.onLocationModeChanged(locationEnabled ? 1 : 0, /*restricted*/ false);
    }

    /**
     * Asserts that the location footer exists and is equal to the expected string.
     * @param footerStringId The string resource id to assert.
     */
    private void assertLocationFooter(int footerStringId) {
        ArgumentCaptor<CharSequence> title = ArgumentCaptor.forClass(CharSequence.class);
        verify(mFooterPreference, times(2)).setTitle(title.capture());
        assertThat(title.getValue().toString())
                .isEqualTo(
                        Html.fromHtml(mContext.getString(footerStringId)).toString()
                                + "\n\n"
                                + mContext.getString(R.string.location_settings_footer_general));
    }

    private void mockTelephonyService() {
        when(mContext.getSystemServiceName(TelephonyManager.class))
                .thenReturn(Context.TELEPHONY_SERVICE);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
    }

    private void setConfigShowSimInfo(boolean enabled) {
        SettingsShadowResources.overrideResource(R.bool.config_show_sim_info, enabled);
    }
}
