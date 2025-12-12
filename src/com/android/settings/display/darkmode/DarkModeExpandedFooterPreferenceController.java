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
package com.android.settings.display.darkmode;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.widget.FooterPreference;

// LINT.IfChange
public class DarkModeExpandedFooterPreferenceController extends BasePreferenceController {
    static final String DARK_MODE_EXPANDED_FOOTER_KEY = "dark_theme_expanded_footer";

    public DarkModeExpandedFooterPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);

    }

    @Override
    public int getAvailabilityStatus() {
        return android.view.accessibility.Flags.forceInvertColor()
                ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        FooterPreference footerPreference = checkNotNull(screen.findPreference(getPreferenceKey()));
        footerPreference.setOrder(
                DarkModePreferenceOrderUtil.Order.EXPANDED_DARK_THEME_FOOTER.getValue());

        final Intent helpIntent;
        helpIntent = HelpUtils.getHelpIntent(
                mContext, mContext.getString(R.string.help_url_dark_theme_link),
                mContext.getClass().getName());
        if (helpIntent != null) {
            footerPreference.setLearnMoreAction(
                    view -> view.startActivityForResult(helpIntent, 0));
            footerPreference.setLearnMoreText(
                    mContext.getString(
                            R.string.accessibility_dark_theme_footer_learn_more_helper_link));
        }
    }
}
// LINT.ThenChange(DarkModeExpandedFooterPreference.kt)
