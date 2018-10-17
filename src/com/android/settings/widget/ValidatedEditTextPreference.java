/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settingslib.CustomEditTextPreference;

/**
 * {@code EditTextPreference} that supports input validation.
 */
public class ValidatedEditTextPreference extends CustomEditTextPreference {

    public interface Validator {
        boolean isTextValid(String value);
    }

    private final EditTextWatcher mTextWatcher = new EditTextWatcher();
    private Validator mValidator;
    private boolean mIsPassword;
    private boolean mIsSummaryPassword;

    public ValidatedEditTextPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public ValidatedEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ValidatedEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ValidatedEditTextPreference(Context context) {
        super(context);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        final EditText editText = view.findViewById(android.R.id.edit);
        if (editText != null && !TextUtils.isEmpty(editText.getText())) {
            editText.setSelection(editText.getText().length());
        }
        if (mValidator != null && editText != null) {
            editText.removeTextChangedListener(mTextWatcher);
            if (mIsPassword) {
                editText.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                editText.setMaxLines(1);
            }
            editText.addTextChangedListener(mTextWatcher);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView textView = (TextView) holder.findViewById(android.R.id.summary);
        if (textView == null) {
            return;
        }
        if (mIsSummaryPassword) {
            textView.setInputType(
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    public void setIsPassword(boolean isPassword) {
        mIsPassword = isPassword;
    }

    public void setIsSummaryPassword(boolean isPassword) {
        mIsSummaryPassword = isPassword;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public boolean isPassword() {
        return mIsPassword;
    }

    public void setValidator(Validator validator) {
        mValidator = validator;
    }

    private class EditTextWatcher implements TextWatcher {
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            final EditText editText = getEditText();
            if (mValidator != null && editText != null) {
                final AlertDialog dialog = (AlertDialog) getDialog();
                final boolean valid = mValidator.isTextValid(editText.getText().toString());
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(valid);
            }
        }
    }

}
