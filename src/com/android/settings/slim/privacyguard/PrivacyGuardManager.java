/*
 * Copyright (C) 2013 SlimRoms Project
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

package com.android.settings.slim.privacyguard;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.applications.AppOpsDetails;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PrivacyGuardManager extends Fragment
        implements OnItemClickListener, OnItemLongClickListener {

    private static final String TAG = "PrivacyGuardManager";

    // Dialog identifiers used in showDialog
    private static final int DLG_BASE = 0;
    private static final int DLG_RESET = DLG_BASE + 1;
    private static final int DLG_HELP = DLG_BASE + 2;
    private static final int DLG_APP_OPS_DETAIL = DLG_BASE + 3;

    private static final String LIST_STATE = "pgListState";
    private Parcelable mListState = null;

    private TextView mNoUserAppsInstalled;
    private ListView mAppsList;
    private PrivacyGuardAppListAdapter mAdapter;
    private List<AppInfo> mApps;
    private AppInfo mCurrentApp;

    private PackageManager mPm;
    private Activity mActivity;

    private SharedPreferences mPreferences;
    private AppOpsManager mAppOps;

    // holder for package data passed into the adapter
    public static final class AppInfo {
        String title;
        String packageName;
        boolean enabled;
        int privacyGuardState;
        boolean hasPrivacyGuardOps;
        int uid;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mActivity = getActivity();
        mPm = mActivity.getPackageManager();
        mAppOps = (AppOpsManager)getActivity().getSystemService(Context.APP_OPS_SERVICE);

        return inflater.inflate(R.layout.privacy_guard_manager, container, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id.privacy_guard_prefs);
        if (f != null && !fm.isDestroyed()) {
            fm.beginTransaction().remove(f).commit();
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(LIST_STATE);
        }

        mNoUserAppsInstalled = (TextView) mActivity.findViewById(R.id.error);

        mAppsList = (ListView) mActivity.findViewById(R.id.apps_list);
        mAppsList.setOnItemClickListener(this);
        mAppsList.setOnItemLongClickListener(this);

        // get shared preference
        mPreferences = mActivity.getSharedPreferences("privacy_guard_manager", Activity.MODE_PRIVATE);
        if (!mPreferences.getBoolean("first_help_shown", false)) {
            showDialogInner(DLG_HELP);
        }

        // load apps and construct the list
        loadApps();
        setHasOptionsMenu(true);
    }

    private void loadApps() {
        mApps = loadInstalledApps();

        // if app list is empty inform the user
        // else go ahead and construct the list
        if (mApps == null || mApps.isEmpty()) {
            mNoUserAppsInstalled.setText(R.string.privacy_guard_no_user_apps);
            mNoUserAppsInstalled.setVisibility(View.VISIBLE);
            mAppsList.setVisibility(View.GONE);
        } else {
            mNoUserAppsInstalled.setVisibility(View.GONE);
            mAppsList.setVisibility(View.VISIBLE);
            mAdapter = createAdapter();
            mAppsList.setAdapter(mAdapter);
            mAppsList.setFastScrollEnabled(true);
        }
    }

    private PrivacyGuardAppListAdapter createAdapter() {
        String lastSectionIndex = null;
        ArrayList<String> sections = new ArrayList<String>();
        ArrayList<Integer> positions = new ArrayList<Integer>();
        int count = mApps.size(), offset = 0;

        for (int i = 0; i < count; i++) {
            AppInfo app = mApps.get(i);
            String sectionIndex;

            if (!app.enabled) {
                sectionIndex = "--"; //XXX
            } else if (app.title.isEmpty()) {
                sectionIndex = "";
            } else {
                sectionIndex = app.title.substring(0, 1).toUpperCase();
            }
            if (lastSectionIndex == null) {
                lastSectionIndex = sectionIndex;
            }

            if (!TextUtils.equals(sectionIndex, lastSectionIndex)) {
                sections.add(sectionIndex);
                positions.add(offset);
                lastSectionIndex = sectionIndex;
            }
            offset++;
        }

        return new PrivacyGuardAppListAdapter(mActivity, mApps, sections, positions);
    }

    private void resetPrivacyGuard() {
        if (mApps == null || mApps.isEmpty()) {
            return;
        }
        // turn off privacy guard for all apps shown in the current list
        for (AppInfo app : mApps) {
            app.privacyGuardState = AppOpsManager.PRIVACY_GUARD_DISABLED;
            mAppOps.setPrivacyGuardSettingForPackage(
                app.uid, app.packageName, false, true);
        }
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // on click change the privacy guard status for this item
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);

        boolean enabled = app.privacyGuardState > AppOpsManager.PRIVACY_GUARD_DISABLED_PLUS;

        if (!enabled && !app.hasPrivacyGuardOps) {
            mCurrentApp = app;
            showDialogInner(DLG_APP_OPS_DETAIL);
        } else {
            mAppOps.setPrivacyGuardSettingForPackage(app.uid, app.packageName,
                !enabled, false);
            app.privacyGuardState = mAppOps.getPrivacyGuardSettingForPackage(
                    app.uid, app.packageName);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // on long click open app details window
        final AppInfo app = (AppInfo) parent.getItemAtPosition(position);
        startAppOpsDetails(app);
        return true;
    }

    private void startAppOpsDetails(AppInfo app) {
        Bundle args = new Bundle();
        args.putString(AppOpsDetails.ARG_PACKAGE_NAME, app.packageName);

        PreferenceActivity pa = (PreferenceActivity) getActivity();
        pa.startPreferencePanel(AppOpsDetails.class.getName(), args,
                R.string.app_ops_settings, null, this, 2);
    }

    /**
    * Uses the package manager to query for all currently installed apps
    * for the list.
    *
    * @return the complete List off installed applications (@code PrivacyGuardAppInfo)
    */
    private List<AppInfo> loadInstalledApps() {
        List<AppInfo> apps = new ArrayList<AppInfo>();
        List<PackageInfo> packages = mPm.getInstalledPackages(
            PackageManager.GET_PERMISSIONS | PackageManager.GET_SIGNATURES);
        boolean showSystemApps = shouldShowSystemApps();
        boolean filterByPermission = shouldFilterByPermission();
        boolean hasPrivacyGuardOps;
        Signature platformCert;

        try {
            PackageInfo sysInfo = mPm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            platformCert = sysInfo.signatures[0];
        } catch (PackageManager.NameNotFoundException e) {
            platformCert = null;
        }

        for (PackageInfo info : packages) {
            final ApplicationInfo appInfo = info.applicationInfo;
            hasPrivacyGuardOps = mAppOps.getPrivacyGuardOpsForPackage(info.packageName).size() > 0;

            // hide apps without privacy guard permissions
            if (filterByPermission && !hasPrivacyGuardOps) {
                continue;
            }

            // hide apps signed with the platform certificate to avoid the user
            // shooting himself in the foot
            if (platformCert != null && info.signatures != null
                    && platformCert.equals(info.signatures[0])) {
                continue;
            }

            // skip all system apps if they shall not be included
            if (!showSystemApps && (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            AppInfo app = new AppInfo();
            app.title = appInfo.loadLabel(mPm).toString();
            app.packageName = info.packageName;
            app.enabled = appInfo.enabled;
            app.uid = info.applicationInfo.uid;
            app.privacyGuardState = mAppOps.getPrivacyGuardSettingForPackage(
                    app.uid, app.packageName);
            app.hasPrivacyGuardOps = hasPrivacyGuardOps;
            apps.add(app);
        }

        // sort the apps by their enabled state, then by title
        Collections.sort(apps, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                if (lhs.enabled != rhs.enabled) {
                    return lhs.enabled ? -1 : 1;
                }
                return lhs.title.compareToIgnoreCase(rhs.title);
            }
        });

        return apps;
    }

    private boolean shouldShowSystemApps() {
        return mPreferences.getBoolean("show_system_apps", false);
    }

    private boolean shouldFilterByPermission() {
        return mPreferences.getBoolean("filter_by_permission", true);
    }


    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        PrivacyGuardManager getOwner() {
            return (PrivacyGuardManager) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_reset_title)
                    .setMessage(R.string.privacy_guard_app_ops_detail_reset_dialog_text)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetPrivacyGuard();
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
                case DLG_HELP:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_help_title)
                    .setMessage(R.string.privacy_guard_help_text)
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
                case DLG_APP_OPS_DETAIL:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.privacy_guard_app_ops_detail_dialog_title)
                    .setMessage(R.string.privacy_guard_app_ops_detail_dialog_text)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().startAppOpsDetails(getOwner().mCurrentApp);
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_HELP:
                    getOwner().mPreferences.edit()
                        .putBoolean("first_help_shown", true).commit();
                    break;
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.privacy_guard_manager, menu);
        menu.findItem(R.id.show_system_apps).setChecked(shouldShowSystemApps());
        menu.findItem(R.id.filter_app_permissions).setChecked(shouldFilterByPermission());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.help:
                showDialogInner(DLG_HELP);
                return true;
            case R.id.reset:
                showDialogInner(DLG_RESET);
                return true;
            case R.id.filter_app_permissions:
            case R.id.show_system_apps:
                final String prefName = item.getItemId() == R.id.filter_app_permissions
                        ? "filter_by_permission" : "show_system_apps";
                // set the menu checkbox and save it in
                // shared preference and rebuild the list
                item.setChecked(!item.isChecked());
                mPreferences.edit().putBoolean(prefName, item.isChecked()).commit();
                loadApps();
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // rebuild the list; the user might have changed settings inbetween
        loadApps();
        if (mListState != null && mAppsList != null) {
            mAppsList.onRestoreInstanceState(mListState);
        }
        mListState = null;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mAppsList != null) {
            mListState = mAppsList.onSaveInstanceState();
            state.putParcelable(LIST_STATE, mListState);
        }
    }

}
