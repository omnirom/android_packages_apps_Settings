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

package com.android.settings.testutils;

import java.time.Instant;
import java.time.InstantSource;

/** A fake {@link InstantSource} for testing. */
public class FakeInstantSource implements InstantSource {
    private long mNowMillis = 441644400000L;

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(mNowMillis);
    }

    /** Simulates passing time by the given number of milliseconds. */
    public void advanceByMillis(long millis) {
        mNowMillis += millis;
    }
}
