/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationClusterer;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData.SingleRule;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleInterface;

public class GlobalSettingsActivity extends Activity {

    String packName = "";
    int placeID = -2;
    RuleInterface ruleBridge;
    RuleData currentRules;
    Button apply;
    Button reset;
    Button remove;
    Button getFromMap;
    RadioGroup backRadioGroup;
    RadioGroup foreRadioGroup;
    SeekBar trackLevel;
    SeekBar profLevel;
    EditText latitude;
    EditText longitude;
    SparseIntArray foreMapping;
    SparseIntArray backMapping;
    Switch globalCtl;

    RadioButton highdiffprivacy;
    RadioButton fixed;
    TextView trackLevelDisplay;
    TextView profileLevelDisplay;
    TextView placeIDView;
    OnCheckedChangeListener highdiffprivacyList = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            System.out.println("highdiffprivacyList  " + isChecked);
            trackLevel.setEnabled(isChecked);
            if (placeID == -1) {
                profLevel.setEnabled(isChecked);
            }
            latitude.setEnabled(fixed.isChecked());
            longitude.setEnabled(fixed.isChecked());
            getFromMap.setEnabled(fixed.isChecked());
        }
    };
    OnCheckedChangeListener fixedList = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            System.out.println("fixedList  " + isChecked);
            trackLevel.setEnabled(highdiffprivacy.isChecked());
            if (placeID == -1) {
                profLevel.setEnabled(highdiffprivacy.isChecked());
            }
            latitude.setEnabled(isChecked);
            longitude.setEnabled(isChecked);
            getFromMap.setEnabled(fixed.isChecked());
        }
    };
    OnClickListener removeList = new OnClickListener() {

        @Override
        public void onClick(View v) {
            //have to find everything from the UI
            boolean res = ruleBridge.removeRule(packName, placeID);
            if (res) {
                Toast.makeText(getApplicationContext(), "removed rule from db", Toast.LENGTH_LONG).show();
                globalCtl.setChecked(false);
            }
        }

    };
    String trackLevelStrs[] = {"None", "Low", "Medium", "High"};
    int trackLevelClrs[] = {android.graphics.Color.rgb(255, 0, 0),
            android.graphics.Color.rgb(255, 128, 0),
            android.graphics.Color.rgb(255, 178, 102),
            android.graphics.Color.rgb(0, 255, 0)};

    //highdiffprivacy.setOnCheckedChangeListener(highdiffprivacyList);
    //fixed.setOnCheckedChangeListener(fixedList);
    OnSeekBarChangeListener trackLevelList = new OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //progress should belong to: {0,1,2,3}
            trackLevelDisplay.setText(trackLevelStrs[progress]);
            trackLevelDisplay.setTextColor(trackLevelClrs[progress]);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
    OnSeekBarChangeListener profileLevelList = new OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //progress should belong to: {0,1,2,3}
            profileLevelDisplay.setText(trackLevelStrs[progress]);
            profileLevelDisplay.setTextColor(trackLevelClrs[progress]);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };
    OnClickListener applyList = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //have to find everything from the UI
            int foreOption = getForegroundOption();
            int backOption = getBackgroundOption();
            int persOption = getPersistentOption();
            int track = getTrackLevel();
            int prof = getProfLevel();
            double latFixed = getFixedLatitude();
            double lonFixed = getFixedLongitude();
            SingleRule newRule = new SingleRule(placeID, foreOption, backOption, persOption, track, prof, latFixed, lonFixed);
            ruleBridge.addRule(packName, placeID, newRule);
            Toast.makeText(getApplicationContext(), "added new rule into db", Toast.LENGTH_LONG).show();
        }

    };
    OnClickListener resetList = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //got to clear the UI
        }

    };
    OnClickListener getFromMapList = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //pop up a map
            //let the user select the location from the map
            Intent intent = new Intent(GlobalSettingsActivity.this, SecondarySettingsActivity.class);
            intent.putExtra("option", SecondarySettingsActivity.SET_FIXED_LOCATION);
            startActivity(intent);
        }

    };
    OnCheckedChangeListener mainSwitchListen = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) { //ON
                setEnabledTotal(true);
            } else { //OFF
                setEnabledTotal(false);
            }
        }
    };
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent locationIntent) {
            // Extract data included in the Intent
            String action = locationIntent.getStringExtra("action");
            System.out.println("location intent received in global settings activity");
            if (action.equals("fixedLocation")) {
                LatLng location = (LatLng) locationIntent.getExtras().get("Location");
                if (location != null) {
                    System.out.println("location intent received in global settings activity");
                    latitude.setText(location.latitude + "");
                    longitude.setText(location.longitude + "");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_settings);

        reset = (Button) findViewById(R.id.reset);
        reset.setOnClickListener(resetList);
        remove = (Button) findViewById(R.id.removeRule);
        remove.setOnClickListener(removeList);
        apply = (Button) findViewById(R.id.apply);
        apply.setOnClickListener(applyList);
        getFromMap = (Button) findViewById(R.id.get_from_map);
        getFromMap.setOnClickListener(getFromMapList);
        fillMapping();

        foreRadioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        backRadioGroup = (RadioGroup) findViewById(R.id.radioGroup2);

        highdiffprivacy = (RadioButton) findViewById(R.id.highdiffprivacy);
        fixed = (RadioButton) findViewById(R.id.fixed);

        highdiffprivacy.setOnCheckedChangeListener(highdiffprivacyList);
        fixed.setOnCheckedChangeListener(fixedList);

        trackLevel = (SeekBar) findViewById(R.id.tracklevel);
        trackLevel.setOnSeekBarChangeListener(trackLevelList);
        trackLevelDisplay = (TextView) findViewById(R.id.trackLevelDisplay);

        profLevel = (SeekBar) findViewById(R.id.proflevel);
        profLevel.setOnSeekBarChangeListener(profileLevelList);
        profileLevelDisplay = (TextView) findViewById(R.id.profileLevelDisplay);

        latitude = (EditText) findViewById(R.id.lat_global);
        longitude = (EditText) findViewById(R.id.lon_global);
        globalCtl = (Switch) findViewById(R.id.main_switch);
        globalCtl.setOnCheckedChangeListener(mainSwitchListen);

        placeIDView = (TextView) findViewById(R.id.placeIDView);


        Intent intent = getIntent();
        packName = intent.getExtras().getString("appName");
        placeID = intent.getExtras().getInt("placeID");

        //get place from here as well. this applies from both
        //place = -1 --> global
        //place = -2 --> new place
        //place o.w. --> localized
        //fetch the rule?

        if (placeID < -2) {
            super.finish();
        }

        if (placeID == -2) {
            //need to create a new place for this locaton
            LatLng loc = (LatLng) intent.getExtras().get("Location");
            if (loc == null) {
                super.finish();
            }
            placeID = LocationClusterer.getPlaceForUI(loc, 10);
        }

        setMetaData(packName);
        fetchRule(placeID);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        //by this point of time we have a valid placeid to fill the rule in!
        registerReceiver(mMessageReceiver, new IntentFilter("MyMainActivity.INTENT_ACTION"));
    }

    @Override
    public void onResume() {
        //fetchRule(placeID);
        super.onResume();
    }

    public void fetchRule(int place) {
        ruleBridge = new RuleInterface();
        currentRules = ruleBridge.fetchAllRules(packName);
        SingleRule rule = currentRules.getRuleByPlace(place);
        if (rule == null) {
            System.out.println("need to create a new policy for app: " + packName + " and place: " + place);
            globalCtl.setChecked(false);
        } else {
            System.out.println(rule.toString());
            globalCtl.setChecked(true);
            populateRule(rule);
        }
    }

    private void populateRule(SingleRule rule) {
        //fill in everything
        setForegroundOption(rule.foreRule);
        setBackgroundOption(rule.persRule);
        //setPersistentOption (rule.persRule);
        setTrackLevel(rule.trackLevel);
        setProfLevel(rule.profLevel);
        setFixedLatitude(rule.lat);
        setFixedLongitude(rule.lon);

        if (placeID > -1) {
            profLevel.setEnabled(false);
        }

        if (rule.foreRule != RuleData.HIGH_DIFF_PRIV) {
            trackLevel.setEnabled(false);
            profLevel.setEnabled(false);
        }

        if (rule.foreRule != RuleData.FIXED) {
            latitude.setEnabled(false);
            longitude.setEnabled(false);
            getFromMap.setEnabled(false);
        }

        if (rule.foreRule == RuleData.CITY_LEVEL || rule.foreRule == RuleData.BLOCK) {
            //force change this way :)
            trackLevel.setProgress(0);
            profLevel.setProgress(0);
            trackLevel.setProgress(trackLevel.getMax());
            profLevel.setProgress(trackLevel.getMax());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.global_settings, menu);
        return true;
    }

    private void fillMapping() {
        foreMapping = new SparseIntArray();
        foreMapping.put(RuleData.NO_ACTION, R.id.noaction);
        foreMapping.put(RuleData.BLOCK, R.id.block);
        foreMapping.put(RuleData.CITY_LEVEL, R.id.citylevel);
        foreMapping.put(RuleData.HIGH_DIFF_PRIV, R.id.highdiffprivacy);
        foreMapping.put(RuleData.LOW_DIFF_PRIV, R.id.lowdiffprivacy);
        foreMapping.put(RuleData.FIXED, R.id.fixed);

        backMapping = new SparseIntArray();
        backMapping.put(RuleData.NO_ACTION, R.id.noaction_back);
        backMapping.put(RuleData.BLOCK, R.id.block_back);
        backMapping.put(RuleData.CITY_LEVEL, R.id.citylevel_back);
        backMapping.put(RuleData.SYN_ROUTE, R.id.synroute_back);

    }

    private int getForegroundOption() {
        int selectedID = foreRadioGroup.getCheckedRadioButtonId();
        int rule = foreMapping.keyAt(foreMapping.indexOfValue(selectedID));
        return rule;
    }

    private void setForegroundOption(int foreRule) {
        System.out.println(foreRule);
        int idToSelect = foreMapping.get(foreRule);
        System.out.println(idToSelect);
        foreRadioGroup.check(idToSelect);
    }

    private int getBackgroundOption() {
        int selectedID = backRadioGroup.getCheckedRadioButtonId();
        int rule = backMapping.keyAt(backMapping.indexOfValue(selectedID));
        return rule;
    }

    private void setBackgroundOption(int backRule) {
        int idToSelect = backMapping.get(backRule);
        backRadioGroup.check(idToSelect);
    }

    private int getPersistentOption() {
        int selectedID = backRadioGroup.getCheckedRadioButtonId();
        int rule = backMapping.keyAt(backMapping.indexOfValue(selectedID));
        return rule;
    }

    private void setPersistentOption(int persRule) {
        return;
    }

    private int getTrackLevel() {
        return trackLevel.getProgress();
    }

    private void setTrackLevel(int track) {
        trackLevel.setProgress(trackLevel.getMax() - track);
        trackLevel.setProgress(track);
    }

    private int getProfLevel() {
        return profLevel.getProgress();
    }

    private void setProfLevel(int prof) {
        profLevel.setProgress(profLevel.getMax() - prof);
        profLevel.setProgress(prof);
    }

    private double getFixedLatitude() {
        return Double.parseDouble(latitude.getText().toString());
    }

    private void setFixedLatitude(double lat) {
        latitude.setText("" + lat);
    }

    private double getFixedLongitude() {
        return Double.parseDouble(longitude.getText().toString());
    }

    private void setFixedLongitude(double lon) {
        longitude.setText("" + lon);
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

    public void onDestroy() {
        unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private void setMetaData(String packName) {

        String appName = Util.getAppName(packName, this);
        setTitle(appName);
        if (placeID == -1) {
            placeIDView.setText("Overall ");
        } else {
            LatLng loc = LocationClusterer.getLoc(placeID);
            double lat = Double.parseDouble(new DecimalFormat("##.###").format(loc.latitude));
            double lon = Double.parseDouble(new DecimalFormat("##.###").format(loc.longitude));
            placeIDView.setText(lat + "," + lon + " ");
        }


        Drawable iconTemp;
        try {
            iconTemp = getPackageManager().getApplicationIcon(packName);
            getActionBar().setIcon(iconTemp);

        } catch (Exception e) {

        }
    }

    void setEnabledTotal(boolean value) {
        setEnabledLayout(foreRadioGroup, value);
        setEnabledLayout(backRadioGroup, value);
        foreRadioGroup.check(R.id.citylevel);
        backRadioGroup.check(R.id.citylevel_back);
        trackLevel.setEnabled(false);
        profLevel.setEnabled(false);
        apply.setEnabled(value);
        reset.setEnabled(value);
        latitude.setEnabled(false);
        longitude.setEnabled(false);
        getFromMap.setEnabled(false);
    }

    void setEnabledLayout(ViewGroup layout, boolean value) {

        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            child.setEnabled(value);
        }
    }

}
