/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.android.internal.widget.LockPatternUtils;

public class NfcUnlockSettings extends Activity {

    private View mView;
    private LockPatternUtils mLockPatternUtils;
    private PendingIntent mPendingIntent = null;
    private IntentFilter[] mIntentFiltersArray = null;
    private String[][] mTechListsArray = null;
    private NfcAdapter mNfcAdapter = null;
    private boolean mAlreadySetup = false;
    private ListView mListView = null;
    private boolean mDialogShowing = false;
    private String mTagIds = null;
    private String mTagNames = null;
    private ArrayAdapter<String> mAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_unlock);
        mLockPatternUtils = new LockPatternUtils(this);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mListView = (ListView)findViewById(R.id.nfc_unlock_tag_list);
        mListView.setEmptyView(findViewById(R.id.nfc_unlock_tag_list_empty));
        mListView.setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                    int pos, long id) {
                String uuid = mTagIds.split("\\|")[pos];
                AlertDialog dialog = createDialog(uuid, false);
                dialog.show();
                mDialogShowing = true;
                return true;
            }
        });

        String[] tags = mLockPatternUtils.getNfcUnlockTags();
        mTagIds = tags[0];
        mTagNames = tags[1];
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        mListView.setAdapter(mAdapter);

        if (mTagNames != null) {
            String[] tagsNamesArr = mTagNames.split("\\|");
            mAdapter.addAll(tagsNamesArr);
        }
    }

    @Override
    public void onPause() {
        if (mNfcAdapter == null)
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter == null)
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter != null) {
            setUpForegroundDispatchSystem();
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent,
                    mIntentFiltersArray, mTechListsArray);
        }
    }

    private void setUpForegroundDispatchSystem() {
        if (mAlreadySetup)
            return;
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
            getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        IntentFilter tech = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

        mIntentFiltersArray = new IntentFilter[] {tech};
          mTechListsArray = new String[][] {
            new String[] { NfcA.class.getName() },
            new String[] { NfcB.class.getName() },
            new String[] { NfcF.class.getName() },
            new String[] { NfcV.class.getName() },
            new String[] { IsoDep.class.getName() },
            new String[] { MifareClassic.class.getName() },
            new String[] { MifareUltralight.class.getName() },
            new String[] { NdefFormatable.class.getName() },
            new String[] { Ndef.class.getName() }
        };

        mAlreadySetup = true;
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mDialogShowing)
            return;

        Tag t = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] uid = (byte[])t.getId();
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String uidStr = "";

        for (j = 0 ; j < uid.length ; ++j) {
            in = (int) uid[j] & 0xff;
            i = (in >> 4) & 0x0f;
            uidStr += hex[i];
            i = in & 0x0f;
            uidStr += hex[i];
        }

        if (mTagIds == null) {
            mTagIds = "";
            mTagNames = "";
        } else if (mTagIds.contains(uidStr + "|")) {
            Toast.makeText(this, R.string.nfc_unlock_tag_exists, Toast.LENGTH_SHORT).show();
            return;
          }

        AlertDialog dialog = createDialog(uidStr, true);
        dialog.show();
        dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        mDialogShowing = true;
    }

    private AlertDialog createDialog(final String uuid, final boolean addTag) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(20, 0, 30, 0);

        final EditText input = new EditText(this);
        if (addTag) {
            builder.setTitle(R.string.nfc_unlock_tag_detected);
            builder.setMessage(R.string.nfc_unlock_tag_name);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            input.requestFocus();
            layout.addView(input);
        } else {
            builder.setTitle(R.string.nfc_unlock_remove_tag);
            builder.setMessage(R.string.nfc_unlock_remove_tag_confirm);
        }
        builder.setView(layout);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (addTag) {
                    String name = input.getText().toString().replace("|", "_");
                    if (TextUtils.isEmpty(name))
                        name = getString(R.string.nfc_unlock_unnamed_tag);
                    mTagIds = mTagIds + uuid + "|";
                    mTagNames = mTagNames + name + "|";
                    mAdapter.add(name);
                } else {
                    String[] tagIds = mTagIds.split("\\|");
                    String[] tagNames = mTagNames.split("\\|");

                    if (tagIds.length == 1) {
                        mTagIds = null;
                        mTagNames = null;
                        mAdapter.clear();
                    } else {
                        mTagIds = "";
                        mTagNames = "";

                        for (int i = 0; i < tagIds.length; i++) {
                            if (tagIds[i].equals(uuid)) {
                                mAdapter.remove(tagNames[i]);
                            } else {
                                mTagIds = mTagIds + tagIds[i] + "|";
                                mTagNames = mTagNames + tagNames[i] + "|";
                            }
                        }
                    }
                }
                mAdapter.notifyDataSetChanged();
                mLockPatternUtils.setNfcUnlockTags(mTagIds, mTagNames);
                mDialogShowing = false;
                dialog.dismiss();
                return;
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mDialogShowing = false;
                dialog.dismiss();
                return;
            }
        });

        builder.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mDialogShowing = false;
                dialog.dismiss();
            }
        });

        final AlertDialog dialog = builder.create();
        input.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
                }
                return false;
            }
        });
        return dialog;
    }
}
