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

package com.android.settings.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

/**
 * A {@link TextInputEditText} that can schedule the soft input keyboard to be shown.
 *
 * <p>To have the keyboard appear, simply call {@link #scheduleShowSoftInput()}.
 *
 * <p>A typical use for this view is to have an EditText that shows the keyboard right away when
 * entering a new Activity or launching a Dialog.
 *
 * <p>See b/74014088 and b/63942122 for more information.
 */
public class ImeAwareTextInputEditText extends TextInputEditText {
    private boolean mHasPendingShowSoftInputRequest;
    private final Runnable mRunShowSoftInputIfNecessary = this::showSoftInputIfNecessary;

    public ImeAwareTextInputEditText(@NonNull Context context) {
        super(context, null);
    }

    public ImeAwareTextInputEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ImeAwareTextInputEditText(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * This method is called back by the system when the system is about to establish a connection
     * to
     * the current input method.
     *
     * <p>This is a good and reliable signal to schedule a pending task to call {@link
     * InputMethodManager#showSoftInput(View, int)}.
     *
     * @param editorInfo context about the text input field.
     * @return {@link InputConnection} to be passed to the input method.
     */
    @Nullable
    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo editorInfo) {
        final InputConnection ic = super.onCreateInputConnection(editorInfo);
        if (mHasPendingShowSoftInputRequest) {
            removeCallbacks(mRunShowSoftInputIfNecessary);
            post(mRunShowSoftInputIfNecessary);
        }
        return ic;
    }

    private void showSoftInputIfNecessary() {
        if (mHasPendingShowSoftInputRequest) {
            InputMethodManager imm =
                    (InputMethodManager) getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(this, 0);
            mHasPendingShowSoftInputRequest = false;
        }
    }

    public void scheduleShowSoftInput() {
        InputMethodManager imm =
                (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive(this)) {
            // This means that ImeAwareEditText is already connected to the IME.
            // InputMethodManager#showSoftInput() is guaranteed to pass client-side focus check.
            mHasPendingShowSoftInputRequest = false;
            removeCallbacks(mRunShowSoftInputIfNecessary);
            imm.showSoftInput(this, 0);
            return;
        }

        // Otherwise, InputMethodManager#showSoftInput() should be deferred after
        // onCreateInputConnection().
        mHasPendingShowSoftInputRequest = true;
    }
}
