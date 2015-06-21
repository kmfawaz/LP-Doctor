/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.anonymization;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

import edu.umich.eecs.rtcl.lp_doctor.AppMonitorHelper;
import edu.umich.eecs.rtcl.lp_doctor.MonitoringService;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.MobilityModel;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData.SingleRule;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleInterface;
import edu.umich.eecs.rtcl.lp_doctor.ui.DialogHelper;
import edu.umich.eecs.rtcl.lp_doctor.ui.NotificationControl;
import edu.umich.eecs.rtcl.lp_doctor.utilities.LocationAccessDetector;
import edu.umich.eecs.rtcl.lp_doctor.utilities.MathTools;

/**
 * Created by kmfawaz on 1/22/2015.
 */

//to decide what to do with location
//decide what location gets sent to the app
public class LocationAnonymizer {

    public static final String INTENT_ACTION = "edu.umich.eecs.rtcl.lp_doctor.anonymization.notification";
    public static final String ACTION_ANON = "anonymize";
    public static final String ACTION_DEANON = "deanonymize";
    public static final String ACTION_ANON_REDUCE = "reduce_anon";
    private static final int DO_NOTHING = 0;
    private static final int ISSUE_PROMPT = 1;
    private static final int DISP_NOTIF = 2;
    static public HashMap<String, SingleRule> appForeground;
    public NotificationControl notifCtl;
    public RuleInterface ruleBridge;
    Context context;
    MonitoringService mainService;
    MobilityModel mobHelper;
    DialogHelper diagHelper; //when we need to prompt the user if threat exists
    AppMonitorHelper appHelper;
    //current anonymization level, if same feed, if different feed another one, add more custom levels
    int currentAnonymizationLevel = 0;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // for the notification
            String action = intent.getStringExtra("action");
            String app = intent.getStringExtra("appName");
            int place = intent.getIntExtra("placeID", -1);
            //I guess app and place are here?

            if (action == null) {
                return;// do nothing
            }
            // might have to switch the icon back
            if (action.equals(ACTION_ANON)) {
                //apply anonymization for this session; detect if app has accessed location or not
                int privacyLevel = ruleBridge.getProtectionLevel(app);
                //we can check if use has a supplied anon level, but put medium just for simplicity
                anonymizeLocation(app, "notification enabled", Util.FAKE_INTENT_SOURCE, 0, RuleData.ANON_MEDIUM);
                Util.Log("rule", "apply anon");
                notifCtl.cancelNotification();
                notifCtl.issueNotification(ACTION_DEANON, app, place);

            } else if (action.equals(ACTION_DEANON)) {
                // shut down the mock location providers
                //also got to manage if the app accessed location
                mainService.stopLocationAnon();
                notifCtl.cancelNotification();
                notifCtl.issueNotification(ACTION_ANON, app, place);
                Util.Log("rule", "get back to the real location");
            } else if (action.equals(ACTION_ANON_REDUCE)) { // do the other things
                // shut down the mock location providers
                //also got to manage if the app accessed location
                //need to find the current anon status and reduce from the intent basically
                int reducedLevel = currentAnonymizationLevel - 1;
                reducedLevel = reducedLevel < 1 ? 1 : reducedLevel;//make sure it is not less than 1.

                anonymizeLocation(app, "notification enabled", Util.FAKE_INTENT_SOURCE, 0, reducedLevel);
                Util.Log("rule", "reduce anon with this new level:" + reducedLevel);
                //notifCtl.cancelNotification();
                //notifCtl.issueNotification(ACTION_DEANON,app,place);
            }
        }
    };


    //got to organize code structure, who calls whom?

    //what do we need to make the decision?
    // app name -- done
    // current place -- done
    // app expected to access location? -- done
    // app histogram -- done
    // app mobility model -- done
    // we need the rules -- later not now

    //seems we are ready to go, also get rules from here??


    public LocationAnonymizer(Context context, MonitoringService mainService) {
        this.context = context;
        this.mainService = mainService;
        mobHelper = new MobilityModel();
        diagHelper = new DialogHelper(context, this);
        appHelper = new AppMonitorHelper(context, mainService);
        ruleBridge = new RuleInterface();
        notifCtl = new NotificationControl(context, this);
        appForeground = new HashMap<String, SingleRule>();

        //register intent receiver
        context.registerReceiver(mMessageReceiver, new IntentFilter(INTENT_ACTION));
    }

    public void processAppLaunch(Intent locationIntent, int currentPlace) {
        String app = locationIntent.getStringExtra("app");
        String source = locationIntent.getStringExtra("source");

        Util.Log(Util.SESSION_TAG, "app launched:\t" + app);

        //no fine permission --> do nothing
        //fine permission --> fetch rule
        //no rule --> issue prompt and fill rule then perform decision based on rule

        //get rule
        //if rule says implement privacy then perform the below
        boolean hasFinePerm = Util.doesAppHasFinePerms(context, app);
        boolean isSystemApp = Util.isSystemApp(context, app);

        //doesn't need fine permissions, so no need for anything
        if (!hasFinePerm || isSystemApp) {
            finishDecisionMaking(app, "no fine permission", source, 0);
            return;
        }
        handleForegroundAccess(app, currentPlace, source);


    }

    //this is the entry point
    private void handleForegroundAccess(String app, int place, String source) {
        // if rule is in cache
        long t1 = System.nanoTime();
        SingleRule rule = ruleBridge.fetchUpToDateRule(app, place); //got to cache it :)
        Util.Log("lp_time", "rule fetch: " + (System.nanoTime() - t1));
        if (rule == null) {
            //////System.out.println("No Rule");
            // we need good management here
            diagHelper.globalFirstAlert(app, place, source);
            //rule option do nothing then do nothing
        } else if (rule.foreRule == RuleData.NO_ACTION) { //only when the app has it as the global rule highest place
            Util.Log("rule", rule.toString());
            finishDecisionMaking(app, "nothing to do", source, 0);
            //basically because user asked for nothing to be done
        } else if (rule.foreRule == RuleData.BLOCK) { //only when the app has it as the global rule highest place
            Util.Log("rule", rule.toString());
            //apply anon here with non-relevant location (0,0)
            finishDecisionMaking(app, "nothing to do", source, 0);
            //basically because user asked for nothing to be done
        } else if (rule.foreRule == RuleData.FIXED) {
            Util.Log("rule", rule.toString());
            //apply anon here with the location set in the rule
            finishDecisionMaking(app, "nothing to do", source, 0);
            //basically because user asked for nothing to be done
        } else if (rule.foreRule == RuleData.HIGH_DIFF_PRIV) { //user says: help me with privacy
            Util.Log("rule", rule.toString());
            //rule option anonymize then perform rest through processForegroundRule
            processForegroundRule(app, place, rule, source);
        }
    }

    //process foreground rule
    //
    private void processForegroundRule(String app, int currentPlace, SingleRule rule, String source) {
        //get the decision
        //perform privacy protection
        //if rule place is -1; means there is no rule for this place, we have to prompt the user

        //get the protection level here
        int privacyLevel = ruleBridge.getProtectionLevel(app); // there is a problem here


        if (rule.placeid == -1) {
            //there is no rule for this place, ask user to set per-place rule. (first time app used from this place)
            // we will try another combination: only per place rule; NOT per place-app rule
            // easy, as we have -1 for place, we have 0_all_apps is an all apps flag
            //-1 sort descending
            //app sort descending
            // the first entry should be highest place and app
            diagHelper.perPlaceFirstAlert(app, currentPlace, source);
        } else {
            int decision = getPrivacyDecision(app, currentPlace, privacyLevel);//also level has to be an input here
            performPrivacyProtection(decision, app, currentPlace, source, privacyLevel);
        }
    }

    //gather the required information and perform the decision process
    private int getPrivacyDecision(String app, int currentPlace, int privacyLevel) {

        boolean hasFinePerm = Util.doesAppHasFinePerms(context, app);
        double rate = LocationAccessDetector.getLocationAccessRate(context, app);

        SparseArray<Integer> appHistogram = mainService.histHelper.getHistogram(app);
        SparseArray<Double> mobility = mobHelper.getMobilityPDF();
        boolean fitsModel = MathTools.fitsMobModel(mobility, appHistogram, currentPlace, privacyLevel); //this must be the new p-value
        boolean lessThreat = MathTools.isDistanceIncreased(mobility, appHistogram, currentPlace);
        Util.Log(Util.TAG, "fine:\t" + hasFinePerm + "\trate:\t" + rate + "\tfitsModel:\t" + fitsModel + "\tlessThreat\t" + lessThreat);
        //wrong app
        if (app == null) {
            return DO_NOTHING;
        }

        //have to check if first visit to the location for this particular app
        if (!hasFinePerm) {
            return DO_NOTHING;
        }
        // if rate = -1 --> we don't know any thing --> issue prompt
        // if rate < 0.5  --> put notification (with threat level) -- not spontaneous location access
        // if rate > 0.5 --> issue prompt (after computing threat) -- spontaneous location access
        if (rate == -1) {
            return ISSUE_PROMPT;// first time
        } else if (rate < 0.5) {
            //display notification along with threat level
            return DISP_NOTIF; //that's tricky actually
            //we need to tell whether app accessed real location or fake location? needs some logic to store
            //point at which location was accessed?, or keep it simple?
        }
        //now rate is:rate >0.5 --> app is expected to access location
        if (lessThreat) {
            return DO_NOTHING;
        }
        // now higher information leak if location released
        if (!fitsModel) {
            return DO_NOTHING;
        }
        //releasing location will pose a potential threat
        return ISSUE_PROMPT;
    }

    //has been tested
    private void performPrivacyProtection(int decision, String app, int currentPlace, String source, int privacyLevel) {
        switch (decision) {
            case DO_NOTHING:
                Util.Log(Util.TAG, "decision: DO NOTHING");
                finishDecisionMaking(app, "nothing to do", source, 0);
                break;
            case ISSUE_PROMPT:
                Util.Log(Util.TAG, "ISSUE PROMPT");
                //issuePrompt(app, "prompt",source,currentPlace);
                //DisplayPrivacyNotification(app,ACTION_DEANON,source,currentPlace); //must be fake intent here
                anonymizeLocation(app, "Yes -- new dialog", source, 0, privacyLevel);
                // should anonymize directly
                break;
            case DISP_NOTIF:
                Util.Log(Util.TAG, "decision: DISPLAY NOTIFICATION");
                DisplayPrivacyNotification(app, ACTION_ANON, source, currentPlace);
                break;
            default:
                finishDecisionMaking(app, "nothing to do", source, 0);
                break;
        }
    }

    /*
    //these are the annoying prompts that we are getting rid of.
    private void issuePrompt (String appLPPM, String result,String source,int reportedPlaceID) {
        diagHelper.displayAlert(appLPPM, source);
        //finishDecisionMaking (appLPPM, result,source,reportedPlaceID); //this shouldn't be here later
    }
    */

    private void DisplayPrivacyNotification(String appLPPM, String action, String source, int reportedPlaceID) {

        //build and display the notification here.
        //notification has actions to enable/disable anonymization
        //same notification structure is to be used throughout the system's operation
        //we need to show the user the threat values

        notifCtl.issueNotification(action, appLPPM, reportedPlaceID);

        finishDecisionMaking(appLPPM, "", source, reportedPlaceID);
    }

    public void anonymizeLocation(String app, String result, String source, int reportedPlaceID, int privacyLevel) {
        currentAnonymizationLevel = privacyLevel; //always keep up to date
        //do stuff related to anonymizing location which is basically adding noise to location
        //add Laplacian noise, and then turn in fixed, like we did before?
        //through the mock location provider

        //here check if there is a fixed location for place and app, and use it; otherwise create a new one
        //we can easily access the current place
        LatLng fakeLocation = getFakeLocationForCurrentPlace(app, privacyLevel);
        mainService.startLocationAnon(fakeLocation); //this should take LatLng acc as inputs

        //behavior varies on whether this is from notification or real launch event
        if (!source.equals(Util.FAKE_INTENT_SOURCE)) {
            finishDecisionMaking(app, result, source, reportedPlaceID);
        }

    }

    private LatLng getFakeLocationForCurrentPlace(String app, int privacyLevel) {
        double lat = 0;
        double lon = 0;
        //add another interface using shared preferences (app+place) being the key
        //doesn't exist, create one. And then add to it --- doesn't interfere with the existing structure of rules
        //everything passes through the rule interface

        Util.Log("rule", "" + privacyLevel);
        int currentPlace = mainService.getCurrentPlace().getPlaceID();
        LatLng currentLoc = mainService.getCurrentPlace().getCurrentLoc();

        long t1 = System.nanoTime();
        LatLng fakeLocation = ruleBridge.getFixedLocation(app, currentPlace, privacyLevel, context); //privacy level as well
        Util.Log("lp_time", "fixed location: " + (System.nanoTime() - t1));
        if (fakeLocation != null) {
            Util.Log("rule", "found existing rule" + fakeLocation.toString());
            return fakeLocation;
        }
        //now if null, get it from math tools

        fakeLocation = MathTools.getFakeLocation(currentLoc, privacyLevel);
        ruleBridge.setFixedLocation(app, currentPlace, fakeLocation, privacyLevel, context); //next time we won't calculate.
        return fakeLocation;
    }
    //how to stop the anonymization?

    //leaf node in all the decision making process
    public void finishDecisionMaking(String appLPPM, String result, String source, int reportedPlaceID) {
        //send the intent back to the app launcher to allow the app to start

        mainService.instructApptoLaunch(appLPPM, result, source, reportedPlaceID);
        appHelper.setStartedApp(appLPPM, reportedPlaceID);

    }


}
