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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.testutils.AccessibilityTestUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link ToggleScreenReaderPreferenceFragmentForSetupWizard}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleScreenReaderPreferenceFragmentForSetupWizardTest extends
        BaseShortcutInteractionsInSuwTestCases<ToggleScreenReaderPreferenceFragmentForSetupWizard> {

    private static final String PLACEHOLDER_PACKAGE_NAME = "com.placeholder.example";
    private static final String A11Y_SERVICE_CLASS_NAME = "fakeA11yServiceClass";

    private static final String DEFAULT_INTRO = "default intro";
    private static final String SHORTCUT_PREF_KEY = "service_shortcut";
    private static final ComponentName PLACEHOLDER_A11Y_SERVICE =
            new ComponentName(PLACEHOLDER_PACKAGE_NAME, A11Y_SERVICE_CLASS_NAME);

    @Test
    public void getMetricsCategory() {
        assertThat(
                new ToggleScreenReaderPreferenceFragmentForSetupWizard().getMetricsCategory()
        ).isEqualTo(SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_READER);
    }

    @NonNull
    @Override
    public ToggleScreenReaderPreferenceFragmentForSetupWizard launchFragment() {
        AccessibilityServiceInfo a11yServiceInfo =
                spy(AccessibilityTestUtils.createAccessibilityServiceInfo(
                        getContext(),
                        PLACEHOLDER_A11Y_SERVICE,
                        /* isAlwaysOnService= */ false));
        when(a11yServiceInfo.loadIntro(any())).thenReturn(DEFAULT_INTRO);
        getA11yManager().setInstalledAccessibilityServiceList(List.of(a11yServiceInfo));
        getA11yManager().setAccessibilityServiceWarningExempted(a11yServiceInfo.getComponentName());

        return super.launchFragment();
    }

    @NonNull
    @Override
    public ComponentName getFeatureComponent() {
        return PLACEHOLDER_A11Y_SERVICE;
    }

    @Nullable
    @Override
    public Bundle getFragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(
                AccessibilitySettings.EXTRA_COMPONENT_NAME, getFeatureComponent());
        return bundle;
    }

    @NonNull
    @Override
    public String getSetupWizardDescription() {
        return getContext().getString(R.string.talkback_summary);
    }

    @NonNull
    @Override
    public Class<ToggleScreenReaderPreferenceFragmentForSetupWizard> getFragmentClazz() {
        return ToggleScreenReaderPreferenceFragmentForSetupWizard.class;
    }

    @Nullable
    @Override
    public ShortcutPreference getShortcutToggle() {
        return getFragment() != null ? getFragment().findPreference(SHORTCUT_PREF_KEY) : null;
    }
}
