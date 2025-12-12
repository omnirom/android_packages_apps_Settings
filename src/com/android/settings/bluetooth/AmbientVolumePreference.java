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
import static android.view.View.GONE;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES;
import static android.view.View.VISIBLE;

import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME_SLIDER;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import android.content.Context;
import android.util.ArrayMap;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.hearingdevices.ui.AmbientVolumeUi;
import com.android.settingslib.widget.Expandable;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.SliderPreference;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;

import java.util.Map;
import java.util.Set;

/**
 * A preference group of ambient volume controls.
 *
 * <p> It consists of a header with an expand icon and volume sliders for unified control and
 * separated control for devices in the same set. Toggle the expand icon will make the UI switch
 * between unified and separated control.
 */
public class AmbientVolumePreference extends PreferenceGroup implements AmbientVolumeUi,
        Expandable {

    private static final int ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED = 0;
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED = 1;

    private static final String METRIC_KEY_AMBIENT_SLIDER = "ambient_slider";
    private static final String METRIC_KEY_AMBIENT_MUTE = "ambient_mute";
    private static final String METRIC_KEY_AMBIENT_EXPAND = "ambient_expand";

    @Nullable
    private AmbientVolumeUiListener mListener;
    @Nullable
    private View mExpandIcon;
    @Nullable
    private View mVolumeIconFrame;
    @Nullable
    private ImageView mVolumeIcon;

    private final BiMap<Integer, SliderPreference> mSideToSliderMap = HashBiMap.create();
    private final Map<Integer, Integer> mSideToMuteStateMap = new ArrayMap<>();
    private boolean mExpandable = true;
    private boolean mExpanded = false;
    private int mVolumeLevel = AMBIENT_VOLUME_LEVEL_DEFAULT;
    private int mMetricsCategory;

    private final OnPreferenceChangeListener mPreferenceChangeListener =
            (slider, v) -> {
                if (slider instanceof SliderPreference && v instanceof final Integer value) {
                    final Integer side = mSideToSliderMap.inverse().get(slider);
                    if (side != null) {
                        logMetrics(METRIC_KEY_AMBIENT_SLIDER, side);
                        if (mListener != null) {
                            mListener.onSliderValueChange(side, value);
                        }
                    }
                    return true;
                }
                return false;
            };

    public AmbientVolumePreference(@NonNull Context context) {
        super(context, null);
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            setLayoutResource(com.android.settingslib.widget.theme
                    .R.layout.settingslib_expressive_preference);
            setWidgetLayoutResource(com.android.settingslib.widget.preference.expandable
                    .R.layout.settingslib_widget_expandable_icon);
        } else {
            setLayoutResource(R.layout.preference_ambient_volume);
        }
        setIcon(com.android.settingslib.R.drawable.ic_ambient_volume);
        setTitle(R.string.bluetooth_ambient_volume_control);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);

        mVolumeIcon = holder.itemView.requireViewById(com.android.internal.R.id.icon);
        mVolumeIcon.getDrawable().mutate().setTint(getContext().getColor(
                com.android.internal.R.color.materialColorOnPrimaryContainer));
        mVolumeIconFrame = holder.itemView.requireViewById(R.id.icon_frame);
        int volumeIconBackgroundResId = SettingsThemeHelper.isExpressiveTheme(getContext())
                ? R.drawable.ambient_icon_background_expressive
                : R.drawable.ambient_icon_background;
        mVolumeIconFrame.setBackgroundResource(volumeIconBackgroundResId);
        mVolumeIconFrame.setOnClickListener(v -> {
            if (!isMutable()) {
                return;
            }
            int updatedMuteState = isMuted() ? MUTE_NOT_MUTED : MUTE_MUTED;
            setSliderMuteState(SIDE_UNIFIED, updatedMuteState);
            logMetrics(METRIC_KEY_AMBIENT_MUTE, isMuted() ? 1 : 0);
            if (mListener != null) {
                mListener.onAmbientVolumeIconClick();
            }
        });
        updateVolumeIcon();

        mExpandIcon = holder.itemView.requireViewById(R.id.expand_icon);
        mExpandIcon.setOnClickListener(v -> {
            if (!isControlExpandable()) {
                return;
            }
            setControlExpanded(!mExpanded);
            logMetrics(METRIC_KEY_AMBIENT_EXPAND, mExpanded ? 1 : 0);
            if (mListener != null) {
                mListener.onExpandIconClick();
            }
        });
        updateExpandIcon();
    }

    @Override
    public void setControlExpandable(boolean expandable) {
        if (mExpandable != expandable) {
            mExpandable = expandable;
            if (!mExpandable) {
                setControlExpanded(false);
            }
            updateExpandIcon();
        }
    }

    @Override
    public boolean isControlExpandable() {
        return mExpandable;
    }

    @Override
    public void setControlExpanded(boolean expanded) {
        if (mExpanded != expanded) {
            mExpanded = expanded;
            updateExpandIcon();
            updateLayout();
        }
    }

    @Override
    public boolean isControlExpanded() {
        return mExpanded;
    }

    @Override
    public boolean isMutable() {
        return mSideToMuteStateMap.values().stream().anyMatch(mute -> mute != MUTE_DISABLED);
    }

    @Override
    public boolean isMuted() {
        return mSideToMuteStateMap.values().stream().allMatch(mute -> mute == MUTE_MUTED);
    }

    @Override
    public void setSliderMuteState(int side, int muteState) {
        if (side == SIDE_UNIFIED) {
            // propagate the mute state to all other sliders
            mSideToSliderMap.keySet().forEach(s -> {
                if (s != SIDE_UNIFIED) {
                    setSliderMuteState(s, muteState);
                }
            });
        } else {
            SliderPreference slider = mSideToSliderMap.get(side);
            if (slider != null) {
                mSideToMuteStateMap.put(side, muteState);
                if (muteState == MUTE_MUTED) {
                    slider.setValue(slider.getMin());
                }
                SliderPreference unifiedSlider = mSideToSliderMap.get(SIDE_UNIFIED);
                if (isMuted() && unifiedSlider != null) {
                    unifiedSlider.setValue(unifiedSlider.getMin());
                }
                updateVolumeLevel();
            }
        }
    }

    @Override
    public int getSliderMuteState(int side) {
        if (side == SIDE_UNIFIED) {
            if (!isMutable()) {
                return MUTE_DISABLED;
            } else {
                return isMuted() ? MUTE_MUTED : MUTE_NOT_MUTED;
            }
        } else {
            return mSideToMuteStateMap.getOrDefault(side, MUTE_DISABLED);
        }
    }

    @Override
    public void setListener(@Nullable AmbientVolumeUiListener listener) {
        mListener = listener;
    }

    @Override
    public void setupSliders(@NonNull Set<Integer> sides) {
        sides.forEach(side -> createSlider(side, ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED + side));
        createSlider(SIDE_UNIFIED, ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED);

        if (!mSideToSliderMap.isEmpty()) {
            for (int side : VALID_SIDES) {
                final SliderPreference slider = mSideToSliderMap.get(side);
                if (slider != null && findPreference(slider.getKey()) == null) {
                    addPreference(slider);
                }
            }
        }
        updateLayout();
    }

    @Override
    public void setSliderEnabled(int side, boolean enabled) {
        SliderPreference slider = mSideToSliderMap.get(side);
        if (slider != null) {
            slider.setEnabled(enabled);
            if (!enabled) {
                slider.setValue(slider.getMin());
            }
            updateVolumeLevel();
        }
    }

    @Override
    public void setSliderValue(int side, int value) {
        SliderPreference slider = mSideToSliderMap.get(side);
        if (slider != null && slider.getValue() != value) {
            slider.setValue(value);
            updateVolumeLevel();
        }
    }

    @Override
    public void setSliderRange(int side, int min, int max) {
        SliderPreference slider = mSideToSliderMap.get(side);
        if (slider != null) {
            slider.setMin(min);
            slider.setMax(max);
        }
    }

    private void updateLayout() {
        mSideToSliderMap.forEach((side, slider) -> {
            if (side == SIDE_UNIFIED) {
                slider.setVisible(!mExpanded);
            } else {
                slider.setVisible(mExpanded);
            }
        });
        updateVolumeLevel();
    }

    /** Sets the metrics category. */
    public void setMetricsCategory(int category) {
        mMetricsCategory = category;
    }

    private int getMetricsCategory() {
        return mMetricsCategory;
    }

    private void updateVolumeLevel() {
        int leftLevel, rightLevel;
        if (isControlExpanded()) {
            leftLevel = getVolumeLevel(SIDE_LEFT);
            rightLevel = getVolumeLevel(SIDE_RIGHT);
        } else {
            final int unifiedLevel = getVolumeLevel(SIDE_UNIFIED);
            leftLevel = unifiedLevel;
            rightLevel = unifiedLevel;
        }
        mVolumeLevel = Ints.constrainToRange(leftLevel * 5 + rightLevel,
                AMBIENT_VOLUME_LEVEL_MIN, AMBIENT_VOLUME_LEVEL_MAX);
        updateVolumeIcon();
    }

    private int getVolumeLevel(int side) {
        SliderPreference slider = mSideToSliderMap.get(side);
        if (slider == null || !slider.isEnabled()) {
            return 0;
        }
        final double min = slider.getMin();
        final double max = slider.getMax();
        final double levelGap = (max - min) / 4.0;
        final int value = slider.getValue();
        return (int) Math.ceil((value - min) / levelGap);
    }

    private void updateExpandIcon() {
        if (mExpandIcon == null) {
            return;
        }
        mExpandIcon.setVisibility(isControlExpandable() ? VISIBLE : GONE);
        mExpandIcon.setRotation(isControlExpanded() ? ROTATION_EXPANDED : ROTATION_COLLAPSED);
        if (isControlExpandable()) {
            final int stringRes = isControlExpanded()
                    ? R.string.bluetooth_ambient_volume_control_collapse
                    : R.string.bluetooth_ambient_volume_control_expand;
            mExpandIcon.setContentDescription(getContext().getString(stringRes));
        } else {
            mExpandIcon.setContentDescription(null);
        }
    }

    private void updateVolumeIcon() {
        if (mVolumeIcon == null || mVolumeIconFrame == null) {
            return;
        }
        mVolumeIcon.setImageLevel(mVolumeLevel);
        if (isMutable()) {
            final int stringRes = isMuted() ? R.string.bluetooth_ambient_volume_unmute
                    : R.string.bluetooth_ambient_volume_mute;
            mVolumeIcon.setContentDescription(getContext().getString(stringRes));
            mVolumeIconFrame.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        }  else {
            mVolumeIcon.setContentDescription(null);
            mVolumeIconFrame.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }

    private void createSlider(int side, int order) {
        if (mSideToSliderMap.containsKey(side)) {
            return;
        }
        SliderPreference slider = new SliderPreference(getContext());
        slider.setKey(KEY_AMBIENT_VOLUME_SLIDER + "_" + side);
        slider.setOrder(order);
        slider.setOnPreferenceChangeListener(mPreferenceChangeListener);
        if (side == SIDE_LEFT) {
            slider.setTitle(
                    getContext().getString(R.string.bluetooth_ambient_volume_control_left));
            slider.setSliderContentDescription(getContext().getString(
                    R.string.bluetooth_ambient_volume_control_left_description));
        } else if (side == SIDE_RIGHT) {
            slider.setTitle(
                    getContext().getString(R.string.bluetooth_ambient_volume_control_right));
            slider.setSliderContentDescription(getContext().getString(
                    R.string.bluetooth_ambient_volume_control_right_description));
        } else {
            slider.setSliderContentDescription(getContext().getString(
                    R.string.bluetooth_ambient_volume_control_description));
        }
        mSideToSliderMap.put(side, slider);
    }

    @VisibleForTesting
    Map<Integer, SliderPreference> getSliders() {
        return mSideToSliderMap;
    }

    private void logMetrics(String key, int value) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().changed(
                getMetricsCategory(), key, value);
    }

    @Override
    public boolean isExpanded() {
        // isExpanded() is different from isControlExpanded(), this is at the point of view if a
        // preference group shows any of its child preference.
        // Should always return true for AmbientVolumePreference as it always shows at least one
        // child preference no matter in collapsed or expanded mode.
        return true;
    }
}
