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

import static com.android.settings.accessibility.FeedbackManager.CATEGORY_TAG_EXTRA;
import static com.android.settings.accessibility.FeedbackManager.TRIGGER_ID_EXTRA;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import com.android.server.accessibility.Flags;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link FeedbackManager}. */
@RunWith(RobolectricTestRunner.class)
public class FeedbackManagerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String PACKAGE_NAME = "test.feedback.package";
    private static final String DEFAULT_CATEGORY = "default category";
    private static final String DEFAULT_TRIGGER_ID = "default triggerId";

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void isAvailable_enableLowVisionGenericFeedbackWithValidParams_returnsTrue() {
        FeedbackManager feedbackManager =
                new FeedbackManager(PACKAGE_NAME, DEFAULT_CATEGORY, DEFAULT_TRIGGER_ID);

        assertThat(feedbackManager.isAvailable()).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void isAvailable_disableLowVisionGenericFeedback_returnsFalse() {
        FeedbackManager feedbackManager =
                new FeedbackManager(PACKAGE_NAME, DEFAULT_CATEGORY, DEFAULT_TRIGGER_ID);

        assertThat(feedbackManager.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void isAvailable_withNullCategory_returnsFalse() {
        FeedbackManager feedbackManager =
                new FeedbackManager(PACKAGE_NAME, /* category= */ null,
                        DEFAULT_TRIGGER_ID);

        assertThat(feedbackManager.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void isAvailable_withNullReporterPackage_returnsFalse() {
        FeedbackManager feedbackManager =
                new FeedbackManager(/* reporterPackage= */ null, DEFAULT_CATEGORY,
                        DEFAULT_TRIGGER_ID);

        assertThat(feedbackManager.isAvailable()).isFalse();
    }

    @Test
    public void getFeedbackIntent_withValidParams_returnsExpectedIntent() {
        FeedbackManager feedbackManager =
                new FeedbackManager(PACKAGE_NAME, DEFAULT_CATEGORY, DEFAULT_TRIGGER_ID);

        Intent startedIntent = feedbackManager.getFeedbackIntent();
        assertThat(startedIntent.getAction()).isEqualTo(Intent.ACTION_BUG_REPORT);
        assertThat(startedIntent.getPackage()).isEqualTo(PACKAGE_NAME);
        Bundle extras = startedIntent.getExtras();
        assertThat(extras).isNotNull();
        assertThat(extras.getString(CATEGORY_TAG_EXTRA)).isEqualTo(DEFAULT_CATEGORY);
        assertThat(extras.getString(TRIGGER_ID_EXTRA)).isEqualTo(DEFAULT_TRIGGER_ID);
    }
}
