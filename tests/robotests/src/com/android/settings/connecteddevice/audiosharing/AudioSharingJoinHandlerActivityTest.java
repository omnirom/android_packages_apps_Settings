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

package com.android.settings.connecteddevice.audiosharing;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Intent;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.fragment.app.FragmentManager;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class})
public class AudioSharingJoinHandlerActivityTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private AudioSharingJoinHandlerActivity mActivity;

    @Before
    public void setUp() {
        mActivity = spy(Robolectric.buildActivity(AudioSharingJoinHandlerActivity.class).get());
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onCreate_flagOn_create() {
        mActivity.onCreate(new Bundle());
        verify(mActivity, never()).finish();
    }

    @Test
    public void isValidFragment_returnsTrue() {
        assertThat(mActivity.isValidFragment(
                AudioSharingJoinHandlerDashboardFragment.class.getName())).isTrue();
    }

    @Test
    public void isValidFragment_returnsFalse() {
        assertThat(mActivity.isValidFragment("")).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onNewIntent_flagOn_handleDeviceConnectedFromIntent() {
        FragmentManager fragmentManager = mock(FragmentManager.class);
        AudioSharingJoinHandlerDashboardFragment fragment = mock(
                AudioSharingJoinHandlerDashboardFragment.class);
        when(mActivity.getSupportFragmentManager()).thenReturn(fragmentManager);
        when(fragmentManager.getFragments()).thenReturn(ImmutableList.of(fragment));
        Intent intent = new Intent();
        mActivity.onNewIntent(intent);
        verify(fragment).handleDeviceConnectedFromIntent(intent);
    }
}
