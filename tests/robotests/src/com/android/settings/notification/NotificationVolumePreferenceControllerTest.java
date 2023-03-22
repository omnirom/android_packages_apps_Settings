/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.telephony.TelephonyManager;

import com.android.internal.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class NotificationVolumePreferenceControllerTest {

    @Mock
    private AudioHelper mHelper;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private AudioManager mAudioManager;
    @Mock
    private Vibrator mVibrator;
    @Mock
    private Resources mResources;

    private Context mContext;
    private NotificationVolumePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(Context.AUDIO_SERVICE)).thenReturn(mAudioManager);
        when(mContext.getSystemService(Context.VIBRATOR_SERVICE)).thenReturn(mVibrator);
        when(mContext.getResources()).thenReturn(mResources);

        mController = new NotificationVolumePreferenceController(mContext);
        mController.setAudioHelper(mHelper);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_whenNotVisible_shouldReturnFalse() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_singleVolume_shouldReturnFalse() {
        when(mHelper.isSingleVolume()).thenReturn(true);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_voiceCapable_aliasedWithRing_shouldReturnFalse() {
        when(mResources.getBoolean(
                com.android.settings.R.bool.config_show_notification_volume)).thenReturn(true);
        when(mResources.getBoolean(R.bool.config_alias_ring_notif_stream_types)).thenReturn(true);

        NotificationVolumePreferenceController controller =
                new NotificationVolumePreferenceController(mContext);
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(controller.isAvailable()).isFalse();
    }

    /**
     * With the introduction of ring-notification volume separation, voice-capable devices could now
     * display the notification volume slider.
     */
    @Test
    public void isAvailable_voiceCapable_separatedFromRing_shouldReturnTrue() {
        when(mResources.getBoolean(
                com.android.settings.R.bool.config_show_notification_volume)).thenReturn(true);
        when(mResources.getBoolean(R.bool.config_alias_ring_notif_stream_types)).thenReturn(false);

        NotificationVolumePreferenceController controller =
                new NotificationVolumePreferenceController(mContext);

        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(controller.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_notShowNotificationVolume_shouldReturnFalse() {
        when(mResources.getBoolean(
                com.android.settings.R.bool.config_show_notification_volume)).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notSingleVolume_notVoiceCapable_shouldReturnTrue() {
        when(mResources.getBoolean(
                com.android.settings.R.bool.config_show_notification_volume)).thenReturn(true);
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAudioStream_shouldReturnNotification() {
        assertThat(mController.getAudioStream()).isEqualTo(AudioManager.STREAM_NOTIFICATION);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final NotificationVolumePreferenceController controller =
                new NotificationVolumePreferenceController(mContext);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    @Test
    public void setHintsRing_DoesNotMatch() {
        assertThat(mController.hintsMatch(
                NotificationListenerService.HINT_HOST_DISABLE_CALL_EFFECTS)).isFalse();
    }

    @Test
    public void setHintsAll_Matches() {
        assertThat(mController.hintsMatch(NotificationListenerService.HINT_HOST_DISABLE_EFFECTS))
                .isTrue();
    }

    @Test
    public void setHintNotification_Matches() {
        assertThat(mController
                .hintsMatch(NotificationListenerService.HINT_HOST_DISABLE_NOTIFICATION_EFFECTS))
                .isTrue();
    }

}
