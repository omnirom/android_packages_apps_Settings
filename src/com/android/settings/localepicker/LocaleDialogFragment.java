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
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.CustomDialogHelper;

/**
 * Create a dialog for system locale events.
 */
public class LocaleDialogFragment extends InstrumentedDialogFragment {
    private static final String TAG = LocaleDialogFragment.class.getSimpleName();

    static final int DIALOG_CONFIRM_SYSTEM_DEFAULT = 1;
    static final int DIALOG_NOT_AVAILABLE_LOCALE = 2;
    static final int DIALOG_ADD_SYSTEM_LOCALE = 3;

    static final String ARG_DIALOG_TYPE = "arg_dialog_type";
    static final String ARG_TARGET_LOCALE = "arg_target_locale";
    static final String ARG_SHOW_DIALOG = "arg_show_dialog";
    static final String ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED = "arg_show_dialog_for_not_translated";

    private boolean mShouldKeepDialog;
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
                return SettingsEnums.DIALOG_SYSTEM_LOCALE_UNAVAILABLE;
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
            if ((type == DIALOG_CONFIRM_SYSTEM_DEFAULT || type == DIALOG_ADD_SYSTEM_LOCALE)
                    && !mShouldKeepDialog) {
                dismiss();
            }
        }

        mShouldKeepDialog = true;
        LocaleListEditor parentFragment = (LocaleListEditor) getParentFragment();
        LocaleDialogController controller = getLocaleDialogController(getContext(), this,
                parentFragment);
        Dialog dialog = createDialog(getContext(), controller);
        dialog.setCanceledOnTouchOutside(false);
        getOnBackInvokedDispatcher(dialog).registerOnBackInvokedCallback(PRIORITY_DEFAULT,
                mBackCallback);
        dialog.setOnDismissListener(dialogInterface -> {
            getOnBackInvokedDispatcher(dialog).unregisterOnBackInvokedCallback(
                    mBackCallback);
        });

        return dialog;
    }

    private Dialog createDialog(Context context, LocaleDialogController controller) {
        CustomDialogHelper dialogHelper = new CustomDialogHelper(context);
        LocaleDialogController.DialogContent dialogContent = controller.getDialogContent();
        dialogHelper.setIcon(context.getDrawable(R.drawable.ic_settings_language_32dp))
                .setTitle(dialogContent.mTitle)
                .setMessage(dialogContent.mMessage)
                .setIconPadding(0,
                        context.getResources().getDimensionPixelSize(
                                R.dimen.locale_picker_dialog_icon_padding),
                        0, 0)
                .setTitlePadding(0,
                        context.getResources().getDimensionPixelSize(
                                R.dimen.locale_picker_dialog_title_padding),
                        0,
                        context.getResources().getDimensionPixelSize(
                                R.dimen.locale_picker_dialog_title_padding))
                .setMessagePadding(context.getResources().getDimensionPixelSize(
                                R.dimen.locale_picker_dialog_message_padding_left_right), 0,
                        context.getResources().getDimensionPixelSize(
                                R.dimen.locale_picker_dialog_message_padding_left_right),
                        context.getResources().getDimensionPixelSize(
                                R.dimen.locale_picker_dialog_message_padding_bottom))
                .setPositiveButton(dialogContent.mPositiveButton,
                        view -> {
                            controller.onClick(dialogHelper.getDialog(),
                                    DialogInterface.BUTTON_POSITIVE);
                            dialogHelper.getDialog().dismiss();
                        });
        if (dialogContent.mNegativeButton != 0) {
            dialogHelper.setBackButton(dialogContent.mNegativeButton, view -> {
                controller.onClick(dialogHelper.getDialog(), DialogInterface.BUTTON_NEGATIVE);
                dialogHelper.getDialog().dismiss();
            });
        }
        return dialogHelper.getDialog();
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
    public @NonNull OnBackInvokedDispatcher getOnBackInvokedDispatcher(@NonNull Dialog dialog) {
        if (mBackDispatcher != null) {
            return mBackDispatcher;
        } else {
            return dialog.getOnBackInvokedDispatcher();
        }
    }

    @VisibleForTesting
    LocaleDialogController getLocaleDialogController(Context context,
            LocaleDialogFragment dialogFragment, LocaleListEditor parentFragment) {
        return new LocaleDialogController(context, dialogFragment, parentFragment);
    }

    class LocaleDialogController implements DialogInterface.OnClickListener {
        private final Context mContext;
        private final int mDialogType;
        private final LocaleStore.LocaleInfo mLocaleInfo;
        private final MetricsFeatureProvider mMetricsFeatureProvider;
        private final boolean mShowDialogForNotTranslated;

        private LocaleListEditor mParent;

        LocaleDialogController(
                @NonNull Context context, @NonNull LocaleDialogFragment dialogFragment,
                LocaleListEditor parentFragment) {
            mContext = context;
            Bundle arguments = dialogFragment.getArguments();
            mDialogType = arguments.getInt(ARG_DIALOG_TYPE);
            mShowDialogForNotTranslated = arguments.getBoolean(ARG_SHOW_DIALOG_FOR_NOT_TRANSLATED);
            mLocaleInfo = (LocaleStore.LocaleInfo) arguments.getSerializable(ARG_TARGET_LOCALE);
            mMetricsFeatureProvider =
                    FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
            mParent = parentFragment;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mDialogType == DIALOG_CONFIRM_SYSTEM_DEFAULT
                    || mDialogType == DIALOG_ADD_SYSTEM_LOCALE) {
                int result = Activity.RESULT_CANCELED;
                boolean changed = false;
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    result = Activity.RESULT_OK;
                    changed = true;
                }
                Intent intent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putInt(ARG_DIALOG_TYPE, mDialogType);
                bundle.putSerializable(LocaleDialogFragment.ARG_TARGET_LOCALE, mLocaleInfo);
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
                case DIALOG_CONFIRM_SYSTEM_DEFAULT:
                    dialogContent.mTitle = String.format(mContext.getString(
                            R.string.title_change_system_locale), mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(
                            R.string.desc_notice_device_locale_settings_change);
                    dialogContent.mPositiveButton =
                            R.string.button_label_confirmation_of_system_locale_change;
                    dialogContent.mNegativeButton = R.string.cancel;
                    break;
                case DIALOG_NOT_AVAILABLE_LOCALE:
                    dialogContent.mTitle = String.format(mContext.getString(
                            R.string.title_unavailable_locale), mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(R.string.desc_unavailable_locale);
                    dialogContent.mPositiveButton = R.string.okay;
                    break;
                case DIALOG_ADD_SYSTEM_LOCALE:
                    dialogContent.mTitle = String.format(mContext.getString(
                                    R.string.title_system_locale_addition),
                            mLocaleInfo.getFullNameNative());
                    dialogContent.mMessage = mContext.getString(
                            R.string.desc_system_locale_addition);
                    dialogContent.mPositiveButton = R.string.add;
                    dialogContent.mNegativeButton = R.string.cancel;
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
            int mPositiveButton = 0;
            int mNegativeButton = 0;
        }
    }
}
