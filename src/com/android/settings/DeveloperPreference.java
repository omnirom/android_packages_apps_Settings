/*
 * Copyright (C) 2014 AnimeROM
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

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.net.Uri;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DeveloperPreference extends Preference {
    private static final String TAG = "DeveloperPreference";
    private ImageView twitterButton;
    private ImageView githubButton;

    private TextView devName;

    private String nameDev;
    private String twitterName;
    private String githubLink;
    private final Display mDisplay;
    private TypedArray typedArray;
    private TextView twitter;

    public DeveloperPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        typedArray = context.obtainStyledAttributes(attrs, R.styleable.DeveloperPreference);
        nameDev = typedArray.getString(R.styleable.DeveloperPreference_nameDev);
        twitterName = typedArray.getString(R.styleable.DeveloperPreference_twitterHandle);
        githubLink = typedArray.getString(R.styleable.DeveloperPreference_githubLink);

        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        super.onCreateView(parent);
        View layout = View.inflate(getContext(), R.layout.dev_pref, null);
        twitterButton = (ImageView) layout.findViewById(R.id.twitter_button);
        githubButton = (ImageView) layout.findViewById(R.id.github_button);
        devName = (TextView) layout.findViewById(R.id.name);
        return layout;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        try {
            if (githubLink != null) {
                final OnClickListener openGithub = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri githubURL = Uri.parse(githubLink);
                        final Intent intent = new Intent(Intent.ACTION_VIEW, githubURL);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        getContext().startActivity(intent);
                    }
                };
                githubButton.setOnClickListener(openGithub);
            } else {
                githubButton.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            // Do nothing
        }

        try {
            if (twitterName != null) {
                final OnPreferenceClickListener openTwitter = new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Uri twitterURL = Uri.parse("http://twitter.com/#!/" + twitterName);
                        final Intent intent = new Intent(Intent.ACTION_VIEW, twitterURL);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        getContext().startActivity(intent);
                        return true;
                    }
                };

                // changed to clicking the preference to open twitter
                // it was a hit or miss to click the twitter bird
                this.setOnPreferenceClickListener(openTwitter);
            } else {
                twitterButton.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            // Do nothing
        }
        if (devName != null) {
            devName.setText(nameDev);
        }
    }
}
