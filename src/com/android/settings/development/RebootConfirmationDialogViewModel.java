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

package com.android.settings.development;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

/**
 * {@link ViewModel} for the reboot confirmation dialog.
 *
 * This class holds the data necessary to display a confirmation dialog for reboots.
 */
public class RebootConfirmationDialogViewModel extends ViewModel {
    @Nullable
    private RebootConfirmationDialogHost mHost = null;
    private int mMessageId;
    private int mCancelButtonId;

    @Nullable
    public RebootConfirmationDialogHost getHost() {
        return mHost;
    }

    public void setHost(RebootConfirmationDialogHost mHost) {
        this.mHost = mHost;
    }

    public int getMessageId() {
        return mMessageId;
    }

    public void setMessageId(int mMessageId) {
        this.mMessageId = mMessageId;
    }

    public int getCancelButtonId() {
        return mCancelButtonId;
    }

    public void setCancelButtonId(int mCancelButtonId) {
        this.mCancelButtonId = mCancelButtonId;
    }
}
