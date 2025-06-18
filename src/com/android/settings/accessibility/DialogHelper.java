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

package com.android.settings.accessibility;

/**
 * The {@code DialogHelper} interface provides methods for displaying dialogs.
 * It helps the dialog delegate to show the dialog, and will be injected to the dialog delegate.
 */
public interface DialogHelper {
    /**
     * Shows a dialog with the specified dialog ID.
     *
     * @param dialogId The ID of the dialog to display.
     */
    void showDialog(int dialogId);
}
