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

package com.android.settings.dashboard.profileselector;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;
import android.os.Looper;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import static com.android.settings.flags.Flags.FLAG_DISABLE_UPDATE_HEIGHT_PROFILE_SELECT_LOCATION_SERVICES;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ProfileSelectLocationServicesFragmentTest {

    private ProfileSelectLocationServicesFragment mFragment;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    @UiThreadTest
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mFragment = new ProfileSelectLocationServicesFragment();
    }

    @Test
    @DisableFlags(FLAG_DISABLE_UPDATE_HEIGHT_PROFILE_SELECT_LOCATION_SERVICES)
    public void test_initializeOptionsMenuInvalidatesExistingMenu() {
        final Activity activity = mock(Activity.class);
        assertThat(mFragment.forceUpdateHeight()).isTrue();
    }

    @Test
    @EnableFlags(FLAG_DISABLE_UPDATE_HEIGHT_PROFILE_SELECT_LOCATION_SERVICES)
    public void test_initializeOptionsMenuInvalidatesExistingMenu_flagEnabled() {
        final Activity activity = mock(Activity.class);
        assertThat(mFragment.forceUpdateHeight()).isFalse();
    }
}
