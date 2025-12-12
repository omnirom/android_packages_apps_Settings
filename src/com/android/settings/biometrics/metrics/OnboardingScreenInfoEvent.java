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

package com.android.settings.biometrics.metrics;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;


/**
 * Represents OnboardingScreenInfo proto message. See Settings/proto/biometrics_onboarding.proto.
 */
public class OnboardingScreenInfoEvent implements Parcelable {
    private final int mScreen;
    private final long mDuration;
    private final int[] mActions;

    public OnboardingScreenInfoEvent(int screen, long duration, int[] actions) {
        mScreen = screen;
        mDuration = duration;
        mActions = actions;
    }

    protected OnboardingScreenInfoEvent(Parcel in) {
        mScreen = in.readInt();
        mDuration = in.readLong();
        mActions = in.createIntArray();
    }

    public static final Creator<OnboardingScreenInfoEvent> CREATOR = new Creator<>() {
        @Override
        public OnboardingScreenInfoEvent createFromParcel(Parcel in) {
            return new OnboardingScreenInfoEvent(in);
        }

        @Override
        public OnboardingScreenInfoEvent[] newArray(int size) {
            return new OnboardingScreenInfoEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mScreen);
        dest.writeLong(mDuration);
        dest.writeIntArray(mActions);
    }

    public int getScreen() {
        return mScreen;
    }

    public long getDuration() {
        return mDuration;
    }

    public int[] getActions() {
        return mActions;
    }

    @Override
    public String toString() {
        return "OnboardingScreenInfoEvent{"
                + "screen=" + mScreen
                + ", duration=" + mDuration
                + ", action=" + Arrays.toString(mActions)
                + "}";
    }
}
