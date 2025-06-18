/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.display.darkmode;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.accessibility.Flags;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.TopIntroPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link DarkModeTopIntroPreferenceController}.
 */
@RunWith(RobolectricTestRunner.class)
public class DarkModeTopIntroPreferenceControllerTest {
    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PREFERENCE_KEY = "preference_key";

    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private TopIntroPreference mPreference;
    private DarkModeTopIntroPreferenceController mController;
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DarkModeTopIntroPreferenceController(mContext, PREFERENCE_KEY);
        when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreference);
    }

    @Test
    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    public void enableForceInvert_newPreferenceTitle() {
        mController.displayPreference(mScreen);

        verify(mPreference).setTitle(eq(R.string.dark_ui_text_force_invert));
    }

    @Test
    @DisableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    public void disableForceInvert_originalPreferenceTitle() {
        mController.displayPreference(mScreen);

        verify(mPreference).setTitle(eq(R.string.dark_ui_text));
    }
}
