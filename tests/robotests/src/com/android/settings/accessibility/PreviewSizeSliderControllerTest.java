/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.internal.accessibility.AccessibilityShortcutController.FONT_SIZE_COMPONENT_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.view.LayoutInflater;
import android.widget.PopupWindow;

import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.testutils.shadow.ShadowFragment;
import com.android.settingslib.testutils.shadow.ShadowInteractionJankMonitor;

import com.google.android.material.slider.Slider;
import com.google.android.setupcompat.util.WizardManagerHelper;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

/**
 * Tests for {@link PreviewSizeSliderController}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowInteractionJankMonitor.class})
public class PreviewSizeSliderControllerTest {

    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> rule =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);
    private static final String FONT_SIZE_KEY = "font_size";
    private Activity mContext;
    private PreviewSizeSliderController mSliderController;
    private FontSizeData mFontSizeData;
    private TooltipSliderPreference mSliderPreference;

    private PreferenceScreen mPreferenceScreen;
    private TestFragment mFragment;
    private PreferenceViewHolder mHolder;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    private Slider mSlider;

    @Mock
    private PreviewSizeSliderController.ProgressInteractionListener mInteractionListener;

    private static PopupWindow getLatestPopupWindow() {
        final ShadowApplication shadowApplication =
                Shadow.extract(ApplicationProvider.getApplicationContext());
        return shadowApplication.getLatestPopupWindow();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowInteractionJankMonitor.reset();

        rule.getScenario().onActivity(activity -> mContext = activity);
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mFragment = spy(new TestFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        mPreferenceScreen = spy(new PreferenceScreen(mContext, /* attrs= */ null));
        when(mPreferenceScreen.getPreferenceManager()).thenReturn(mPreferenceManager);
        doReturn(mPreferenceScreen).when(mFragment).getPreferenceScreen();
        mSliderPreference = new TooltipSliderPreference(mContext, /* attrs= */ null);
        mSliderPreference.setKey(FONT_SIZE_KEY);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        mHolder = spy(PreferenceViewHolder.createInstanceForTests(inflater.inflate(
                R.layout.preference_labeled_slider, null)));
        mSlider = Mockito.mock(Slider.class);
        doReturn(mSlider).when(mHolder).findViewById(
                com.android.settingslib.widget.preference.slider.R.id.slider);
        mSliderPreference.onBindViewHolder(mHolder);

        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSliderPreference);

        mFontSizeData = new FontSizeData(mContext);
        mSliderController =
                new PreviewSizeSliderController(mContext, FONT_SIZE_KEY, mFontSizeData) {
                    @Override
                    ComponentName getTileComponentName() {
                        return FONT_SIZE_COMPONENT_NAME;
                    }

                    @Override
                    CharSequence getTileTooltipContent() {
                        return mContext.getText(
                                R.string.accessibility_font_scaling_auto_added_qs_tooltip_content);
                    }
                };
        mSliderController.setInteractionListener(mInteractionListener);
        when(mPreferenceScreen.findPreference(mSliderController.getPreferenceKey())).thenReturn(
                mSliderPreference);
    }

    @Test
    public void initMax_matchResult() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSliderPreference);

        mSliderController.displayPreference(mPreferenceScreen);

        assertThat(mSliderPreference.getMax()).isEqualTo(
                mFontSizeData.getValues().size() - 1);
    }

    @Test
    public void initProgress_matchResult() {
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSliderPreference);

        mSliderController.displayPreference(mPreferenceScreen);

        assertThat(mSliderPreference.getValue()).isEqualTo(mFontSizeData.getInitialIndex());
    }

    @Test
    public void resetToDefaultState_matchResult() {
        final int defaultProgress =
                mFontSizeData.getValues().indexOf(mFontSizeData.getDefaultValue());
        when(mPreferenceScreen.findPreference(anyString())).thenReturn(mSliderPreference);

        mSliderController.displayPreference(mPreferenceScreen);
        mSliderPreference.setValue(defaultProgress + 1);
        mSliderController.resetState();

        assertThat(mSliderPreference.getValue()).isEqualTo(defaultProgress);
    }

    @Test
    public void resetState_verifyOnProgressChanged() {
        mSliderController.displayPreference(mPreferenceScreen);
        mSliderController.resetState();

        verify(mInteractionListener).onProgressChanged();
    }

    @Test
    public void onProgressChanged_verifyNotifyPreferenceChanged() {
        mSliderController.displayPreference(mPreferenceScreen);

        changeSliderValue(mSliderPreference.getMax());
        ShadowLooper.idleMainLooper();

        verify(mInteractionListener).onProgressChanged();
    }

    @Test
    public void onProgressChanged_showTooltipView() {
        mSliderController.displayPreference(mPreferenceScreen);

        // Simulate changing the progress for the first time
        changeSliderValue(mSliderPreference.getMax());

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    public void onProgressChanged_inSuw_toolTipShouldNotShown() {
        Intent intent = mContext.getIntent();
        intent.putExtra(WizardManagerHelper.EXTRA_IS_SETUP_FLOW, true);
        mContext.setIntent(intent);
        mSliderController.displayPreference(mPreferenceScreen);

        // Simulate changing the progress for the first time
        changeSliderValue(mSliderPreference.getMax());

        assertThat(getLatestPopupWindow()).isNull();
    }

    @Test
    public void onProgressChanged_tooltipViewHasBeenShown_notShowTooltipView() {
        mSliderController.displayPreference(mPreferenceScreen);
        // Simulate changing the progress for the first time
        changeSliderValue(mSliderPreference.getMax());
        getLatestPopupWindow().dismiss();

        // Simulate progress changing for the second time
        changeSliderValue(mSliderPreference.getMin());

        assertThat(getLatestPopupWindow().isShowing()).isFalse();
    }

    @Test
    @Config(shadows = ShadowFragment.class)
    @Ignore("b/414851104")
    public void enabledNeedsQSTooltipReshow_showTooltipView() {
        mSliderPreference.setNeedsQSTooltipReshow(true);

        mSliderController.displayPreference(mPreferenceScreen);
        mSliderController.onStart(null);
        ShadowLooper.idleMainLooper();

        assertThat(getLatestPopupWindow().isShowing()).isTrue();
    }

    @Test
    public void onProgressChanged_setCorrespondingCustomizedStateDescription() {
        String[] stateLabels = new String[]{"1", "2", "3", "4", "5"};
        mSliderController.setProgressStateLabels(stateLabels);
        mSliderController.displayPreference(mPreferenceScreen);

        int progress = 3;
        changeSliderValue(progress);

        verify(mSlider).setStateDescription(stateLabels[progress]);
    }

    private void changeSliderValue(int newValue) {
        mSliderPreference.setValue(newValue);
        mSliderController.getSliderChangeListener().onValueChange(
                mSlider, newValue, /* fromUser= */ false);
    }

    private static class TestFragment extends SettingsPreferenceFragment {

        @Override
        protected boolean shouldSkipForInitialSUW() {
            return false;
        }

        @Override
        public int getMetricsCategory() {
            return 0;
        }
    }
}
