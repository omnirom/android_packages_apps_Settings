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

import static com.android.settingslib.flags.Flags.FLAG_ENABLE_BLUETOOTH_DIAGNOSIS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.util.Pair;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.flags.Flags;
import com.android.settingslib.testutils.shadow.ShadowBluetoothAdapter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowAlertDialogCompat.class,
        com.android.settings.testutils.shadow.ShadowBluetoothUtils.class})
public class BluetoothDevicePreferenceTest {
    private static final boolean SHOW_DEVICES_WITHOUT_NAMES = true;
    private static final String TEST_MAC_ADDRESS = "04:52:C7:0B:D8:3C";
    private static final String TEST_MAC_ADDRESS_1 = "05:52:C7:0B:D8:3C";
    private static final String TEST_MAC_ADDRESS_2 = "06:52:C7:0B:D8:3C";
    private static final String TEST_MAC_ADDRESS_3 = "07:52:C7:0B:D8:3C";
    private static final Comparator<BluetoothDevicePreference> COMPARATOR =
            Comparator.naturalOrder();
    private static final String FAKE_DESCRIPTION = "fake_description";
    private static final String BLUETOOTH_DIAGNOSIS_KEY = "cs_bt_diagnostics_enabled";
    private static final int TEST_DEVICE_GROUP_ID = 1;

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private CachedBluetoothDevice mCachedDevice1;
    @Mock
    private CachedBluetoothDevice mCachedDevice2;
    @Mock
    private CachedBluetoothDevice mCachedDevice3;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice1;
    @Mock
    private BluetoothDevice mBluetoothDevice2;
    @Mock
    private BluetoothDevice mBluetoothDevice3;
    @Mock
    private Drawable mDrawable;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private CachedBluetoothDeviceManager mDeviceManager;

    private Context mContext = ApplicationProvider.getApplicationContext();
    private ShadowBluetoothAdapter mShadowBluetoothAdapter;
    private FakeFeatureFactory mFakeFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    private BluetoothDevicePreference mPreference;
    private List<BluetoothDevicePreference> mPreferenceList = new ArrayList<>();

    @Before
    public void setUp() {
        mContext.setTheme(R.style.Theme_Settings);
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFakeFeatureFactory.getMetricsFeatureProvider();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mDeviceManager);
        prepareCachedBluetoothDevice(mCachedBluetoothDevice, TEST_MAC_ADDRESS,
                new Pair<>(mDrawable, FAKE_DESCRIPTION), TEST_DEVICE_GROUP_ID, mBluetoothDevice);
        prepareCachedBluetoothDevice(mCachedDevice1, TEST_MAC_ADDRESS_1,
                new Pair<>(mDrawable, FAKE_DESCRIPTION), TEST_DEVICE_GROUP_ID, mBluetoothDevice1);
        prepareCachedBluetoothDevice(mCachedDevice2, TEST_MAC_ADDRESS_2,
                new Pair<>(mDrawable, FAKE_DESCRIPTION), TEST_DEVICE_GROUP_ID, mBluetoothDevice2);
        prepareCachedBluetoothDevice(mCachedDevice3, TEST_MAC_ADDRESS_3,
                new Pair<>(mDrawable, FAKE_DESCRIPTION), TEST_DEVICE_GROUP_ID, mBluetoothDevice3);
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedBluetoothDevice));

        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                SHOW_DEVICES_WITHOUT_NAMES, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        mPreference.mBluetoothAdapter = mBluetoothAdapter;
    }

    @Test
    public void onClicked_deviceConnected_shouldLogBluetoothDisconnectEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(true);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider)
                .action(mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_DISCONNECT);
    }

    @Test
    public void onClicked_deviceBonded_shouldLogBluetoothConnectEvent() {
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);

        mPreference.onClicked();

        verify(mMetricsFeatureProvider)
                .action(mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_CONNECT);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LE_AUDIO_SHARING)
    public void onClicked_deviceNotBonded_blockPairing() {
        mShadowBluetoothAdapter = Shadow.extract(BluetoothAdapter.getDefaultAdapter());
        mShadowBluetoothAdapter.setEnabled(true);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastSourceSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        mShadowBluetoothAdapter.setIsLeAudioBroadcastAssistantSupported(
                BluetoothStatusCodes.FEATURE_SUPPORTED);
        LocalBluetoothProfileManager profileManager = mock(LocalBluetoothProfileManager.class);
        LocalBluetoothLeBroadcast broadcast = mock(LocalBluetoothLeBroadcast.class);
        LocalBluetoothLeBroadcastAssistant assistant = mock(
                LocalBluetoothLeBroadcastAssistant.class);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(profileManager);
        when(profileManager.getLeAudioBroadcastProfile()).thenReturn(broadcast);
        when(profileManager.getLeAudioBroadcastAssistantProfile()).thenReturn(assistant);
        when(broadcast.isEnabled(null)).thenReturn(true);
        when(broadcast.getLatestBroadcastId()).thenReturn(1);
        BluetoothDevice device1 = mock(BluetoothDevice.class);
        BluetoothDevice device2 = mock(BluetoothDevice.class);
        CachedBluetoothDevice cachedDevice1 = mock(CachedBluetoothDevice.class);
        CachedBluetoothDevice cachedDevice2 = mock(CachedBluetoothDevice.class);
        when(cachedDevice1.getDevice()).thenReturn(device1);
        when(cachedDevice2.getDevice()).thenReturn(device2);
        when(cachedDevice1.getGroupId()).thenReturn(1);
        when(cachedDevice2.getGroupId()).thenReturn(2);
        when(mDeviceManager.findDevice(device1)).thenReturn(cachedDevice1);
        when(mDeviceManager.findDevice(device2)).thenReturn(cachedDevice2);
        when(assistant.getAllConnectedDevices()).thenReturn(ImmutableList.of(device1, device2));
        BluetoothLeBroadcastReceiveState state = mock(BluetoothLeBroadcastReceiveState.class);
        when(state.getBroadcastId()).thenReturn(1);
        when(assistant.getAllSources(any())).thenReturn(ImmutableList.of(state));
        when(mCachedBluetoothDevice.isConnected()).thenReturn(false);
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);
        when(mCachedBluetoothDevice.startPairing()).thenReturn(true);
        when(mCachedBluetoothDevice.hasHumanReadableName()).thenReturn(true);

        mPreference.onClicked();
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        AlertDialog dialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        assertThat(dialog).isNotNull();

        ShadowAlertDialogCompat shadowAlertDialog = ShadowAlertDialogCompat.shadowOf(dialog);
        assertThat(shadowAlertDialog.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.audio_sharing_block_pairing_dialog_title));

        verify(mMetricsFeatureProvider, never())
                .action(mContext, MetricsEvent.ACTION_SETTINGS_BLUETOOTH_PAIR);
        verify(mCachedBluetoothDevice, never()).startPairing();
        verify(mMetricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_SETTINGS_BLUETOOTH_PAIR_IN_AUDIO_SHARING);
        verify(mMetricsFeatureProvider)
                .action(mContext,
                        SettingsEnums.ACTION_SETTINGS_BLUETOOTH_PAIR_BLOCKED_IN_AUDIO_SHARING);
    }

    @Test
    public void getSecondTargetResource_shouldBeGearIconLayout() {
        assertThat(mPreference.getSecondTargetResId()).isEqualTo(R.layout.preference_widget_gear);
    }

    @Test
    public void shouldHideSecondTarget_noDevice_shouldReturnTrue() {
        ReflectionHelpers.setField(mPreference, "mCachedDevice", null);

        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_notBond_shouldReturnTrue() {
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_NONE);

        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_hasUserRestriction_shouldReturnTrue() {
        final UserManager um = mock(UserManager.class);
        ReflectionHelpers.setField(mPreference, "mUserManager", um);
        when(um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(true);

        assertThat(mPreference.shouldHideSecondTarget()).isTrue();
    }

    @Test
    public void shouldHideSecondTarget_hasBoundDeviceAndNoRestriction_shouldReturnFalse() {
        when(mCachedBluetoothDevice.getBondState()).thenReturn(BluetoothDevice.BOND_BONDED);
        final UserManager um = mock(UserManager.class);
        ReflectionHelpers.setField(mPreference, "mUserManager", um);
        when(um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH)).thenReturn(false);

        assertThat(mPreference.shouldHideSecondTarget()).isFalse();
    }

    @Test
    public void isVisible_showDeviceWithoutNames_visible() {
        doReturn(false).when(mCachedBluetoothDevice).hasHumanReadableName();
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        SHOW_DEVICES_WITHOUT_NAMES,
                        BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        assertThat(preference.isVisible()).isTrue();
    }

    @Test
    public void isVisible_hideDeviceWithoutNames_invisible() {
        doReturn(false).when(mCachedBluetoothDevice).hasHumanReadableName();
        BluetoothDevicePreference preference =
                new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                        false, BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        assertThat(preference.isVisible()).isFalse();
    }

    @Test
    public void setNeedNotifyHierarchyChanged_updateValue() {
        mPreference.setNeedNotifyHierarchyChanged(true);

        assertThat(mPreference.mNeedNotifyHierarchyChanged).isTrue();
    }

    @Test
    public void compareTo_sortTypeFIFO() {
        final BluetoothDevicePreference preference3 = new BluetoothDevicePreference(mContext,
                mCachedDevice3, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_FIFO);
        final BluetoothDevicePreference preference2 = new BluetoothDevicePreference(mContext,
                mCachedDevice2, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_FIFO);
        final BluetoothDevicePreference preference1 = new BluetoothDevicePreference(mContext,
                mCachedDevice1, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_FIFO);

        mPreferenceList.add(preference1);
        mPreferenceList.add(preference2);
        mPreferenceList.add(preference3);
        Collections.sort(mPreferenceList, COMPARATOR);

        assertThat(mPreferenceList.get(0).getCachedDevice().getAddress())
                .isEqualTo(preference3.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(1).getCachedDevice().getAddress())
                .isEqualTo(preference2.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(2).getCachedDevice().getAddress())
                .isEqualTo(preference1.getCachedDevice().getAddress());
    }

    @Test
    public void compareTo_sortTypeDefault() {
        final BluetoothDevicePreference preference3 = new BluetoothDevicePreference(mContext,
                mCachedDevice3, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        final BluetoothDevicePreference preference2 = new BluetoothDevicePreference(mContext,
                mCachedDevice2, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        final BluetoothDevicePreference preference1 = new BluetoothDevicePreference(mContext,
                mCachedDevice1, SHOW_DEVICES_WITHOUT_NAMES,
                BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        mPreferenceList.add(preference1);
        mPreferenceList.add(preference2);
        mPreferenceList.add(preference3);
        Collections.sort(mPreferenceList, COMPARATOR);

        assertThat(mPreferenceList.get(0).getCachedDevice().getAddress())
                .isEqualTo(preference1.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(1).getCachedDevice().getAddress())
                .isEqualTo(preference2.getCachedDevice().getAddress());
        assertThat(mPreferenceList.get(2).getCachedDevice().getAddress())
                .isEqualTo(preference3.getCachedDevice().getAddress());
    }

    @Test
    public void onAttached_callbackNotRemoved_doNotRegisterCallback() {
        mPreference.onAttached();
        // After the onAttached(), the callback is registered.

        // If it goes to the onAttached() again, then it do not register again, since the
        // callback is not removed.
        mPreference.onAttached();

        verify(mCachedBluetoothDevice, times(1)).registerCallback(eq(mContext.getMainExecutor()),
                any());
        verify(mBluetoothAdapter, times(1)).addOnMetadataChangedListener(any(), any(), any());
    }

    @Test
    public void onAttached_callbackRemoved_registerCallback() {
        mPreference.onAttached();

        mPreference.onPrepareForRemoval();
        mPreference.onAttached();

        verify(mCachedBluetoothDevice, times(1)).unregisterCallback(any());
        verify(mCachedBluetoothDevice, times(2)).registerCallback(eq(mContext.getMainExecutor()),
                any());
        verify(mBluetoothAdapter, times(2)).addOnMetadataChangedListener(any(), any(), any());
    }

    @Test
    public void onDeviceAttributesChanged_updatePreference() {
        when(mCachedBluetoothDevice.getName()).thenReturn("Name");
        mPreference.onAttached();
        final String updatedName = "updatedName";
        when(mCachedBluetoothDevice.getName()).thenReturn(updatedName);

        getCachedBluetoothDeviceCallback().onDeviceAttributesChanged();

        assertThat(mPreference.getTitle().toString()).isEqualTo(updatedName);
    }

    @Test
    public void onAttached_memberDevicesAdded_registerAllCallback() {
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(
                ImmutableSet.of(mCachedDevice1, mCachedDevice2, mCachedDevice3));
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedBluetoothDevice, mCachedDevice1, mCachedDevice2,
                        mCachedDevice3));
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                SHOW_DEVICES_WITHOUT_NAMES, BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        mPreference.onAttached();

        verify(mCachedBluetoothDevice).registerCallback(eq(mContext.getMainExecutor()), any());
        verify(mCachedDevice1).registerCallback(eq(mContext.getMainExecutor()), any());
        verify(mCachedDevice2).registerCallback(eq(mContext.getMainExecutor()), any());
        verify(mCachedDevice3).registerCallback(eq(mContext.getMainExecutor()), any());
    }

    @Test
    public void onDetached_memberDevicesAdded_unregisterAllCallback() {
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(
                ImmutableSet.of(mCachedDevice1, mCachedDevice2, mCachedDevice3));
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedBluetoothDevice, mCachedDevice1, mCachedDevice2,
                        mCachedDevice3));
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                SHOW_DEVICES_WITHOUT_NAMES, BluetoothDevicePreference.SortType.TYPE_DEFAULT);

        mPreference.onAttached();
        mPreference.onDetached();

        verify(mCachedBluetoothDevice).unregisterCallback(any());
        verify(mCachedDevice1).unregisterCallback(any());
        verify(mCachedDevice2).unregisterCallback(any());
        verify(mCachedDevice3).unregisterCallback(any());
    }

    @Test
    public void onDeviceAttributesChanged_memberDevicesChanged_registerOnlyExistDeviceCallback() {
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(
                ImmutableSet.of(mCachedDevice1, mCachedDevice2, mCachedDevice3));
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedBluetoothDevice, mCachedDevice1, mCachedDevice2,
                        mCachedDevice3));
        mPreference = new BluetoothDevicePreference(mContext, mCachedBluetoothDevice,
                SHOW_DEVICES_WITHOUT_NAMES, BluetoothDevicePreference.SortType.TYPE_DEFAULT);
        mPreference.onAttached();
        when(mCachedBluetoothDevice.getMemberDevice()).thenReturn(
                ImmutableSet.of(mCachedDevice1, mCachedDevice2));
        when(mDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedBluetoothDevice, mCachedDevice1, mCachedDevice2));

        getCachedBluetoothDeviceCallback().onDeviceAttributesChanged();

        verify(mCachedBluetoothDevice, times(2)).registerCallback(eq(mContext.getMainExecutor()),
                any());
        verify(mCachedDevice1, times(2)).registerCallback(eq(mContext.getMainExecutor()), any());
        verify(mCachedDevice2, times(2)).registerCallback(eq(mContext.getMainExecutor()), any());
        verify(mCachedDevice3, times(1)).registerCallback(eq(mContext.getMainExecutor()), any());
    }

    @Test
    @EnableFlags(FLAG_ENABLE_BLUETOOTH_DIAGNOSIS)
    public void onPreferenceAttributesChanged_pairingFailure_summaryCanNotPair() {
        Settings.Secure.putInt(mContext.getContentResolver(), BLUETOOTH_DIAGNOSIS_KEY, 1);
        when(mCachedBluetoothDevice.getBondFailureTimeMillis()).thenReturn(10000L);
        SystemClock.setCurrentTimeMillis(20000L);

        mPreference.onPreferenceAttributesChanged();

        assertThat(mPreference.getSummary().toString())
                .isEqualTo(
                        mContext.getString(
                                com.android.settingslib.R.string.bluetooth_pairing_failure));
    }

    @Test
    @EnableFlags(FLAG_ENABLE_BLUETOOTH_DIAGNOSIS)
    public void onPreferenceAttributesChanged_connectionFailure_summaryCanNotConnect() {
        Settings.Secure.putInt(mContext.getContentResolver(), BLUETOOTH_DIAGNOSIS_KEY, 1);
        when(mCachedBluetoothDevice.getConnectionFailureTimeMillis()).thenReturn(10000L);
        SystemClock.setCurrentTimeMillis(20000L);

        mPreference.onPreferenceAttributesChanged();

        assertThat(mPreference.getSummary().toString())
                .isEqualTo(
                        mContext.getString(
                                com.android.settingslib.R.string.bluetooth_connection_failure));
    }

    private void prepareCachedBluetoothDevice(CachedBluetoothDevice cachedDevice, String address,
            Pair<Drawable, String> drawableWithDescription, int groupId,
            BluetoothDevice bluetoothDevice) {
        when(cachedDevice.getAddress()).thenReturn(address);
        when(cachedDevice.getDrawableWithDescription()).thenReturn(drawableWithDescription);
        when(cachedDevice.getGroupId()).thenReturn(groupId);
        when(cachedDevice.getDevice()).thenReturn(bluetoothDevice);
    }

    private CachedBluetoothDevice.Callback getCachedBluetoothDeviceCallback() {
        ArgumentCaptor<CachedBluetoothDevice.Callback> callbackCaptor = ArgumentCaptor.forClass(
                CachedBluetoothDevice.Callback.class);
        verify(mCachedBluetoothDevice).registerCallback(eq(mContext.getMainExecutor()),
                callbackCaptor.capture());

        return callbackCaptor.getValue();
    }
}
