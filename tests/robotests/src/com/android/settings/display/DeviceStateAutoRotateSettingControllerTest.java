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

package com.android.settings.display;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;
import static com.android.settings.testutils.DeviceStateAutoRotateSettingTestUtils.DEFAULT_DEVICE_STATE;
import static com.android.settings.testutils.DeviceStateAutoRotateSettingTestUtils.setDeviceStateRotationLockEnabled;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.devicestate.DeviceStateManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowRotationPolicy;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.devicestate.DeviceStateAutoRotateSettingManager;
import com.android.settingslib.devicestate.DeviceStateAutoRotateSettingManager.DeviceStateAutoRotateSettingListener;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRotationPolicy.class})
public class DeviceStateAutoRotateSettingControllerTest {

    private static final String DEFAULT_DEVICE_STATE_DESCRIPTION = "Device state description";
    private static final int DEFAULT_ORDER = -10;

    private final Context mContext = Mockito.spy(RuntimeEnvironment.application);
    private DeviceStateAutoRotateSettingManager mSpyAutoRotateSettingsManager;

    @Captor private ArgumentCaptor<DeviceStateAutoRotateSettingListener>
            mDeviceStateAutoRotateSettingListenerCaptor;

    @Mock private MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock private DeviceStateManager mDeviceStateManager;
    @Mock private Resources mResources;

    private DeviceStateAutoRotateSettingController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(mContext).when(mContext).getApplicationContext();
        when(mContext.getResources()).thenReturn(mResources);
        doReturn(mDeviceStateManager).when(mContext).getSystemService(DeviceStateManager.class);
        setDeviceStateRotationLockEnabled(/* enable= */ false, mResources, mDeviceStateManager);
        mSpyAutoRotateSettingsManager =
                Mockito.spy(
                        DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(mContext));

        mController = new DeviceStateAutoRotateSettingController(
                mContext,
                DEFAULT_DEVICE_STATE.getIdentifier(),
                DEFAULT_DEVICE_STATE_DESCRIPTION,
                DEFAULT_ORDER,
                mMetricsFeatureProvider,
                mSpyAutoRotateSettingsManager
        );
    }

    @Test
    public void displayPreference_addsPreferenceToPreferenceScreen() {
        PreferenceScreen screen = new PreferenceManager(mContext).createPreferenceScreen(mContext);

        mController.displayPreference(screen);

        assertThat(screen.getPreferenceCount()).isEqualTo(1);
        Preference preference = screen.getPreference(0);
        assertThat(preference.getTitle().toString()).isEqualTo(DEFAULT_DEVICE_STATE_DESCRIPTION);
        assertThat(preference.getOrder()).isEqualTo(DEFAULT_ORDER);
        assertThat(preference.getKey()).isEqualTo(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_rotationAndDeviceStateRotationEnabled_returnsAvailable() {
        ShadowRotationPolicy.setRotationSupported(true);
        setDeviceStateRotationLockEnabled(/* enable= */ true, mResources, mDeviceStateManager);

        int availability = mController.getAvailabilityStatus();

        assertThat(availability).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_deviceStateRotationDisabled_returnsUnsupported() {
        ShadowRotationPolicy.setRotationSupported(true);
        setDeviceStateRotationLockEnabled(false, mResources, mDeviceStateManager);

        int availability = mController.getAvailabilityStatus();

        assertThat(availability).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_rotationDisabled_returnsUnsupported() {
        ShadowRotationPolicy.setRotationSupported(false);
        setDeviceStateRotationLockEnabled(/* enable= */ true, mResources, mDeviceStateManager);

        int availability = mController.getAvailabilityStatus();

        assertThat(availability).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getPreferenceKey_returnsKeyBasedOnDeviceState() {
        String key = mController.getPreferenceKey();

        String expectedKey = "auto_rotate_device_state_" + DEFAULT_DEVICE_STATE.getIdentifier();
        assertThat(key).isEqualTo(expectedKey);
    }

    @Test
    public void isChecked_settingForStateIsUnlocked_returnsTrue() {
        mController.onStart();
        verify(mSpyAutoRotateSettingsManager, atLeastOnce()).registerListener(
                mDeviceStateAutoRotateSettingListenerCaptor.capture());
        when(mSpyAutoRotateSettingsManager.isRotationLocked(
                eq(DEFAULT_DEVICE_STATE.getIdentifier()))).thenReturn(false);

        mDeviceStateAutoRotateSettingListenerCaptor.getValue().onSettingsChanged();

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_settingForStateIsLocked_returnsFalse() {
        mController.onStart();
        verify(mSpyAutoRotateSettingsManager, atLeastOnce()).registerListener(
                mDeviceStateAutoRotateSettingListenerCaptor.capture());
        when(mSpyAutoRotateSettingsManager.isRotationLocked(
                eq(DEFAULT_DEVICE_STATE.getIdentifier()))).thenReturn(true);

        mDeviceStateAutoRotateSettingListenerCaptor.getValue().onSettingsChanged();

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_true_deviceStateSettingIsUnlocked() {
        mController.onStart();
        verify(mSpyAutoRotateSettingsManager, atLeastOnce()).registerListener(
                mDeviceStateAutoRotateSettingListenerCaptor.capture());

        mController.setChecked(true);

        verify(mSpyAutoRotateSettingsManager, atLeastOnce()).updateSetting(
                eq(DEFAULT_DEVICE_STATE.getIdentifier()), eq(false));
    }

    @Test
    public void setChecked_false_deviceStateSettingIsLocked() {
        mController.onStart();
        verify(mSpyAutoRotateSettingsManager, atLeastOnce()).registerListener(
                mDeviceStateAutoRotateSettingListenerCaptor.capture());

        mController.setChecked(false);

        verify(mSpyAutoRotateSettingsManager, atLeastOnce()).updateSetting(
                eq(DEFAULT_DEVICE_STATE.getIdentifier()), eq(true));
    }

    @Test
    public void setChecked_true_logsDeviceStateBasedSettingOn() {
        mController.setChecked(true);

        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_ENABLE_AUTO_ROTATION_DEVICE_STATE,
                DEFAULT_DEVICE_STATE.getIdentifier());
    }

    @Test
    public void setChecked_false_logsDeviceStateBasedSettingOff() {
        mController.setChecked(false);

        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_DISABLE_AUTO_ROTATION_DEVICE_STATE,
                DEFAULT_DEVICE_STATE.getIdentifier());
    }

    @Test
    public void updateRawDataToIndex_addsItemToList() {
        List<SearchIndexableRaw> rawData = new ArrayList<>();

        mController.updateRawDataToIndex(rawData);

        assertThat(rawData).hasSize(1);
        SearchIndexableRaw item = rawData.get(0);
        assertThat(item.key).isEqualTo(mController.getPreferenceKey());
        assertThat(item.title).isEqualTo(DEFAULT_DEVICE_STATE_DESCRIPTION);
        assertThat(item.screenTitle).isEqualTo(mContext.getString(R.string.accelerometer_title));
    }

    @Test
    public void getSliceHighlightMenuRes_returnsMenuKeyDisplay() {
        int sliceHighlightMenuRes = mController.getSliceHighlightMenuRes();

        assertThat(sliceHighlightMenuRes).isEqualTo(R.string.menu_key_display);
    }

    @Test
    public void isSliceable_returnsTrue() {
        assertThat(mController.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnsTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
