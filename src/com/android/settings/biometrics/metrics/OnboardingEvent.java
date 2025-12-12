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

import androidx.annotation.NonNull;

import com.android.settings.biometrics.BiometricsOnboardingProto;
import com.android.settings.biometrics.BiometricsOnboardingProto.OnboardingScreenInfo;
import com.android.settings.biometrics.BiometricsOnboardingProto.SettingsBiometricsOnboarding;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents SettingsBiometricsOnboarding proto message.
 * See Settings/proto/biometrics_onboarding.proto.
 */
public class OnboardingEvent implements Parcelable {
    private int mModality;
    private int mFromSource;
    private int mUserId;
    private int mEnrolledCount;
    private long mDuration;
    private int mCapybaraStatus;
    private int mResultCode;
    private int mErrorCode;
    private List<OnboardingScreenInfoEvent> mScreenInfos = new ArrayList<>();

    public OnboardingEvent() {
    }

    public OnboardingEvent(SettingsBiometricsOnboarding message) {
        mModality = message.getModality().getNumber();
        mFromSource = message.getFromSource().getNumber();
        mUserId = message.getUser();
        mEnrolledCount = message.getEnrolledCount();
        mDuration = message.getDurationMillis();
        mCapybaraStatus = message.getCapybaraStatus();
        mResultCode = message.getResultCode().getNumber();
        mErrorCode = message.getErrorCode();
        final List<OnboardingScreenInfo> infoList =
                message.getOnboardingScreenInfoList().getInfoListList();
        for (OnboardingScreenInfo info : infoList) {
            mScreenInfos.add(new OnboardingScreenInfoEvent(
                    info.getOnboardingScreen().getNumber(),
                    info.getDwellTimeMillis(),
                    info.getOnboardingActionsList().stream().mapToInt(
                            BiometricsOnboardingProto.OnboardingAction::getNumber).toArray()
            ));
        }
    }

    protected OnboardingEvent(Parcel in) {
        mModality = in.readInt();
        mFromSource = in.readInt();
        mUserId = in.readInt();
        mEnrolledCount = in.readInt();
        mDuration = in.readLong();
        mCapybaraStatus = in.readInt();
        mResultCode = in.readInt();
        mErrorCode = in.readInt();
        in.readTypedList(mScreenInfos, OnboardingScreenInfoEvent.CREATOR);
    }

    public int getModality() {
        return mModality;
    }

    public void setModality(int modality) {
        mModality = modality;
    }

    public int getFromSource() {
        return mFromSource;
    }

    public void setFromSource(int fromSource) {
        mFromSource = fromSource;
    }

    public int getUserId() {
        return mUserId;
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    public int getEnrolledCount() {
        return mEnrolledCount;
    }

    public void setEnrolledCount(int enrolledCount) {
        mEnrolledCount = enrolledCount;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        mDuration = duration;
    }

    public int getCapybaraStatus() {
        return mCapybaraStatus;
    }

    public void setCapybaraStatus(int capybaraStatus) {
        mCapybaraStatus = capybaraStatus;
    }

    public int getResultCode() {
        return mResultCode;
    }

    public void setResultCode(int resultCode) {
        mResultCode = resultCode;
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    public void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }

    @NonNull
    public List<OnboardingScreenInfoEvent> getScreenInfos() {
        return mScreenInfos;
    }

    /** Add screen info */
    public void addScreenInfo(@NonNull OnboardingScreenInfoEvent screenInfo) {
        mScreenInfos.add(screenInfo);
    }

    public static final Creator<OnboardingEvent> CREATOR = new Creator<>() {
        @Override
        public OnboardingEvent createFromParcel(Parcel in) {
            return new OnboardingEvent(in);
        }

        @Override
        public OnboardingEvent[] newArray(int size) {
            return new OnboardingEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mModality);
        dest.writeInt(mFromSource);
        dest.writeInt(mUserId);
        dest.writeInt(mEnrolledCount);
        dest.writeLong(mDuration);
        dest.writeInt(mCapybaraStatus);
        dest.writeInt(mResultCode);
        dest.writeInt(mErrorCode);
        dest.writeTypedList(mScreenInfos);
    }

    @Override
    public String toString() {
        return "BiometricsOnboardingEvent{"
                + "mModality=" + mModality
                + ", mSource=" + mFromSource
                + ", mUserId=" + mUserId
                + ", mEnrolledCount=" + mEnrolledCount
                + ", mTotalTime=" + mDuration
                + ", mCapybaraStatus=" + mCapybaraStatus
                + ", mResultCode=" + mResultCode
                + ", mErrorCode=" + mErrorCode
                + ", mScreenInfos=" + screenInfosToString()
                + "}";
    }

    private String screenInfosToString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (OnboardingScreenInfoEvent screenInfo : mScreenInfos) {
            sb.append(screenInfo.toString());
            sb.append(", ");
        }
        return sb.append("}").toString();
    }
}
