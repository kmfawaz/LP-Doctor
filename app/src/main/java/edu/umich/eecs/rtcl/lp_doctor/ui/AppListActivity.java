/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.LinkedList;

import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.Util;

//import android.content.pm.PackageManager.NameNotFoundException;
//import android.graphics.drawable.Drawable;

public class AppListActivity extends Activity {

    LinkedList<String> appList;
    //open the corresponding setting activity depending on the option chosen
    private OnClickListener AppPaneListener = new OnClickListener() {
        public void onClick(final View v) {
            String pack = appList.get(v.getId() - 1);
            String title = Util.getAppName(pack, AppListActivity.this);
            presentDialog(title, pack);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_list);

        View view = this.getWindow().getDecorView();
        view.setBackgroundColor(android.graphics.Color.DKGRAY);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.container, new PlaceholderFragment()).commit(); //no idea what this does :P
        }
        appList = Util.getInstalledApps(this);
        createImageViews();
    }

    void createImageViews() {

        LinearLayout layout = (LinearLayout) findViewById(R.id.main_layout);
        layout.setBackgroundColor(android.graphics.Color.DKGRAY);
        for (int i = 0; i < appList.size(); i++) {
            AppPane temp = new AppPane(this, appList.get(i));
            temp.setId(i + 1);
            RelativeLayout.LayoutParams vp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (i >= 1) {
                vp.addRule(RelativeLayout.BELOW, i);
            }
            temp.setLayoutParams(vp);
            temp.setOnClickListener(AppPaneListener);
            layout.addView(temp, vp);
        }
    }

    private void presentDialog(String title, final String pack) {
        AlertDialog alertDialog = new AlertDialog.Builder(AppListActivity.this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage("View/Edit Global policy, or set policy by location?");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "global policy", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(AppListActivity.this, GlobalSettingsActivity.class);
                intent.putExtra("appName", pack);
                intent.putExtra("placeID", -1); //global rule
                intent.putExtra("option", SecondarySettingsActivity.DISPLAY_PLACE_POLICIES);
                startActivity(intent);
            }
        });

        //go to map activity to choose from places.
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, " per location policy", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(AppListActivity.this, SecondarySettingsActivity.class);
                intent.putExtra("appName", pack);
                intent.putExtra("option", SecondarySettingsActivity.DISPLAY_PLACE_POLICIES);
                startActivity(intent);
            }
        });

        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_app_list, container, false);
            return rootView;
        }
    }

}
