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

package com.android.settings.accessibility;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.TopIntroPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link HearingDevicePairingIntroPreferenceController}.
 */
@RunWith(RobolectricTestRunner.class)
public class HearingDevicePairingIntroPreferenceControllerTest {

    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final Resources mResources = mContext.getResources();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private HearingAidHelper mHelper;
    private HearingDevicePairingIntroPreferenceController mController;
    private TopIntroPreference mPreference;

    @Before
    public void setUp() {
        mController = new HearingDevicePairingIntroPreferenceController(mContext, "test_key",
                mHelper);
        mPreference = new TopIntroPreference(mContext);
        mPreference.setKey("test_key");

        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mContext.getResources()).thenReturn(mResources);
    }

    @Test
    public void getAvailabilityStatus_hearingAidSupported_available() {
        when(mHelper.isHearingAidSupported()).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hearingAidNotSupported_unsupportedOnDevice() {
        when(mHelper.isHearingAidSupported()).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void displayPreference_ashaHapSupported_expectedTitle() {
        when(mHelper.isAshaProfileSupported()).thenReturn(true);
        when(mHelper.isHapClientProfileSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_hearing_device_pairing_intro));
    }

    @Test
    public void displayPreference_ashaSupported_expectedTitle() {
        when(mHelper.isAshaProfileSupported()).thenReturn(true);
        when(mHelper.isHapClientProfileSupported()).thenReturn(false);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_hearing_device_pairing_asha_only_intro));
    }

    @Test
    public void displayPreference_hapSupported_expectedTitle() {
        when(mHelper.isAshaProfileSupported()).thenReturn(false);
        when(mHelper.isHapClientProfileSupported()).thenReturn(true);

        mController.displayPreference(mScreen);

        assertThat(mPreference.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_hearing_device_pairing_hap_only_intro));
    }
}
