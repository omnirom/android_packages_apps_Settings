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

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.android.settings.R;

public class ChangesFragment extends ListFragment implements LoaderManager.LoaderCallbacks<ArrayList<Map<String, String>>>
{

    final   static String                          GERRIT_URL  = "https://gerrit.omnirom.org/";
    final   static SimpleDateFormat                mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    private static ArrayList<Map<String, String>>  mListData;
    private           BaseAdapter                     mAdapter;
    

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        
        getActivity().getActionBar().setTitle(R.string.changelog_title);
        setEmptyText(getResources().getString(R.string.changelog_no_changes));
        
        setListAdapter(mAdapter);
        setListShown(false);
        getActivity().getLoaderManager().initLoader(10, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public Loader<ArrayList<Map<String, String>>> onCreateLoader(int arg0, Bundle arg1)
    {
        MyLoader loader = new MyLoader(getActivity(),this);
        loader.forceLoad();
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Map<String, String>>> arg0, ArrayList<Map<String, String>> arg1)
    {
        mListData = arg1;
        getListView().setAdapter(new ChangesAdapter(getActivity(), arg1));
        setListShown(true);
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Map<String, String>>> arg0)
    {

    }

    @Override
    public void onListItemClick(ListView l, View v, final int position, long id)
    {
        new AlertDialog.Builder(getActivity())
        .setCancelable(true)
        .setTitle(R.string.changelog_commit_message)
        .setMessage(mListData.get(position).get("message"))
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton("Gerrit", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Uri uri = Uri.parse(GERRIT_URL+"#/c/" + mListData.get(position).get("number"));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        })
        .show();
    }
    
    static class MyLoader extends AsyncTaskLoader<ArrayList<Map<String, String>>>
    {

        private Activity     mActivity;
        private JSONObject   mCurrentObject;
        private Date         mDate;
        private ListFragment mListFragment;
        private JSONObject   mJSONTemp;
        
        public MyLoader(Activity a,ListFragment lf)
        {
            super(a);
            mActivity = a;
            mListFragment = lf;
        }

        @Override
        public ArrayList<Map<String, String>> loadInBackground()
        {

            ArrayList<Map<String, String>> load_list = new ArrayList<Map<String, String>>();

            String previousSortKey = "";
            boolean needsMode = true;
            // It will load 5 items at once until the date is less than Build.TIME
            while(needsMode)
            {
                JSONArray mJSONArray = null;
                try
                {
                    String query_url = GERRIT_URL+"changes/?q=status:merged&pp=0&o=CURRENT_REVISION&o=CURRENT_COMMIT&n=5";
                    if(!previousSortKey.equals(""))
                        query_url = query_url + "&N=" + previousSortKey;
                    
                    HttpURLConnection con = (HttpURLConnection) new URL(query_url).openConnection();
                    String res = convertStreamToString(con.getInputStream());
                    res = res.replace(")]}'", ""); // Gerrit uses malformed JSON
                    mJSONArray = new JSONArray(res);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
    
                if (mJSONArray != null)
                {
                    // Loop through changes
                    for (int i = 0; i < mJSONArray.length(); i++)
                    {
                        try
                        {
                            mCurrentObject = mJSONArray.getJSONObject(i);
                            mDate = mDateFormat.parse(mCurrentObject.getString("updated"));
    
                            if (mDate.getTime() > Build.TIME)
                            {
                                HashMap<String, String> new_item = new HashMap<String, String>();
                                new_item.put("title", mCurrentObject.getString("subject"));
                                new_item.put("owner", mDateFormat.format(mDate.getTime()) + " by " +
                                                ((JSONObject) mCurrentObject.get("owner")).getString("name"));
                                new_item.put("number", mCurrentObject.getString("_number"));
                                
                                mJSONTemp = (JSONObject) mCurrentObject.get("revisions");   // Revisions
                                mJSONTemp = (JSONObject) mJSONTemp.get(mCurrentObject.getString("current_revision")); // Current revision
                                mJSONTemp = (JSONObject) mJSONTemp.get("commit");             // Current commit
                                new_item.put("message", mJSONTemp.getString("message"));    // Commit message
                                load_list.add(new_item);
                            }
                            else{
                                needsMode = false; // Stop loading new data
                                break;                 // No need to loop through older data.
                            }
                            
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    
                    try{
                        previousSortKey = mJSONArray.getJSONObject(4).getString("_sortkey");
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        needsMode = false; // Stop loading new data to prevent loop
                        mActivity.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                mListFragment.setEmptyText(mActivity.getResources().getString(R.string.changelog_parse_error));
                            }
                        });
                    }
                }
                else{
                    mActivity.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mListFragment.setEmptyText(mActivity.getResources().getString(R.string.changelog_network_error));
                        }
                    });
                    needsMode = false; // Stop loading new data to prevent loop
                }
            }

            return load_list;
        }

        @Override
        public void deliverResult(ArrayList<Map<String, String>> data)
        {
            super.deliverResult(data);
            mListFragment.getListView().setAdapter(new ChangesAdapter(mActivity, data));
        }
    }

    static String convertStreamToString(java.io.InputStream is)
    {
        java.util.Scanner scanner = new java.util.Scanner(is);
        scanner.useDelimiter("\\A");
        String ret = scanner.hasNext() ? scanner.next() : "";
        scanner.close();
        return ret;
    }

}
