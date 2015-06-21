/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Created by kmfawaz on 1/19/2015.
 */

public class LocationAccessDetector {

    public static final String PREF_FILE = "edu.umich.eecs.rtcl.lp_doctor.location_access_detect";
    //save the fact that there was an app session, and whether location was accessed or not
    private static final String SESSION_TAG = "-session_total";
    private static final String LOCATION_TAG = "-session_loc_accessed";

    // dumpsys activity service com.google.android.location.internal.GoogleLocationManagerService
    //app event since the duration
    //I guess something wrong is going on here
    //wrapper around this. and another boolean from LMS dump location
    public static boolean dumpSysLocation(String app, long duration) throws IOException {
        System.out.println("trying to detect location access");
        Date current = new Date();

        ArrayList<String> commandLine = new ArrayList<String>();
        commandLine.add("dumpsys"); //$NON-NLS-1$
        commandLine.add("activity"); //$NON-NLS-1$
        commandLine.add("service"); //$NON-NLS-1$
        commandLine.add("com.google.android.location.internal"); //$NON-NLS-1$
        commandLine.add("|"); //$NON-NLS-1$
        commandLine.add("grep"); //$NON-NLS-1$
        commandLine.add(app); //$NON-NLS-1$

        Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String s = "";


        boolean lookingAtEvents = false;
        while ((s = bufferedReader.readLine()) != null) {
            s = s.trim();
            System.out.println(s);
            if (s.contains("Event Log")) {
                lookingAtEvents = true;
                continue;
            }
            //string sample: 01-20 15:01:37: Received Wifi Location

            if (!s.isEmpty() && s.contains(":")) {
                try {

                    String time = s.substring(0, 14);
                    DateFormat df = new SimpleDateFormat("MM-dd kk:mm:ss", Locale.ENGLISH);
                    Date result = df.parse(time);
                    result.setYear(current.getYear());

                    //check only events in the past "duration"
                    long timeDiff = current.getTime() - result.getTime();
                    if (timeDiff > duration) {
                        continue;
                    }
                    if (s.contains(app)) {
                        //Util.Log("myLocation",(current.getTime()-result.getTime())/60000 + "\t" + time + "\t" + s);
                        return true;
                    }

                } catch (Exception e) {
                    // e.printStackTrace();
                }

                //get time
                //if not recent -- break
                //if found package of interest break
            }
            //System.out.println("myLOG:::"+s);
        }
        return false;
    }

    public static void addAppSession(Context context, String app, boolean isLocationAccessed) {

        SharedPreferences sharedPref = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        String keySessionTotal = app + SESSION_TAG;
        String keySessionLocationAccess = app + LOCATION_TAG;

        int sessionTotal = getSessionTotal(context, app);
        int sessionsWithLocationAccess = getSessionsWithLocationAccess(context, app);

        editor.putInt(keySessionTotal, sessionTotal + 1);
        if (isLocationAccessed) {
            editor.putInt(keySessionLocationAccess, sessionsWithLocationAccess + 1);
        }


        editor.commit();

    }

    //get portion of location accesses
    //first time app runs this will crash :)
    public static double getLocationAccessRate(Context context, String app) {

        int sessionTotal = getSessionTotal(context, app);
        int sessionsWithLocationAccess = getSessionsWithLocationAccess(context, app);
        if (sessionTotal == 0) {
            return -1; // indicating no data yet.
        }
        //if the number of sessions is low, are we confident either way?
        return sessionsWithLocationAccess * 1.0 / sessionTotal;

    }

    //get portion of location accesses
    private static int getSessionTotal(Context context, String app) {

        SharedPreferences sharedPref = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        int sessionTotal = sharedPref.getInt(app + SESSION_TAG, 0);
        return sessionTotal;
    }

    //get portion of location accesses
    private static int getSessionsWithLocationAccess(Context context, String app) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        int sessionsWithLocationAccess = sharedPref.getInt(app + LOCATION_TAG, 0);
        return sessionsWithLocationAccess;

    }

    class SysDumper extends AsyncTask<Location, Void, Void> {

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Location... location) {


            return null;
        }
    }
}
