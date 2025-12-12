/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.media;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaRoute2ProviderService;
import android.media.MediaRouter2Manager;
import android.media.RoutingSessionInfo;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.slices.ShadowSliceBackgroundWorker;
import com.android.settings.testutils.shadow.ShadowAudioManager;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAudioManager.class, ShadowBluetoothAdapter.class,
        ShadowBluetoothUtils.class, ShadowSliceBackgroundWorker.class})
public class MediaDeviceUpdateWorkerTest {

    private static final Uri URI = Uri.parse("content://com.android.settings.slices/test");
    private static final Uri URI1 = Uri.parse("content://com.android.settings.slices/action/"
            + "media_output?media_package_name=com.music1");
    private static final Uri URI2 = Uri.parse("content://com.android.settings.slices/action/"
            + "media_output?media_package_name=com.music2");
    private static final String TEST_DEVICE_PACKAGE_NAME1 = "com.music1";
    private static final String TEST_DEVICE_PACKAGE_NAME2 = "com.music2";

    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private BluetoothEventManager mBluetoothEventManager;

    private MediaDeviceUpdateWorker mMediaDeviceUpdateWorker;
    private ContentResolver mResolver;
    private Context mContext;
    private MediaDevice mMediaDevice1;
    private ShadowApplication mShadowApplication;
    private AudioManager mAudioManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mMediaDeviceUpdateWorker = new MediaDeviceUpdateWorker(mContext, URI);
        mMediaDeviceUpdateWorker.mManager = mock(MediaRouter2Manager.class);
        mResolver = mock(ContentResolver.class);
        mShadowApplication = shadowOf((Application) ApplicationProvider.getApplicationContext());
        mAudioManager = mContext.getSystemService(AudioManager.class);

        doReturn(mResolver).when(mContext).getContentResolver();
    }

    @Test
    public void onDeviceListUpdate_shouldNotifyChange() {
        mMediaDeviceUpdateWorker.onDeviceListUpdate(ImmutableList.of());

        verify(mResolver).notifyChange(URI, null);
    }

    @Test
    public void onSelectedDeviceStateChanged_shouldNotifyChange() {
        mMediaDeviceUpdateWorker.onSelectedDeviceStateChanged(mMediaDevice1,
                LocalMediaManager.MediaDeviceState.STATE_CONNECTED);

        verify(mResolver).notifyChange(URI, null);
    }

    @Test
    public void onDeviceAttributesChanged_shouldNotifyChange() {
        mMediaDeviceUpdateWorker.onDeviceAttributesChanged();

        verify(mResolver).notifyChange(URI, null);
    }

    @Test
    public void onRequestFailed_shouldNotifyChange() {
        mMediaDeviceUpdateWorker.onRequestFailed(MediaRoute2ProviderService.REASON_UNKNOWN_ERROR);

        verify(mResolver).notifyChange(URI, null /* observer */);
    }

    @Test
    public void onReceive_inCallState_shouldNotifyChange() {
        mMediaDeviceUpdateWorker.mLocalMediaManager = mock(LocalMediaManager.class);
        mAudioManager.setMode(AudioManager.MODE_IN_CALL);

        mMediaDeviceUpdateWorker.onSlicePinned();
        final Intent intent = new Intent(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
        for (BroadcastReceiver receiver : mShadowApplication.getReceiversForIntent(intent)) {
            receiver.onReceive(mContext, intent);
        }

        verify(mResolver).notifyChange(URI, null);
    }

    @Test
    public void onReceive_notInCallState_doNothing() {
        mMediaDeviceUpdateWorker.mLocalMediaManager = mock(LocalMediaManager.class);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);

        mMediaDeviceUpdateWorker.onSlicePinned();
        final Intent intent = new Intent(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
        for (BroadcastReceiver receiver : mShadowApplication.getReceiversForIntent(intent)) {
            receiver.onReceive(mContext, intent);
        }

        verify(mResolver, never()).notifyChange(URI, null);
    }

    @Test
    public void getActiveRemoteMediaSession_verifyList() {
        mMediaDeviceUpdateWorker.mLocalMediaManager = mock(LocalMediaManager.class);
        final List<RoutingSessionInfo> routingSessionInfos = new ArrayList<>();
        final RoutingSessionInfo remoteSessionInfo = mock(RoutingSessionInfo.class);
        when(remoteSessionInfo.isSystemSession()).thenReturn(false);
        routingSessionInfos.add(remoteSessionInfo);
        when(mMediaDeviceUpdateWorker.mLocalMediaManager.getRemoteRoutingSessions())
                .thenReturn(routingSessionInfos);

        assertThat(mMediaDeviceUpdateWorker.getActiveRemoteMediaDevices())
                .containsExactly(remoteSessionInfo);
    }

    @Test
    public void onSlicePinned_packageUpdated_checkPackageName() {
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        when(mLocalBluetoothManager.getEventManager()).thenReturn(mBluetoothEventManager);

        mMediaDeviceUpdateWorker = new MediaDeviceUpdateWorker(mContext, URI1);
        mMediaDeviceUpdateWorker.mManager = mock(MediaRouter2Manager.class);
        mMediaDeviceUpdateWorker.mLocalMediaManager = mock(LocalMediaManager.class);
        when(mMediaDeviceUpdateWorker.mLocalMediaManager.getPackageName())
                .thenReturn(TEST_DEVICE_PACKAGE_NAME1);
        mMediaDeviceUpdateWorker.onSlicePinned();

        assertThat(mMediaDeviceUpdateWorker.mLocalMediaManager.getPackageName()).matches(
                TEST_DEVICE_PACKAGE_NAME1);

        mMediaDeviceUpdateWorker = new MediaDeviceUpdateWorker(mContext, URI2);
        mMediaDeviceUpdateWorker.mManager = mock(MediaRouter2Manager.class);
        mMediaDeviceUpdateWorker.mLocalMediaManager = mock(LocalMediaManager.class);
        when(mMediaDeviceUpdateWorker.mLocalMediaManager.getPackageName())
                .thenReturn(TEST_DEVICE_PACKAGE_NAME2);
        mMediaDeviceUpdateWorker.onSlicePinned();

        assertThat(mMediaDeviceUpdateWorker.mLocalMediaManager.getPackageName()).matches(
                TEST_DEVICE_PACKAGE_NAME2);
    }
}
