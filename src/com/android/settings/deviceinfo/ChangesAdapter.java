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
package com.android.settings.deviceinfo;

import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.settings.R;

public class ChangesAdapter extends BaseAdapter {

    ArrayList<Map<String, String>> mArrayList;
    Activity mActivity;
    private final LayoutInflater mInflater;

    public ChangesAdapter(Activity a, ArrayList<Map<String, String>> arrayList)
    {
        mArrayList = arrayList;
        mActivity = a;
        mInflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mArrayList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;

        if (convertView == null)
            view = mInflater.inflate(R.layout.changelog_list_entry, parent, false);
        else
            view = convertView;
        
        ((TextView)view.findViewById(R.id.title)).setText(mArrayList.get(position).get("title"));
        ((TextView)view.findViewById(R.id.owner)).setText(mArrayList.get(position).get("owner"));
        
        return view;
    }
}
