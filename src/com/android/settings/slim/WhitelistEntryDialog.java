/*
 *  Copyright (C) 2014 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.settings.slim;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DialerKeyListener;
import android.text.method.ArrowKeyMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.blacklist.ToggleImageView;
import com.android.internal.util.slim.QuietHoursHelper;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

public class WhitelistEntryDialog extends DialogFragment implements
        TextWatcher, DialogInterface.OnClickListener {

    private EditText mEditText;
    private ImageButton mContactPickButton;
    private CheckBox mBypassCalls;
    private CheckBox mBypassMessages;
    private Button mOkButton;
    public QuietHoursHelper.WhitelistContact mContact;
    public WhitelistSettings mParent;

    private static final String[] NUMBER_PROJECTION = { CommonDataKinds.Phone.NUMBER };

    private static final int REQUEST_CODE_PICKER = 1;
    private static final int COLUMN_NUMBER = 0;

    public interface WhitelistEntryDialogListener {
        void onFinishDelete(QuietHoursHelper.WhitelistContact contact);

        void onFinishAdd(QuietHoursHelper.WhitelistContact contact);
    }

    public static WhitelistEntryDialog newInstance(WhitelistSettings parent,
            QuietHoursHelper.WhitelistContact contact) {
        WhitelistEntryDialog fragment = new WhitelistEntryDialog();
        fragment.mParent = parent;
        fragment.mContact = contact;
        return fragment;
    }

    public WhitelistEntryDialog() {
        super();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.quiet_hours_whitelist_edit_dialog_title)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, null)
                .setView(createDialogView());

        if (mContact != null) {
            builder.setNeutralButton(R.string.blacklist_button_delete, this);
        }
        return builder.create();
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog dialog = (AlertDialog) getDialog();
        Button neutralButton = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
        neutralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mParent.onFinishDelete(mContact);
                dismiss();
            }
        });
        updateOkButtonState();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            updateEntry();
        }
    }

    private View createDialogView() {
        final Activity activity = getActivity();
        final LayoutInflater inflater = (LayoutInflater) activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(
                R.layout.dialog_whitelist_edit_entry, null);

        mEditText = (EditText) view.findViewById(R.id.number_edit);
        mEditText.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        mEditText.setKeyListener(DialerKeyListener.getInstance());
        mEditText.addTextChangedListener(this);

        mContactPickButton = (ImageButton) view
                .findViewById(R.id.select_contact);
        mContactPickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent contactListIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contactListIntent
                        .setType(CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

                startActivityForResult(contactListIntent, REQUEST_CODE_PICKER,
                        null);
            }
        });

        mBypassCalls = (CheckBox) view.findViewById(R.id.bypass_calls);
        mBypassMessages = (CheckBox) view.findViewById(R.id.bypass_messages);

        if (mContact != null) {
            mEditText.setText(mContact.mNumber);
            mBypassCalls.setChecked(mContact.mBypassCall);
            mBypassMessages.setChecked(mContact.mBypassMessage);
        } else {
            mEditText.setText("");
            mBypassCalls.setChecked(true);
            mBypassMessages.setChecked(true);
        }

        return view;
    }

    private void updateEntry() {
        String number = mEditText.getText().toString();
        boolean bypassCall = mBypassCalls.isChecked();
        boolean bypassMessage = mBypassMessages.isChecked();

        if (mContact == null) {
            QuietHoursHelper.WhitelistContact contact = new QuietHoursHelper.WhitelistContact(
                    number, bypassCall, bypassMessage);
            mContact = contact;
        } else {
            mContact.mNumber = number;
            mContact.mBypassCall = bypassCall;
            mContact.mBypassMessage = bypassMessage;
        }
        mParent.onFinishAdd(mContact);
    }

    private void updateOkButtonState() {
        if (mOkButton == null) {
            AlertDialog dialog = (AlertDialog) getDialog();
            if (dialog != null) {
                mOkButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            }
        }

        if (mOkButton != null) {
            mOkButton.setEnabled(mEditText.getText().length() != 0);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateOkButtonState();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_PICKER) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            Cursor cursor = getActivity().getContentResolver().query(
                    data.getData(), NUMBER_PROJECTION, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    mEditText.setText(cursor.getString(COLUMN_NUMBER));
                }
                cursor.close();
            }
        }
    }
}
