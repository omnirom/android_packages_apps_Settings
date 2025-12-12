/**
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.core;

import android.annotation.LayoutRes;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toolbar;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.CategoryMixin.CategoryHandler;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarDelegate;
import com.android.settingslib.collapsingtoolbar.FloatingToolbarHandler;
import com.android.settingslib.collapsingtoolbar.widget.ScrollableToolbarItemLayout;
import com.android.settingslib.core.lifecycle.HideNonSystemOverlayMixin;
import com.android.settingslib.transition.SettingsTransitionHelper.TransitionType;
import com.android.settingslib.widget.ExpressiveDesignEnabledProvider;
import com.android.settingslib.widget.SettingsThemeHelper;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingtoolbar.FloatingToolbarLayout;
import com.google.android.material.resources.TextAppearanceConfig;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.transition.TransitionHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.List;

/** Base activity for Settings pages */
public class SettingsBaseActivity extends FragmentActivity implements CategoryHandler,
        FloatingToolbarHandler, ExpressiveDesignEnabledProvider {

    /**
     * What type of page transition should be apply.
     */
    public static final String EXTRA_PAGE_TRANSITION_TYPE = "page_transition_type";

    protected static final boolean DEBUG_TIMING = false;
    private static final String TAG = "SettingsBaseActivity";
    private static final int DEFAULT_REQUEST = -1;

    private static final int EXPRESSIVE_LAYOUT_ID =
            com.android.settingslib.collapsingtoolbar.R.layout.settingslib_expressive_collapsing_toolbar_base_layout;
    private static final int COLLAPSING_LAYOUT_ID =
            com.android.settingslib.collapsingtoolbar.R.layout.collapsing_toolbar_base_layout;

    private static final String SETUPWIZARD_THEME_PROP = "setupwizard.theme";
    private static final String SETUPWIZARD_THEME_PREFIX = "glif_expressive";


    protected CategoryMixin mCategoryMixin;
    protected CollapsingToolbarLayout mCollapsingToolbarLayout;
    protected AppBarLayout mAppBarLayout;
    private Toolbar mToolbar;

    private CollapsingToolbarDelegate mToolbardelegate;

    @Override
    public CategoryMixin getCategoryMixin() {
        return mCategoryMixin;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final boolean isAnySetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        if (isAnySetupWizard) {
            TransitionHelper.applyForwardTransition(this);
            TransitionHelper.applyBackwardTransition(this);

            // Apply SetupWizard light theme during setup flow. This is for SubSettings pages.
            if (this instanceof SubSettings) {
                // setTheme needs to be called before inflating any views (e.g. before calling
                // super.onCreate)
                if (SettingsThemeHelper.isExpressiveTheme(this)) {
                    setTheme(R.style.SettingsPreferenceTheme_SetupWizard_Expressive);
                } else {
                    setTheme(R.style.SettingsPreferenceTheme_SetupWizard);
                }
                ThemeHelper.trySetSuwTheme(this);
            }
        }
        super.onCreate(savedInstanceState);
        if (isFinishing()) {
            return;
        }
        if (isLockTaskModePinned() && !isSettingsRunOnTop()) {
            Log.w(TAG, "Devices lock task mode pinned.");
            finish();
        }
        final long startTime = System.currentTimeMillis();
        if (!isAnySetupWizard) {
            Utils.setupEdgeToEdge(this);
            hideInternalActionBar();
        }
        getLifecycle().addObserver(new HideNonSystemOverlayMixin(this));
        TextAppearanceConfig.setShouldLoadFontSynchronously(true);

        mCategoryMixin = new CategoryMixin(this);
        getLifecycle().addObserver(mCategoryMixin);

        final TypedArray theme = getTheme().obtainStyledAttributes(android.R.styleable.Theme);
        if (!theme.getBoolean(android.R.styleable.Theme_windowNoTitle, false)) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }

        if (isToolbarEnabled() && !isAnySetupWizard) {
            int resId = SettingsThemeHelper.isExpressiveTheme(getApplicationContext())
                    ? EXPRESSIVE_LAYOUT_ID : COLLAPSING_LAYOUT_ID;
            super.setContentView(resId);
            mCollapsingToolbarLayout =
                    findViewById(com.android.settingslib.collapsingtoolbar.R.id.collapsing_toolbar);
            mAppBarLayout = findViewById(R.id.app_bar);
            getToolbarDelegate().initCollapsingToolbar(mCollapsingToolbarLayout, mAppBarLayout);
        } else {
            super.setContentView(R.layout.settings_base_layout);
        }

        if (isAnySetupWizard) {
            findViewById(R.id.content_parent).setFitsSystemWindows(false);
        }

        // This is to hide the toolbar from those pages which don't need a toolbar originally.
        final Toolbar toolbar = findViewById(R.id.action_bar);
        if (!isToolbarEnabled() || isAnySetupWizard) {
            toolbar.setVisibility(View.GONE);
            return;
        }
        setActionBar(toolbar);

        getToolbarDelegate().initToolbarPrimaryButton(
                findViewById(com.android.settingslib.collapsingtoolbar.R.id.primary_button));
        getToolbarDelegate().initToolbarSecondaryButton(
                findViewById(com.android.settingslib.collapsingtoolbar.R.id.secondary_button));
        getToolbarDelegate().initToolbarActionButton(
                findViewById(com.android.settingslib.collapsingtoolbar.R.id.action_button));

        FloatingToolbarLayout toolbarLayout = findViewById(
                com.android.settingslib.collapsingtoolbar.R.id.floating_toolbar);
        getToolbarDelegate().initFloatingToolbar(getApplicationContext(), toolbarLayout);

        if (DEBUG_TIMING) {
            Log.d(TAG, "onCreate took " + (System.currentTimeMillis() - startTime) + " ms");
        }
    }

    @Override
    public void setActionBar(@androidx.annotation.Nullable Toolbar toolbar) {
        super.setActionBar(toolbar);

        mToolbar = toolbar;
    }

    @Override
    public boolean onNavigateUp() {
        if (!super.onNavigateUp()) {
            finishAfterTransition();
        }
        return true;
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode,
            @androidx.annotation.Nullable Bundle options) {
        final int transitionType = getTransitionType(intent);
        super.startActivityForResult(intent, requestCode, options);
        if (transitionType == TransitionType.TRANSITION_SLIDE) {
            overridePendingTransition(
                    com.google.android.setupdesign.R.anim.sud_slide_next_in,
                    com.google.android.setupdesign.R.anim.sud_slide_next_out);
        } else if (transitionType == TransitionType.TRANSITION_FADE) {
            overridePendingTransition(
                    android.R.anim.fade_in, com.google.android.setupdesign.R.anim.sud_stay);
        }
    }

    @Override
    protected void onPause() {
        // For accessibility activities launched from setup wizard.
        if (getTransitionType(getIntent()) == TransitionType.TRANSITION_FADE) {
            overridePendingTransition(
                    com.google.android.setupdesign.R.anim.sud_stay, android.R.anim.fade_out);
        }
        super.onPause();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        final ViewGroup parent = findViewById(R.id.content_frame);
        if (parent != null) {
            parent.removeAllViews();
        }
        LayoutInflater.from(this).inflate(layoutResID, parent);
    }

    @Override
    public void setContentView(View view) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        ((ViewGroup) findViewById(R.id.content_frame)).addView(view, params);
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (mCollapsingToolbarLayout != null) {
            mCollapsingToolbarLayout.setTitle(title);
        }
    }

    @Override
    public void setTitle(int titleId) {
        super.setTitle(getText(titleId));
        if (mCollapsingToolbarLayout != null) {
            mCollapsingToolbarLayout.setTitle(getText(titleId));
        }
    }

    /**
     * SubSetting page should show a toolbar by default. If the page wouldn't show a toolbar,
     * override this method and return false value.
     *
     * @return ture by default
     */
    protected boolean isToolbarEnabled() {
        return true;
    }

    private boolean isLockTaskModePinned() {
        final ActivityManager activityManager =
                getApplicationContext().getSystemService(ActivityManager.class);
        return activityManager.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_PINNED;
    }

    private boolean isSettingsRunOnTop() {
        final ActivityManager activityManager =
                getApplicationContext().getSystemService(ActivityManager.class);
        final String taskPkgName = activityManager.getRunningTasks(1 /* maxNum */)
                .get(0 /* index */).baseActivity.getPackageName();
        return TextUtils.equals(getPackageName(), taskPkgName);
    }

    /**
     * @return whether or not the enabled state actually changed.
     */
    public boolean setTileEnabled(ComponentName component, boolean enabled) {
        final PackageManager pm = getPackageManager();
        int state = pm.getComponentEnabledSetting(component);
        boolean isEnabled = state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        if (isEnabled != enabled || state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            if (enabled) {
                mCategoryMixin.removeFromDenylist(component);
            } else {
                mCategoryMixin.addToDenylist(component);
            }
            pm.setComponentEnabledSetting(component, enabled
                            ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                            : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            return true;
        }
        return false;
    }

    private int getTransitionType(Intent intent) {
        if (intent == null) {
            return TransitionType.TRANSITION_NONE;
        }
        return intent.getIntExtra(EXTRA_PAGE_TRANSITION_TYPE, TransitionType.TRANSITION_NONE);
    }

    /**
     * This internal ActionBar will be appeared automatically when the
     * Utils.setupEdgeToEdge is invoked.
     *
     * @see Utils.setupEdgeToEdge
     */
    private void hideInternalActionBar() {
        final View actionBarContainer =
                findViewById(com.android.internal.R.id.action_bar_container);
        if (actionBarContainer != null) {
            actionBarContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Show/Hide the primary button on the Toolbar.
     * @param enabled true to show the button, otherwise it's hidden.
     */
    public void setPrimaryButtonEnabled(boolean enabled) {
        getToolbarDelegate().setPrimaryButtonEnabled(enabled);
    }

    /** Set the icon to the primary button */
    public void setPrimaryButtonIcon(@DrawableRes int drawableRes) {
        getToolbarDelegate().setPrimaryButtonIcon(this, drawableRes);
    }

    /** Set the OnClick listener to the primary button. */
    public void setPrimaryButtonOnClickListener(@Nullable View.OnClickListener listener) {
        getToolbarDelegate().setPrimaryButtonOnClickListener(listener);
    }

    /** Set the content description to the primary button */
    public void setPrimaryButtonContentDescription(@Nullable CharSequence contentDescription) {
        getToolbarDelegate().setPrimaryButtonContentDescription(contentDescription);
    }

    /**
     * Show/Hide the secondary button on the Toolbar.
     * @param enabled true to show the button, otherwise it's hidden.
     */
    public void setSecondaryButtonEnabled(boolean enabled) {
        getToolbarDelegate().setSecondaryButtonEnabled(enabled);
    }

    /** Set the icon to the secondary button */
    public void setSecondaryButtonIcon(@DrawableRes int drawableRes) {
        getToolbarDelegate().setSecondaryButtonIcon(this, drawableRes);
    }

    /** Set the OnClick listener to the secondary button */
    public void setSecondaryButtonOnClickListener(@Nullable View.OnClickListener listener) {
        getToolbarDelegate().setSecondaryButtonOnClickListener(listener);
    }

    /** Set the content description to the secondary button */
    public void setSecondaryButtonContentDescription(@Nullable CharSequence contentDescription) {
        getToolbarDelegate().setSecondaryButtonContentDescription(contentDescription);
    }

    /**
     * Show/Hide the action button on the Toolbar.
     * @param enabled true to show the button, otherwise it's hidden.
     */
    public void setActionButtonEnabled(boolean enabled) {
        getToolbarDelegate().setActionButtonEnabled(enabled);
    }

    /**
     * Enable/Disable the action button on the Toolbar (being clickable or not).
     * @param clickable true to enable the button, otherwise it's disabled.
     */
    public void setActionButtonClickable(boolean clickable) {
        getToolbarDelegate().setActionButtonClickable(clickable);
    }

    /** Set the icon to the action button */
    public void setActionButtonIcon(@DrawableRes int drawableRes) {
        getToolbarDelegate().setActionButtonIcon(this, drawableRes);
    }

    /** Set the text to the action button */
    public void setActionButtonText(@Nullable CharSequence text) {
        getToolbarDelegate().setActionButtonText(text);
    }

    /** Set the OnClick listener to the action button */
    public void setActionButtonListener(@Nullable View.OnClickListener listener) {
        getToolbarDelegate().setActionButtonOnClickListener(listener);
    }

    /** Set the content description to the action button */
    public void setActionButtonContentDescription(@Nullable CharSequence contentDescription) {
        getToolbarDelegate().setActionButtonContentDescription(contentDescription);
    }

    @Override
    public void setFloatingToolbarVisibility(boolean visible) {
        getToolbarDelegate().setFloatingToolbarVisibility(visible);
    }

    @Override
    public void setToolbarItems(@NonNull List<ScrollableToolbarItemLayout.ToolbarItem> items) {
        getToolbarDelegate().setToolbarItems(items);
    }

    /**
     * Sets the selected toolbar item by its zero-based index.
     */
    public void setToolbarSelectedItem(int position) {
        getToolbarDelegate().setSelectedItem(position);
    }

    @Override
    public void setOnItemSelectedListener(
            @NonNull ScrollableToolbarItemLayout.OnItemSelectedListener listener) {
        getToolbarDelegate().setOnItemSelectedListener(listener);
    }

    @Override
    public void removeOnItemSelectedListener() {
        getToolbarDelegate().removeOnItemSelectedListener();
    }

    private CollapsingToolbarDelegate getToolbarDelegate() {
        if (mToolbardelegate == null) {
            mToolbardelegate = new CollapsingToolbarDelegate(new EmptyDelegateCallback(), true);
        }
        return mToolbardelegate;
    }

    @Override
    public boolean isExpressiveDesignEnabled() {
        if (!WizardManagerHelper.isAnySetupWizard(getIntent())) {
            return SettingsThemeHelper.isExpressiveDesignEnabled();
        }

        return SystemProperties.get(SETUPWIZARD_THEME_PROP).startsWith(SETUPWIZARD_THEME_PREFIX);
    }

    private class EmptyDelegateCallback implements CollapsingToolbarDelegate.HostCallback {
        @Nullable
        @Override
        public ActionBar setActionBar(Toolbar toolbar) {
            return null;
        }

        @Override
        public void setOuterTitle(CharSequence title) {
        }
    }
}
