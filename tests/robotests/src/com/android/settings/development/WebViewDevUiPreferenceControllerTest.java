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

package com.android.settings.development;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.webview.WebViewUpdateServiceWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                ShadowToast.class,
        })
public class WebViewDevUiPreferenceControllerTest {
    private static final String CURRENT_WEBVIEW_PROVIDER = "current.webview.provider";
    private static final String DEVTOOLS_ACTION = "com.android.webview.SHOW_DEV_UI";

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private Preference mPreference;
    @Mock
    private WebViewUpdateServiceWrapper mWebViewUpdateServiceWrapper;

    private Context mContext;
    private WebViewDevUiPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mController = new WebViewDevUiPreferenceController(
                mContext, mWebViewUpdateServiceWrapper);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void handlePreferenceTreeClick_notWebViewDevUiPreference_shouldReturnFalse() {
        when(mPreference.getKey()).thenReturn("Some random key");

        final boolean isHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(isHandled).isFalse();
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
        verify(mContext, never()).startActivity(argThat(expectedIntent()));
    }

    @Test
    public void handlePreferenceTreeClick_webViewDevUiAvailable_startsActivity() {
        final String preferenceKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(preferenceKey);
        PackageInfo packageInfo = webviewProviderPackage();
        when(mWebViewUpdateServiceWrapper.getCurrentWebViewPackage()).thenReturn(packageInfo);
        doNothing().when(mContext).startActivity(argThat(expectedIntent()));

        final boolean isHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(isHandled).isTrue();
        assertThat(ShadowToast.shownToastCount()).isEqualTo(0);
        verify(mContext).startActivity(argThat(expectedIntent()));
    }

    @Test
    public void handlePreferenceTreeClick_noPackage_showsToast() {
        final String preferenceKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(preferenceKey);
        when(mWebViewUpdateServiceWrapper.getCurrentWebViewPackage()).thenReturn(null);

        final boolean isHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(isHandled).isTrue();
        assertThat(ShadowToast.shownToastCount()).isEqualTo(1);
        assertThat(ShadowToast.showedToast(
                mContext.getString(
                        com.android.settingslib.R.string.webview_launch_devtools_no_package)))
                .isTrue();
        verify(mContext, never()).startActivity(any());
    }

    @Test
    public void handlePreferenceTreeClick_resolveFails_showsToast() {
        final String preferenceKey = mController.getPreferenceKey();
        when(mPreference.getKey()).thenReturn(preferenceKey);
        PackageInfo packageInfo = webviewProviderPackage();
        when(mWebViewUpdateServiceWrapper.getCurrentWebViewPackage()).thenReturn(packageInfo);
        doThrow(new ActivityNotFoundException()).when(mContext)
                .startActivity(argThat(expectedIntent()));

        final boolean isHandled = mController.handlePreferenceTreeClick(mPreference);

        assertThat(isHandled).isTrue();
        verify(mContext).startActivity(any());
        assertThat(ShadowToast.shownToastCount()).isEqualTo(1);
        assertThat(ShadowToast.showedToast(
                mContext.getString(
                        com.android.settingslib.R.string.webview_launch_devtools_no_activity)))
                .isTrue();
    }

    private PackageInfo webviewProviderPackage() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = CURRENT_WEBVIEW_PROVIDER;
        return packageInfo;
    }

    private ArgumentMatcher<Intent> expectedIntent() {
        return intent -> CURRENT_WEBVIEW_PROVIDER.equals(intent.getPackage())
                && DEVTOOLS_ACTION.equals(intent.getAction())
                && ((intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK)
                        == Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
