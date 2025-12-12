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

package com.android.settings.connecteddevice.audiosharing;

import static com.android.settings.connecteddevice.audiosharing.AudioSharingDeviceVolumeControlUpdater.PREF_KEY_PREFIX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Looper;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.BluetoothDevicePreference;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class AudioSharingDeviceVolumeControlUpdaterTest {
    private static final String TEST_DEVICE_NAME = "test";
    private static final String TEST_DEVICE_ADDRESS = "XX:XX:XX:XX:XX:11";
    private static final String TAG = "AudioSharingVolUpdater";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock private CachedBluetoothDevice mCachedDevice;
    @Mock private BluetoothDevice mDevice;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock private LocalBluetoothProfileManager mLocalBtProfileManager;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private BluetoothLeBroadcastReceiveState mState;

    private Context mContext;
    private AudioSharingDeviceVolumeControlUpdater mDeviceUpdater;
    private Collection<CachedBluetoothDevice> mCachedDevices;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mLocalBtProfileManager);
        when(mLocalBtProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mLocalBtProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        when(mDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        when(mCachedDevice.getName()).thenReturn(TEST_DEVICE_NAME);
        when(mCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getMemberDevice()).thenReturn(ImmutableSet.of());
        mCachedDevices = new ArrayList<>();
        mCachedDevices.add(mCachedDevice);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        doNothing().when(mDevicePreferenceCallback).onDeviceAdded(any(Preference.class));
        doNothing().when(mDevicePreferenceCallback).onDeviceRemoved(any(Preference.class));
        mDeviceUpdater =
                spy(
                        new AudioSharingDeviceVolumeControlUpdater(
                                mContext, mDevicePreferenceCallback, /* metricsCategory= */ 0));
        mDeviceUpdater.setPrefContext(mContext);
    }

    @After
    public void tearDown() {
        ShadowBluetoothUtils.reset();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_leaDeviceConnected_noSharing_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mBroadcast.isEnabled(null)).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void
            onProfileConnectionStateChanged_leaDeviceConnected_noSharing_sliderPref_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mBroadcast.isEnabled(null)).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumeSliderPreference).isTrue();
        assertThat(((AudioSharingDeviceVolumeSliderPreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_leaDeviceConnected_noSource_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mAssistant.getAllSources(mDevice)).thenReturn(ImmutableList.of());
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void
            onProfileConnectionStateChanged_leaDeviceConnected_noSource_sliderPref_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mAssistant.getAllSources(mDevice)).thenReturn(ImmutableList.of());
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumeSliderPreference).isTrue();
        assertThat(((AudioSharingDeviceVolumeSliderPreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_deviceIsNotInList_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        mCachedDevices.clear();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_deviceIsNotInList_sliderPref_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        mCachedDevices.clear();
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(mCachedDevices);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumeSliderPreference).isTrue();
        assertThat(((AudioSharingDeviceVolumeSliderPreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_leaDeviceDisconnected_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_leaDeviceDisconnected_sliderPref_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumeSliderPreference).isTrue();
        assertThat(((AudioSharingDeviceVolumeSliderPreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_leaDeviceDisconnecting_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        assertThat(((AudioSharingDeviceVolumePreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_leaDeviceDisconnecting_sliderPref_removesPref() {
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(false);
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);

        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumeSliderPreference).isTrue();
        assertThat(((AudioSharingDeviceVolumeSliderPreference) captor.getValue()).getCachedDevice())
                .isEqualTo(mCachedDevice);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_leaDeviceConnected_hasSource_addsPreference() {
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        AudioSharingDeviceVolumePreference preference =
                (AudioSharingDeviceVolumePreference) captor.getValue();
        assertThat(preference.getCachedDevice()).isEqualTo(mCachedDevice);
        assertThat(preference.getTitle().toString()).isEqualTo(TEST_DEVICE_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void
            onProfileConnectionStateChanged_leaDeviceConnected_hasSource_slider_addsPreference() {
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        setupPreferenceMapWithDevice();
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumeSliderPreference).isTrue();
        AudioSharingDeviceVolumeSliderPreference preference =
                (AudioSharingDeviceVolumeSliderPreference) captor.getValue();
        assertThat(preference.getCachedDevice()).isEqualTo(mCachedDevice);
        assertThat(preference.getTitle().toString()).isEqualTo(TEST_DEVICE_NAME);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_hasLeaMemberConnected_hasSource_addsPref() {
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        setupPreferenceMapWithDevice();
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(false);
        when(mCachedDevice.hasConnectedLeAudioMemberDevice()).thenReturn(true);
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        AudioSharingDeviceVolumePreference preference =
                (AudioSharingDeviceVolumePreference) captor.getValue();
        assertThat(preference.getCachedDevice()).isEqualTo(mCachedDevice);
        assertThat(preference.getTitle().toString()).isEqualTo(TEST_DEVICE_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onProfileConnectionStateChanged_hasLeaMemberConnected_hasSource_slider_addsPref() {
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        setupPreferenceMapWithDevice();
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(false);
        when(mCachedDevice.hasConnectedLeAudioMemberDevice()).thenReturn(true);
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        AudioSharingDeviceVolumeSliderPreference preference =
                (AudioSharingDeviceVolumeSliderPreference) captor.getValue();
        assertThat(preference.getCachedDevice()).isEqualTo(mCachedDevice);
        assertThat(preference.getTitle().toString()).isEqualTo(TEST_DEVICE_NAME);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mDeviceUpdater.getLogTag()).isEqualTo(TAG);
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mDeviceUpdater.getPreferenceKeyPrefix()).isEqualTo(PREF_KEY_PREFIX);
    }

    @Test
    public void addPreferenceWithSortType_doNothing() {
        mDeviceUpdater.addPreference(
                mCachedDevice, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        // Verify AudioSharingDeviceVolumeControlUpdater overrides BluetoothDeviceUpdater and won't
        // trigger add preference.
        verifyNoInteractions(mDevicePreferenceCallback);
    }

    @Test
    public void launchDeviceDetails_doNothing() {
        Preference preference = mock(Preference.class);
        mDeviceUpdater.launchDeviceDetails(preference);
        // Verify AudioSharingDeviceVolumeControlUpdater overrides BluetoothDeviceUpdater and won't
        // launch device details
        verifyNoInteractions(preference);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void refreshPreference_havePreference_refresh() {
        setupPreferenceMapWithDevice();
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        AudioSharingDeviceVolumePreference preference =
                (AudioSharingDeviceVolumePreference) captor.getValue();

        when(mCachedDevice.getName()).thenReturn("new");
        mDeviceUpdater.refreshPreference();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(preference.getTitle().toString()).isEqualTo("new");
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void refreshPreference_staledPreference_remove() {
        setupPreferenceMapWithDevice();
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        AudioSharingDeviceVolumePreference preference =
                (AudioSharingDeviceVolumePreference) captor.getValue();

        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(ImmutableList.of());
        mDeviceUpdater.refreshPreference();
        shadowOf(Looper.getMainLooper()).idle();

        verify(mDevicePreferenceCallback).onDeviceRemoved(preference);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void refreshPreference_inconsistentPreference_doNothing() {
        setupPreferenceMapWithDevice();
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        CachedBluetoothDevice subCachedDevice = mock(CachedBluetoothDevice.class);
        BluetoothDevice subDevice = mock(BluetoothDevice.class);
        when(subDevice.getAddress()).thenReturn("XX:XX:XX:XX:XX:22");
        when(subCachedDevice.getDevice()).thenReturn(subDevice);
        when(mCachedDevice.getMemberDevice()).thenReturn(ImmutableSet.of(subCachedDevice));
        when(subCachedDevice.isConnectedLeAudioDevice()).thenReturn(true);
        when(mAssistant.getAllSources(subDevice)).thenReturn(ImmutableList.of(mState));
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        AudioSharingDeviceVolumePreference preference =
                (AudioSharingDeviceVolumePreference) captor.getValue();

        // Main device disconnected, CSIP switches the content of main and member.
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(false);
        when(mCachedDevice.getDevice()).thenReturn(subDevice);
        when(subCachedDevice.getDevice()).thenReturn(mDevice);
        when(mCachedDevice.getMemberDevice()).thenReturn(ImmutableSet.of());
        when(subCachedDevice.getMemberDevice()).thenReturn(ImmutableSet.of(subCachedDevice));
        when(mCachedDeviceManager.getCachedDevicesCopy())
                .thenReturn(ImmutableList.of(subCachedDevice));

        // New main device is added, its preference won't be added to the preference group because
        // it equals to previous preference for the old main device.
        mDeviceUpdater.onDeviceAdded(subCachedDevice);
        shadowOf(Looper.getMainLooper()).idle();

        mDeviceUpdater.refreshPreference();
        shadowOf(Looper.getMainLooper()).idle();

        // The preference should not be removed after refresh the preference group.
        verify(mDevicePreferenceCallback, never()).onDeviceRemoved(preference);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void refreshPreference_staledInconsistentPreference_remove() {
        setupPreferenceMapWithDevice();
        ArgumentCaptor<Preference> captor = ArgumentCaptor.forClass(Preference.class);
        mDeviceUpdater.onProfileConnectionStateChanged(
                mCachedDevice, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.LE_AUDIO);
        shadowOf(Looper.getMainLooper()).idle();
        verify(mDevicePreferenceCallback).onDeviceAdded(captor.capture());
        assertThat(captor.getValue() instanceof AudioSharingDeviceVolumePreference).isTrue();
        AudioSharingDeviceVolumePreference preference =
                (AudioSharingDeviceVolumePreference) captor.getValue();

        BluetoothDevice subDevice = mock(BluetoothDevice.class);
        when(subDevice.getAddress()).thenReturn("XX:XX:XX:XX:XX:22");
        when(mCachedDevice.getDevice()).thenReturn(subDevice);

        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(ImmutableList.of());
        mDeviceUpdater.refreshPreference();

        verify(mDevicePreferenceCallback).onDeviceRemoved(preference);
    }

    private void setupPreferenceMapWithDevice() {
        // Add device to preferenceMap
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        when(mAssistant.getAllSources(mDevice)).thenReturn(ImmutableList.of(mState));
        when(mDeviceUpdater.isDeviceConnected(any(CachedBluetoothDevice.class))).thenReturn(true);
        when(mCachedDevice.isConnectedLeAudioDevice()).thenReturn(true);
    }
}
