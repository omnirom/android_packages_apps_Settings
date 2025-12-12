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

package com.android.settings.display;

import static android.provider.Settings.Secure.HDR_BRIGHTNESS_BOOST_LEVEL;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.res.Configuration;
import android.os.LocaleList;
import android.provider.Settings;
import android.testing.TestableContext;

import androidx.preference.PreferenceScreen;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settingslib.widget.SliderPreference;

import com.google.testing.junit.testparameterinjector.TestParameters;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestParameterInjector;

import java.util.Locale;

@RunWith(RobolectricTestParameterInjector.class)
public class HdrBrightnessLevelPreferenceControllerTest {
    private HdrBrightnessLevelPreferenceController mPreferenceController;
    private AutoCloseable mOpenMocks;

    @Mock
    private SliderPreference mPreference;

    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getContext());

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);

        Configuration configuration = mock(Configuration.class);
        LocaleList locales = mock(LocaleList.class);
        when(configuration.getLocales()).thenReturn(locales);
        when(locales.get(0)).thenReturn(Locale.US);
        mContext.getOrCreateTestableResources().overrideConfiguration(configuration);
        mPreferenceController = new HdrBrightnessLevelPreferenceController(mContext, "test");

        PreferenceScreen screen = mock(PreferenceScreen.class);
        when(screen.findPreference(mPreferenceController.getPreferenceKey())).thenReturn(
                mPreference);
        Settings.Secure.putFloat(mContext.getContentResolver(), HDR_BRIGHTNESS_BOOST_LEVEL, 1);
        mPreferenceController.displayPreference(screen);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    @TestParameters({
            "{value: 0}",
            "{value: 0.13f}",
            "{value: 0.2f}",
            "{value: 0.3f}",
            "{value: 0.67f}",
            "{value: 0.88f}",
            "{value: 1}"
    })
    public void getSliderPosition(float value) {
        Settings.Secure.putFloat(mContext.getContentResolver(), HDR_BRIGHTNESS_BOOST_LEVEL, value);

        assertEquals(Math.round(value * mPreferenceController.getMax()),
                mPreferenceController.getSliderPosition());
    }

    @Test
    @TestParameters({
            "{position: 0}",
            "{position: 13}",
            "{position: 200}",
            "{position: 1002}",
            "{position: 21587}",
            "{position: 30987}",
            "{position: 50789}"
    })
    public void setSliderPosition(int position) {
        mPreferenceController.setSliderPosition(position);

        assertEquals((float) position / mPreferenceController.getMax(),
                Settings.Secure.getFloat(mContext.getContentResolver(),
                        HDR_BRIGHTNESS_BOOST_LEVEL, /* def= */ 1), /* delta= */ 0);
    }

    @Test
    @TestParameters({
            "{value: 0.1333f, percentage: 13%}",
            "{value: 0.2f, percentage: 20%}",
            "{value: 0.3f, percentage: 30%}",
            "{value: 0.6779f, percentage: 67%}",
            "{value: 0.881f, percentage: 88%}",
            "{value: 1, percentage: 100%}"
    })
    public void formatPercentage(float value, String percentage) {
        verify(mPreference).setSliderStateDescription("100%");
        clearInvocations(mPreference);

        mPreferenceController.setSliderPosition(Math.round(value * mPreferenceController.getMax()));
        verify(mPreference).setSliderStateDescription(percentage);
    }
}
