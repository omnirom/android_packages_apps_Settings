/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.bluetooth.BluetoothDevice.BOND_NONE;

import static com.android.settings.bluetooth.BluetoothDetailsHearingDeviceSettingsController.KEY_HEARING_DEVICE_SETTINGS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.app.Application;
import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothDevice;
import android.companion.CompanionDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.FeatureFlagUtils;
import android.view.InputDevice;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            com.android.settings.testutils.shadow.ShadowUserManager.class,
            com.android.settings.testutils.shadow.ShadowBluetoothUtils.class
        })
public class BluetoothDeviceDetailsFragmentTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String TEST_ADDRESS = "55:66:77:88:99:AA";
    private static final int TEST_DEVICE_ID = 123;

    private BluetoothDeviceDetailsFragment mFragment;
    private Context mContext = spy(ApplicationProvider.getApplicationContext());
    private FragmentActivity mActivity;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CachedBluetoothDevice mCachedDevice;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalManager;

    @Mock private InputManager mInputManager;
    @Mock private CompanionDeviceManager mCompanionDeviceManager;
    @Mock private BluetoothDevice mBluetoothDevice;

    @Before
    public void setUp() {
        mSetFlagsRule.disableFlags(
                com.android.settingslib.flags.Flags.FLAG_HEARING_DEVICES_AMBIENT_VOLUME_CONTROL);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalManager;
        doReturn(new int[] {TEST_DEVICE_ID}).when(mInputManager).getInputDeviceIds();
        doReturn(TEST_ADDRESS).when(mInputManager).getInputDeviceBluetoothAddress(TEST_DEVICE_ID);

        ShadowApplication shadowApplication =
                shadowOf((Application) ApplicationProvider.getApplicationContext());
        shadowApplication.setSystemService(
                Context.COMPANION_DEVICE_SERVICE, mCompanionDeviceManager);
        shadowApplication.setSystemService(Context.INPUT_SERVICE, mInputManager);
        doReturn(mCompanionDeviceManager)
                .when(mContext)
                .getSystemService(CompanionDeviceManager.class);
        doReturn(ImmutableList.of()).when(mCompanionDeviceManager).getAllAssociations();
    }

    @Test
    public void verifyOnAttachResult() {
        runFragmentTest(
                () -> {
                    assertThat(mFragment.deviceAddress).isEqualTo(TEST_ADDRESS);
                    assertThat(mFragment.localBluetoothManager).isEqualTo(mLocalManager);
                    assertThat(mFragment.cachedDevice).isEqualTo(mCachedDevice);
                    assertThat(mFragment.mInputDevice).isEqualTo(null);
                });
    }

    @Test
    public void verifyOnAttachResult_flagEnabledAndInputDeviceSet_returnsInputDevice() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES, true);
        InputDevice inputDevice = mock(InputDevice.class);
        doReturn(inputDevice).when(mInputManager).getInputDevice(TEST_DEVICE_ID);

        runFragmentTest(
                () -> {
                    assertThat(mFragment.deviceAddress).isEqualTo(TEST_ADDRESS);
                    assertThat(mFragment.localBluetoothManager).isEqualTo(mLocalManager);
                    assertThat(mFragment.cachedDevice).isEqualTo(mCachedDevice);
                    assertThat(mFragment.mInputDevice).isEqualTo(inputDevice);
                });
    }

    @Test
    public void verifyOnAttachResult_flagDisabled_returnsNullInputDevice() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES, false);
        InputDevice inputDevice = mock(InputDevice.class);
        doReturn(inputDevice).when(mInputManager).getInputDevice(TEST_DEVICE_ID);
        runFragmentTest(
                () -> {
                    assertThat(mFragment.deviceAddress).isEqualTo(TEST_ADDRESS);
                    assertThat(mFragment.localBluetoothManager).isEqualTo(mLocalManager);
                    assertThat(mFragment.cachedDevice).isEqualTo(mCachedDevice);
                    assertThat(mFragment.mInputDevice).isEqualTo(null);
                });
    }

    @Test
    public void getTitle_inputDeviceTitle() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES, true);
        InputDevice inputDevice = mock(InputDevice.class);
        doReturn(true).when(inputDevice).supportsSource(InputDevice.SOURCE_STYLUS);
        doReturn(inputDevice).when(mInputManager).getInputDevice(TEST_DEVICE_ID);

        runFragmentTest(
                () -> {
                    assertThat(mActivity.getTitle().toString())
                            .isEqualTo(mContext.getString(R.string.stylus_device_details_title));
                });
    }

    @Test
    public void getTitle_inputDeviceNull_doesNotSetTitle() {
        FeatureFlagUtils.setEnabled(
                mContext, FeatureFlagUtils.SETTINGS_SHOW_STYLUS_PREFERENCES, true);
        doReturn(null).when(mInputManager).getInputDevice(TEST_DEVICE_ID);
        runFragmentTest(
                () -> {
                    assertThat(mActivity.getTitle().toString())
                            .isNotEqualTo(mContext.getString(R.string.stylus_device_details_title));
                });
    }

    @Test
    public void finishFragmentIfNecessary_deviceIsBondNone_finishFragment() {
        when(mCachedDevice.getBondState()).thenReturn(BOND_NONE);

        runFragmentTest(
                () -> {
                    mFragment.finishFragmentIfNecessary();

                    assertThat(mActivity.isFinishing()).isTrue();
                });
    }

    @Test
    public void createPreferenceControllers_launchFromHAPage_deviceControllerNotExist() {
        runFragmentTest(
                SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS,
                () -> {
                    List<AbstractPreferenceController> controllerList =
                            mFragment.createPreferenceControllers(mContext);
                    boolean hasController =
                            controllerList.stream()
                                    .anyMatch(
                                            controller ->
                                                    controller
                                                            .getPreferenceKey()
                                                            .equals(KEY_HEARING_DEVICE_SETTINGS));
                    assertThat(hasController).isFalse();
                });
    }

    @Test
    public void createPreferenceControllers_notLaunchFromHAPage_deviceControllerExist() {
        runFragmentTest(
                SettingsEnums.PAGE_UNKNOWN,
                () -> {
                    List<AbstractPreferenceController> controllerList =
                            mFragment.createPreferenceControllers(mContext);
                    boolean hasController =
                            controllerList.stream()
                                    .anyMatch(
                                            controller ->
                                                    controller
                                                            .getPreferenceKey()
                                                            .equals(KEY_HEARING_DEVICE_SETTINGS));

                    assertThat(hasController).isTrue();
                });
    }

    private void runFragmentTest(Runnable testBody) {
        runFragmentTest(SettingsEnums.PAGE_UNKNOWN, testBody);
    }

    private void runFragmentTest(int sourceMetricsCategory, Runnable testBody) {
        when(mLocalManager.getBluetoothAdapter().getRemoteDevice(TEST_ADDRESS))
                .thenReturn(mBluetoothDevice);
        when(mLocalManager.getCachedDeviceManager().findDevice(mBluetoothDevice))
                .thenReturn(mCachedDevice);
        doReturn(TEST_ADDRESS).when(mCachedDevice).getAddress();
        doReturn(TEST_ADDRESS).when(mCachedDevice).getIdentityAddress();

        Bundle args = new Bundle();
        args.putString("device_address", TEST_ADDRESS);
        Intent intent =
                new SubSettingLauncher(mContext)
                        .setDestination(BluetoothDeviceDetailsFragment.class.getName())
                        .setArguments(args)
                        .setTitleRes(R.string.device_details_title)
                        .setSourceMetricsCategory(sourceMetricsCategory)
                        .toIntent();

        try (ActivityScenario<SubSettings> activityScenario = ActivityScenario.launch(intent)) {
            activityScenario.onActivity(
                    activity -> {
                        mActivity = activity;
                        mFragment =
                                (BluetoothDeviceDetailsFragment)
                                        activity.getSupportFragmentManager().getFragments().get(0);
                        testBody.run();
                    });
        }

        shadowMainLooper().idle();
    }
}
