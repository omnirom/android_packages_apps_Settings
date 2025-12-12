/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.localepicker;

import static android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT;

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.language.LanguageAndRegionSettings;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.Locale;

/**
 * Create a dialog for system locale events.
 */
public class LocaleDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = LocaleDialogFragment.class.getSimpleName();

    public static final int DIALOG_CONFIRM_SYSTEM_DEFAULT = 1;
    public static final int DIALOG_NOT_AVAILABLE_LOCALE = 2;
    public static final int DIALOG_ADD_SYSTEM_LOCALE = 3;
    public static final int DIALOG_REMOVE_LOCALE = 4;
    public static final int DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL = 5;
    public static final int DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE = 6;

    public static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    public static final String ARG_TARGET_LOCALE = "arg_target_locale";
    public static final String ARG_SELECTED_LOCALE = "arg_selected_locale";
    public static final String ARG_MENU_ITEM_ID = "arg_menu_item_id";
    public static final String ARG_SHOW_DIALOG = "arg_show_dialog";
    public static final String ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED =
            "arg_show_dialog_for_not_translated";

    private boolean mShouldKeepDialog;
    @SuppressWarnings("NullAway")
    private AlertDialog mAlertDialog;
    private OnBackInvokedDispatcher mBackDispatcher;

    private OnBackInvokedCallback mBackCallback = () -> {
        Log.d(TAG, "Do not back to previous page if the dialog is displaying.");
    };

    public static LocaleDialogFragment newInstance() {
        return new LocaleDialogFragment();
    }

    @Override
    public int getMetricsCategory() {
        int dialogType = getArguments().getInt(ARG_DIALOG_TYPE);
        switch (dialogType) {
            case DIALOG_CONFIRM_SYSTEM_DEFAULT:
                return SettingsEnums.DIALOG_SYSTEM_LOCALE_CHANGE;
            case DIALOG_NOT_AVAILABLE_LOCALE:
            case DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL:
                return SettingsEnums.DIALOG_SYSTEM_LOCALE_UNAVAILABLE;
            case DIALOG_REMOVE_LOCALE:
            case DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE:
                return SettingsEnums.ACTION_REMOVE_LANGUAGE;
            default:
                return SettingsEnums.DIALOG_SYSTEM_LOCALE_CHANGE;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ARG_SHOW_DIALOG, mShouldKeepDialog);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            Bundle arguments = getArguments();
            int type = arguments.getInt(ARG_DIALOG_TYPE);
            mShouldKeepDialog = savedInstanceState.getBoolean(ARG_SHOW_DIALOG, false);
            // Keep the dialog if user rotates the device, otherwise close the confirm system
            // default dialog only when user changes the locale.
            if ((type == DIALOG_CONFIRM_SYSTEM_DEFAULT || type == DIALOG_ADD_SYSTEM_LOCALE
                || type == DIALOG_REMOVE_LOCALE || type == DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE
                || type == DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL
                || type == DIALOG_NOT_AVAILABLE_LOCALE)
                && !mShouldKeepDialog) {
                dismiss();
            }
        }

        mShouldKeepDialog = true;
        LanguageAndRegionSettings parentFragment = (LanguageAndRegionSettings) getParentFragment();
        LocaleDialogController controller = getLocaleDialogController(getContext(), this,
                parentFragment);
        LocaleDialogController.DialogContent dialogContent = controller.getDialogContent();
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(getContext()).inflate(
                R.layout.locale_dialog, null);
        setDialogTitle(viewGroup, dialogContent.mTitle);
        setDialogMessage(viewGroup, dialogContent.mMessage);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setView(viewGroup);
        if (!dialogContent.mPositiveButton.isEmpty()) {
            builder.setPositiveButton(dialogContent.mPositiveButton, controller);
        }
        if (!dialogContent.mNegativeButton.isEmpty()) {
            builder.setNegativeButton(dialogContent.mNegativeButton, controller);
        }
        mAlertDialog = builder.create();
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(PRIORITY_DEFAULT, mBackCallback);
        mAlertDialog.setCanceledOnTouchOutside(false);
        mAlertDialog.setOnDismissListener(dialogInterface -> {
            mAlertDialog.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                    mBackCallback);
        });

        return mAlertDialog;
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
    public OnBackInvokedCallback getBackInvokedCallback() {
        return mBackCallback;
    }

    @VisibleForTesting
    public void setBackDispatcher(OnBackInvokedDispatcher dispatcher) {
        mBackDispatcher = dispatcher;
    }

    @VisibleForTesting
    public @NonNull OnBackInvokedDispatcher getOnBackInvokedDispatcher() {
        if (mBackDispatcher != null) {
            return mBackDispatcher;
        } else {
            return mAlertDialog.getOnBackInvokedDispatcher();
        }
    }

    @VisibleForTesting
    LocaleDialogController getLocaleDialogController(Context context,
            LocaleDialogFragment dialogFragment, LanguageAndRegionSettings parentFragment) {
        return new LocaleDialogController(context, dialogFragment, parentFragment);
    }

    class LocaleDialogController implements DialogInterface.OnClickListener {
        private final Context mContext;
        private final int mDialogType;
        private final LocaleStore.LocaleInfo mLocaleInfo;
        private final LocaleStore.LocaleInfo mSelectedLocaleInfo;
        private final int mMenuItemId;
        private final MetricsFeatureProvider mMetricsFeatureProvider;
        private boolean mShowDialogForNotTranslated;

        private LanguageAndRegionSettings mParent;

        LocaleDialogController(
                @NonNull Context context, @NonNull LocaleDialogFragment dialogFragment,
                LanguageAndRegionSettings parentFragment) {
            mContext = context;
            Bundle arguments = dialogFragment.getArguments();
            mDialogType = arguments.getInt(ARG_DIALOG_TYPE);
            mShowDialogForNotTranslated = arguments.getBoolean(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED);
            mLocaleInfo = (LocaleStore.LocaleInfo) arguments.getSerializable(ARG_TARGET_LOCALE);
            mSelectedLocaleInfo = (LocaleStore.LocaleInfo) arguments.getSerializable(
                    ARG_SELECTED_LOCALE);
            mMenuItemId = arguments.getInt(ARG_MENU_ITEM_ID);
            mMetricsFeatureProvider =
                    FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
            mParent = parentFragment;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mDialogType == DIALOG_CONFIRM_SYSTEM_DEFAULT
                || mDialogType == DIALOG_ADD_SYSTEM_LOCALE || mDialogType == DIALOG_REMOVE_LOCALE
                || mDialogType == DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE
                || mDialogType == DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL) {
                int result = Activity.RESULT_CANCELED;
                boolean changed = false;
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    result = Activity.RESULT_OK;
                    changed = true;
                }
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putInt(ARG_DIALOG_TYPE, mDialogType);
                bundle.putInt(ARG_MENU_ITEM_ID, mMenuItemId);
                bundle.putSerializable(ARG_SELECTED_LOCALE, mSelectedLocaleInfo);
                bundle.putSerializable(ARG_TARGET_LOCALE, mLocaleInfo);
                intent.putExtras(bundle);
                intent.putExtra(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED, mShowDialogForNotTranslated);
                mParent.onActivityResult(mDialogType, result, intent);
                mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_CHANGE_LANGUAGE,
                        changed);
            }
            mShouldKeepDialog = false;
        }

        @VisibleForTesting
        DialogContent getDialogContent() {
            DialogContent dialogContent = new DialogContent();
            switch (mDialogType) {
                case DIALOG_ADD_SYSTEM_LOCALE:
                    dialogContent.mTitle = String.format(mContext.getString(
                            R.string.title_system_locale_addition),
                        mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(
                        R.string.desc_system_locale_addition);
                    dialogContent.mPositiveButton = mContext.getString(R.string.add);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                case DIALOG_CONFIRM_SYSTEM_DEFAULT:
                    dialogContent.mTitle = String.format(mContext.getString(
                            R.string.title_change_system_locale), mLocaleInfo.getFullNameNative());
                    LocaleStore.LocaleInfo defaultLocaleInfo = LocaleStore.getLocaleInfo(
                            Locale.getDefault());
                    Locale locale = mLocaleInfo.getLocale();
                    boolean useEnglishAsDefault = !defaultLocaleInfo.isTranslated();
                    String localeLanguage = useEnglishAsDefault
                            ? locale.getDisplayLanguage(Locale.ENGLISH)
                            : locale.getDisplayLanguage();
                    String localeCountry = useEnglishAsDefault
                            ? locale.getDisplayCountry(Locale.ENGLISH)
                            : locale.getDisplayCountry();
                    String dialogMessage = mContext.getString(
                            R.string.desc_notice_device_locale_and_region_settings_change,
                            localeLanguage, localeCountry);
                    dialogContent.mMessage =
                            mMenuItemId == R.id.remove && mSelectedLocaleInfo.toString().equals(
                                    defaultLocaleInfo.toString())
                                    ? mContext.getString(
                                    R.string.dlg_desc_delete_preferred_default_locale,
                                    defaultLocaleInfo.getFullNameNative())
                                    : dialogMessage;
                    dialogContent.mPositiveButton = mContext.getString(
                            R.string.button_label_confirmation_of_system_locale_change);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                case DIALOG_NOT_AVAILABLE_LOCALE:
                    dialogContent.mTitle = mContext.getString(
                            R.string.title_unavailable_locale, mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(R.string.desc_unavailable_locale);
                    dialogContent.mPositiveButton = mContext.getString(R.string.okay);
                    break;
                case DIALOG_REMOVE_LOCALE:
                    boolean changeSystemLanguage = mLocaleInfo.getLocale().toString().equals(
                            Locale.getDefault().toString());
                    LocaleStore.LocaleInfo localeInfo;
                    if (changeSystemLanguage) {
                        localeInfo = LocaleUtils.getUserLocaleList().stream().filter(
                                i -> (i.isTranslated() && !i.getLocale().equals(
                                        Locale.getDefault()))).findFirst().orElse(null);
                        if (localeInfo == null) {
                            mShowDialogForNotTranslated = true;
                        }
                    }
                    dialogContent.mTitle = mContext.getString(
                        R.string.dlg_title_delete_preferred_locale,
                        mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(
                            R.string.dlg_desc_delete_preferred_locale,
                            mLocaleInfo.getFullNameNative());
                    dialogContent.mPositiveButton = mContext.getString(R.string.remove);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                case DIALOG_REMOVE_AND_CHANGE_SYSTEM_LOCALE:
                    dialogContent.mTitle = mContext.getString(R.string.title_change_system_locale,
                        mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(
                        R.string.dlg_desc_delete_preferred_locale,
                        mSelectedLocaleInfo.getFullNameNative());
                    dialogContent.mPositiveButton = mContext.getString(
                        R.string.button_label_confirmation_of_system_locale_change);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                case DIALOG_NOT_AVAILABLE_LOCALE_WITH_CANCEL:
                    dialogContent.mTitle = mContext.getString(
                        R.string.title_unavailable_locale, mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(R.string.desc_unavailable_locale);
                    dialogContent.mPositiveButton = mContext.getString(R.string.okay);
                    dialogContent.mNegativeButton = mContext.getString(R.string.cancel);
                    break;
                default:
                    break;
            }
            return dialogContent;
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
