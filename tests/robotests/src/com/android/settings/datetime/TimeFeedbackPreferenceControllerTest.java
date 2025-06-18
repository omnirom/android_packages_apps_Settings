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

package com.android.settings.datetime;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;

import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TimeFeedbackPreferenceControllerTest {

    private static final String PACKAGE = "com.android.settings.test";
    private static final String TEST_INTENT_URI =
            "intent:#Intent;"
                    + "action=com.android.settings.test.LAUNCH_USER_FEEDBACK;"
                    + "package=com.android.settings.test.target;"
                    + "end";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private PackageManager mMockPackageManager;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = spy(Robolectric.setupActivity(Activity.class));
    }

    @Test
    @EnableFlags({Flags.FLAG_DATETIME_FEEDBACK})
    public void emptyIntentUri_controllerNotAvailable() {
        String emptyIntentUri = "";
        TimeFeedbackPreferenceController controller =
                new TimeFeedbackPreferenceController(mContext, mContext.getPackageManager(),
                        "test_key", emptyIntentUri);
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @DisableFlags({Flags.FLAG_DATETIME_FEEDBACK})
    public void datetimeFeedbackDisabled_controllerNotAvailable() {
        TimeFeedbackPreferenceController controller =
                new TimeFeedbackPreferenceController(
                        mContext, mContext.getPackageManager(), "test_key", TEST_INTENT_URI);
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags({Flags.FLAG_DATETIME_FEEDBACK})
    public void validIntentUri_targetHandlerNotFound_returnsConditionallyUnavailable() {
        when(mMockPackageManager.resolveActivity(any(), anyInt())).thenReturn(null);

        TimeFeedbackPreferenceController controller =
                new TimeFeedbackPreferenceController(mContext, mMockPackageManager, "test_key",
                        TEST_INTENT_URI);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags({Flags.FLAG_DATETIME_FEEDBACK})
    public void validIntentUri_targetHandlerAvailable_returnsAvailable() {
        when(mMockPackageManager.resolveActivity(any(), anyInt())).thenReturn(
                createDummyResolveInfo());

        TimeFeedbackPreferenceController controller =
                new TimeFeedbackPreferenceController(mContext, mMockPackageManager, "test_key",
                        TEST_INTENT_URI);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @EnableFlags({Flags.FLAG_DATETIME_FEEDBACK})
    public void clickPreference() {
        Preference preference = new Preference(mContext);

        TimeFeedbackPreferenceController controller =
                new TimeFeedbackPreferenceController(mContext, mContext.getPackageManager(),
                        "test_key", TEST_INTENT_URI);

        // Click a preference that's not controlled by this controller.
        preference.setKey("fake_key");
        assertThat(controller.handlePreferenceTreeClick(preference)).isFalse();

        // Check for startActivity() call.
        verify(mContext, never()).startActivity(any());

        // Click a preference controlled by this controller.
        preference.setKey(controller.getPreferenceKey());
        assertThat(controller.handlePreferenceTreeClick(preference)).isTrue();

        // Check for startActivity() call.
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        Intent actualIntent = intentCaptor.getValue();
        assertThat(actualIntent.getAction()).isEqualTo(
                "com.android.settings.test.LAUNCH_USER_FEEDBACK");
        assertThat(actualIntent.getPackage()).isEqualTo("com.android.settings.test.target");
    }

    private static ResolveInfo createDummyResolveInfo() {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.packageName = PACKAGE;
        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.applicationInfo = applicationInfo;
        activityInfo.name = "TestActivity";

        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        return resolveInfo;
    }
}
