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

import static com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.accessibility.Flags;
import com.android.settings.accessibility.notification.NotificationHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for {@link AccessibilitySurveyNotificationJobService}
 */
@RunWith(RobolectricTestRunner.class)
public class AccessibilitySurveyNotificationJobServiceTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private Context mContext;
    @Mock
    private JobScheduler mMockJobScheduler;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private NotificationHelper mNotificationHelper;
    @Captor
    private ArgumentCaptor<JobInfo> mJobInfoCaptor;
    private AccessibilitySurveyNotificationJobService mJobService;

    private static final int TEST_PAGE_ID = 22222;
    private static final int TEST_DELAY_MILLIS = 500;

    @Before
    public void setUp() {
        mJobService = new AccessibilitySurveyNotificationJobService(mNotificationHelper);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mMockJobScheduler);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getApplicationContext()).thenReturn(mContext);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void scheduleJob_whenJobAlreadyPending_notSchedule() {
        int jobId = mJobService.getJobId(TEST_PAGE_ID);
        when(mMockJobScheduler.getPendingJob(jobId)).thenReturn(mock(JobInfo.class));

        mJobService.scheduleJob(mContext, TEST_PAGE_ID, TEST_DELAY_MILLIS);

        verify(mMockJobScheduler, never()).schedule(any(JobInfo.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void scheduleJob_whenNewJob_scheduleSuccessfullyWithCorrectParameters() {
        int expectedJobId = mJobService.getJobId(TEST_PAGE_ID);
        when(mMockJobScheduler.getPendingJob(expectedJobId)).thenReturn(null);
        when(mMockJobScheduler.schedule(any(JobInfo.class)))
                .thenReturn(JobScheduler.RESULT_SUCCESS);

        mJobService.scheduleJob(mContext, TEST_PAGE_ID, TEST_DELAY_MILLIS);

        verify(mMockJobScheduler, times(1))
                .schedule(mJobInfoCaptor.capture());
        JobInfo scheduledJobInfo = mJobInfoCaptor.getValue();
        assertThat(scheduledJobInfo).isNotNull();
        assertThat(scheduledJobInfo.getId()).isEqualTo(expectedJobId);
        assertThat(scheduledJobInfo.getMinLatencyMillis()).isEqualTo(TEST_DELAY_MILLIS);
        assertThat(scheduledJobInfo.isPeriodic()).isFalse();
        assertThat(scheduledJobInfo.isPersisted()).isFalse();
        assertThat(scheduledJobInfo.getService().getClassName()).isEqualTo(
                AccessibilitySurveyNotificationJobService.class.getName());
        PersistableBundle extras = scheduledJobInfo.getExtras();
        assertThat(extras).isNotNull();
        assertThat(extras.getInt(EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN))
                .isEqualTo(TEST_PAGE_ID);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void scheduleJob_disableHaTS_notSchedule() {
        mJobService.scheduleJob(mContext, TEST_PAGE_ID, TEST_DELAY_MILLIS);

        verify(mMockJobScheduler, never()).schedule(any(JobInfo.class));
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartJob_withValidPageId_showSurveyNotification() {
        PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_PAGE_ID, TEST_PAGE_ID);
        JobParameters mockParams = getTestJobParameters(TEST_PAGE_ID, extras);

        boolean result = mJobService.onStartJob(mockParams);

        assertThat(result).isFalse();
        verify(mNotificationHelper).showSurveyNotification(TEST_PAGE_ID);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartJob_withUnknownPageId_notShowSurveyNotification() {
        int unknownPageId = SettingsEnums.PAGE_UNKNOWN;
        PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_PAGE_ID, unknownPageId);
        JobParameters mockParams = getTestJobParameters(unknownPageId, extras);

        boolean result = mJobService.onStartJob(mockParams);

        assertThat(result).isFalse();
        verify(mNotificationHelper, never()).showSurveyNotification(unknownPageId);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartJob_withMissingPageIdInExtras_notShowSurveyNotification() {
        PersistableBundle extras = new PersistableBundle();
        JobParameters mockParams = getTestJobParameters(TEST_PAGE_ID, extras);

        boolean result = mJobService.onStartJob(mockParams);

        assertThat(result).isFalse();
        verify(mNotificationHelper, never()).showSurveyNotification(TEST_PAGE_ID);
    }


    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStartJob_withNullExtras_notShowSurveyNotification() {
        JobParameters mockParams = getTestJobParameters(TEST_PAGE_ID, null);

        boolean result = mJobService.onStartJob(mockParams);

        assertThat(result).isFalse();
        verify(mNotificationHelper, never()).showSurveyNotification(TEST_PAGE_ID);
    }


    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    public void onStopJob_returnTrue() {
        int stopReason = JobParameters.STOP_REASON_TIMEOUT;
        JobParameters mockParams = getTestJobParametersForStop(TEST_PAGE_ID, stopReason);

        boolean result = mJobService.onStopJob(mockParams);

        assertThat(result).isTrue();
    }

    private JobParameters getTestJobParameters(int pageId, PersistableBundle extras) {
        JobParameters mockParams = mock(JobParameters.class);
        when(mockParams.getJobId()).thenReturn(mJobService.getJobId(pageId));
        when(mockParams.getExtras()).thenReturn(extras);
        return mockParams;
    }

    private JobParameters getTestJobParametersForStop(int pageId, int stopReason) {
        JobParameters mockParams = mock(JobParameters.class);
        when(mockParams.getJobId()).thenReturn(mJobService.getJobId(pageId));
        when(mockParams.getStopReason()).thenReturn(stopReason);
        when(mockParams.getExtras()).thenReturn(PersistableBundle.EMPTY);
        return mockParams;
    }
}
