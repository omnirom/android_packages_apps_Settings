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

package com.android.settings.enterprise;

import static android.app.admin.DevicePolicyResources.Strings.Settings.DISABLED_BY_IT_ADMIN_TITLE;

import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Process;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.Utils;
import com.android.settingslib.enterprise.ActionDisabledByAdminController;
import com.android.settingslib.enterprise.ActionDisabledByAdminControllerFactory;

import java.util.Objects;

/**
 * Helper class for {@link ActionDisabledByAdminDialog} which sets up the dialog.
 */
public final class ActionDisabledByAdminDialogHelper {

    private static final String TAG = ActionDisabledByAdminDialogHelper.class.getName();
    @VisibleForTesting
    EnforcedAdmin mEnforcedAdmin;
    @Nullable EnforcingAdmin mEnforcingAdmin;
    private ViewGroup mDialogView;
    @Nullable
    private String mRestriction;
    private final ActionDisabledByAdminController mActionDisabledByAdminController;
    private final Activity mActivity;

    public ActionDisabledByAdminDialogHelper(Activity activity) {
        this(activity, null /* restriction */);
    }

    public ActionDisabledByAdminDialogHelper(Activity activity, String restriction) {
        mActivity = activity;
        mDialogView = (ViewGroup) LayoutInflater.from(mActivity).inflate(
                R.layout.support_details_dialog, null);
        mActionDisabledByAdminController = ActionDisabledByAdminControllerFactory
                .createInstance(mActivity, restriction,
                        new DeviceAdminStringProviderImpl(mActivity),
                        UserHandle.SYSTEM);
        DevicePolicyManager devicePolicyManager =
                mActivity.getSystemService(DevicePolicyManager.class);

        TextView title = mDialogView.findViewById(R.id.admin_support_dialog_title);
        title.setText(devicePolicyManager.getResources().getString(DISABLED_BY_IT_ADMIN_TITLE,
                () -> mActivity.getString(R.string.disabled_by_policy_title)));

    }

    private @UserIdInt int getEnforcingAdminUserId(@NonNull EnforcingAdmin admin) {
        return admin.getUserHandle() == null
                ? UserHandle.USER_NULL
                : admin.getUserHandle().getIdentifier();
    }

    private @UserIdInt int getEnforcementAdminUserId(@NonNull EnforcedAdmin admin) {
        return admin.user == null ? UserHandle.USER_NULL : admin.user.getIdentifier();
    }

    private @UserIdInt int getEnforcementAdminUserId() {
        return getEnforcementAdminUserId(mEnforcedAdmin);
    }

    /** @deprecated Please use the same method that takes {@link EnforcingAdmin}. */
    @Deprecated
    public AlertDialog.Builder prepareDialogBuilder(String restriction,
            EnforcedAdmin enforcedAdmin) {
        DialogInterface.OnClickListener listener = mActionDisabledByAdminController
                .getPositiveButtonListener(mActivity, enforcedAdmin);
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                .setPositiveButton(listener == null
                        ? R.string.suggestion_button_close : R.string.okay, listener)
                .setView(mDialogView);
        prepareDialogBuilder(builder, restriction, enforcedAdmin);
        return builder;
    }

    /**
     * Prepares an alert dialog that shows information about a restriction set by the given admin.
     */
    @NonNull
    public AlertDialog.Builder prepareDialogBuilder(
            @Nullable String restriction, @Nullable EnforcingAdmin enforcingAdmin) {
        DialogInterface.OnClickListener listener =
                mActionDisabledByAdminController.getPositiveButtonListener(
                        mActivity, enforcingAdmin);
        AlertDialog.Builder builder =
                new AlertDialog.Builder(mActivity)
                        .setPositiveButton(
                                listener == null ? R.string.suggestion_button_close : R.string.okay,
                                listener)
                        .setView(mDialogView);
        prepareDialogBuilder(builder, restriction, enforcingAdmin);
        return builder;
    }

    @VisibleForTesting
    void prepareDialogBuilder(AlertDialog.Builder builder, String restriction,
            EnforcedAdmin enforcedAdmin) {
        mActionDisabledByAdminController.initialize(
                new ActionDisabledLearnMoreButtonLauncherImpl(mActivity, builder));

        mEnforcedAdmin = enforcedAdmin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcedAdmin, getEnforcementAdminUserId(),
                mRestriction);
        mActionDisabledByAdminController.setupLearnMoreButton(mActivity);
    }

    private void prepareDialogBuilder(
            AlertDialog.Builder builder,
            @Nullable String restriction,
            @Nullable EnforcingAdmin enforcingAdmin) {
        mActionDisabledByAdminController.initialize(
                new ActionDisabledLearnMoreButtonLauncherImpl(mActivity, builder));

        mEnforcingAdmin = enforcingAdmin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcingAdmin, mRestriction);
        mActionDisabledByAdminController.setupLearnMoreButton(mActivity);
    }

    /** @deprecated Please use the same method that takes {@link EnforcingAdmin}. */
    @Deprecated
    public void updateDialog(String restriction, EnforcedAdmin admin) {
        if (mEnforcedAdmin.equals(admin) && Objects.equals(mRestriction, restriction)) {
            return;
        }
        mEnforcedAdmin = admin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcedAdmin, getEnforcementAdminUserId(),
                mRestriction);
    }

    /** Updates the dialog view to show information about a restriction set by the given admin. */
    public void updateDialog(@Nullable String restriction, @Nullable EnforcingAdmin admin) {
        if (mEnforcingAdmin.equals(admin) && Objects.equals(mRestriction, restriction)) {
            return;
        }
        mEnforcingAdmin = admin;
        mRestriction = restriction;
        initializeDialogViews(mDialogView, mEnforcingAdmin, mRestriction);
    }

    private void initializeDialogViews(View root, EnforcedAdmin enforcedAdmin, int userId,
            String restriction) {
        ComponentName admin = enforcedAdmin.component;
        if (admin == null) {
            return;
        }

        mActionDisabledByAdminController.updateEnforcedAdmin(enforcedAdmin, userId);
        setAdminSupportIcon(root);

        if (isNotCurrentUserOrProfile(admin, userId)) {
            admin = null;
        }

        setAdminSupportTitle(root, restriction);

        final UserHandle user;
        if (userId == UserHandle.USER_NULL) {
            user = null;
        } else {
            user = UserHandle.of(userId);
        }

        setAdminSupportDetails(mActivity, root, new EnforcedAdmin(admin, user));
    }

    private void initializeDialogViews(
            View root, @Nullable EnforcingAdmin enforcingAdmin, @Nullable String restriction) {
        mActionDisabledByAdminController.updateEnforcingAdmin(enforcingAdmin);
        if (enforcingAdmin == null) {
            return;
        }
        setAdminSupportIcon(root);
        setAdminSupportTitle(root, restriction);
        setAdminSupportDetails(mActivity, root, enforcingAdmin);
    }

    private boolean isNotCurrentUserOrProfile(ComponentName admin, int userId) {
        return !RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(mActivity, admin)
                || !RestrictedLockUtils.isCurrentUserOrProfile(mActivity, userId);
    }

    void setAdminSupportIcon(View root) {
        ImageView supportIconView = root.requireViewById(R.id.admin_support_icon);
        supportIconView.setImageDrawable(
                mActivity.getDrawable(R.drawable.ic_lock_closed));

        supportIconView.setImageTintList(Utils.getColorAccent(mActivity));
    }

    @VisibleForTesting
    void setAdminSupportTitle(View root, @Nullable String restriction) {
        final TextView titleView = root.findViewById(R.id.admin_support_dialog_title);
        if (titleView == null) {
            return;
        }
        titleView.setText(mActionDisabledByAdminController.getAdminSupportTitle(restriction));
    }

    @VisibleForTesting
    void setAdminSupportDetails(final Activity activity, final View root,
            final EnforcedAdmin enforcedAdmin) {
        if (enforcedAdmin == null || enforcedAdmin.component == null) {
            return;
        }

        final DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        CharSequence supportMessage = null;
        if (!RestrictedLockUtilsInternal.isAdminInCurrentUserOrProfile(activity,
                enforcedAdmin.component) || !RestrictedLockUtils.isCurrentUserOrProfile(
                activity, getEnforcementAdminUserId(enforcedAdmin))) {
            enforcedAdmin.component = null;
        } else {
            if (enforcedAdmin.user == null) {
                enforcedAdmin.user = UserHandle.of(UserHandle.myUserId());
            }
            if (UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
                supportMessage = dpm.getShortSupportMessageForUser(enforcedAdmin.component,
                        getEnforcementAdminUserId(enforcedAdmin));
            }
        }
        final CharSequence supportContentString =
                mActionDisabledByAdminController.getAdminSupportContentString(
                        mActivity, supportMessage);
        final TextView textView = root.findViewById(R.id.admin_support_msg);
        if (supportContentString != null) {
            textView.setText(supportContentString);
        }
    }

    @VisibleForTesting
    void setAdminSupportDetails(
            final Activity activity,
            final View root,
            @Nullable final EnforcingAdmin enforcingAdmin) {
        if (enforcingAdmin == null) {
            return;
        }
        final DevicePolicyManager dpm = activity.getSystemService(DevicePolicyManager.class);
        CharSequence supportMessage = null;

        if (enforcingAdmin.getComponentName() != null
                && UserHandle.isSameApp(Process.myUid(), Process.SYSTEM_UID)) {
            supportMessage =
                    dpm.getShortSupportMessageForUser(
                            enforcingAdmin.getComponentName(),
                            getEnforcingAdminUserId(enforcingAdmin));
        }
        final CharSequence supportContentString =
                mActionDisabledByAdminController.getAdminSupportContentString(
                        mActivity, supportMessage);
        final TextView textView = root.findViewById(R.id.admin_support_msg);
        if (supportContentString != null) {
            textView.setText(supportContentString);
        }
    }
}
