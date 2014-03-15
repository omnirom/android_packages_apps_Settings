/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.backup.IBackupManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;

import org.omnirom.omnigears.backup.BackupService;
import org.omnirom.omnigears.preference.NumberPickerPreference;

/**
 * Gesture lock pattern settings.
 */
public class PrivacySettings extends SettingsPreferenceFragment implements
        DialogInterface.OnClickListener, OnPreferenceChangeListener {

    private static final String TAG = "PrivacySettings";

    // Vendor specific
    private static final String GSETTINGS_PROVIDER = "com.google.settings";
    private static final String BACKUP_CATEGORY = "backup_category";
    private static final String BACKUP_DATA = "backup_data";
    private static final String AUTO_RESTORE = "auto_restore";
    private static final String CONFIGURE_ACCOUNT = "configure_account";
    private IBackupManager mBackupManager;
    private CheckBoxPreference mBackup;
    private CheckBoxPreference mAutoRestore;
    private Dialog mConfirmDialog;
    private PreferenceScreen mConfigure;

    private static final int DIALOG_ERASE_BACKUP = 2;
    private int mDialogType;

    // Omnirom backup
    private static final int SELECT_BACKUP_FOLDER_REQUEST_CODE = 126;
    private static final String KEY_OMNIROM_BACKUP_CATEGORY = "omnirom_backup_category";
    private static final String KEY_BACKUP_LOCATION = "backup_location";
    private static final String KEY_BACKUP_HISTORY = "backup_history";

    private Preference mBackupLocation;
    private NumberPickerPreference mBackupHistory;
    private BackupService mBackupService;

    private ServiceConnection mBackupServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            mBackupService = ((BackupService.BackupServiceBinder) service).getService();
            mBackupLocation.setSummary(getBackupLocationName());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackupService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.privacy_settings);
        final PreferenceScreen screen = getPreferenceScreen();

        mBackupManager = IBackupManager.Stub.asInterface(
                ServiceManager.getService(Context.BACKUP_SERVICE));

        mBackup = (CheckBoxPreference) screen.findPreference(BACKUP_DATA);
        mAutoRestore = (CheckBoxPreference) screen.findPreference(AUTO_RESTORE);
        mConfigure = (PreferenceScreen) screen.findPreference(CONFIGURE_ACCOUNT);

        // Vendor specific
        if (getActivity().getPackageManager().
                resolveContentProvider(GSETTINGS_PROVIDER, 0) == null) {
            screen.removePreference(findPreference(BACKUP_CATEGORY));
        }

        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            getActivity().bindService(new Intent(getActivity(), BackupService.class),
                    mBackupServiceConnection, Context.BIND_AUTO_CREATE);

            mBackupHistory = (NumberPickerPreference) findPreference(KEY_BACKUP_HISTORY);
            mBackupHistory.setMinValue(1);
            mBackupHistory.setMaxValue(Integer.MAX_VALUE);
            mBackupHistory.setOnPreferenceChangeListener(this);
            mBackupLocation = findPreference(KEY_BACKUP_LOCATION);
        } else {
            screen.removePreference(screen.findPreference(KEY_OMNIROM_BACKUP_CATEGORY));
        }
        updateToggles();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Refresh UI
        updateToggles();
    }

    @Override
    public void onStop() {
        if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
            mConfirmDialog.dismiss();
        }
        mConfirmDialog = null;
        mDialogType = 0;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(mBackupServiceConnection);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mBackup) {
            if (!mBackup.isChecked()) {
                showEraseBackupDialog();
            } else {
                setBackupEnabled(true);
            }
        } else if (preference == mAutoRestore) {
            boolean curState = mAutoRestore.isChecked();
            try {
                mBackupManager.setAutoRestore(curState);
            } catch (RemoteException e) {
                mAutoRestore.setChecked(!curState);
            }
        } else if (KEY_BACKUP_LOCATION.equals(preference.getKey())) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(DocumentsContract.Document.MIME_TYPE_DIR);
            startActivityForResult(intent, SELECT_BACKUP_FOLDER_REQUEST_CODE);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trims backups after value was decremented.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mBackupHistory &&
                (Integer) value < ((NumberPickerPreference) preference).getValue()) {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.backup_history_trim)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Log.i(TAG, "Trimming backup history for all packages after " +
                                    "preference change.");
                            mBackupService.listBackups(null,
                                    mBackupService.new TrimBackupHistory());
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
        return true;
    }

    private void showEraseBackupDialog() {
        mBackup.setChecked(true);

        mDialogType = DIALOG_ERASE_BACKUP;
        CharSequence msg = getResources().getText(R.string.backup_erase_dialog_message);
        // TODO: DialogFragment?
        mConfirmDialog = new AlertDialog.Builder(getActivity()).setMessage(msg)
                .setTitle(R.string.backup_erase_dialog_title)
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .show();
    }

    /*
     * Creates toggles for each available location provider
     */
    private void updateToggles() {
        ContentResolver res = getContentResolver();

        boolean backupEnabled = false;
        Intent configIntent = null;
        String configSummary = null;
        try {
            backupEnabled = mBackupManager.isBackupEnabled();
            String transport = mBackupManager.getCurrentTransport();
            configIntent = mBackupManager.getConfigurationIntent(transport);
            configSummary = mBackupManager.getDestinationString(transport);
        } catch (RemoteException e) {
            // leave it 'false' and disable the UI; there's no backup manager
            mBackup.setEnabled(false);
        }
        mBackup.setChecked(backupEnabled);

        mAutoRestore.setChecked(Settings.Secure.getInt(res,
                Settings.Secure.BACKUP_AUTO_RESTORE, 1) == 1);
        mAutoRestore.setEnabled(backupEnabled);

        final boolean configureEnabled = (configIntent != null) && backupEnabled;
        mConfigure.setEnabled(configureEnabled);
        mConfigure.setIntent(configIntent);
        setConfigureSummary(configSummary);
}

    private void setConfigureSummary(String summary) {
        if (summary != null) {
            mConfigure.setSummary(summary);
        } else {
            mConfigure.setSummary(R.string.backup_configure_account_default_summary);
        }
    }

    private void updateConfigureSummary() {
        try {
            String transport = mBackupManager.getCurrentTransport();
            String summary = mBackupManager.getDestinationString(transport);
            setConfigureSummary(summary);
        } catch (RemoteException e) {
            // Not much we can do here
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            //updateProviders();
            if (mDialogType == DIALOG_ERASE_BACKUP) {
                setBackupEnabled(false);
                updateConfigureSummary();
            }
        }
        mDialogType = 0;
    }

    /**
     * Informs the BackupManager of a change in backup state - if backup is disabled,
     * the data on the server will be erased.
     * @param enable whether to enable backup
     */
    private void setBackupEnabled(boolean enable) {
        if (mBackupManager != null) {
            try {
                mBackupManager.setBackupEnabled(enable);
            } catch (RemoteException e) {
                mBackup.setChecked(!enable);
                mAutoRestore.setEnabled(!enable);
                return;
            }
        }
        mBackup.setChecked(enable);
        mAutoRestore.setEnabled(enable);
        mConfigure.setEnabled(enable);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    /**
     * Returns the backup location in the format 'rootname: path'.
     */
    private String getBackupLocationName() {
        Uri folder = mBackupService.getBackupLocation();

        Uri rootsUri = DocumentsContract.buildRootsUri(folder.getAuthority());
        Cursor cursor = getContentResolver().query(rootsUri, null, null, null, null);
        while (cursor.moveToNext()) {
            // HACK: Extract the root ID from the folder Uri by dropping the path and
            // taking the last segment. This might fail if a provider has a different Uri format.
            // "%3A" is urlencode(':')
            String[] split = folder.toString().split("%3A", 2)[0].split("/");
            String rootId = split[split.length - 1];

            String currentId = cursor.getString(cursor.getColumnIndex(
                    DocumentsContract.Root.COLUMN_ROOT_ID));
            if (rootId.equals(currentId)) {
                return cursor.getString(
                        cursor.getColumnIndex(DocumentsContract.Root.COLUMN_TITLE)) +
                        ": " + folder.getPath().split(":")[1];
            }
        }
        return null;
    }

    /**
     * Sets new backup location after user chose it in DocumentsUI.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_BACKUP_FOLDER_REQUEST_CODE &&
                resultCode == Activity.RESULT_OK && data != null) {
            final Uri uriOld = mBackupService.getBackupLocation();
            final Uri uriNew = data.getData();

            Log.i(TAG, "Setting new backup location: " + uriNew.toString());
            PreferenceManager.getDefaultSharedPreferences(getActivity())
                    .edit()
                    .putString("backup_location", uriNew.toString())
                    .apply();

            mBackupLocation.setSummary(getBackupLocationName());
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.backup_move_new_location)
                    .setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mBackupService.moveBackups(uriOld, uriNew);
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

}
