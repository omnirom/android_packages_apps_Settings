/**
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ShowHdrSdrRatioPreferenceControllerTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TwoStatePreference mPreference;
    @Mock
    private IBinder mSurfaceFlinger;
    private ShowHdrSdrRatioPreferenceController mController;
    private Context mContext;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new ShowHdrSdrRatioPreferenceController(mContext, mSurfaceFlinger, true);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void onPreferenceChange_settingEnabled_shouldChecked() throws RemoteException {
        assertTrue(mController.isAvailable());
        mockSurfaceFlingerTransactResponse(true);
        mController.onPreferenceChange(mPreference, true /* new value */);
        verify(mPreference).setChecked(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void onPreferenceChange_settingDisabled_shouldUnchecked() throws RemoteException {
        assertTrue(mController.isAvailable());
        mockSurfaceFlingerTransactResponse(false);
        mController.onPreferenceChange(mPreference, false /* new value */);
        verify(mPreference).setChecked(false);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void updateState_settingEnabled_shouldChecked() throws RemoteException {
        assertTrue(mController.isAvailable());
        mockSurfaceFlingerTransactResponse(true);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(true);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void updateState_settingDisabled_shouldUnchecked() throws RemoteException {
        assertTrue(mController.isAvailable());
        mockSurfaceFlingerTransactResponse(false);
        mController.updateState(mPreference);
        verify(mPreference).setChecked(false);
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void settingNotAvailable_isHdrSdrRatioAvailableFalse_flagsOff() {
        mController = new ShowHdrSdrRatioPreferenceController(mContext, mSurfaceFlinger, true);
        assertFalse(mController.isAvailable());
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void settingNotAvailable_isHdrSdrRatioAvailableTrue_flagsOn() {
        mController = new ShowHdrSdrRatioPreferenceController(mContext, mSurfaceFlinger, false);
        assertFalse(mController.isAvailable());
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void onDeveloperOptionsSwitchDisabled_preferenceUnchecked_shouldNotTurnOffPreference()
            throws RemoteException {
        assertTrue(mController.isAvailable());
        mockSurfaceFlingerTransactResponse(false);
        when(mPreference.isChecked()).thenReturn(false);
        mController.onDeveloperOptionsSwitchDisabled();

        mController.writeShowHdrSdrRatioSetting(true);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVELOPMENT_HDR_SDR_RATIO)
    public void onDeveloperOptionsSwitchDisabled_preferenceChecked_shouldTurnOffPreference()
            throws RemoteException {
        assertTrue(mController.isAvailable());
        mockSurfaceFlingerTransactResponse(true);
        when(mPreference.isChecked()).thenReturn(true);
        mController.onDeveloperOptionsSwitchDisabled();

        mController.writeShowHdrSdrRatioSetting(false);
        verify(mPreference).setChecked(false);
        verify(mPreference).setEnabled(false);
    }

    private void mockSurfaceFlingerTransactResponse(boolean replyResult) throws RemoteException {
        doAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                // Get the arguments passed to the mocked method
                Object[] args = invocation.getArguments();
                if (args[2] instanceof Parcel reply) {
                    reply.writeBoolean(replyResult);
                    reply.setDataPosition(0);
                }
                return true;
            }
        }).when(mSurfaceFlinger).transact(anyInt(), any(), any(), eq(0 /* flags */));
    }
}

