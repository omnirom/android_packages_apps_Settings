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

package com.android.settings.accessibility.actionbar;

import static com.android.settings.accessibility.actionbar.FeedbackMenuController.MENU_FEEDBACK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.testing.EmptyFragmentActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.android.settings.accessibility.FeedbackManager;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link FeedbackMenuController} */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@RunWith(RobolectricTestRunner.class)
public class FeedbackMenuControllerTest {
    private static final String PACKAGE_NAME = "com.android.test";
    private static final String DEFAULT_CATEGORY = "default category";

    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public ActivityScenarioRule<EmptyFragmentActivity> mActivityScenario =
            new ActivityScenarioRule<>(EmptyFragmentActivity.class);

    private FragmentActivity mActivity;
    private InstrumentedPreferenceFragment mHost;
    private FeedbackManager mFeedbackManager;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private Menu mMenu;
    @Mock
    private MenuItem mMenuItem;

    @Before
    public void setUp() {
        mActivityScenario.getScenario().onActivity(activity -> mActivity = activity);
        mHost = spy(new InstrumentedPreferenceFragment() {
            @Override
            public int getMetricsCategory() {
                return 0;
            }
        });
        when(mHost.getActivity()).thenReturn(mActivity);
        when(mMenu.add(anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(mMenuItem);
        when(mMenuItem.getItemId()).thenReturn(MENU_FEEDBACK);
        mFeedbackManager = new FeedbackManager(mActivity, PACKAGE_NAME, DEFAULT_CATEGORY);
    }

    @Test
    public void init_withPageId_shouldAttachToLifecycle() {
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);

        FeedbackMenuController.init(mHost, SettingsEnums.ACCESSIBILITY);

        verify(mLifecycle).addObserver(any(FeedbackMenuController.class));
    }

    @Test
    public void init_withFeedbackManager_shouldAttachToLifecycle() {
        when(mHost.getSettingsLifecycle()).thenReturn(mLifecycle);

        FeedbackMenuController.init(mHost, mFeedbackManager);

        verify(mLifecycle).addObserver(any(FeedbackMenuController.class));
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void onCreateOptionsMenu_enableLowVisionGenericFeedback_shouldAddSendFeedbackMenu() {
        FeedbackMenuController.init(mHost, mFeedbackManager);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu).add(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void onCreateOptionsMenu_disableLowVisionGenericFeedback_shouldNotAddSendFeedbackMenu() {
        FeedbackMenuController.init(mHost, mFeedbackManager);

        mHost.getSettingsLifecycle().onCreateOptionsMenu(mMenu, /* inflater= */ null);

        verify(mMenu, never()).add(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void onOptionsItemSelected_enableLowVisionGenericFeedback_shouldStartSendFeedback() {
        FeedbackMenuController.init(mHost, mFeedbackManager);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_BUG_REPORT);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    public void onOptionsItemSelected_disableLowVisionGenericFeedback_shouldNotStartSendFeedback() {
        FeedbackMenuController.init(mHost, mFeedbackManager);

        mHost.getSettingsLifecycle().onOptionsItemSelected(mMenuItem);

        Intent intent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(intent).isNull();
    }
}
