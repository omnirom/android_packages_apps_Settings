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
import static org.mockito.Mockito.verify;

import android.content.Intent;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioSharingJoinHandlerDashboardFragmentTest {
    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();

    private AudioSharingJoinHandlerDashboardFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new AudioSharingJoinHandlerDashboardFragment();
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId())
                .isEqualTo(R.xml.bluetooth_le_audio_sharing_join_handler);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("AudioSharingJoinHandlerFrag");
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(0);
    }

    @Test
    public void handleDeviceConnectedFromIntent() {
        AudioSharingJoinHandlerController controller = mock(
                AudioSharingJoinHandlerController.class);
        mFragment.setController(controller);
        Intent intent = new Intent();
        mFragment.handleDeviceConnectedFromIntent(intent);
        verify(controller).handleDeviceConnectedFromIntent(intent);
    }
}
