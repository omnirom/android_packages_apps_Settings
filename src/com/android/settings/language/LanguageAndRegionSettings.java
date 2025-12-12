/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.language;

import static android.os.UserManager.DISALLOW_CONFIG_LOCALE;

import static com.android.settings.flags.Flags.localeNotificationEnabled;
import static com.android.settings.flags.Flags.regionalPreferencesApiEnabled;
import static com.android.settings.localepicker.AppLocalePickerActivity.EXTRA_APP_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_DIALOG_TYPE;
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_MENU_ITEM_ID;
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_SELECTED_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED;
import static com.android.settings.localepicker.LocaleDialogFragment.ARG_TARGET_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_ADD_SYSTEM_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_CONFIRM_SYSTEM_DEFAULT;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE;
import static com.android.settings.localepicker.LocaleDialogFragment.DIALOG_REMOVE_LOCALE;
import static com.android.settings.localepicker.LocaleUtils.getUserLocaleList;
import static com.android.settings.localepicker.LocaleUtils.mayAppendUnicodeTags;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.LocaleList;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.ViewModelProvider;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.localepicker.LocaleDialogFragment;
import com.android.settings.localepicker.LocaleUtils;
import com.android.settings.localepicker.SystemLocalePickerFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.PreferenceCategoryController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.Instrumentable;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.ButtonPreference;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// LINT.IfChange
@SearchIndexable
public class LanguageAndRegionSettings extends RestrictedDashboardFragment {
    protected static final String EXTRA_SYSTEM_LOCALE_DIALOG_TYPE = "system_locale_dialog_type";
    protected static final String LOCALE_SUGGESTION = "locale_suggestion";

    private static final String KEY_SPEECH_CATEGORY = "speech_category";
    private static final String KEY_ON_DEVICE_RECOGNITION = "on_device_recognition_settings";
    private static final String KEY_TEXT_TO_SPEECH = "tts_settings_summary";
    private static final String KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST =
            "user_preferred_locale_list";
    private static final String TAG = "LanguageAndRegionSettings";
    private static final String CFGKEY_ADD_LOCALE = "localeAdded";
    private static final String KEY_ADD_A_LANGUAGE = "add_a_language";
    private static final String TAG_DIALOG_NOT_AVAILABLE = "dialog_not_available_locale";
    private static final String TAG_DIALOG_ADD_SYSTEM_LOCALE = "dialog_add_system_locale";
    private static final String INDEX_KEY_ADD_LANGUAGE = "add_language";
    private static final String TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT = "dialog_confirm_system_default";
    private static final String TAG_DIALOG_REMOVE_LOCALE = "dialog_remove_locale";
    private static final String TAG_DIALOG_REMOVE_AND_CHANGE_LOCALE =
            "dialog_remove_and_change_locale";
    private static final String TAG_DIALOG_NOT_AVAILABLE_WITH_CANCEL =
            "dialog_not_available_locale_with_cancel";

    private ButtonPreference mAddLanguagePreference;
    private boolean mLocaleAdditionMode = false;
    private boolean mIsUiRestricted;
    private UserPreferredLocalePreferenceController mUserPreferredLocalePreferenceController;
    private LanguageAndRegionViewModel mViewModel;

    @SuppressWarnings("NullAway")
    public LanguageAndRegionSettings() {
        super(DISALLOW_CONFIG_LOCALE);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_LANGUAGES_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleStore.fillCache(this.getContext());
    }

    @Override
    public @NonNull View onCreateView(@NonNull LayoutInflater inflater,
            @NonNull ViewGroup container, @NonNull Bundle savedInstanceState) {
        mAddLanguagePreference = (ButtonPreference) getPreferenceScreen().findPreference(
                KEY_ADD_A_LANGUAGE);
        mAddLanguagePreference.setOnClickListener(v -> {
            FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                    .logSettingsTileClick(INDEX_KEY_ADD_LANGUAGE, getMetricsCategory());
            mMetricsFeatureProvider.action(getContext(), SettingsEnums.ACTION_ADD_LANGUAGE);
            new SubSettingLauncher(getContext())
                    .setDestination(SystemLocalePickerFragment.class.getName())
                    .setSourceMetricsCategory(Instrumentable.METRICS_CATEGORY_UNKNOWN)
                    .launch();
        });
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(LanguageAndRegionViewModel.class);
        mViewModel.getDialogType().observe(this, this::showConfirmDialogByType);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hack to update action bar title. It's necessary to refresh title because this page user
        // can change locale from here and fragment won't relaunch. Once language changes, title
        // must display in the new language.
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.setTitle(R.string.languages_settings);

        final boolean previouslyRestricted = mIsUiRestricted;
        mIsUiRestricted = isUiRestricted();
        final TextView emptyView = getEmptyTextView();
        if (mIsUiRestricted && !previouslyRestricted) {
            // Lock it down.
            emptyView.setText(R.string.language_empty_list_user_restricted);
            emptyView.setVisibility(View.VISIBLE);
        } else if (!mIsUiRestricted && previouslyRestricted) {
            // Unlock it.
            emptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewStateRestored(@NonNull Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mLocaleAdditionMode = savedInstanceState.getBoolean(CFGKEY_ADD_LOCALE, false);
        }
        Log.d(TAG, "LocaleAdditionMode:" + mLocaleAdditionMode);
        if (!mLocaleAdditionMode && shouldShowAddedLocaleDialog()) {
            showDialogForAddedLocale();
            mLocaleAdditionMode = true;
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(CFGKEY_ADD_LOCALE, mLocaleAdditionMode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (data == null || data.getExtras() == null) {
            Log.d(TAG, "Fail to get intent from dialog. Request code = " + requestCode);
            return;
        }
        LocaleStore.LocaleInfo localeInfo;
        if (requestCode == DIALOG_CONFIRM_SYSTEM_DEFAULT) {
            if (resultCode == Activity.RESULT_OK) {
                boolean showNotTranslatedDialog = data.getBooleanExtra(
                        ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, true);
                LocaleStore.LocaleInfo selectedLocaleInfo =
                        (LocaleStore.LocaleInfo) data.getExtras().getSerializable(
                                ARG_SELECTED_LOCALE);
                int menuId = data.getExtras().getInt(ARG_MENU_ITEM_ID);
                updatePreference(selectedLocaleInfo, menuId);
                localeInfo = LocaleStore.getLocaleInfo(Locale.getDefault());
                if (showNotTranslatedDialog && !localeInfo.isTranslated()) {
                    showUnavailableDialog(localeInfo);
                }
            }
        } else if (requestCode == DIALOG_ADD_SYSTEM_LOCALE) {
            int action = SettingsEnums.ACTION_CANCEL_SYSTEM_LOCALE_FROM_RECOMMENDATION;
            if (resultCode == Activity.RESULT_OK) {
                List<LocaleStore.LocaleInfo> feedItemList = getUserLocaleList();
                localeInfo = (LocaleStore.LocaleInfo) data.getExtras().getSerializable(
                        ARG_TARGET_LOCALE);
                String preferencesTags = Settings.System.getString(
                        getContext().getContentResolver(), Settings.System.LOCALE_PREFERENCES);
                feedItemList.add(mayAppendUnicodeTags(localeInfo, preferencesTags));
                LocaleList localeList = new LocaleList(feedItemList.stream()
                        .map(LocaleStore.LocaleInfo::getLocale)
                        .toArray(Locale[]::new));
                LocaleList.setDefault(localeList);
                LocalePicker.updateLocales(localeList);
                action = SettingsEnums.ACTION_ADD_SYSTEM_LOCALE_FROM_RECOMMENDATION;
            }
            mMetricsFeatureProvider.action(getContext(), action);
        } else if (requestCode == DIALOG_REMOVE_LOCALE
                || requestCode == DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE) {
            if (resultCode == Activity.RESULT_OK) {
                LocaleStore.LocaleInfo removedLocaleInfo =
                        (LocaleStore.LocaleInfo) data.getExtras().getSerializable(
                                requestCode == DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE
                                        ? ARG_SELECTED_LOCALE : ARG_TARGET_LOCALE);
                updatePreference(removedLocaleInfo, R.id.remove);
                if (requestCode == DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE) {
                    boolean showNotTranslatedDialog = data.getBooleanExtra(
                            ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, true);
                    if (showNotTranslatedDialog) {
                        showUnavailableDialog(LocaleStore.getLocaleInfo(Locale.getDefault()));
                    }
                }
            }
        } else if (requestCode == DIALOG_NOT_AVAILABLE_LOCALE
                || requestCode == DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL) {
            if (resultCode == Activity.RESULT_OK) {
                LocaleStore.LocaleInfo selectedLocaleInfo =
                        (LocaleStore.LocaleInfo) data.getExtras().getSerializable(
                                ARG_SELECTED_LOCALE);
                int menuId = data.getExtras().getInt(ARG_MENU_ITEM_ID);
                updatePreference(selectedLocaleInfo, menuId);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updatePreference(LocaleStore.LocaleInfo localeInfo, int menuId) {
        mUserPreferredLocalePreferenceController.setUpdatedLocaleList(localeInfo, menuId);
        mUserPreferredLocalePreferenceController.doTheUpdate();
        mUserPreferredLocalePreferenceController.updatePreferences();
    }

    private void showConfirmDialogByType(Integer type) {
        if (isAdded()) {
            Log.d(TAG, "getDialogType, type = " + type);
            LocaleDialogFragment localeDialogFragment = LocaleDialogFragment.newInstance();
            Bundle args = new Bundle();
            args.putInt(ARG_DIALOG_TYPE, type.intValue());
            LocaleStore.LocaleInfo defaultAfterChange = mViewModel.getDefaultAfterChange();
            LocaleStore.LocaleInfo selectedLocaleInfo = mViewModel.getSelectedLocaleInfo();
            int menuItemId = mViewModel.getMenuItemId();
            switch (type) {
                case DIALOG_CONFIRM_SYSTEM_DEFAULT -> {
                    args.putSerializable(ARG_TARGET_LOCALE, defaultAfterChange);
                    args.putSerializable(ARG_SELECTED_LOCALE, selectedLocaleInfo);
                    args.putInt(ARG_MENU_ITEM_ID, menuItemId);
                    localeDialogFragment.setArguments(args);
                    localeDialogFragment.show(getChildFragmentManager(),
                            TAG_DIALOG_CONFIRM_SYSTEM_DEFAULT);
                    mViewModel.clearType();
                }
                case DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL -> {
                    args.putSerializable(ARG_TARGET_LOCALE, defaultAfterChange);
                    args.putSerializable(ARG_SELECTED_LOCALE, selectedLocaleInfo);
                    args.putInt(ARG_MENU_ITEM_ID, menuItemId);
                    localeDialogFragment.setArguments(args);
                    localeDialogFragment.show(getChildFragmentManager(),
                            TAG_DIALOG_NOT_AVAILABLE_WITH_CANCEL);
                    mViewModel.clearType();
                    mMetricsFeatureProvider.action(getContext(),
                            SettingsEnums.ACTION_NOT_SUPPORTED_SYSTEM_LANGUAGE);
                }
                case DIALOG_REMOVE_LOCALE -> {
                    args.putSerializable(ARG_TARGET_LOCALE, defaultAfterChange);
                    localeDialogFragment.setArguments(args);
                    localeDialogFragment.show(getChildFragmentManager(),
                            TAG_DIALOG_REMOVE_LOCALE);
                    mViewModel.clearType();
                }
                case DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE -> {
                    args.putSerializable(ARG_TARGET_LOCALE, defaultAfterChange);
                    args.putBoolean(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED,
                            !defaultAfterChange.isTranslated());
                    args.putSerializable(ARG_SELECTED_LOCALE, selectedLocaleInfo);
                    localeDialogFragment.setArguments(args);
                    localeDialogFragment.show(getChildFragmentManager(),
                            TAG_DIALOG_REMOVE_AND_CHANGE_LOCALE);
                    mViewModel.clearType();
                }
            }
        }
    }

    private boolean shouldShowAddedLocaleDialog() {
        Intent intent = this.getIntent();
        String dialogType = intent.getStringExtra(EXTRA_SYSTEM_LOCALE_DIALOG_TYPE);
        String localeTag = intent.getStringExtra(EXTRA_APP_LOCALE);
        String callingPackage = getActivity().getCallingPackage();
        if (!localeNotificationEnabled()
                || !getContext().getPackageName().equals(callingPackage)
                || !isValidDialogType(dialogType)
                || !isValidLocale(localeTag)
                || LocaleUtils.isLanguageInSystemLocale(localeTag)) {
            return false;
        }
        return true;
    }

    private boolean isValidDialogType(String type) {
        return LOCALE_SUGGESTION.equals(type);
    }

    private boolean isValidLocale(String tag) {
        if (TextUtils.isEmpty(tag)) {
            return false;
        }
        String[] systemLocales = getSupportedLocales();
        for (String systemTag : systemLocales) {
            if (systemTag.equals(tag)) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    String[] getSupportedLocales() {
        return LocalePicker.getSupportedLocales(getContext());
    }

    private void showUnavailableDialog(LocaleStore.LocaleInfo localeInfo) {
        Log.d(TAG, "showUnavailableDialog");
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, DIALOG_NOT_AVAILABLE_LOCALE);
        args.putSerializable(ARG_TARGET_LOCALE, localeInfo);
        LocaleDialogFragment localeDialogFragment = LocaleDialogFragment.newInstance();
        localeDialogFragment.setArguments(args);
        localeDialogFragment.show(getChildFragmentManager(), TAG_DIALOG_NOT_AVAILABLE);
        mMetricsFeatureProvider.action(getContext(),
                SettingsEnums.ACTION_NOT_SUPPORTED_SYSTEM_LANGUAGE);
    }

    private void showDialogForAddedLocale() {
        Log.d(TAG, "showDialogForAddedLocale");
        Intent intent = this.getIntent();
        String appLocaleTag = intent.getStringExtra(EXTRA_APP_LOCALE);

        LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(
                Locale.forLanguageTag(appLocaleTag));
        final LocaleDialogFragment localeDialogFragment = LocaleDialogFragment.newInstance();
        Bundle args = new Bundle();
        args.putInt(ARG_DIALOG_TYPE, DIALOG_ADD_SYSTEM_LOCALE);
        args.putSerializable(ARG_TARGET_LOCALE, localeInfo);
        localeDialogFragment.setArguments(args);
        localeDialogFragment.show(getChildFragmentManager(), TAG_DIALOG_ADD_SYSTEM_LOCALE);
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return LanguageAndRegionScreen.KEY;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.language_and_region_settings;
    }

    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private List<AbstractPreferenceController> buildPreferenceControllers(
            @NonNull Context context, @Nullable Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        mUserPreferredLocalePreferenceController =
                new UserPreferredLocalePreferenceController(context,
                        KEY_PREFERENCE_USER_PREFERRED_LOCALE_LIST, getSettingsLifecycle(), this);
        controllers.add(mUserPreferredLocalePreferenceController);

        final DefaultVoiceInputPreferenceController defaultVoiceInputPreferenceController =
                new DefaultVoiceInputPreferenceController(context, lifecycle);
        final TtsPreferenceController ttsPreferenceController =
                new TtsPreferenceController(context, KEY_TEXT_TO_SPEECH);
        final OnDeviceRecognitionPreferenceController onDeviceRecognitionPreferenceController =
                new OnDeviceRecognitionPreferenceController(context, KEY_ON_DEVICE_RECOGNITION);

        controllers.add(defaultVoiceInputPreferenceController);
        controllers.add(ttsPreferenceController);
        List<AbstractPreferenceController> speechCategoryChildren = new ArrayList<>(
                List.of(defaultVoiceInputPreferenceController, ttsPreferenceController));

        if (onDeviceRecognitionPreferenceController.isAvailable()) {
            controllers.add(onDeviceRecognitionPreferenceController);
            speechCategoryChildren.add(onDeviceRecognitionPreferenceController);
        }

        controllers.add(new PreferenceCategoryController(context, KEY_SPEECH_CATEGORY)
                .setChildren(speechCategoryChildren));

        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.language_and_region_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    if (regionalPreferencesApiEnabled()) {
                        return true;
                    }
                    return false;
                }
            };
}
// LINT.ThenChange(LanguageAndRegionScreen.kt)
