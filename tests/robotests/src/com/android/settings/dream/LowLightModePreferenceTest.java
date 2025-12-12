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

package com.android.settings.dream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LowLightModePreferenceTest {
    private LowLightModePreference mPreference;

    @Before
    public void setUp() {
        final Context context = RuntimeEnvironment.application;
        mPreference = new LowLightModePreference(context, null);
    }

    @Test
    public void enablingPreference_alsoEnablesSwitch() {
        mPreference.setEnabled(true);
        assertTrue(mPreference.isSwitchEnabled());
    }

    @Test
    public void disablingPreference_alsoDisablesSwitch() {
        mPreference.setEnabled(false);
        assertFalse(mPreference.isSwitchEnabled());
    }
}
