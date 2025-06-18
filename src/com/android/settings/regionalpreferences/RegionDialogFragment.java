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

package com.android.settings.regionalpreferences;

import static android.app.settings.SettingsEnums.ACTION_CHANGE_PREFERRED_LANGUAGE_REGION_POSITIVE_BTN_CLICKED;
import static android.app.settings.SettingsEnums.ACTION_CHANGE_PREFERRED_LANGUAGE_REGION_NEGATIVE_BTN_CLICKED;
import static android.app.settings.SettingsEnums.ACTION_CHANGE_REGION_DIALOG_NEGATIVE_BTN_CLICKED;
import static android.app.settings.SettingsEnums.ACTION_CHANGE_REGION_DIALOG_POSITIVE_BTN_CLICKED;
import static android.app.settings.SettingsEnums.CHANGE_REGION_DIALOG;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.LocaleList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Locale;
import java.util.Set;

/**
 * Create a dialog for system region events.
 */
public class RegionDialogFragment extends InstrumentedDialogFragment {

    public static final String ARG_CALLING_PAGE = "arg_calling_page";
    public static final int CALLING_PAGE_LANGUAGE_CHOOSE_A_REGION = 0;
    public static final int CALLING_PAGE_REGIONAL_PREFERENCES_REGION_PICKER = 1;
    public static final int DIALOG_CHANGE_SYSTEM_LOCALE_REGION = 1;
    public static final int DIALOG_CHANGE_PREFERRED_LOCALE_REGION = 2;
    public static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    public static final String ARG_TARGET_LOCALE = "arg_target_locale";
    public static final String ARG_REPLACED_TARGET_LOCALE = "arg_replaced_target_locale";

    private static final String TAG = "RegionDialogFragment";

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RegionDialogFragment.
     */
    @NonNull
    public static RegionDialogFragment newInstance() {
        return new RegionDialogFragment();
    }

    @Override
    public int getMetricsCategory() {
        return CHANGE_REGION_DIALOG;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // TODO(385834414): Migrate to use MaterialAlertDialogBuilder
        RegionDialogController controller = getRegionDialogController(getContext(), this);
        RegionDialogController.DialogContent dialogContent = controller.getDialogContent();
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(getContext()).inflate(
                R.layout.locale_dialog, null);
        setDialogTitle(viewGroup, dialogContent.mTitle);
        setDialogMessage(viewGroup, dialogContent.mMessage);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setView(viewGroup);
        if (!dialogContent.mPositiveButton.isEmpty()) {
            builder.setPositiveButton(dialogContent.mPositiveButton, controller);
        }
        if (!dialogContent.mNegativeButton.isEmpty()) {
            builder.setNegativeButton(dialogContent.mNegativeButton, controller);
        }
        return builder.create();
    }

    private static void setDialogTitle(View root, String content) {
        TextView titleView = root.findViewById(R.id.dialog_title);
        if (titleView == null) {
            return;
        }
        titleView.setText(content);
    }

    private static void setDialogMessage(View root, String content) {
        TextView textView = root.findViewById(R.id.dialog_msg);
        if (textView == null) {
            return;
        }
        textView.setText(content);
    }

    @VisibleForTesting
    RegionDialogController getRegionDialogController(Context context,
            RegionDialogFragment dialogFragment) {
        return new RegionDialogController(context, dialogFragment);
    }

    class RegionDialogController implements DialogInterface.OnClickListener {
        private final Context mContext;
        private final int mDialogType;
        private final int mCallingPage;
        private final LocaleStore.LocaleInfo mLocaleInfo;
        private final Locale mReplacedLocale;
        private final MetricsFeatureProvider mMetricsFeatureProvider;

        RegionDialogController(
                @NonNull Context context, @NonNull RegionDialogFragment dialogFragment) {
            mContext = context;
            Bundle arguments = dialogFragment.getArguments();
            mDialogType = arguments.getInt(ARG_DIALOG_TYPE);
            mCallingPage = arguments.getInt(ARG_CALLING_PAGE);
            mLocaleInfo = (LocaleStore.LocaleInfo) arguments.getSerializable(ARG_TARGET_LOCALE);
            mReplacedLocale = (Locale) arguments.getSerializable(ARG_REPLACED_TARGET_LOCALE);
            mMetricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        }

        @Override
        public void onClick(@NonNull DialogInterface dialog, int which) {
            if (mDialogType == DIALOG_CHANGE_SYSTEM_LOCALE_REGION
                    || mDialogType == DIALOG_CHANGE_PREFERRED_LOCALE_REGION) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    updateRegion(mLocaleInfo.getLocale().toLanguageTag());
                    mMetricsFeatureProvider.action(
                            mContext,
                            mDialogType == DIALOG_CHANGE_SYSTEM_LOCALE_REGION
                                ? ACTION_CHANGE_REGION_DIALOG_POSITIVE_BTN_CLICKED
                                : ACTION_CHANGE_PREFERRED_LANGUAGE_REGION_POSITIVE_BTN_CLICKED,
                            mCallingPage);
                    dismiss();
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                } else {
                    mMetricsFeatureProvider.action(
                            mContext,
                            mDialogType == DIALOG_CHANGE_SYSTEM_LOCALE_REGION
                                ? ACTION_CHANGE_REGION_DIALOG_NEGATIVE_BTN_CLICKED
                                : ACTION_CHANGE_PREFERRED_LANGUAGE_REGION_NEGATIVE_BTN_CLICKED,
                            mCallingPage);
                    dismiss();
                }
            }
        }

        @VisibleForTesting
        DialogContent getDialogContent() {
            DialogContent dialogContent = new DialogContent();
            switch (mDialogType) {
                case DIALOG_CHANGE_SYSTEM_LOCALE_REGION:
                    dialogContent.mTitle = String.format(mContext.getString(
                        R.string.title_change_system_region),
                        mLocaleInfo.getLocale().getDisplayCountry());
                    dialogContent.mMessage = mContext.getString(
                        R.string.desc_notice_device_region_change,
                        Locale.getDefault().getDisplayLanguage());
                    dialogContent.mPositiveButton = mContext.getString(
                        R.string.button_label_confirmation_of_system_locale_change);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                case DIALOG_CHANGE_PREFERRED_LOCALE_REGION:
                    dialogContent.mTitle = String.format(mContext.getString(
                            R.string.title_change_system_region),
                        mLocaleInfo.getLocale().getDisplayCountry());
                    dialogContent.mMessage = mContext.getString(
                        R.string.desc_notice_device_region_change_for_preferred_language,
                        mLocaleInfo.getFullNameNative(),
                        LocaleStore.getLocaleInfo(mReplacedLocale).getFullNameNative());
                    dialogContent.mPositiveButton = mContext.getString(
                        R.string.button_label_confirmation_of_system_locale_change);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                default:
                    break;
            }
            return dialogContent;
        }

        private void updateRegion(String selectedLanguageTag) {
            Locale[] newLocales = getUpdatedLocales(Locale.forLanguageTag(selectedLanguageTag));
            LocalePicker.updateLocales(new LocaleList(newLocales));
        }

        private Locale[] getUpdatedLocales(Locale selectedLocale) {
            LocaleList localeList = LocaleList.getDefault();
            Locale[] newLocales = new Locale[localeList.size()];
            for (int i = 0; i < localeList.size(); i++) {
                Locale target = localeList.get(i);
                if (sameLanguageAndScript(selectedLocale, target)) {
                    newLocales[i] = appendLocaleExtension(selectedLocale);
                } else {
                    newLocales[i] = localeList.get(i);
                }
            }
            return newLocales;
        }

        private Locale appendLocaleExtension(Locale selectedLocale) {
            Locale systemLocale = Locale.getDefault();
            Set<Character> extensionKeys = systemLocale.getExtensionKeys();
            Locale.Builder builder = new Locale.Builder();
            builder.setLocale(selectedLocale);
            if (!extensionKeys.isEmpty()) {
                for (Character extKey : extensionKeys) {
                    builder.setExtension(extKey, systemLocale.getExtension(extKey));
                }
            }
            return builder.build();
        }

        private static boolean sameLanguageAndScript(Locale source, Locale target) {
            String sourceLanguage = source.getLanguage();
            String targetLanguage = target.getLanguage();
            String sourceLocaleScript = source.getScript();
            String targetLocaleScript = target.getScript();
            if (sourceLanguage.equals(targetLanguage)) {
                if (!sourceLocaleScript.isEmpty() && !targetLocaleScript.isEmpty()) {
                    return sourceLocaleScript.equals(targetLocaleScript);
                }
                return true;
            }
            return false;
        }

        @VisibleForTesting
        static class DialogContent {
            String mTitle = "";
            String mMessage = "";
            String mPositiveButton = "";
            String mNegativeButton = "";
        }
    }
}
