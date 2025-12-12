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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AddSourceWaitForResponseFromQrStateTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = spy(ApplicationProvider.getApplicationContext());
    @Mock
    private AudioStreamScanHelper mScanHelper;
    private AddSourceWaitForResponseFromQrState mInstance;

    @Before
    public void setUp() {
        mInstance = new AddSourceWaitForResponseFromQrState();
    }

    @Test
    public void testGetInstance() {
        mInstance = AddSourceWaitForResponseFromQrState.getInstance();
        assertThat(mInstance).isNotNull();
        assertThat(mInstance).isInstanceOf(AudioStreamStateHandler.class);
    }

    @Test
    public void testGetStateEnum() {
        AudioStreamsProgressCategoryController.AudioStreamState stateEnum =
                mInstance.getStateEnum();
        assertThat(stateEnum)
                .isEqualTo(
                        AudioStreamsProgressCategoryController.AudioStreamState
                                .ADD_SOURCE_WAIT_FOR_RESPONSE_FROM_QR);
    }

    @Test
    public void testOnExit_restartScanWithoutFilter() {
        mSetFlagsRule.enableFlags(Flags.FLAG_AUDIO_STREAM_SCAN_WITH_FILTER);
        mInstance.onExit(mScanHelper,
                AudioStreamsProgressCategoryController.AudioStreamState.SYNCED);

        verify(mScanHelper).restartScanningWithoutFilter();
    }
}
