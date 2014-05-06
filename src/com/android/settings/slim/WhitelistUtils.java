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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 */
public class WhitelistUtils {
    private static final String TAG = "WhitelistUtils";
    private static final boolean DEBUG = false;

    public static class WhitelistContact {
        public String mNumber;
        public boolean mBypassCall;
        public boolean mBypassMessage;

        public WhitelistContact(String number, boolean bypassCall, boolean bypassMessage) {
            mNumber = number;
            mBypassCall = bypassCall;
            mBypassMessage = bypassMessage;
        }

        public WhitelistContact() {
        }

        public void setBypassCall(boolean value){
            mBypassCall = value;
        }

        public void setBypassMessage(boolean value){
            mBypassMessage = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (!(o instanceof WhitelistContact)) {
                return false;
            }

            WhitelistContact lhs = (WhitelistContact) o;
            return mNumber.equals(lhs.mNumber);
        }

        public String toString() {
            return mNumber + "##" + (mBypassCall ? "1" : "0") + "##" + (mBypassMessage ? "1" : "0");
        }

        public void fromString(String str) {
            String[] parts = str.split("##");
            mNumber = parts[0];
            mBypassCall = Integer.parseInt(parts[1]) == 1;
            mBypassMessage = Integer.parseInt(parts[2]) == 1;
        }
    }

    public static List<WhitelistContact> loadContacts(Context context){
        List<WhitelistContact> contacts = new ArrayList<WhitelistContact>();

        String str = Settings.System.getString(context.getContentResolver(), Settings.System.QUIET_HOURS_WHITELIST);
        if (str != null && str.length() != 0){
            String[] parts = str.split("\\|\\|");
            for (int i = 0; i < parts.length; i++){
                WhitelistContact contact = new WhitelistContact();
                contact.fromString(parts[i]);
                contacts.add(contact);
            }
        }
        return contacts;
    }

    public static boolean isCallBypass(Context context, String number){
        List<WhitelistContact> contacts = loadContacts(context);
        Iterator<WhitelistContact> nextContact = contacts.iterator();
        while (nextContact.hasNext()){
            WhitelistContact contact = nextContact.next();
            if (contact.mNumber.equals(number)){
                return contact.mBypassCall;
            }
        }
        return false;
    }

    public static boolean hasCallBypass(Context context){
        List<WhitelistContact> contacts = loadContacts(context);
        Iterator<WhitelistContact> nextContact = contacts.iterator();
        while (nextContact.hasNext()){
            WhitelistContact contact = nextContact.next();
            if (contact.mBypassCall){
                return true;
            }
        }
        return false;
    }

    public static boolean isMessageBypass(Context context, String number){
        List<WhitelistContact> contacts = loadContacts(context);
        Iterator<WhitelistContact> nextContact = contacts.iterator();
        while (nextContact.hasNext()){
            WhitelistContact contact = nextContact.next();
            if (contact.mNumber.equals(number)){
                return contact.mBypassMessage;
            }
        }
        return false;
    }

    public static boolean hasMessageBypass(Context context){
        List<WhitelistContact> contacts = loadContacts(context);
        Iterator<WhitelistContact> nextContact = contacts.iterator();
        while (nextContact.hasNext()){
            WhitelistContact contact = nextContact.next();
            if (contact.mBypassMessage){
                return true;
            }
        }
        return false;
    }

    public static boolean hasBypass(Context context){
        List<WhitelistContact> contacts = loadContacts(context);
        Iterator<WhitelistContact> nextContact = contacts.iterator();
        while (nextContact.hasNext()){
            WhitelistContact contact = nextContact.next();
            if (contact.mBypassCall || contact.mBypassMessage){
                return true;
            }
        }
        return false;
    }
}
