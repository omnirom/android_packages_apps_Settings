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
package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowIntent;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class, ShadowBluetoothUtils.class})
public class BluetoothKeyMissingDialogTest {
    @Mock private BluetoothDevice mBluetoothDevice;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private LocalBluetoothManager mLocalBtManager;

    private BluetoothKeyMissingDialogFragment mFragment = null;
    private FragmentActivity mActivity = null;

    private static final String MAC_ADDRESS = "12:34:56:78:90:12";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mBluetoothDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mLocalBtManager.getBluetoothAdapter().getRemoteDevice(MAC_ADDRESS))
                .thenReturn(mBluetoothDevice);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mFragment = BluetoothKeyMissingDialogFragment.newInstance(mBluetoothDevice);
        mActivity
                .getSupportFragmentManager()
                .beginTransaction()
                .add(mFragment, null)
                .commit();
        shadowMainLooper().idle();
    }

    @Test
    public void clickDeviceSettings_launchDeviceDetails() {
        mFragment.onClick(mFragment.getDialog(), AlertDialog.BUTTON_POSITIVE);

        Intent startedIntent = shadowOf(mActivity).getNextStartedActivity();
        ShadowIntent shadowIntent = shadowOf(startedIntent);
        assertThat(shadowIntent.getIntentClass()).isEqualTo(SubSettings.class);
        assertThat(startedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(BluetoothDeviceDetailsFragment.class.getName());
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void clickCancel_notLaunchDeviceDetails() {
        mFragment.onClick(mFragment.getDialog(), AlertDialog.BUTTON_NEGATIVE);

        Intent startedIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(startedIntent).isNull();
        assertThat(mActivity.isFinishing()).isTrue();
    }
}
