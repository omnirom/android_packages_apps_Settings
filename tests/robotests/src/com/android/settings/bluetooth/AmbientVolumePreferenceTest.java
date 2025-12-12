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

package com.android.settings.bluetooth;

import static android.bluetooth.AudioInputControl.MUTE_DISABLED;
import static android.bluetooth.AudioInputControl.MUTE_MUTED;
import static android.bluetooth.AudioInputControl.MUTE_NOT_MUTED;

import static com.android.settings.bluetooth.AmbientVolumePreference.ROTATION_COLLAPSED;
import static com.android.settings.bluetooth.AmbientVolumePreference.ROTATION_EXPANDED;
import static com.android.settings.bluetooth.AmbientVolumePreference.SIDE_UNIFIED;
import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME;
import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME_SLIDER;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.bluetooth.hearingdevices.ui.AmbientVolumeUi;
import com.android.settingslib.widget.SliderPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;
import java.util.Set;

/** Tests for {@link AmbientVolumePreference}. */
@RunWith(RobolectricTestRunner.class)
public class AmbientVolumePreferenceTest {

    private static final int TEST_LEFT_VOLUME_LEVEL = 1;
    private static final int TEST_RIGHT_VOLUME_LEVEL = 2;
    private static final int TEST_UNIFIED_VOLUME_LEVEL = 3;
    private static final String KEY_UNIFIED_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_UNIFIED;
    private static final String KEY_LEFT_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_LEFT;
    private static final String KEY_RIGHT_SLIDER = KEY_AMBIENT_VOLUME_SLIDER + "_" + SIDE_RIGHT;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private AmbientVolumeUi.AmbientVolumeUiListener mListener;
    @Mock
    private View mItemView;

    private AmbientVolumePreference mPreference;
    private ImageView mExpandIcon;
    private ImageView mVolumeIcon;

    @Before
    public void setUp() {
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new AmbientVolumePreference(mContext);
        mPreference.setKey(KEY_AMBIENT_VOLUME);
        mPreference.setListener(mListener);
        mPreference.setControlExpandable(true);
        preferenceScreen.addPreference(mPreference);

        mPreference.setupSliders(Set.of(SIDE_LEFT, SIDE_RIGHT));
        mPreference.getSliders().forEach((side, slider) -> {
            slider.setMin(0);
            slider.setMax(4);
            if (side == SIDE_LEFT) {
                slider.setKey(KEY_LEFT_SLIDER);
                slider.setValue(TEST_LEFT_VOLUME_LEVEL);
            } else if (side == SIDE_RIGHT) {
                slider.setKey(KEY_RIGHT_SLIDER);
                slider.setValue(TEST_RIGHT_VOLUME_LEVEL);
            } else {
                slider.setKey(KEY_UNIFIED_SLIDER);
                slider.setValue(TEST_UNIFIED_VOLUME_LEVEL);
            }
        });

        mExpandIcon = new ImageView(mContext);
        mVolumeIcon = new ImageView(mContext);
        mVolumeIcon.setImageResource(com.android.settingslib.R.drawable.ic_ambient_volume);
        mVolumeIcon.setImageLevel(0);
        when(mItemView.requireViewById(R.id.expand_icon)).thenReturn(mExpandIcon);
        when(mItemView.requireViewById(com.android.internal.R.id.icon)).thenReturn(mVolumeIcon);
        when(mItemView.requireViewById(R.id.icon_frame)).thenReturn(mVolumeIcon);

        PreferenceViewHolder preferenceViewHolder = PreferenceViewHolder.createInstanceForTests(
                mItemView);
        mPreference.onBindViewHolder(preferenceViewHolder);
    }

    @Test
    public void setControlExpandable_expandable_expandIconVisible() {
        mPreference.setControlExpandable(true);

        assertThat(mExpandIcon.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void setControlExpandable_notExpandable_expandIconGone() {
        // Change the state from its default (false) to true. This ensures that the subsequent call
        // to setControlExpandable(false) will trigger the update logic.
        mPreference.setControlExpandable(true);
        mPreference.setControlExpandable(false);

        assertThat(mExpandIcon.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void setControlExpanded_expanded_assertControlUiCorrect() {
        mPreference.setControlExpanded(true);

        assertControlUiCorrect();
        int expectedLevel = calculateVolumeLevel(TEST_LEFT_VOLUME_LEVEL, TEST_RIGHT_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void setControlExpanded_notExpanded_assertControlUiCorrect() {
        // Change the state from its default (false) to true. This ensures that the subsequent call
        // to setControlExpanded(false) will trigger the update logic.
        mPreference.setControlExpanded(true);
        mPreference.setControlExpanded(false);

        assertControlUiCorrect();
        int expectedLevel = calculateVolumeLevel(TEST_UNIFIED_VOLUME_LEVEL,
                TEST_UNIFIED_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void setSliderEnabled_expandedAndLeftIsDisabled_volumeIconIcCorrect() {
        mPreference.setControlExpanded(true);
        mPreference.setSliderEnabled(SIDE_LEFT, false);

        int expectedLevel = calculateVolumeLevel(0, TEST_RIGHT_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void setSliderValue_expandedAndLeftValueChanged_volumeIconIsCorrect() {
        mPreference.setControlExpanded(true);
        mPreference.setSliderValue(SIDE_LEFT, 4);

        int expectedLevel = calculateVolumeLevel(4, TEST_RIGHT_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void isMutable_bothSideNotMutable_returnFalse() {
        mPreference.setSliderMuteState(SIDE_LEFT, MUTE_DISABLED);
        mPreference.setSliderMuteState(SIDE_RIGHT, MUTE_DISABLED);

        assertThat(mPreference.isMutable()).isFalse();
    }

    @Test
    public void isMutable_oneSideMutable_returnTrue() {
        mPreference.setSliderMuteState(SIDE_LEFT, MUTE_DISABLED);
        mPreference.setSliderMuteState(SIDE_RIGHT, MUTE_NOT_MUTED);

        assertThat(mPreference.isMutable()).isTrue();
    }

    @Test
    public void isMuted_bothSideMuted_returnTrue() {
        mPreference.setSliderMuteState(SIDE_LEFT, MUTE_MUTED);
        mPreference.setSliderMuteState(SIDE_RIGHT, MUTE_MUTED);

        assertThat(mPreference.isMuted()).isTrue();
    }

    @Test
    public void isMuted_oneSideNotMuted_returnFalse() {
        mPreference.setSliderMuteState(SIDE_LEFT, MUTE_MUTED);
        mPreference.setSliderMuteState(SIDE_RIGHT, MUTE_NOT_MUTED);

        assertThat(mPreference.isMuted()).isFalse();
    }

    @Test
    public void setSliderMuteState_muteLeft_volumeIconIsCorrect() {
        mPreference.setControlExpanded(true);
        mPreference.setSliderMuteState(SIDE_LEFT, MUTE_MUTED);
        mPreference.setSliderMuteState(SIDE_RIGHT, MUTE_NOT_MUTED);

        int expectedLevel = calculateVolumeLevel(0, TEST_RIGHT_VOLUME_LEVEL);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    @Test
    public void setSliderMuteState_muteLeftAndRight_volumeIconIsCorrect() {
        mPreference.setControlExpanded(true);
        mPreference.setSliderMuteState(SIDE_LEFT, MUTE_MUTED);
        mPreference.setSliderMuteState(SIDE_RIGHT, MUTE_MUTED);

        int expectedLevel = calculateVolumeLevel(0, 0);
        assertThat(mVolumeIcon.getDrawable().getLevel()).isEqualTo(expectedLevel);
    }

    private int calculateVolumeLevel(int left, int right) {
        return left * 5 + right;
    }

    private void assertControlUiCorrect() {
        final boolean expanded = mPreference.isControlExpanded();
        Map<Integer, SliderPreference> sliders = mPreference.getSliders();
        assertThat(sliders.get(SIDE_UNIFIED).isVisible()).isEqualTo(!expanded);
        assertThat(sliders.get(SIDE_LEFT).isVisible()).isEqualTo(expanded);
        assertThat(sliders.get(SIDE_RIGHT).isVisible()).isEqualTo(expanded);
        final float rotation = expanded ? ROTATION_EXPANDED : ROTATION_COLLAPSED;
        assertThat(mExpandIcon.getRotation()).isEqualTo(rotation);
    }
}
