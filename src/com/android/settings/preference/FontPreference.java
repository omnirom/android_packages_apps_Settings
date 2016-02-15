/*
 *  Copyright (C) 2016 The OmniROM Project
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
 package com.android.settings.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.graphics.Typeface;
import android.preference.ListPreference;
import android.util.Log;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.utils.FontManager;

public class FontPreference extends ListPreference {
    private TextView mFontSample;

    public FontPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_font_picker);

        HashMap< String, String > fonts = FontManager.enumerateFonts();
        List<CharSequence>fontPaths = new ArrayList<CharSequence>();
        List<CharSequence> fontNames = new ArrayList<CharSequence>();

        List<String> keys = new ArrayList<String>();
        keys.addAll(fonts.keySet());
        Collections.sort(keys);

        for (String name : keys) {
            fontNames.add(name);
            fontPaths.add(fonts.get(name));
        }

        setEntries(fontNames.toArray(new CharSequence[fontNames.size()]));
        setEntryValues(fontPaths.toArray(new CharSequence[fontPaths.size()]));

        //setEntries(getContext().getResources().getStringArray(R.array.font_picker_values));
        //setEntryValues(getContext().getResources().getStringArray(R.array.font_picker_values));
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mFontSample = (TextView) view.findViewById(R.id.font_sample);
        if (getValue() != null) {
            mFontSample.setTypeface(Typeface.createFromFile(getValue()));
            //mFontSample.setTypeface(Typeface.create(getValue(), Typeface.NORMAL));
        }
    }
}
