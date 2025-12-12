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

package com.android.settings.gestures;

import static com.android.settings.gestures.DoubleTapPowerLockscreenTipPreferenceController.AFFORDANCE_NAME_COLUMN;
import static com.android.settings.gestures.DoubleTapPowerLockscreenTipPreferenceController.SLOT_ID_COLUMN;
import static com.android.settings.gestures.DoubleTapPowerLockscreenTipPreferenceController.CAMERA_KEYGUARD_QUICK_AFFORDANCE_NAME;
import static com.android.settings.gestures.DoubleTapPowerLockscreenTipPreferenceController.KEYGUARD_QUICK_AFFORDANCE_SELECTIONS_URI;
import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_DISABLED_MODE;
import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.MatrixCursor;
import android.text.TextUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.CardPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DoubleTapPowerLockscreenTipPreferenceControllerTest {

    private static final String KEY = "gesture_double_tap_power_lockscreen_shortcut_tip";
    private Context mContext;
    private Resources mResources;
    private DoubleTapPowerLockscreenTipPreferenceController mController;
    private CardPreference mPreference;
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        mContentResolver = mock(ContentResolver.class);
        when(mContext.getContentResolver()).thenReturn(mContentResolver);

        mController = new DoubleTapPowerLockscreenTipPreferenceController(mContext, KEY);
        mPreference = new CardPreference(mContext);
    }

    @Test
    public void updateState_doubleTapPowerGestureDisabled_preferenceNotVisible() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, false);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_noLockscreenShortcutData_preferenceNotVisible() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_noLockscreenShortcutDataColumn_preferenceNotVisible() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);
        when(mContentResolver.query(
                eq(KEYGUARD_QUICK_AFFORDANCE_SELECTIONS_URI),
                any(),
                any(),
                any())
        ).thenReturn(new MatrixCursor(new String[]{}));

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_noTargetActionInLockscreenShortcut_preferenceNotVisible() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);
        setSelectedLockScreenShortcuts();

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_onDismissed_preferenceNotVisible() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);
        setSelectedLockScreenShortcuts(CAMERA_KEYGUARD_QUICK_AFFORDANCE_NAME);
        mController.onDismiss(mPreference);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_targetActionInLockscreenShortcut_preferenceVisible() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForCameraLaunch(mContext);
        setSelectedLockScreenShortcutWithSlotId(CAMERA_KEYGUARD_QUICK_AFFORDANCE_NAME, "left");

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
        assertThat(
                TextUtils.equals(mPreference.getSummary(),
                        mContext.getString(
                                R.string.double_tap_power_lockscreen_shortcut_tip_description,
                                CAMERA_KEYGUARD_QUICK_AFFORDANCE_NAME
                        )
                )).isTrue();
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureNotAvailable_preferenceUnsupported() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_DISABLED_MODE);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureAvailable_preferenceAvailable() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_MULTI_TARGET_MODE);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    private void setSelectedLockScreenShortcuts(String... affordanceNames) {
        final MatrixCursor cursor = new MatrixCursor(
                new String[]{AFFORDANCE_NAME_COLUMN});
        for (final String name : affordanceNames) {
            cursor.addRow(new Object[]{name});
        }

        when(
                mContentResolver
                        .query(eq(KEYGUARD_QUICK_AFFORDANCE_SELECTIONS_URI), any(), any(), any()))
                .thenReturn(cursor);
    }

    private void setSelectedLockScreenShortcutWithSlotId(String affordanceName, String slotId) {
        final MatrixCursor cursor = new MatrixCursor(
                new String[]{AFFORDANCE_NAME_COLUMN, SLOT_ID_COLUMN});
        cursor.addRow(new Object[]{affordanceName, slotId});
        when(
                mContentResolver
                        .query(eq(KEYGUARD_QUICK_AFFORDANCE_SELECTIONS_URI), any(), any(),
                                any()))
                .thenReturn(cursor);
    }
}