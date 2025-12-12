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

package com.android.settings.bluetooth;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef(
        value = {
            BluetoothDiagnosisEntryPoint.ENTRY_POINT_UNSPECIFIED,
            BluetoothDiagnosisEntryPoint.ENTRY_POINT_CAN_NOT_FIND,
            BluetoothDiagnosisEntryPoint.ENTRY_POINT_CAN_NOT_PAIR,
            BluetoothDiagnosisEntryPoint.ENTRY_POINT_CAN_NOT_CONNECT,
            BluetoothDiagnosisEntryPoint.ENTRY_POINT_CONTEXTUAL_DIAGNOSTIC,
            BluetoothDiagnosisEntryPoint.ENTRY_POINT_NON_CONTEXTUAL_DIAGNOSTIC,
        },
        open = true)
public @interface BluetoothDiagnosisEntryPoint {
    /** The entry point is not specified. */
    int ENTRY_POINT_UNSPECIFIED = 0;

    /** The entry point is the can-not-find entry point. */
    int ENTRY_POINT_CAN_NOT_FIND = 1;

    /** The entry point is the can-not-pair entry point. */
    int ENTRY_POINT_CAN_NOT_PAIR = 2;

    /** The entry point is the can-not-connect entry point. */
    int ENTRY_POINT_CAN_NOT_CONNECT = 3;

    /** The entry point is the contextual diagnostic entry point. */
    int ENTRY_POINT_CONTEXTUAL_DIAGNOSTIC = 4;

    /** The entry point is the non-contextual diagnostic entry point. */
    int ENTRY_POINT_NON_CONTEXTUAL_DIAGNOSTIC = 5;
}
