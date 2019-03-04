/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.notification;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.media.RingtoneManager;
import android.telephony.TelephonyManager;

import android.support.v7.preference.PreferenceScreen;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
public class PhoneRingtone2PreferenceControllerTest {

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DefaultRingtonePreference mPreference;

    private Context mContext;
    private PhoneRingtone2PreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
        mContext = RuntimeEnvironment.application;
        mController = new PhoneRingtone2PreferenceController(mContext);
    }

    @Test
    public void displayPreference_shouldSetSlotId() {
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);

        verify(mPreference).setSlotId(1);
    }

    @Test
    public void isAvailable_notVoiceCapable_shouldReturnFalse() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notMultiSimEnabled_shouldReturnFalse() {
        when(mTelephonyManager.isMultiSimEnabled()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_VoiceCapable_and_MultiSimEnabled_shouldReturnTrue() {
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);
        when(mTelephonyManager.isMultiSimEnabled()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getRingtoneType_shouldReturnRingtone() {
        assertThat(mController.getRingtoneType()).isEqualTo(RingtoneManager.TYPE_RINGTONE);
    }
}
