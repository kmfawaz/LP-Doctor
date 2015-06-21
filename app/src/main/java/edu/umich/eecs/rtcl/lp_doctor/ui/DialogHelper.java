/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;

import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.anonymization.LocationAnonymizer;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleInterface;

/**
 * Created by kmfawaz on 1/14/2015.
 */
public class DialogHelper {

    Context context;
    LocationAnonymizer locAnon;

    public DialogHelper(Context context, LocationAnonymizer locAnon) {
        this.context = context;
        this.locAnon = locAnon;
    }


    //displayed as part of the ongoing privacy protection
    //which is to be used no more! (annoying)
/*
    public void displayAlert(final String appLPPM, final String source) {

        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("LP-Doctor")
                .setMessage("Allow location access? for:\t"+appLPPM)
                .setNegativeButton("Protect me!", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        locAnon.anonymizeLocation(appLPPM, "Yes -- new dialog", source, 0);
                    }
                }).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        locAnon.finishDecisionMaking(appLPPM, "Yes -- new dialog", source, 0);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        locAnon.finishDecisionMaking(appLPPM, "cancelled -- new dialog", source, 0);
                    }
                }).setIcon(R.drawable.ic_launcher).create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        dialog.show();

    }
*/
    //also has to do the rules for first time the app launches ever
    //all decision leaves implemented
    public void globalFirstAlert(final String appLPPM, final int place, final String source) {

        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("LP-Doctor")
                .setMessage("App might accesses location for:\t" + appLPPM + "\t what's your decision?")
                .setNegativeButton("allow this time\n(ask everytime)", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //DONE, do nothing
                        locAnon.finishDecisionMaking(appLPPM, "No -- new dialog", source, 1);
                    }
                }).setPositiveButton("allow always", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setNothingRule(appLPPM, place, source);

                    }
                }).setNeutralButton("help me protect\n(ask casually)", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        setAnonymizationRule(appLPPM, place, source);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        locAnon.finishDecisionMaking(appLPPM, "cancelled -- new dialog", source, 0);
                    }
                }).setIcon(R.drawable.ic_launcher).create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        dialog.show();

    }

    //nothing rule is the most important
    private void setNothingRule(String app, int place, String source) {
        RuleData.SingleRule rule = new RuleData.SingleRule(RuleInterface.PLACE_FLAG, RuleData.NO_ACTION, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, 0, 0, 0);
        locAnon.ruleBridge.updateForeRule(app, RuleInterface.PLACE_FLAG, rule);
        //insert global rule
        //what about per-place rules?
        locAnon.finishDecisionMaking(app, "Yes -- new dialog", source, 0);
    }

    private void setAnonymizationRule(String app, int place, String source) {
        RuleData.SingleRule rule = new RuleData.SingleRule(-1, RuleData.HIGH_DIFF_PRIV, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, RuleData.ANON_MEDIUM, 0, 0);
        locAnon.ruleBridge.updateForeRule(app, -1, rule);

        rule = new RuleData.SingleRule(place, RuleData.HIGH_DIFF_PRIV, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, RuleData.ANON_MEDIUM, 0, 0);
        locAnon.ruleBridge.updateForeRule(app, place, rule);
        //insert global rule
        //what about per-place rules?
        locAnon.anonymizeLocation(app, "Yes -- new dialog", source, 0, RuleData.ANON_MEDIUM); //by default unless changed otherwise
    }


    //all decision leaves implemented
    // the first time an app accesses location from a place.
    // App has to have fine location permissions and neither block nor allow rules

    public void perPlaceFirstAlert(final String appLPPM, final int place, final String source) {

        AlertDialog dialog = new AlertDialog.Builder(context).setTitle("LP-Doctor")
                .setMessage("App is accessesing in the place location for:\t" + appLPPM + "\t for the first time, what's your decision?")
                .setNegativeButton("Block Completely", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //DONE, do nothing
                        setRulePerPlaceBlock(appLPPM, place, source);
                        locAnon.finishDecisionMaking(appLPPM, "No -- new dialog", source, 1);
                    }
                }).setPositiveButton("help me protect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // add a rule for this place so as not to ask later
                        setRulePerPlaceAnonymize(appLPPM, place, source);
                        locAnon.finishDecisionMaking(appLPPM, "cancelled -- new dialog", source, 0);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        locAnon.finishDecisionMaking(appLPPM, "cancelled -- new dialog", source, 0);
                    }
                }).setIcon(R.drawable.ic_launcher).create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        dialog.show();

    }

    //insert the per place rules
    // replace app with the app flag? -- will reduce prompting for sure
    // will ask for each new place at most once
    // Before, we would ask for each new place at least none.
    // New prompts are a subset of the old prompts, so leq by definition

    // so per place and app rules are only set from settings activity

    private void setRulePerPlaceAnonymize(String app, int place, String source) {

        RuleData.SingleRule rule = new RuleData.SingleRule(place, RuleData.HIGH_DIFF_PRIV, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, RuleData.ANON_MEDIUM, 0, 0);
        //locAnon.ruleBridge.updateForeRule(app, place, rule);
        locAnon.ruleBridge.updateForeRule(RuleInterface.APP_FLAG, place, rule);
        // app is RuleInterface.APP_FLAG
        locAnon.anonymizeLocation(app, "Yes -- new dialog", source, 0, RuleData.ANON_MEDIUM); //I don't like where is this going
    }

    //insert the per place rule
    private void setRulePerPlaceBlock(String app, int place, String source) {

        //medium anon by default, unless overridden by app
        RuleData.SingleRule rule = new RuleData.SingleRule(place, RuleData.BLOCK, RuleData.CITY_LEVEL, RuleData.NO_ACTION, 0, RuleData.ANON_MEDIUM, 0, 0);
        //locAnon.ruleBridge.updateForeRule(app, place, rule);
        locAnon.ruleBridge.updateForeRule(RuleInterface.APP_FLAG, place, rule);

        locAnon.finishDecisionMaking(app, "cancelled -- new dialog", source, 0);
    }

}
