/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import edu.umich.eecs.rtcl.lp_doctor.App;
import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleInterface;

public class AppInstalledActivity extends ActionBarActivity {

    Button doneButton;
    TextView appLabel;
    ImageView icon;
    CheckBox preventCheck;

    RadioButton protectRadio;
    RadioGroup optionGroup;

    TextView levelView;
    TextView levelLabel;

    String appInstalled;
    RuleInterface ruleBridge;

    String messagePre = "You just installed ";

    String messagePost = ". This app can access your accurate location information." +
            "\n\nWhat behavior do you want applied?";

    SeekBar profLevel;
    SeekBar.OnSeekBarChangeListener profLevelListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            Util.Log("rule", progress + "");
            if (progress == 0) {
                levelView.setText("Low");
            } else if (progress == 1) {
                levelView.setText("Medium");
            } else if (progress == 2) {
                levelView.setText("High");
            }
            //there shouldn't be any other option
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };
    RadioGroup.OnCheckedChangeListener optionSelectListener = new RadioGroup.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {

            if (checkedId == R.id.protectRadio) {
                // preventCheck.setVisibility(CheckBox.VISIBLE);
                levelView.setVisibility(TextView.VISIBLE);
                profLevel.setVisibility(SeekBar.VISIBLE);
                levelLabel.setVisibility(SeekBar.VISIBLE);
            } else {
                // preventCheck.setVisibility(CheckBox.INVISIBLE);
                levelView.setVisibility(TextView.INVISIBLE);
                profLevel.setVisibility(SeekBar.INVISIBLE);
                levelLabel.setVisibility(SeekBar.INVISIBLE);
            }
            //keep the checkbox invisible for now
        }
    };
    View.OnClickListener doneButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            //get us back to the main launcher app

            //get ID, and set corresponding rules
            int chosenID = optionGroup.getCheckedRadioButtonId();

            switch (chosenID) {
                case R.id.protectRadio:
                    addProtectRule(appInstalled);
                    break;
                case R.id.allowRadio:
                    addAllowRule(appInstalled);
                    break;
                case R.id.blockRadio:
                    addBlockRule(appInstalled);
                    break;
                default:
                    Util.Log("rule", "chosen ID" + chosenID + "");
                    //present Toast, please choose an option
                    Toast.makeText(AppInstalledActivity.this, "Please choose an option", Toast.LENGTH_LONG).show();
                    return;
            }
            backToHomeScreen();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_installed);


        doneButton = (Button) this.findViewById(R.id.doneButton);
        doneButton.setOnClickListener(doneButtonListener);

        appLabel = (TextView) this.findViewById(R.id.appLabel);
        icon = (ImageView) this.findViewById(R.id.appIcon);

        preventCheck = (CheckBox) this.findViewById(R.id.preventCheck);
        protectRadio = (RadioButton) this.findViewById(R.id.protectRadio);
        optionGroup = (RadioGroup) this.findViewById(R.id.optionGroup);

        profLevel = (SeekBar) this.findViewById(R.id.proflevel);
        profLevel.setOnSeekBarChangeListener(profLevelListener);
        levelView = (TextView) this.findViewById(R.id.levelView);
        levelLabel = (TextView) this.findViewById(R.id.levelLabel);

        levelView.setVisibility(TextView.INVISIBLE);
        profLevel.setVisibility(SeekBar.INVISIBLE);
        levelLabel.setVisibility(SeekBar.INVISIBLE);

        optionGroup.setOnCheckedChangeListener(optionSelectListener);

        preventCheck.setVisibility(CheckBox.INVISIBLE);

        //the intent must have app just installed
        Intent intent = getIntent();
        String appNew = intent.getExtras().getString("app");
        if (appNew == null) {
            finish(); //donothing
        }
        String message = messagePre + Util.getAppName(appNew, this) + messagePost;
        appLabel.setText(message);
        appInstalled = appNew;

        ruleBridge = new RuleInterface();
        setIcon(appNew);

    }

    private void setIcon(String app) {
        try {
            Drawable iconTemp = getPackageManager().getApplicationIcon(app);
            icon.setImageDrawable(scaleImage(iconTemp));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Drawable scaleImage(Drawable image) throws PackageManager.NameNotFoundException {

        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }


        BitmapDrawable iconTemp = (BitmapDrawable) getPackageManager().getApplicationIcon(App.PACKAGE_NAME);
        int height = iconTemp.getIntrinsicHeight();
        int width = iconTemp.getIntrinsicWidth();

        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, width, height, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }

    // we are the only to load this
    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    //insert rules here
    private void backToHomeScreen() {
        finish();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }

    //simple settings menu to create the rules and control location access/ prompting
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_app_installed, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //functions to insert rules here:
    //1. block rule //supersede LP-Doctor; very high place -- app
    //2. allow rule //supersede LP-Doctor; very high place -- app
    //3. protect rule; -1 -- app

    private void addAllowRule(String app) {
        RuleData.SingleRule rule = new RuleData.SingleRule(RuleInterface.PLACE_FLAG, RuleData.NO_ACTION, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, 0, 0, 0);
        ruleBridge.updateForeRule(app, RuleInterface.PLACE_FLAG, rule);
    }

    private void addBlockRule(String app) {

        RuleData.SingleRule rule = new RuleData.SingleRule(RuleInterface.PLACE_FLAG, RuleData.BLOCK, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, 0, 0, 0);
        //locAnon.ruleBridge.updateForeRule(app, place, rule);
        ruleBridge.updateForeRule(app, RuleInterface.PLACE_FLAG, rule);
    }

    private void addProtectRule(String app) {

        //get the level from the seekbar, and set the bar, then use that to control \alpha
        // we have to respect the level in Location Anonymizer class
        int level = getPrivacyLevel(); //values will be 1,2,3
        RuleData.SingleRule rule = new RuleData.SingleRule(-1, RuleData.HIGH_DIFF_PRIV, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, level, 0, 0);
        ruleBridge.updateForeRule(app, -1, rule);
    }

    private int getPrivacyLevel() {
        switch (profLevel.getProgress()) {
            case 0:
                return RuleData.ANON_LOW;
            case 1:
                return RuleData.ANON_MEDIUM;
            case 2:
                return RuleData.ANON_HIGH;
        }
        return RuleData.ANON_LOW;
    }
}
