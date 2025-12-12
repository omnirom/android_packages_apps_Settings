/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.widget;

import static com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.accessibility.AccessibilityUtil;
import com.android.settingslib.widget.SettingsPreferenceGroupAdapter;
import com.android.settingslib.widget.SettingsThemeHelper;

import com.google.android.material.appbar.AppBarLayout;

public class HighlightablePreferenceGroupAdapter extends SettingsPreferenceGroupAdapter {

    private static final String TAG = "HighlightableAdapter";
    @VisibleForTesting static final long DELAY_COLLAPSE_DURATION_MILLIS = 300L;
    @VisibleForTesting static final long DELAY_HIGHLIGHT_DURATION_MILLIS = 600L;
    @VisibleForTesting static final long DELAY_HIGHLIGHT_DURATION_MILLIS_A11Y = 300L;
    private static final long HIGHLIGHT_DURATION = 15000L;
    private static final int HIGHLIGHT_FADE_OUT_DURATION = 500;
    private static final int HIGHLIGHT_FADE_IN_DURATION = 200;

    @VisibleForTesting @DrawableRes final int mHighlightBackgroundRes;
    @VisibleForTesting boolean mFadeInAnimated;

    private final Context mContext;
    private final @DrawableRes int mNormalBackgroundRes;
    private final @Nullable String mHighlightKey;
    private boolean mHighlightRequested;
    private int mHighlightPosition = RecyclerView.NO_POSITION;

    /**
     * Tries to override initial expanded child count.
     *
     * <p>Initial expanded child count will be ignored if: 1. fragment contains request to highlight
     * a particular row. 2. count value is invalid.
     */
    public static void adjustInitialExpandedChildCount(SettingsPreferenceFragment host) {
        if (host == null) {
            return;
        }
        final PreferenceScreen screen = host.getPreferenceScreen();
        if (screen == null) {
            return;
        }
        final Bundle arguments = host.getArguments();
        if (arguments != null) {
            final String highlightKey = arguments.getString(EXTRA_FRAGMENT_ARG_KEY);
            if (!TextUtils.isEmpty(highlightKey)) {
                // Has highlight row - expand everything
                screen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
                return;
            }
        }

        final int initialCount = host.getInitialExpandedChildCount();
        if (initialCount <= 0) {
            return;
        }
        screen.setInitialExpandedChildrenCount(initialCount);
    }

    public HighlightablePreferenceGroupAdapter(
            @NonNull PreferenceGroup preferenceGroup,
            @Nullable String key,
            boolean highlightRequested) {
        super(preferenceGroup);
        mHighlightKey = key;
        mHighlightRequested = highlightRequested;
        mContext = preferenceGroup.getContext();
        final TypedValue outValue = new TypedValue();
        mNormalBackgroundRes = R.drawable.preference_background;
        mHighlightBackgroundRes = R.drawable.preference_background_highlighted;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        updateBackground(holder, position);
    }

    @VisibleForTesting
    void updateBackground(PreferenceViewHolder holder, int position) {
        View v = holder.itemView;
        Preference preference = getItem(position);
        if (preference != null
                && position == mHighlightPosition
                && (mHighlightKey != null && TextUtils.equals(mHighlightKey, preference.getKey()))
                && v.isShown()) {
            // This position should be highlighted. If it's highlighted before - skip animation.
            v.requestAccessibilityFocus();
            addHighlightBackground(holder, !mFadeInAnimated, position);
        } else if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // View with highlight is reused for a view that should not have highlight
            removeHighlightBackground(holder, false /* animate */, position);
        }
    }

    /**
     * A function can highlight a specific setting in recycler view. note: Before highlighting a
     * setting, screen collapses tool bar with an animation.
     */
    public void requestHighlight(View root, RecyclerView recyclerView, AppBarLayout appBarLayout) {
        if (mHighlightRequested || recyclerView == null || TextUtils.isEmpty(mHighlightKey)) {
            return;
        }
        final int position = getPreferenceAdapterPosition(mHighlightKey);
        if (position < 0) {
            return;
        }

        // Highlight request accepted
        mHighlightRequested = true;
        // Collapse app bar after 300 milliseconds.
        if (appBarLayout != null) {
            root.postDelayed(
                    () -> appBarLayout.setExpanded(false, true),
                    DELAY_COLLAPSE_DURATION_MILLIS);
        }

        // Remove the animator as early as possible to avoid a RecyclerView crash.
        recyclerView.setItemAnimator(null);
        // Scroll to correct position after a short delay.
        root.postDelayed(
                () -> {
                    if (ensureHighlightPosition()) {
                        recyclerView.smoothScrollToPosition(mHighlightPosition);
                        highlightAndFocusTargetItem(recyclerView, mHighlightPosition);
                    }
                },
                AccessibilityUtil.isTouchExploreEnabled(mContext)
                        ? DELAY_HIGHLIGHT_DURATION_MILLIS_A11Y
                        : DELAY_HIGHLIGHT_DURATION_MILLIS);
    }

    private void highlightAndFocusTargetItem(RecyclerView recyclerView, int highlightPosition) {
        ViewHolder target = recyclerView.findViewHolderForAdapterPosition(highlightPosition);
        if (target != null) { // view already visible
            notifyItemChanged(mHighlightPosition);
            target.itemView.requestFocus();
        } else { // otherwise we're about to scroll to that view (but we might not be scrolling yet)
            recyclerView.addOnScrollListener(
                    new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(
                                @NonNull RecyclerView recyclerView, int newState) {
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                notifyItemChanged(mHighlightPosition);
                                ViewHolder target =
                                        recyclerView.findViewHolderForAdapterPosition(
                                                highlightPosition);
                                if (target != null) {
                                    target.itemView.requestFocus();
                                }
                                recyclerView.removeOnScrollListener(this);
                            }
                        }
                    });
        }
    }

    /**
     * Make sure we highlight the real-wanted position in case of preference position already
     * changed when the delay time comes.
     */
    private boolean ensureHighlightPosition() {
        if (TextUtils.isEmpty(mHighlightKey)) {
            return false;
        }
        final int position = getPreferenceAdapterPosition(mHighlightKey);
        final boolean allowHighlight = position >= 0;
        if (allowHighlight && mHighlightPosition != position) {
            Log.w(TAG, "EnsureHighlight: position has changed since last highlight request");
            // Make sure RecyclerView always uses latest correct position to avoid exceptions.
            mHighlightPosition = position;
        }
        return allowHighlight;
    }

    public boolean isHighlightRequested() {
        return mHighlightRequested;
    }

    @VisibleForTesting
    void requestRemoveHighlightDelayed(PreferenceViewHolder holder, int position) {
        final View v = holder.itemView;
        v.postDelayed(
                () -> {
                    mHighlightPosition = RecyclerView.NO_POSITION;
                    removeHighlightBackground(holder, true /* animate */, position);
                },
                HIGHLIGHT_DURATION);
    }

    private void addHighlightBackground(
            PreferenceViewHolder holder, boolean animate, int position) {
        final View v = holder.itemView;
        final Context context = v.getContext();
        if (context == null) {
            return;
        }

        final int backgroundFrom = getBackgroundRes(position, false);
        final int backgroundTo = getBackgroundRes(position, true);

        Object oldAnimatorTag = v.getTag(R.id.active_background_animator);
        if (oldAnimatorTag instanceof ValueAnimator) {
            ((ValueAnimator) oldAnimatorTag).cancel();
        }

        v.setTag(R.id.active_background_animator, null);
        v.setTag(R.id.preference_highlighted, true);

        Drawable backgroundFromDrawable = ContextCompat.getDrawable(context, backgroundFrom);
        Drawable backgroundToDrawable = ContextCompat.getDrawable(context, backgroundTo);

        if (!animate || backgroundFromDrawable == null || backgroundToDrawable == null) {
            // Fallback
            v.setBackgroundResource(backgroundTo);
            Log.d(TAG, "AddHighlight: Not animation requested - setting highlight background");
            holder.setIsRecyclable(false);
            requestRemoveHighlightDelayed(holder, position);
            return;
        }
        mFadeInAnimated = true;

        TransitionDrawable transitionDrawable = new TransitionDrawable(
                new Drawable[]{backgroundFromDrawable, backgroundToDrawable});
        v.setBackground(transitionDrawable);

        final ValueAnimator fadeInLoop =
                ValueAnimator.ofInt(0, 1);
        fadeInLoop.setDuration(HIGHLIGHT_FADE_IN_DURATION);
        fadeInLoop.setRepeatMode(ValueAnimator.REVERSE);
        fadeInLoop.setRepeatCount(4);
        v.setTag(R.id.active_background_animator, fadeInLoop);

        holder.setIsRecyclable(false);

        fadeInLoop.addListener(new AnimatorListenerAdapter() {
            private boolean mIsReversedForNext = false;

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                transitionDrawable.startTransition(HIGHLIGHT_FADE_IN_DURATION);
                mIsReversedForNext = true;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                super.onAnimationRepeat(animation);
                if (mIsReversedForNext) {
                    transitionDrawable.reverseTransition(HIGHLIGHT_FADE_IN_DURATION);
                } else {
                    transitionDrawable.startTransition(HIGHLIGHT_FADE_IN_DURATION);
                }
                mIsReversedForNext = !mIsReversedForNext;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                mFadeInAnimated = false;
                if (v.getTag(R.id.active_background_animator) == fadeInLoop) {
                    v.setTag(R.id.active_background_animator, null);
                }

                v.setBackgroundResource(backgroundTo);

                if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
                    requestRemoveHighlightDelayed(holder, position);
                } else {
                    holder.setIsRecyclable(true);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                mFadeInAnimated = false;

                if (v.getTag(R.id.active_background_animator) == fadeInLoop) {
                    v.setTag(R.id.active_background_animator, null);
                }

                if (Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
                    v.setBackgroundResource(backgroundTo);
                    requestRemoveHighlightDelayed(holder, position);
                } else {
                    v.setBackgroundResource(backgroundFrom);
                    holder.setIsRecyclable(true);
                }
            }
        });

        fadeInLoop.start();
        Log.d(TAG, "AddHighlight: starting fade in animation");
    }

    private void removeHighlightBackground(
            PreferenceViewHolder holder, boolean animate, int position) {
        final View v = holder.itemView;
        final Context context = v.getContext();

        int backgroundFrom = getBackgroundRes(position, true);
        int backgroundTo = getBackgroundRes(position, false);

        Object oldAnimatorTag = v.getTag(R.id.active_background_animator);
        if (oldAnimatorTag instanceof ValueAnimator) {
            ((ValueAnimator) oldAnimatorTag).cancel();
        }
        Drawable backgroundFromDrawable = ContextCompat.getDrawable(context, backgroundFrom);
        Drawable backgroundToDrawable = ContextCompat.getDrawable(context, backgroundTo);

        if (!animate || backgroundFromDrawable == null || backgroundToDrawable == null) {
            v.setBackgroundResource(backgroundTo);
            v.setTag(R.id.preference_highlighted, false);
            holder.setIsRecyclable(true);
            Log.d(TAG, "RemoveHighlight: No animation requested - setting normal background");
            return;
        }

        v.setTag(R.id.active_background_animator, null);

        if (!Boolean.TRUE.equals(v.getTag(R.id.preference_highlighted))) {
            // Not highlighted, no-op
            Log.d(TAG, "RemoveHighlight: Not highlighted - skipping");
            holder.setIsRecyclable(true);
            return;
        }

        v.setTag(R.id.preference_highlighted, false);

        TransitionDrawable transitionDrawable = new TransitionDrawable(
                new Drawable[]{backgroundFromDrawable, backgroundToDrawable});
        v.setBackground(transitionDrawable);

        final ValueAnimator colorAnimation =
                ValueAnimator.ofInt(0, 1);
        colorAnimation.setDuration(HIGHLIGHT_FADE_OUT_DURATION);
        v.setTag(R.id.active_background_animator, colorAnimation);
        holder.setIsRecyclable(false);

        colorAnimation.addListener(
                new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        transitionDrawable.startTransition(HIGHLIGHT_FADE_OUT_DURATION);
                    }

                    @Override
                    public void onAnimationEnd(@NonNull Animator animation) {
                        super.onAnimationEnd(animation);
                        v.setBackgroundResource(backgroundTo);

                        v.setTag(R.id.preference_highlighted, false);
                        holder.setIsRecyclable(true);

                        if (v.getTag(R.id.active_background_animator) == colorAnimation) {
                            v.setTag(R.id.active_background_animator, null);
                        }
                    }

                    @Override
                    public void onAnimationCancel(@NonNull Animator animation) {
                        super.onAnimationCancel(animation);
                        v.setBackgroundResource(backgroundTo);
                        v.setTag(R.id.preference_highlighted, false);
                        holder.setIsRecyclable(true);

                        if (v.getTag(R.id.active_background_animator) == colorAnimation) {
                            v.setTag(R.id.active_background_animator, null);
                        }
                    }
                });
        colorAnimation.start();
        Log.d(TAG, "Starting fade out animation");
    }

    private @DrawableRes int getBackgroundRes(int position, boolean isHighlighted) {
        int backgroundRes = (isHighlighted) ? mHighlightBackgroundRes : mNormalBackgroundRes;
        if (SettingsThemeHelper.isExpressiveTheme(mContext)) {
            Log.d(TAG, "[Expressive Theme] get rounded background, highlight = " + isHighlighted);
            int roundCornerResId = getRoundCornerDrawableRes(position, false, isHighlighted);
            if (roundCornerResId != 0) {
                return roundCornerResId;
            }
        }
        return backgroundRes;
    }
}
