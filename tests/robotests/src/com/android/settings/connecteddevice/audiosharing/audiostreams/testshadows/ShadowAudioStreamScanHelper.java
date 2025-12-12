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

package com.android.settings.connecteddevice.audiosharing.audiostreams.testshadows;

import com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamScanHelper;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@Implements(value = AudioStreamScanHelper.class, callThroughByDefault = true)
public class ShadowAudioStreamScanHelper {
    private static AudioStreamScanHelper sMockHelper;

    public static void setUseMock(AudioStreamScanHelper mockAudioStreamScanHelper) {
        sMockHelper = mockAudioStreamScanHelper;
    }

    /** Reset static fields */
    @Resetter
    public static void reset() {
        sMockHelper = null;
    }

    /** Starts scanning */
    @Implementation
    public void startScanning() {
        sMockHelper.startScanning();
    }

    /** Stops scanning */
    @Implementation
    public void stopScanning() {
        sMockHelper.stopScanning();
    }

}
