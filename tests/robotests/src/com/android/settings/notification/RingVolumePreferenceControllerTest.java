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

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Vibrator;
import android.service.notification.NotificationListenerService;
import android.telephony.TelephonyManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class RingVolumePreferenceControllerTest {

    @Mock
    private AudioHelper mHelper;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private AudioManager mAudioManager;
    @Mock
    private Vibrator mVibrator;
    @Mock
    private NotificationManager mNotificationManager;
    @Mock
    private ComponentName mSuppressor;
    @Mock
    private Resources mResources;
    @Mock
    private VolumeSeekBarPreference mPreference;

    private Context mContext;

    private RingVolumePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowContext = ShadowApplication.getInstance();
        shadowContext.setSystemService(Context.TELEPHONY_SERVICE, mTelephonyManager);
        shadowContext.setSystemService(Context.AUDIO_SERVICE, mAudioManager);
        shadowContext.setSystemService(Context.VIBRATOR_SERVICE, mVibrator);
        shadowContext.setSystemService(Context.NOTIFICATION_SERVICE, mNotificationManager);
        mContext = spy(RuntimeEnvironment.application);
        when(mNotificationManager.getEffectsSuppressor()).thenReturn(mSuppressor);
        when(mContext.getResources()).thenReturn(mResources);
        mController = new RingVolumePreferenceController(mContext);
        mController.setAudioHelper(mHelper);
    }

    @Test
    public void isAvailable_singleVolume_shouldReturnFalse() {
        when(mHelper.isSingleVolume()).thenReturn(true);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notVoiceCapable_shouldReturnFalse() {
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_notSingleVolume_VoiceCapable_shouldReturnTrue() {
        when(mHelper.isSingleVolume()).thenReturn(false);
        when(mTelephonyManager.isVoiceCapable()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAudioStream_shouldReturnRing() {
        assertThat(mController.getAudioStream()).isEqualTo(AudioManager.STREAM_RING);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final RingVolumePreferenceController controller =
                new RingVolumePreferenceController(mContext);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }

    // todo: verify that the title change is displayed, by examining the underlying preference
    @Test
    public void ringNotificationStreamsNotAliased_sliderTitleSetToRingOnly() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_alias_ring_notif_stream_types))
                .thenReturn(false);
        final RingVolumePreferenceController controller =
                new RingVolumePreferenceController(mContext);

        int expectedTitleId = R.string.separate_ring_volume_option_title;

        assertThat(controller.mTitleId).isEqualTo(expectedTitleId);
    }

    @Test
    public void ringNotificationStreamsAliased_sliderTitleIncludesBothRingNotification() {

        when(mResources.getBoolean(
                com.android.internal.R.bool.config_alias_ring_notif_stream_types)).thenReturn(true);
        final RingVolumePreferenceController control = new RingVolumePreferenceController(mContext);

        int expectedTitleId = R.string.ring_volume_option_title;

        assertThat(control.mTitleId).isEqualTo(expectedTitleId);
    }

    @Test
    public void setHintsRing_aliased_Matches() {
        assertThat(mController.hintsMatch(
                NotificationListenerService.HINT_HOST_DISABLE_CALL_EFFECTS, true)).isTrue();
    }

    @Test
    public void setHintsRingNotification_aliased_Matches() {
        assertThat(mController.hintsMatch(NotificationListenerService.HINT_HOST_DISABLE_EFFECTS,
                true)).isTrue();
    }

    @Test
    public void setHintNotification_aliased_Matches() {
        assertThat(mController
                .hintsMatch(NotificationListenerService.HINT_HOST_DISABLE_NOTIFICATION_EFFECTS,
                true)).isTrue();
    }

    @Test
    public void setHintsRing_unaliased_Matches() {
        assertThat(mController.hintsMatch(
                NotificationListenerService.HINT_HOST_DISABLE_CALL_EFFECTS, false)).isTrue();
    }

    @Test
    public void setHintsRingNotification_unaliased_Matches() {
        assertThat(mController.hintsMatch(NotificationListenerService.HINT_HOST_DISABLE_EFFECTS,
                false)).isTrue();
    }

    @Test
    public void setHintNotification_unaliased_doesNotMatch() {
        assertThat(mController
                .hintsMatch(NotificationListenerService.HINT_HOST_DISABLE_NOTIFICATION_EFFECTS,
                        false)).isFalse();
    }

    @Test
    public void setRingerModeToVibrate_butNoVibratorAvailable_iconIsSilent() {
        when(mHelper.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_VIBRATE);

        mController.setPreference(mPreference);
        mController.setVibrator(null);
        mController.updateRingerMode();

        assertThat(mController.getMuteIcon()).isEqualTo(mController.mSilentIconId);
    }

    @Test
    public void setRingerModeToVibrate_VibratorAvailable_iconIsVibrate() {
        when(mHelper.getRingerModeInternal()).thenReturn(AudioManager.RINGER_MODE_VIBRATE);
        when(mVibrator.hasVibrator()).thenReturn(true);

        mController.setPreference(mPreference);
        mController.setVibrator(mVibrator);
        mController.updateRingerMode();

        assertThat(mController.getMuteIcon()).isEqualTo(mController.mVibrateIconId);
    }

}
