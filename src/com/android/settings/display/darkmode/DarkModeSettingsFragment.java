/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.display.darkmode;

import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_SOURCE;
import static com.android.internal.accessibility.common.NotificationConstants.SOURCE_START_SURVEY;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.BaseSupportFragment;
import com.android.settings.accessibility.FeedbackButtonPreferenceController;
import com.android.settings.accessibility.FeedbackManager;
import com.android.settings.accessibility.Flags;
import com.android.settings.accessibility.ForceInvertSurveyButtonPreferenceController;
import com.android.settings.accessibility.SurveyManager;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for Dark UI Mode
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class DarkModeSettingsFragment extends BaseSupportFragment {

    private static final String TAG = "DarkModeSettingsFrag";
    private static final String DARK_THEME_END_TIME = "dark_theme_end_time";
    private static final String DARK_THEME_START_TIME = "dark_theme_start_time";
    public static final String FORCE_INVERT_SURVEY_KEY = "A11yForceInvertUser";
    private DarkModeObserver mContentObserver;
    private DarkModeCustomPreferenceController mCustomStartController;
    private DarkModeCustomPreferenceController mCustomEndController;
    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Flags.catalystDarkUiMode()) {
            final Context context = getContext();
            mContentObserver = new DarkModeObserver(context);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!Flags.catalystDarkUiMode()) {
            // Listen for changes only while visible.
            mContentObserver.subscribe(() -> {
                PreferenceScreen preferenceScreen = getPreferenceScreen();
                mCustomStartController.displayPreference(preferenceScreen);
                mCustomEndController.displayPreference(preferenceScreen);
                updatePreferenceStates();
            });
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (!Flags.catalystDarkUiMode()) {
            use(FeedbackButtonPreferenceController.class).initialize(
                    new FeedbackManager(context, getMetricsCategory()));
            initForceInvertSurvey(context);
        }
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers =  new ArrayList(2);
        mCustomStartController = new DarkModeCustomPreferenceController(getContext(),
                DARK_THEME_START_TIME, this);
        mCustomEndController = new DarkModeCustomPreferenceController(getContext(),
                DARK_THEME_END_TIME, this);
        controllers.add(mCustomStartController);
        controllers.add(mCustomEndController);
        return controllers;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!Flags.catalystDarkUiMode()) {
            // Stop listening for state changes.
            mContentObserver.unsubscribe();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (DARK_THEME_END_TIME.equals(preference.getKey())) {
            showDialog(DIALOG_END_TIME);
            return true;
        } else if (DARK_THEME_START_TIME.equals(preference.getKey())) {
            showDialog(DIALOG_START_TIME);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    public void refresh() {
        this.updatePreferenceStates();
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        if (dialogId == DIALOG_START_TIME || dialogId == DIALOG_END_TIME) {
            if (dialogId == DIALOG_START_TIME) {
                return mCustomStartController.getDialog();
            } else {
                return mCustomEndController.getDialog();
            }
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dark_mode_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_dark_theme;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DARK_UI_SETTINGS;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_START_TIME:
                return SettingsEnums.DIALOG_DARK_THEME_SET_START_TIME;
            case DIALOG_END_TIME:
                return SettingsEnums.DIALOG_DARK_THEME_SET_END_TIME;
            default:
                return 0;
        }
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@Nullable Context context) {
        return DarkModeScreen.KEY;
    }

    private void initForceInvertSurvey(@NonNull Context context) {
        final SurveyManager surveyManager = new SurveyManager(this, context,
                FORCE_INVERT_SURVEY_KEY, getMetricsCategory());
        final Intent intent = getIntent();
        if (intent != null
                && intent.getStringExtra(EXTRA_SOURCE) != null
                && TextUtils.equals(intent.getStringExtra(EXTRA_SOURCE), SOURCE_START_SURVEY)) {
            surveyManager.startSurvey();
        } else {
            use(ForceInvertSurveyButtonPreferenceController.class).initialize(surveyManager);
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            Flags.catalystDarkUiMode()
                    ? new BaseSearchIndexProvider()
                    : new BaseSearchIndexProvider(R.xml.dark_mode_settings) {
                        @Override
                        protected boolean isPageSearchEnabled(Context context) {
                            return !context.getSystemService(PowerManager.class).isPowerSaveMode();
                        }
                    };

}