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

package com.android.settings.notification.modes;

import static android.app.slice.Slice.EXTRA_TOGGLE_STATE;

import static com.android.settingslib.notification.modes.ZenMode.MANUAL_DND_MODE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.slice.Slice;
import androidx.slice.SliceMetadata;
import androidx.slice.SliceProvider;
import androidx.slice.core.SliceAction;
import androidx.slice.widget.SliceLiveData;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowNotificationManager;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@Config(shadows = {ShadowNotificationManager.class, ShadowRestrictedLockUtilsInternal.class})
@RunWith(RobolectricTestRunner.class)
public class DndModeSliceBuilderTest {

    private Context mContext;

    @Mock
    private ZenModesBackend mBackend;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;

        MockitoAnnotations.initMocks(this);

        // Set-up specs for SliceMetadata.
        SliceProvider.setSpecs(SliceLiveData.SUPPORTED_SPECS);

        ZenModesBackend.setInstance(mBackend);
        when(mBackend.getMode(eq(MANUAL_DND_MODE_ID))).thenReturn(
                TestModeBuilder.MANUAL_DND);
    }

    @After
    public void tearDown() {
        ShadowRestrictedLockUtilsInternal.reset();
    }

    @Test
    public void getZenModeSlice_correctSliceContent() {
        final Slice dndSlice = DndModeSliceBuilder.getSlice(mContext);

        final SliceMetadata metadata = SliceMetadata.from(mContext, dndSlice);
        assertThat(metadata.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.zen_mode_settings_title));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(1);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        assertThat(primaryAction.getIcon()).isNull();
    }

    @Test
    public void getZenModeSlice_dndOn_toggleChecked() {
        when(mBackend.getMode(eq(MANUAL_DND_MODE_ID))).thenReturn(
                new TestModeBuilder(TestModeBuilder.MANUAL_DND).setActive(true).build());

        final Slice dndSlice = DndModeSliceBuilder.getSlice(mContext);
        final SliceMetadata metadata = SliceMetadata.from(mContext, dndSlice);
        final List<SliceAction> toggles = metadata.getToggles();

        assertThat(toggles).hasSize(1);
        assertThat(toggles.get(0).isChecked()).isTrue();
    }

    @Test
    public void getZenModeSlice_dndOff_toggleUnchecked() {
        when(mBackend.getMode(eq(MANUAL_DND_MODE_ID))).thenReturn(
                new TestModeBuilder(TestModeBuilder.MANUAL_DND).setActive(false).build());

        final Slice dndSlice = DndModeSliceBuilder.getSlice(mContext);
        final SliceMetadata metadata = SliceMetadata.from(mContext, dndSlice);
        final List<SliceAction> toggles = metadata.getToggles();

        assertThat(toggles).hasSize(1);
        assertThat(toggles.get(0).isChecked()).isFalse();
    }

    @Test
    public void getZenModeSlice_managedByAdmin_shouldNotHaveToggle() {
        ShadowRestrictedLockUtilsInternal.setRestricted(true);
        final Slice dndSlice = DndModeSliceBuilder.getSlice(mContext);

        final SliceMetadata metadata = SliceMetadata.from(mContext, dndSlice);
        assertThat(metadata.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.zen_mode_settings_title));

        final List<SliceAction> toggles = metadata.getToggles();
        assertThat(toggles).hasSize(0);

        final SliceAction primaryAction = metadata.getPrimaryAction();
        assertThat(primaryAction.getIcon()).isNull();
    }

    @Test
    public void handleUriChange_turnOn_zenModeTurnsOn() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_TOGGLE_STATE, true);

        DndModeSliceBuilder.handleUriChange(mContext, intent);

        verify(mBackend).activateMode(eq(TestModeBuilder.MANUAL_DND), eq(null));
    }

    @Test
    public void handleUriChange_turnOff_zenModeTurnsOff() {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_TOGGLE_STATE, false);

        DndModeSliceBuilder.handleUriChange(mContext, intent);

        verify(mBackend).deactivateMode(eq(TestModeBuilder.MANUAL_DND));
    }
}
