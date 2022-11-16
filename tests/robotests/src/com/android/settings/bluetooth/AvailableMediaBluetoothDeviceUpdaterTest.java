/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.bluetooth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.util.Pair;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowCachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Collection;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAudioManager.class, ShadowBluetoothAdapter.class,
        ShadowCachedBluetoothDeviceManager.class})
public class AvailableMediaBluetoothDeviceUpdaterTest {
    private static final String MAC_ADDRESS = "04:52:C7:0B:D8:3C";

    @Mock
    private DashboardFragment mDashboardFragment;
    @Mock
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private Drawable mDrawable;

    private Context mContext;
    private AvailableMediaBluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private Collection<CachedBluetoothDevice> mCachedDevices;
    private AudioManager mAudioManager;
    private BluetoothDevicePreference mPreference;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private ShadowCachedBluetoothDeviceManager mShadowCachedBluetoothDeviceManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mAudioManager = mContext.getSystemService(AudioManager.class);
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowCachedBluetoothDeviceManager = Shadow.extract(
                Utils.getLocalBtManager(mContext).getCachedDeviceManager());
        mCachedDevices = new ArrayList<>();
        mCachedDevices.add(mCachedBluetoothDevice);
        mShadowCachedBluetoothDeviceManager.setCachedDevicesCopy(mCachedDevices);
        Pair<Drawable, String> pairs = new Pair<>(mDrawable, "fake_device");

        doReturn(mContext).when(mDashboardFragment).getContext();
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCachedBluetoothDevice.getDrawableWithDescription()).thenReturn(pairs);

        mBluetoothDeviceUpdater = spy(new AvailableMediaBluetoothDeviceUpdater(mContext,
                mDashboardFragment, mDevicePreferenceCallback));
        mBluetoothDeviceUpdater.setPrefContext(mContext);
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice, false,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        doNothing().when(mBluetoothDeviceUpdater).addPreference(any());
        doNothing().when(mBluetoothDeviceUpdater).removePreference(any());
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_hfpDeviceConnected_notInCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_inCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onAudioModeChanged_a2dpDeviceConnected_notInCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onAudioModeChanged();

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_notInCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_a2dpDeviceConnected_inCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedA2dpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_notInCall_removePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hfpDeviceConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHfpDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hearingAidDeviceConnected_notInCall_addPreference()
    {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_hearingAidDeviceConnected_inCall_addPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater.
                isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedHearingAidDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.HEARING_AID);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leAudioDeviceConnected_notInCall_addsPreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_leAudioDeviceConnected_inCall_addsPreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).addPreference(mCachedBluetoothDevice);
    }

    @Test
    public void
            onProfileConnectionStateChanged_deviceIsNotInList_notInCall_invokesRemovePreference() {
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceIsNotInList_inCall_invokesRemovePreference() {
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);
        when(mBluetoothDeviceUpdater
                .isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedBluetoothDevice.isConnectedLeAudioDevice()).thenReturn(true);
        mCachedDevices.clear();

        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onProfileConnectionStateChanged_deviceDisconnected_removePreference() {
        mBluetoothDeviceUpdater.onProfileConnectionStateChanged(mCachedBluetoothDevice,
                BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.A2DP);

        verify(mBluetoothDeviceUpdater).removePreference(mCachedBluetoothDevice);
    }

    @Test
    public void onClick_Preference_setActive() {
        mBluetoothDeviceUpdater.onPreferenceClick(mPreference);

        verify(mCachedBluetoothDevice).setActive();
    }
}

