/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Switch;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.LinkedList;

import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.CachedPlace;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationClusterer;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleInterface;

public class SecondarySettingsActivity extends Activity {

    public static final int SET_FIXED_LOCATION = 0;
    public static final int DISPLAY_PLACE_POLICIES = 1;
    Switch mainSwitch;
    ViewGroup test1;
    ViewGroup test2;
    RuleInterface ruleBridge;
    LinkedList<Integer> placeListTotal;
    LinkedList<Integer> placeListInRules;
    GoogleMap map;
    String packName;
    OnInfoWindowClickListener infoList = new OnInfoWindowClickListener() {

        @Override
        public void onInfoWindowClick(Marker marker) {
            String title = marker.getTitle();
            //get place from the marker
            String[] fields = title.split(": ");
            int placeDerived = Integer.parseInt(fields[1].trim());
            //System.out.println("::"+placeDerived+"::");
            openGlobalActivity(placeDerived);
        }

    };

    //two options here, either from main menu, or from global settings; basically re-use the UI
    OnMapLongClickListener mapList = new OnMapLongClickListener() {

        @Override
        public void onMapLongClick(LatLng location) {
            // ask user if to add a rule in that location
            // go to the global settings activity
            // add the rule there according to the below
            // below are done in transaction style :)
            // create a new place for this marker
            // add the rule
            //System.out.println("long press");
            Marker marker = map.addMarker(new MarkerOptions()
                    .title("new policy")//get the city from the DB for example?
                    .position(location));
            presentDialog(location);
            marker.remove();
        }
    };
    OnMapLongClickListener mapListFixed = new OnMapLongClickListener() {

        @Override
        public void onMapLongClick(LatLng location) {
            // ask user if to add a rule in that location
            // go to the global settings activity
            // add the rule there according to the below
            // below are done in transaction style :)
            // create a new place for this marker
            // add the rule
            //System.out.println("long press");
            //extract location
            //present dialog
            //destroy and send intent to parent activity to set the UI elements
            presentDialogFixedLoc(location);
        }
    };
    private int option;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary_settings);

        Intent intent = getIntent();
        option = intent.getExtras().getInt("option");
        map = initMap();

        if (option == SET_FIXED_LOCATION) {
            // probably do nothing?
            map.setOnMapLongClickListener(mapListFixed);
        } else if (option == DISPLAY_PLACE_POLICIES) {
            //fetch the rule?
            packName = intent.getExtras().getString("appName");
            if (packName == null) {
                onDestroy();
            }
            setMetaData(packName); // keep it :)
            placeListInRules = getKeys(packName);
            placeListTotal = LocationClusterer.getAllPlaces();
            addAllMarkers();
            map.setOnMapLongClickListener(mapList);
            map.setOnInfoWindowClickListener(infoList);

        } else {
            //destroy
            onDestroy();
        }
    }

    //refresh :)
    @Override
    protected void onResume() {
        super.onResume();
        if (option == DISPLAY_PLACE_POLICIES) {
            placeListInRules = getKeys(packName);
            placeListTotal = LocationClusterer.getAllPlaces();
            map.clear();
            addAllMarkers();
        }
    }

    private void addAllMarkers() {
        for (int place : placeListTotal) {
            addMarker(place);
        }
    }

    private LinkedList<Integer> getKeys(String packName) {
        ruleBridge = new RuleInterface();
        RuleData currentRules = ruleBridge.fetchAllRules(packName);
        return currentRules.getPlaces();
    }

    private GoogleMap initMap() {
        // Get a handle to the Map Fragment
        GoogleMap map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        //LatLng current = CachedPlace.getCityLoc();
        LatLng current = CachedPlace.currentLoc;
        //String city = CachedPlace.getCityName();
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 9));


        return map;
    }

    private void addMarker(int place) {
        if (placeListInRules.contains(place)) {
            LatLng loc = LocationClusterer.getLoc(place);
            map.addMarker(new MarkerOptions()
                            .title("Policy ID: " + place)//get the city from the DB for example?
                            .snippet("click here to view/edit policy")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            .position(loc)
            );
        } else {
            LatLng loc = LocationClusterer.getLoc(place);
            map.addMarker(new MarkerOptions()
                            .title("Policy ID: " + place)//get the city from the DB for example?
                            .snippet("click here to create new policy")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            .position(loc)
            );
        }

    }

    private void openGlobalActivity(int place) {
        Intent intent = new Intent(this, GlobalSettingsActivity.class);
        intent.putExtra("appName", packName);
        intent.putExtra("placeID", place); //global rule
        startActivity(intent);
    }

    private void presentDialogFixedLoc(final LatLng loc) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(Util.getAppName(packName, this));
        alertDialog.setMessage("Do you want to choose this location ?");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
            }
        });

        //go to map activity to choose from places.
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //send the intent
                Intent intent = new Intent("MyMainActivity.INTENT_ACTION");
                intent.putExtra("action", "fixedLocation"); //new rule
                intent.putExtra("Location", loc); //new rule
                sendBroadcast(intent);
                //close this popup
                //onDestroy();
                finish();
            }
        });

        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.show();
    }

    private void presentDialog(final LatLng loc) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(Util.getAppName(packName, this));
        alertDialog.setMessage("Do you want to add a policy for this location ?");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //do nothing
            }
        });

        //go to map activity to choose from places.
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(SecondarySettingsActivity.this, GlobalSettingsActivity.class);
                intent.putExtra("appName", packName);
                intent.putExtra("placeID", -2); //new rule
                intent.putExtra("Location", loc); //new rule
                startActivity(intent);
            }
        });

        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.show();
    }


    private void setMetaData(String packName) {

        String appName = Util.getAppName(packName, this);
        setTitle(appName);

        Drawable iconTemp;
        try {
            iconTemp = getPackageManager().getApplicationIcon(packName);
            getActionBar().setIcon(iconTemp);

        } catch (Exception e) {

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.secondary_settings, menu);
        return true;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
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

}
