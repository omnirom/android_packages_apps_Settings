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
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.accessibility.screenmagnification.ui.MagnificationPreferenceFragment;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.TopIntroPreference;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupdesign.GlifPreferenceLayout;

public class ToggleScreenMagnificationPreferenceFragmentForSetupWizard
        extends MagnificationPreferenceFragment {

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view instanceof GlifPreferenceLayout) {
            final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
            final String title = getContext().getString(
                    R.string.accessibility_screen_magnification_title);
            final String description = getContext().getString(
                    R.string.accessibility_screen_magnification_intro_text);
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

        hidePreferenceSettingComponents();
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            return new PreferenceAdapterInSuw(preferenceScreen);
        }
        return super.onCreateAdapter(preferenceScreen);
    }

    /**
     * Hide the magnification preference settings in the SuW's vision settings.
     */
    private void hidePreferenceSettingComponents() {
        // Intro
        TopIntroPreference topIntroPreference = findPreference("top_intro");
        if (topIntroPreference != null) {
            topIntroPreference.setVisible(false);
        }
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
    public int getMetricsCategory() {
        return SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
    }

    @Override
    public void onStop() {
        // Log the final choice in value if it's different from the previous value.
        Bundle args = getArguments();
        if ((args != null) && args.containsKey(AccessibilitySettings.EXTRA_CHECKED)) {
            ShortcutPreference shortcutPreference = findPreference(
                    getShortcutPreferenceController().getPreferenceKey());
            if (shortcutPreference.isChecked() != args.getBoolean(
                    AccessibilitySettings.EXTRA_CHECKED)) {
                // TODO: Distinguish between magnification modes
                mMetricsFeatureProvider.action(getContext(),
                        SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION,
                        shortcutPreference.isChecked());
            }
        }
        super.onStop();
    }

    @Override
    public int getHelpResource() {
        // Hides help center in action bar and footer bar in SuW
        return 0;
    }
}
