/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.app.Activity.RESULT_CANCELED;

import android.app.settings.SettingsEnums;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.accessibility.detail.a11yservice.A11yServicePreferenceFragment;
import com.android.settings.accessibility.detail.a11yservice.ui.UseServicePreference;
import com.android.settingslib.widget.SettingsThemeHelper;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

public class ToggleScreenReaderPreferenceFragmentForSetupWizard
        extends A11yServicePreferenceFragment {
    private boolean mToggleSwitchWasInitiallyChecked;
    private final String mMainActionPrefKey = UseServicePreference.KEY;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            final String title = getArguments().getString(AccessibilitySettings.EXTRA_TITLE);
            final String description = getContext().getString(R.string.talkback_summary);
            final Drawable icon = getContext().getDrawable(R.drawable.ic_accessibility_visibility);
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(getContext(), layout, title,
                    description, icon);

            final FooterBarMixin mixin = layout.getMixin(FooterBarMixin.class);
            AccessibilitySetupWizardUtils.setPrimaryButton(getContext(), mixin, R.string.done,
                    () -> {
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }

        TwoStatePreference preference = findPreference(mMainActionPrefKey);
        mToggleSwitchWasInitiallyChecked = preference.isChecked();
    }

    @NonNull
    @Override
    public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater,
            @NonNull ViewGroup parent, @Nullable Bundle savedInstanceState) {
        if (parent instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return AccessibilityFragmentUtils.addCollectionInfoToAccessibilityDelegate(
                    layout.onCreateRecyclerView(inflater, parent, savedInstanceState));
        }
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState);
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            return new PreferenceAdapterInSuw(preferenceScreen);
        }
        return super.onCreateAdapter(preferenceScreen);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_READER;
    }

    @Override
    public void onStop() {
        // Log the final choice in value if it's different from the previous value.
        TwoStatePreference preference = findPreference(mMainActionPrefKey);
        if (preference.isChecked() != mToggleSwitchWasInitiallyChecked) {
            mMetricsFeatureProvider.action(getContext(),
                    SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_READER,
                    preference.isChecked());
        }
        super.onStop();
    }
}
