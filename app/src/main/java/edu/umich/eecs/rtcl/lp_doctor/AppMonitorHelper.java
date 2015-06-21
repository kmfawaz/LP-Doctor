/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimeZone;

import edu.umich.eecs.rtcl.lp_doctor.utilities.LocationAccessDetector;

/**
 * Created by kmfawaz on 1/13/2015.
 */
public class AppMonitorHelper {

    static String activity = "-";
    private final int interval = 7500; // 10 Seconds  -- might have to reduce frequency
    Context context;
    MonitoringService mainService;

    //it is place reported to the app while running
    //must be hashset not just an integer
    HashMap<String, HashSet<Integer>> placesReportedToApp = new HashMap<String, HashSet<Integer>>();
    boolean lockedOld = false;
    String appOld = "start"; //app that is currently running
    appSessionRecord currentAppSession;
    private Handler handler = new Handler();
    private Runnable runnable = new Runnable() {
        public void run() {
            new myAsyncTask().execute();

            handler.postDelayed(runnable, interval);
        }
    };
    private String appStarted = "start";  // populated through the app launcher

    public AppMonitorHelper(Context context, MonitoringService mainService) {
        this.context = context;
        this.mainService = mainService;

        startTimer();

    }

    public void startTimer() {
        handler.postDelayed(runnable, interval);
    }


    //have to make sure that whenever an app runs, we will have the same fake location
    // o.w. it will be very easy to extract user's location
    // let's use shared preferences to save these fake locations?
    // prevent averaging to find the real location

    public void handleClose() {
        handler.removeCallbacks(runnable);
    }

    public String getRunningApp() {
        return appOld;
    }

    //inform the app monitor that this app is running with a certain privacy decision
    //handle session start here (without the "corner case")
    public void setStartedApp(String app, int placeID) {
        appStarted = app;
    }

    //whenever anonymization is invoked with a certain fake location
    public void reportPlaceToApp(String app, int placeID) {
        HashSet<Integer> placeList = placesReportedToApp.get(app);
        if (placeList == null) {
            placeList = new HashSet<Integer>();
        }
        placeList.add(placeID);
        placesReportedToApp.put(app, placeList);//the app will run with this decision
    }

    //handle session end here
    private void handleAppChange(String appOld, String appNew, long duration, int actualPlaceStart, int actualPlaceEnd) {
        //check that the new session matches input from the app launcher

        Util.Log(Util.SESSION_TAG, "start:\t" + appStarted + "\tdetected:\t" + appNew + "\told:\t" + appOld);

        if (!appNew.equals(appStarted) && appOld.equals(appStarted)) {
            Util.Log("RTCL", ":\t" + appOld + "\t" + appStarted);
            mainService.locAnonHelper.notifCtl.cancelNotification();
            mainService.stopLocationAnon();
        }
        handleSessionEnd(appOld, duration, actualPlaceStart, actualPlaceEnd);

        //finalize old session.Once finalized; treat as if another app just started
        if (appOld.equals(appStarted) && !appNew.equals("com.cyanogenmod.trebuchet")) {
            Util.Log(Util.SESSION_TAG, "app started from non-conventional sources:\t" + appNew);
            // apply logic as well
            // treat it as if new app launched with a fake source
            //might have to do some logic here? as starting a session for instance
            instructServiceThatAppLaunched(appNew);
        }

    }

    void instructServiceThatAppLaunched(String app) {
        Intent intent_new = new Intent(MonitoringService.INTENT_FILTER);
        intent_new.putExtra("action", "LPPM");
        intent_new.putExtra("app", app);
        intent_new.putExtra("source", Util.FAKE_INTENT_SOURCE);
        context.sendBroadcast(intent_new);
    }

    //app didn't access location -- no histogram modification
    //app's real location is considered as: app accessed location ^ app fine location ^ app place end
    //app's reported location is considered as: app accessed location ^ app fine location ^ app place reported
    //assume the app is used from two places at most ...
    //logic has to be improved so as to not incur additional overhead
    //follow a decision tree-similar structure
    private void handleSessionEnd(String app, long duration, int actualPlaceStart, int actualPlaceEnd) {
        //app launcher is ok :) as well as system apps

        if (app.equals("com.cyanogenmod.trebuchet")) {
            //cancelNotification

            return;
        }


        boolean hasFinePerm = Util.doesAppHasFinePerms(context, app);
        if (!hasFinePerm) {
            return;
        }

        if (Util.isSystemApp(context, app)) {
            return; //nothing to do for a system app
        }
        //we need to tell if app accessed location while running in the past "duration" interval
        try {
            //10 seconds margin of error - //can tell if app accessed location in foreground
            //end previous session
            //find if app accessed location
            //only do that for app that has fine location permissions --> reduces overhead on the device

            boolean isLocationAccessed = LocationAccessDetector.dumpSysLocation(app, duration + 10000);
            LocationAccessDetector.addAppSession(context, app, isLocationAccessed);

            HashSet<Integer> placesReported = placesReportedToApp.get(app);
            HashSet<Integer> placesActual = new HashSet<Integer>();

            placesActual.add(actualPlaceStart);
            placesActual.add(actualPlaceEnd);

            if (placesReported == null) {
                //do something, app was not started conventionally
                //assume app was fed real location
                //need more logic here
                placesReported = placesActual;
            }
            placesReportedToApp.remove(app);


            Util.Log("myLocation", app + "\t" + isLocationAccessed + "\t" + LocationAccessDetector.getLocationAccessRate(context, app)
                    + "\t" + placesReported + "\t" + placesActual + "\t finePerm?: " + hasFinePerm);

            //decision is simply the location reported. the place the app ran from is evident and should be passed here as well
            //can update the histogram here -- have all the building block ready now!
            //only if app accessed location
            updateHistograms(app, isLocationAccessed, hasFinePerm, placesActual, placesReported);


            //problem: misses on getLastKnownLocation sadly ...

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //should be a set as a matter of fact.
    void updateHistograms(String app, boolean accessedLocation, boolean finePerm, HashSet<Integer> placesActualList, HashSet<Integer> placesReportedList) {

        if (accessedLocation && finePerm) {
            for (int placeActual : placesActualList) {
                mainService.histHelper.updateHistogram(app, placeActual, HistogramManager.ACTUAL_FLAG);
            }

            for (int placeReported : placesReportedList) {
                mainService.histHelper.updateHistogram(app, placeReported, HistogramManager.REPORTED_FLAG);
            }
        }
    }

    appSessionRecord isNewRecord(String appOld, boolean lockedOld, String appNew, boolean lockedNew) {

        //System.out.println(lockedOld+"-"+appOld+"-"+lockedNew+"-"+appNew);
        appSessionRecord retVal = null;
        //String activity = ActivityRecognitionIntentService.activity;
        int oldUID = 0;//Util.getUIDbyPackName(appOld, context);
        int newUID = 0;//Util.getUIDbyPackName(appNew, context);

        //can use this as a heuristic (if app has no tx then app didn't leak?
        long txOldApp = TrafficStats.getUidTxPackets(oldUID);
        long rxOldApp = TrafficStats.getUidRxPackets(oldUID);

        long txNewApp = TrafficStats.getUidTxPackets(newUID);
        long rxNewApp = TrafficStats.getUidRxPackets(newUID);

        int place = mainService.getCurrentPlace().getPlaceID();
        if (currentAppSession == null) {
            currentAppSession = new appSessionRecord();
        }
        if (appOld.equals(appNew) && lockedOld && lockedNew) {
            retVal = null;
        } else if (appOld.equals(appNew) && lockedOld && !lockedNew) {

            currentAppSession.finalize(activity, place, txOldApp, rxOldApp, oldUID);
            //currentAppSession.end = System.currentTimeMillis();
            //currentAppSession.activityEnd = activity;
            //currentAppSession.placeEnd = place;
            //currentAppSession.txEnd = txOldApp;
            //currentAppSession.rxEnd = rxOldApp;

            retVal = currentAppSession;
            currentAppSession = new appSessionRecord(appNew, System.currentTimeMillis(), 0, activity, place, txNewApp, rxNewApp);
        } else if (appOld.equals(appNew) && !lockedOld && lockedNew) {
            currentAppSession.finalize(activity, place, txOldApp, rxOldApp, oldUID);

            retVal = currentAppSession;
            currentAppSession = new appSessionRecord("lock", System.currentTimeMillis(), 0, activity, place, txNewApp, rxNewApp);
        } else if (appOld.equals(appNew) && !lockedOld && !lockedNew) {
            retVal = null;
        } else if (!appOld.equals(appNew) && lockedOld && lockedNew) {
            retVal = null;
        } else if (!appOld.equals(appNew) && lockedOld && !lockedNew) {
            currentAppSession.finalize(activity, place, txOldApp, rxOldApp, oldUID);

            retVal = currentAppSession;
            currentAppSession = new appSessionRecord(appNew, System.currentTimeMillis(), 0, activity, place, txNewApp, rxNewApp);
        } else if (!appOld.equals(appNew) && !lockedOld && lockedNew) {
            currentAppSession.finalize(activity, place, txOldApp, rxOldApp, oldUID);

            retVal = currentAppSession;
            currentAppSession = new appSessionRecord("lock", System.currentTimeMillis(), 0, activity, place, txNewApp, rxNewApp);
        } else if (!appOld.equals(appNew) && !lockedOld && !lockedNew) {
            currentAppSession.finalize(activity, place, txOldApp, rxOldApp, oldUID);

            retVal = currentAppSession;


            currentAppSession = new appSessionRecord(appNew, System.currentTimeMillis(), 0, activity, place, txNewApp, rxNewApp);
        }
        return retVal;
    }

    // a helper function to inform us if the device is locked or not
    private boolean isDeviceLocked(Context context) {
        KeyguardManager kgMgr = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        boolean showing = kgMgr.inKeyguardRestrictedInputMode();
        return showing;
    }

    class myAsyncTask extends AsyncTask<Void, Void, Void> {
        myAsyncTask() {
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);

            String appNew = am.getRunningTasks(1).get(0).topActivity.getPackageName();

            boolean lockedNew = isDeviceLocked(context);
            appSessionRecord retVal = isNewRecord(appOld, lockedOld, appNew, lockedNew);


            //new record is here!
            if (retVal != null) {

                handleAppChange(appOld, appNew, retVal.end - retVal.start, retVal.placeStart, retVal.placeEnd);

            }

            appOld = appNew;
            lockedOld = lockedNew;

            return null;
        }
    }

    //tx always before Rx
    class appSessionRecord {

        final char SEP = ',';
        String app;
        long start;
        long end;
        String activityStart;
        String activityEnd;
        int placeStart;
        int placeEnd;
        long txStart;
        long rxStart;
        long txEnd;
        long rxEnd;
        int uid;

        public appSessionRecord(String app, long start, long end, String activityStart, int placeStart, long txStart, long rxStart) {

            this.app = app;
            this.start = start;
            this.end = end;
            this.activityStart = activityStart;
            this.placeStart = placeStart;
            this.txStart = txStart;
            this.rxStart = rxStart;
        }

        public appSessionRecord() {

            this.app = "";
            this.start = 0;
            this.end = 0;
        }

        public String toString() {

            long txDiff = txEnd - txStart;
            long rxDiff = rxEnd - rxStart;

            return uid + ":" + app + SEP + start + SEP + TimeZone.getDefault().getID() +
                    SEP + (end - start) / 1000 + SEP + activityStart +
                    SEP + activityEnd + SEP + placeStart + SEP + placeEnd + SEP + txDiff + SEP + rxDiff;

        }

        public void finalize(String activity, int place, long tx, long rx, int uid) {

            this.end = System.currentTimeMillis();
            this.activityEnd = activity;
            this.placeEnd = place;
            this.txEnd = tx;
            this.rxEnd = rx;
            this.uid = uid;

        }

    }

}
