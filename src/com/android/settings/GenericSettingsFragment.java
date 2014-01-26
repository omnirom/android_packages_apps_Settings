package com.android.settings;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

// Referenced classes of package com.android.settings:
//            SettingsPreferenceFragment

public class GenericSettingsFragment extends SettingsPreferenceFragment
{

    public GenericSettingsFragment()
    {
    }

    protected int getHelpResource()
    {
        return mHelpResource;
    }

    public void onCreate(Bundle bundle)
    {
        Resources resources = getResources();
        Bundle bundle1 = getArguments();
        String s = bundle1.getString("extra_fragment_preference_xml");
        int i = resources.getIdentifier(s, "xml", getActivity().getPackageName());
        String s1 = bundle1.getString("extra_fragment_help_resource");
        int j;
        if(s1 != null)
            j = resources.getIdentifier(s1, "string", getActivity().getPackageName());
        else
            j = 0;
        mHelpResource = j;
        super.onCreate(bundle);
        if(i != 0)
        {
            addPreferencesFromResource(i);
//            customizePreferences();
            return;
        } else
        {
            Log.e("GenericSettingsFragment", (new StringBuilder()).append("Missing valid extra_fragment_preference_xml=").append(s).toString());
            return;
        }
    }

    private int mHelpResource;
}
