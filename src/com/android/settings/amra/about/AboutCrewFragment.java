package com.android.settings.amra.about;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.R;

import java.util.Random;


public class AboutCrewFragment extends Fragment {

    public AboutCrewFragment() {
        // empty fragment constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about_amra_crew, container, false);

        final Random rng = new Random();

        ViewGroup crewGroup = (ViewGroup) root.findViewById(R.id.crew);

        // remove all developers from the view randomize them, add em back
        int N = crewGroup.getChildCount();
        while (N > 0) {
            final View removed = crewGroup.getChildAt(rng.nextInt(N));
            crewGroup.removeView(removed);
            crewGroup.addView(removed);
            N -= 1;
        }

        return root;
    }

    private void launchActivity(String packageName, String activity)
            throws ActivityNotFoundException {
        Intent launch = new Intent();
        launch.setComponent(new ComponentName(packageName, packageName + activity));
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getActivity().startActivity(launch);
    }

}
