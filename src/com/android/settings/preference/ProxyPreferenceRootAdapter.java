package com.android.settings.preference;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class ProxyPreferenceRootAdapter extends BaseAdapter
{

    public ProxyPreferenceRootAdapter(Context context, ListAdapter listadapter)
    {
        mRootAdapter = listadapter;
        getThemeColors(context);
    }

    private void changeTextColorIfNecessary(View view, int i)
    {
        if(view instanceof TextView)
        {
            TextView textview = (TextView)view;
            if(textview.getTextColors().getDefaultColor() != i)
                textview.setTextColor(i);
        }
    }

    private void getThemeColors(Context context)
    {
        TypedArray typedarray = context.obtainStyledAttributes(new int[] {
            0x1010036, 0x1010038
        });
        mTextColorPrimary = typedarray.getColor(0, 0);
        mTextColorSecondary = typedarray.getColor(1, 0);
        typedarray.recycle();
    }

    public int getCount()
    {
        if(mRootAdapter != null)
            return mRootAdapter.getCount();
        else
            return 0;
    }

    public Object getItem(int i)
    {
        if(mRootAdapter != null)
            return mRootAdapter.getItem(i);
        else
            return null;
    }

    public long getItemId(int i)
    {
        if(mRootAdapter != null)
            return mRootAdapter.getItemId(i);
        else
            return 0L;
    }

    public View getView(int i, View view, ViewGroup viewgroup)
    {
        if(mRootAdapter != null)
        {
            View view1 = mRootAdapter.getView(i, view, viewgroup);
            if(view1 != null && mRootAdapter.getItemViewType(i) == -1)
            {
                changeTextColorIfNecessary(view1.findViewById(0x1020016), mTextColorPrimary);
                changeTextColorIfNecessary(view1.findViewById(0x1020010), mTextColorSecondary);
            }
            return view1;
        } else
        {
            return null;
        }
    }

    ListAdapter mRootAdapter;
    int mTextColorPrimary;
    int mTextColorSecondary;
}
