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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudio;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.BluetoothVolumeControl;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Looper;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowThreadUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.flags.Flags;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
            ShadowBluetoothAdapter.class,
            ShadowBluetoothUtils.class,
            ShadowThreadUtils.class,
        })
public class AudioSharingDeviceVolumeGroupControllerTest {
    private static final String TEST_DEVICE_NAME1 = "test1";
    private static final String TEST_DEVICE_NAME2 = "test2";
    private static final int TEST_DEVICE_GROUP_ID1 = 1;
    private static final int TEST_DEVICE_GROUP_ID2 = 2;
    private static final int TEST_VOLUME_VALUE = 10;
    private static final int TEST_INVALID_VOLUME_VALUE = -1;
    private static final int TEST_MAX_VOLUME_VALUE = 100;
    private static final int TEST_MIN_VOLUME_VALUE = 0;
    private static final String PREF_KEY = "audio_sharing_device_volume_group";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private DevicePreferenceCallback mDevicePreferenceCallback;
    @Mock private CachedBluetoothDevice mCachedDevice1;
    @Mock private CachedBluetoothDevice mCachedDevice2;
    @Mock private BluetoothDevice mDevice1;
    @Mock private BluetoothDevice mDevice2;
    @Mock private LocalBluetoothManager mLocalBtManager;
    @Mock private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock private LocalBluetoothProfileManager mProfileManager;
    @Mock private LeAudioProfile mLeAudio;
    @Mock private LocalBluetoothLeBroadcast mBroadcast;
    @Mock private LocalBluetoothLeBroadcastAssistant mAssistant;
    @Mock private BluetoothLeBroadcastReceiveState mState;
    @Mock private BluetoothLeBroadcastMetadata mSource;
    @Mock private AudioSharingDeviceVolumeControlUpdater mDeviceUpdater;
    @Mock private VolumeControlProfile mVolumeControl;
    @Mock private PreferenceScreen mScreen;
    @Mock private AudioSharingDeviceVolumePreference mPreference1;
    @Mock private AudioSharingDeviceVolumePreference mPreference2;
    @Mock private AudioSharingDeviceVolumeSliderPreference mSliderPreference1;
    @Mock private AudioSharingDeviceVolumeSliderPreference mSliderPreference2;
    @Mock private AudioManager mAudioManager;
    @Mock private PreferenceManager mPreferenceManager;
    @Mock private ContentResolver mContentResolver;
    @Spy private ContentObserver mContentObserver;

    private Context mContext;
    private AudioSharingDeviceVolumeGroupController mController;
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private Lifecycle mLifecycle;
    private LifecycleOwner mLifecycleOwner;
    private PreferenceCategory mPreferenceGroup;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBtManager;
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        when(mLocalBtManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBtManager.getProfileManager()).thenReturn(mProfileManager);
        when(mProfileManager.getLeAudioProfile()).thenReturn(mLeAudio);
        when(mProfileManager.getLeAudioBroadcastProfile()).thenReturn(mBroadcast);
        when(mProfileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(mAssistant);
        when(mProfileManager.getVolumeControlProfile()).thenReturn(mVolumeControl);
        when(mLeAudio.isProfileReady()).thenReturn(true);
        when(mBroadcast.isProfileReady()).thenReturn(true);
        when(mAssistant.isProfileReady()).thenReturn(true);
        when(mVolumeControl.isProfileReady()).thenReturn(true);
        when(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
                .thenReturn(TEST_MAX_VOLUME_VALUE);
        when(mAudioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC))
                .thenReturn(TEST_MIN_VOLUME_VALUE);
        when(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
                .thenReturn(TEST_VOLUME_VALUE);
        when(mContext.getSystemService(AudioManager.class)).thenReturn(mAudioManager);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        doReturn(TEST_DEVICE_NAME1).when(mCachedDevice1).getName();
        doReturn(TEST_DEVICE_GROUP_ID1).when(mCachedDevice1).getGroupId();
        doReturn(mDevice1).when(mCachedDevice1).getDevice();
        doReturn(ImmutableSet.of()).when(mCachedDevice1).getMemberDevice();
        when(mCachedDeviceManager.findDevice(mDevice1)).thenReturn(mCachedDevice1);
        when(mPreference1.getCachedDevice()).thenReturn(mCachedDevice1);
        when(mSliderPreference1.getCachedDevice()).thenReturn(mCachedDevice1);
        doReturn(TEST_DEVICE_NAME2).when(mCachedDevice2).getName();
        doReturn(TEST_DEVICE_GROUP_ID2).when(mCachedDevice2).getGroupId();
        doReturn(mDevice2).when(mCachedDevice2).getDevice();
        doReturn(ImmutableSet.of()).when(mCachedDevice2).getMemberDevice();
        when(mPreference2.getCachedDevice()).thenReturn(mCachedDevice2);
        when(mSliderPreference2.getCachedDevice()).thenReturn(mCachedDevice2);
        doNothing().when(mDevicePreferenceCallback).onDeviceAdded(any(Preference.class));
        doNothing().when(mDevicePreferenceCallback).onDeviceRemoved(any(Preference.class));
        when(mScreen.getContext()).thenReturn(mContext);
        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        when(mScreen.findPreference(PREF_KEY)).thenReturn(mPreferenceGroup);
        mController = new AudioSharingDeviceVolumeGroupController(mContext);
        mController.setDeviceUpdater(mDeviceUpdater);
        mContentObserver = mController.getSettingsObserver();
    }

    @After
    public void tearDown() {
        ShadowThreadUtils.reset();
        ShadowBluetoothUtils.reset();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onStart_flagOff_doNothing() {
        mController.onStart(mLifecycleOwner);
        verify(mAssistant, never())
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater, never()).registerCallback();
        verify(mVolumeControl, never())
                .registerCallback(any(Executor.class), any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver, never())
                .registerContentObserver(
                        Settings.Secure.getUriFor(
                                BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                        false,
                        mContentObserver);
        verify(mLeAudio, never()).registerCallback(any(), any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    @DisableFlags(Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2)
    public void onStart_flagOn_registerCallbacksIncludingContentObserver() {
        mController.onStart(mLifecycleOwner);
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater).registerCallback();
        verify(mVolumeControl)
                .registerCallback(any(Executor.class), any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver)
                .registerContentObserver(
                        Settings.Secure.getUriFor(
                                BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                        false,
                        mContentObserver);
        verify(mLeAudio, never()).registerCallback(any(), any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    @DisableFlags(Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2)
    public void onAudioSharingProfilesConnected_flagOn_registerCallbacksIncludingContentObserver() {
        mController.onAudioSharingProfilesConnected();
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater).registerCallback();
        verify(mVolumeControl)
                .registerCallback(any(Executor.class), any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver)
                .registerContentObserver(
                        Settings.Secure.getUriFor(
                                BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                        false,
                        mContentObserver);
        verify(mLeAudio, never()).registerCallback(any(), any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
        Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2
    })
    public void onAudioSharingProfilesConnected_flagOn_registerCallbacksIncludingLeAudioProfile() {
        mController.onAudioSharingProfilesConnected();
        verify(mAssistant)
                .registerServiceCallBack(
                        any(Executor.class), any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater).registerCallback();
        verify(mVolumeControl)
                .registerCallback(any(Executor.class), any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver, never())
                .registerContentObserver(
                        Settings.Secure.getUriFor(
                                BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                        false,
                        mContentObserver);
        verify(mLeAudio).registerCallback(any(), any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onStop_flagOff_doNothing() {
        mController.onStop(mLifecycleOwner);
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater, never()).unregisterCallback();
        verify(mVolumeControl, never())
                .unregisterCallback(any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver, never()).unregisterContentObserver(mContentObserver);
        verify(mLeAudio, never()).unregisterCallback(any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onStop_flagOn_callbacksNotRegistered_doNothing() {
        mController.setCallbacksRegistered(false);
        mController.onStop(mLifecycleOwner);
        verify(mAssistant, never())
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater, never()).unregisterCallback();
        verify(mVolumeControl, never())
                .unregisterCallback(any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver, never()).unregisterContentObserver(mContentObserver);
        verify(mLeAudio, never()).unregisterCallback(any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    @DisableFlags(Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2)
    public void onStop_flagOn_callbacksRegistered_unregisterCallbacksIncludingContentObserver() {
        mController.setCallbacksRegistered(true);
        mController.onStop(mLifecycleOwner);
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater).unregisterCallback();
        verify(mVolumeControl).unregisterCallback(any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver).unregisterContentObserver(mContentObserver);
        verify(mLeAudio, never()).unregisterCallback(any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_ENABLE_LE_AUDIO_SHARING,
        Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2
    })
    public void onStop_flagOn_callbacksRegistered_unregisterCallbacksIncludingLeAudioProfile() {
        mController.setCallbacksRegistered(true);
        mController.onStop(mLifecycleOwner);
        verify(mAssistant)
                .unregisterServiceCallBack(any(BluetoothLeBroadcastAssistant.Callback.class));
        verify(mDeviceUpdater).unregisterCallback();
        verify(mVolumeControl).unregisterCallback(any(BluetoothVolumeControl.Callback.class));
        verify(mContentResolver, never()).unregisterContentObserver(mContentObserver);
        verify(mLeAudio).unregisterCallback(any(BluetoothLeAudio.Callback.class));
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void displayPreference_flagOff_doNothing() {
        mController.displayPreference(mScreen);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
        verify(mDeviceUpdater, never()).forceUpdate();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void displayPreference_flagOn_updateDeviceList() {
        mController.displayPreference(mScreen);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
        verify(mDeviceUpdater).forceUpdate();
    }

    @Test
    public void getPreferenceKey_returnsCorrectKey() {
        assertThat(mController.getPreferenceKey()).isEqualTo(PREF_KEY);
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceAdded_firstDevice_updateVisibility() {
        when(mPreference1.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreferenceGroup).setVisible(true);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2)
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceAdded_adoptApi_rankFallbackDeviceOnTop() {
        LeAudioProfile leAudioProfile = mock(LeAudioProfile.class);
        when(leAudioProfile.getBroadcastToUnicastFallbackGroup()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mProfileManager.getLeAudioProfile()).thenReturn(leAudioProfile);
        when(mPreference1.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        when(mPreference2.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        mController.onDeviceAdded(mPreference2);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference1).setOrder(1);
        verify(mPreference2).setOrder(0);
    }

    @Test
    @DisableFlags({
        Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2,
        com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN
    })
    public void onDeviceAdded_rankFallbackDeviceOnTop() {
        Settings.Secure.putInt(
                mContentResolver,
                BluetoothUtils.getPrimaryGroupIdUriForBroadcast(),
                TEST_DEVICE_GROUP_ID2);
        when(mPreference1.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        when(mPreference2.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        mController.onDeviceAdded(mPreference2);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference1).setOrder(1);
        verify(mPreference2).setOrder(0);
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceAdded_setVolumeFromVolumeControlService() {
        when(mPreference1.getProgress()).thenReturn(TEST_INVALID_VOLUME_VALUE);
        mController.setVolumeMap(ImmutableMap.of(TEST_DEVICE_GROUP_ID1, TEST_VOLUME_VALUE));
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference1).setProgress(eq(TEST_VOLUME_VALUE));
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceAdded_enableSliderPreference_setVolumeFromVolumeControlService() {
        when(mSliderPreference1.getValue()).thenReturn(TEST_INVALID_VOLUME_VALUE);
        mController.setVolumeMap(ImmutableMap.of(TEST_DEVICE_GROUP_ID1, TEST_VOLUME_VALUE));
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mSliderPreference1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mSliderPreference1).setValue(eq(TEST_VOLUME_VALUE));
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceAdded_setVolumeFromAudioManager() {
        when(mPreference1.getProgress()).thenReturn(TEST_INVALID_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference1).setProgress(eq(26));
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceAdded_enableSliderPreference_setVolumeFromAudioManager() {
        when(mSliderPreference1.getValue()).thenReturn(TEST_INVALID_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mSliderPreference1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mSliderPreference1).setValue(eq(26));
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceRemoved_notLastDevice_isVisible() {
        mPreferenceGroup.addPreference(mPreference2);
        mPreferenceGroup.addPreference(mPreference1);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceRemoved(mPreference1);
        verify(mPreferenceGroup, never()).setVisible(false);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceRemoved_lastDevice_updateVisibility() {
        mPreferenceGroup.addPreference(mPreference1);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceRemoved(mPreference1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreferenceGroup).setVisible(false);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceRemoved_enableExpressiveDesign_notLastDevice_isVisible() {
        mPreferenceGroup.addPreference(mSliderPreference2);
        mPreferenceGroup.addPreference(mSliderPreference1);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceRemoved(mSliderPreference1);
        verify(mPreferenceGroup, never()).setVisible(false);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceRemoved_enableExpressiveDesign_lastDevice_updateVisibility() {
        mPreferenceGroup.addPreference(mSliderPreference1);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceRemoved(mSliderPreference1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreferenceGroup).setVisible(false);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void updateVisibility_emptyPreferenceGroup_doNothing() {
        mController.setCallbacksRegistered(true);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreferenceGroup, never()).setVisible(anyBoolean());
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void updateVisibility_flagOff_setVisibleToFalse() {
        mController.setCallbacksRegistered(true);
        mPreferenceGroup.addPreference(mPreference1);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.getPreferenceCount() > 0).isTrue();
        verify(mPreferenceGroup).setVisible(false);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void updateVisibility_notEmptyPreferenceGroup_noSharing_setVisibleToFalse() {
        mController.setCallbacksRegistered(true);
        mPreferenceGroup.addPreference(mPreference1);
        when(mBroadcast.isEnabled(null)).thenReturn(false);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.getPreferenceCount() > 0).isTrue();
        verify(mPreferenceGroup).setVisible(false);
        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void updateVisibility_notEmptyPreferenceGroup_isSharing_setVisibleToTrue() {
        mController.setCallbacksRegistered(true);
        mPreferenceGroup.addPreference(mPreference1);
        when(mBroadcast.isEnabled(null)).thenReturn(true);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.updateVisibility();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(mPreferenceGroup.getPreferenceCount() > 0).isTrue();
        verify(mPreferenceGroup).setVisible(true);
        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2)
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onBroadcastToUnicastFallbackGroupChanged_adoptApi_updatePreferenceOrder() {
        when(mLeAudio.getBroadcastToUnicastFallbackGroup()).thenReturn(TEST_DEVICE_GROUP_ID2);
        when(mPreference1.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        when(mPreference2.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        mController.onDeviceAdded(mPreference2);
        shadowOf(Looper.getMainLooper()).idle();

        when(mLeAudio.getBroadcastToUnicastFallbackGroup()).thenReturn(TEST_DEVICE_GROUP_ID1);
        mController.mLeAudioCallback.onBroadcastToUnicastFallbackGroupChanged(
                TEST_DEVICE_GROUP_ID1);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference1).setOrder(0);
        verify(mPreference2).setOrder(1);
    }

    @Test
    @DisableFlags({
        Flags.FLAG_ADOPT_PRIMARY_GROUP_MANAGEMENT_API_V2,
        com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN
    })
    public void settingsObserverOnChange_updatePreferenceOrder() {
        Settings.Secure.putInt(
                mContentResolver,
                BluetoothUtils.getPrimaryGroupIdUriForBroadcast(),
                TEST_DEVICE_GROUP_ID2);
        when(mPreference1.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        when(mPreference2.getProgress()).thenReturn(TEST_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        mController.onDeviceAdded(mPreference2);
        shadowOf(Looper.getMainLooper()).idle();

        Settings.Secure.putInt(
                mContentResolver,
                BluetoothUtils.getPrimaryGroupIdUriForBroadcast(),
                TEST_DEVICE_GROUP_ID1);
        mContentObserver.onChange(true);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference1).setOrder(0);
        verify(mPreference2).setOrder(1);
    }

    @Test
    @DisableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceVolumeChanged_updatePreference() {
        when(mPreference1.getProgress()).thenReturn(TEST_MAX_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mPreference1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);

        mController.mVolumeControlCallback.onDeviceVolumeChanged(mDevice1, TEST_VOLUME_VALUE);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mPreference1).setProgress(TEST_VOLUME_VALUE);
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_ENABLE_BLUETOOTH_SETTINGS_EXPRESSIVE_DESIGN)
    public void onDeviceVolumeChanged_enableSliderPreference_updatePreference() {
        when(mSliderPreference1.getValue()).thenReturn(TEST_MAX_VOLUME_VALUE);
        mController.setPreferenceGroup(mPreferenceGroup);
        mController.onDeviceAdded(mSliderPreference1);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);

        mController.mVolumeControlCallback.onDeviceVolumeChanged(mDevice1, TEST_VOLUME_VALUE);
        shadowOf(Looper.getMainLooper()).idle();

        verify(mSliderPreference1).setValue(TEST_VOLUME_VALUE);
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_updateGroup() {
        // onSourceAdded will update group preference
        mController.mBroadcastAssistantCallback.onSourceAdded(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        verify(mDeviceUpdater).forceUpdate();

        // onSourceRemoved will update group preference
        mController.mBroadcastAssistantCallback.onSourceRemoved(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        verify(mDeviceUpdater, times(2)).forceUpdate();
    }

    @Test
    public void testBluetoothLeBroadcastAssistantCallbacks_doNothing() {
        mController.mBroadcastAssistantCallback.onSearchStarted(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStartFailed(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopped(/* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSearchStopFailed(/* reason= */ 1);
        List<Long> bisSyncState = new ArrayList<>();
        bisSyncState.add(1L);
        when(mState.getBisSyncState()).thenReturn(bisSyncState);
        when(mBroadcast.getLatestBroadcastId()).thenReturn(1);
        when(mState.getBroadcastId()).thenReturn(1);
        mController.mBroadcastAssistantCallback.onReceiveStateChanged(
                mDevice1, /* sourceId= */ 1, mState);
        mController.mBroadcastAssistantCallback.onSourceAddFailed(
                mDevice1, mSource, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceRemoveFailed(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModified(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceModifyFailed(
                mDevice1, /* sourceId= */ 1, /* reason= */ 1);
        mController.mBroadcastAssistantCallback.onSourceFound(mSource);
        mController.mBroadcastAssistantCallback.onSourceLost(/* broadcastId= */ 1);
        shadowOf(Looper.getMainLooper()).idle();

        // Above callbacks won't update group preference
        verify(mDeviceUpdater, never()).forceUpdate();
    }
}
