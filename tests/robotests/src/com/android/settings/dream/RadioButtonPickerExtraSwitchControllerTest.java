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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RadioButtonPickerExtraSwitchControllerTest {

    @Mock
    private Preference mPreference;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private RadioButtonPickerExtraSwitchController mController;

    private final RadioButtonPickerExtraSwitchController.PreferenceAccessor
            mFakePreferenceAccessor =
            new RadioButtonPickerExtraSwitchController.PreferenceAccessor() {
                private boolean mValue;

                @Override
                public void setValue(boolean value) {
                    mValue = value;
                }

                @Override
                public boolean getValue() {
                    return mValue;
                }
            };

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mController = new RadioButtonPickerExtraSwitchController(
                RuntimeEnvironment.application,
                0,
                mFakePreferenceAccessor);
    }

    @Test
    public void testOnPreferenceChange() {
        mController.onPreferenceChange(mPreference, Boolean.TRUE);
        assertThat(mFakePreferenceAccessor.getValue()).isTrue();
    }

    @Test
    public void testAddToScreen() {
        mController.addToScreen(mPreferenceScreen);
        verify(mPreferenceScreen).addPreference(any());
    }
}
