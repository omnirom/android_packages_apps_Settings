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

/**
 * Whitelist settings UI for quiet hours.
 */
public class WhitelistSettings extends ListFragment implements WhitelistEntryDialog.WhitelistEntryDialogListener {
    private static final String TAG = "WhitelistSettings";
    private WhitelistAdapter mAdapter;
    private TextView mEmptyView;
    private List<QuietHoursHelper.WhitelistContact> mContacts = new ArrayList<QuietHoursHelper.WhitelistContact>();
    private static String CALL_BYPASS_TAG = "call_bypass";
    private static String MESSAGE_BYPASS_TAG = "message_bypass";

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(com.android.internal.R.layout.preference_list_fragment,
                container, false);
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);

        loadContacts();
        setHasOptionsMenu(true);

        final Activity activity = getActivity();

        mAdapter = new WhitelistAdapter(getActivity(),
                android.R.layout.simple_list_item_single_choice, mContacts);

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);

        final ListView listView = getListView();
        listView.setAdapter(mAdapter);
        listView.setEmptyView(mEmptyView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.whitelist, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.whitelist_add).setEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.whitelist_add:
                showEntryEditDialog(null);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        QuietHoursHelper.WhitelistContact item = mContacts.get(position);
        showEntryEditDialog(item);
    }

    private void showEntryEditDialog(QuietHoursHelper.WhitelistContact item) {
        WhitelistEntryDialog fragment = WhitelistEntryDialog.newInstance(this, item);
        fragment.show(getFragmentManager(), "whitelist_edit");
    }

    private void loadContacts(){
        mContacts.clear();
        List<QuietHoursHelper.WhitelistContact> contacts = QuietHoursHelper.loadContacts(getActivity());
        mContacts.addAll(contacts);
    }

    private void saveContacts(){
        StringBuffer str = new StringBuffer();
        Iterator<QuietHoursHelper.WhitelistContact> nextContact = mContacts.iterator();
        while(nextContact.hasNext()){
            QuietHoursHelper.WhitelistContact contact = nextContact.next();
            str.append(contact.toString() + "||");
        }
        if (str.length() > 3){
            str = str.delete(str.length() - 2, str.length());
        }
        Settings.System.putString(getActivity().getContentResolver(), Settings.System.QUIET_HOURS_WHITELIST, str.toString());
    }

    @Override
    public void onFinishDelete(QuietHoursHelper.WhitelistContact contact) {
        mContacts.remove(contact);
        mAdapter.notifyDataSetChanged();
        saveContacts();
    }

    @Override
    public void onFinishAdd(QuietHoursHelper.WhitelistContact contact) {
        if(!mContacts.contains(contact)){
            mContacts.add(contact);
        }
        mAdapter.notifyDataSetChanged();
        saveContacts();
    }

    private class WhitelistAdapter extends ArrayAdapter<QuietHoursHelper.WhitelistContact>
            implements ToggleImageView.OnCheckedChangeListener {
        private String mCurrentCountryIso;
        private ContentResolver mResolver;

        public WhitelistAdapter(Context context, int resource,
                List<QuietHoursHelper.WhitelistContact> values) {
            super(context, R.layout.whitelist_entry_row, resource, values);

            final CountryDetector detector =
                    (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
            mCurrentCountryIso = detector.detectCountry().getCountryIso();
            mResolver = context.getContentResolver();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) { 
            ViewHolder holder;
            if (convertView != null) {
               holder = (ViewHolder) convertView.getTag();
            } else {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.whitelist_entry_row, parent, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
                holder.position = position;
                holder.mainText = (TextView) convertView.findViewById(R.id.number);
                holder.subText = (TextView) convertView.findViewById(R.id.name);
                holder.callBypass = (ToggleImageView) convertView.findViewById(R.id.bypass_calls);
                holder.callBypass.setOnCheckedChangeListener(this);
                holder.callBypass.setTag(CALL_BYPASS_TAG);
                holder.messageBypass = (ToggleImageView) convertView.findViewById(R.id.bypass_messages);
                holder.messageBypass.setOnCheckedChangeListener(this);
                holder.messageBypass.setTag(MESSAGE_BYPASS_TAG);
            }

            QuietHoursHelper.WhitelistContact item = mContacts.get(position);

            String number = item.mNumber;
            String name = lookupNameForNumber(number);
            String formattedNumber = PhoneNumberUtils.formatNumber(number,
                    null, mCurrentCountryIso);

            if (TextUtils.isEmpty(name)) {
                holder.mainText.setText(formattedNumber);
                holder.subText.setVisibility(View.GONE);
            } else {
                holder.mainText.setText(name);
                holder.subText.setText(formattedNumber);
                holder.subText.setVisibility(View.VISIBLE);
            }

            holder.callBypass.setCheckedInternal(item.mBypassCall, false);
            holder.messageBypass.setCheckedInternal(item.mBypassMessage, false);

            return convertView;
        }

        @Override
        public void onCheckedChanged(ToggleImageView view, boolean isChecked) {
            View parent = (View) view.getParent();
            ViewHolder holder = (ViewHolder) parent.getTag();
            QuietHoursHelper.WhitelistContact item = mContacts.get(holder.position);
            if (view.getTag().equals(CALL_BYPASS_TAG)){
                item.setBypassCall(view.isChecked());
            } else if (view.getTag().equals(MESSAGE_BYPASS_TAG)){
                item.setBypassMessage(view.isChecked());
            }
            saveContacts();
        }

        private String lookupNameForNumber(String number) {
            if (!TextUtils.isEmpty(mCurrentCountryIso)) {
                // Normalise the number: this is needed because the PhoneLookup query
                // below does not accept a country code as an input.
                String numberE164 = PhoneNumberUtils.formatNumberToE164(number,
                        mCurrentCountryIso);
                if (!TextUtils.isEmpty(numberE164)) {
                    // Only use it if the number could be formatted to E164.
                    number = numberE164;
                }
            }

            String result = null;
            final String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };
            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            Cursor cursor = mResolver.query(uri, projection, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    result = cursor.getString(0);
                }
                cursor.close();
            }

            return result;
        }

        private class ViewHolder {
            TextView mainText;
            TextView subText;
            ToggleImageView callBypass;
            ToggleImageView messageBypass;
            int position;
        }
    }
}
