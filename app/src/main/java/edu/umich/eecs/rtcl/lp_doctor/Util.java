/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by kmfawaz on 1/12/2015.
 */
public class Util {
    public static final String TAG = "LP-Doctor";
    public static final String SESSION_TAG = "session";
    public static final String DB_TAG = "database";
    public static final boolean DEBUG = false;
    public static final int LOCATION_POLLING_INTERVAL = 30 * 1000; //for testing purposes.
    public static final double ALPHA = 0.05;

    public static final String FAKE_INTENT_SOURCE = "fake_intent_source";
    private static final String LAUNCHER_PACKNAME = "com.cyanogenmod.trebuchet";
    static public HashMap<String, String> appNameMap = new HashMap<String, String>();
    private static HashMap<String, Boolean> appHasFineLocPerms = new HashMap<String, Boolean>();
    //need to check for system apps and cache the results to reduce processing overhead, like the permission thing
    private static HashMap<String, Boolean> systemApps = new HashMap<String, Boolean>();

    static public String genWhereRange(LatLng center, double range, String latCol, String lonCol) {

        LatLng p1 = Util.calculateDerivedPosition(center, range, 0);
        LatLng p2 = Util.calculateDerivedPosition(center, range, 90);
        LatLng p3 = Util.calculateDerivedPosition(center, range, 180);
        LatLng p4 = Util.calculateDerivedPosition(center, range, 270);

        String strWhere = ""
                + latCol + " > " + String.valueOf(p3.latitude) + " AND "
                + latCol + " < " + String.valueOf(p1.latitude) + " AND "
                + lonCol + " < " + String.valueOf(p2.longitude) + " AND "
                + lonCol + " > " + String.valueOf(p4.longitude);
        return strWhere;
    }


    // boolean loc1  = pm.checkPermission("android.permission.ACCESS_FINE_LOCATION",procName)==PackageManager.PERMISSION_GRANTED;
    // boolean loc2  = pm.checkPermission("android.permission.ACCESS_COARSE_LOCATION",procName)==PackageManager.PERMISSION_GRANTED;

    public static void Log(String TAG, String message) {
        if (DEBUG) {
            android.util.Log.v(TAG, message);
        }
    }

    public static LatLng calculateDerivedPosition(LatLng point, double range, double bearing) {
        double EarthRadius = 6371000; // m

        double latA = Math.toRadians(point.latitude);
        double lonA = Math.toRadians(point.longitude);
        double angularDistance = range / EarthRadius;
        double trueCourse = Math.toRadians(bearing);

        double lat = Math.asin(
                Math.sin(latA) * Math.cos(angularDistance) +
                        Math.cos(latA) * Math.sin(angularDistance)
                                * Math.cos(trueCourse));

        double dlon = Math.atan2(
                Math.sin(trueCourse) * Math.sin(angularDistance)
                        * Math.cos(latA),
                Math.cos(angularDistance) - Math.sin(latA) * Math.sin(lat));

        double lon = ((lonA + dlon + Math.PI) % (Math.PI * 2)) - Math.PI;

        lat = Math.toDegrees(lat);
        lon = Math.toDegrees(lon);

        LatLng newPoint = new LatLng(lat, lon);

        return newPoint;

    }

    public static void removeAppFromPermMap(String app) {
        appHasFineLocPerms.remove(app);
    }

    public static boolean doesAppHasFinePerms(Context context, String app) {
        //check the cache, if there return, o.w. do a look up, update cache and return
        if (appHasFineLocPerms.containsKey(app)) {
            return appHasFineLocPerms.get(app);
        }

        PackageManager pm = context.getPackageManager();

        boolean locPerm = pm.checkPermission("android.permission.ACCESS_FINE_LOCATION", app) == PackageManager.PERMISSION_GRANTED;
        appHasFineLocPerms.put(app, locPerm); //what if the app is updated? gotta catch those events as well
        return locPerm;


    }

    static public String getAppName(String packName, Context context) {
        if (appNameMap.containsKey(packName)) {
            return appNameMap.get(packName);
        }
        final PackageManager pm = context.getApplicationContext().getPackageManager();
        ApplicationInfo ai;
        try {
            ai = pm.getApplicationInfo(packName, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            ai = null;
        }
        String appName = (String) (ai != null ? pm.getApplicationLabel(ai) : packName);
        appNameMap.put(packName, appName);
        return appName;
    }

    static public LinkedList<String> getInstalledApps(Context context) {
        ////System.out.println ("device locked::"+ isDeviceLocked (context));
        LinkedList<String> appList = new LinkedList<String>();

        PackageManager pm = context.getPackageManager();
        //ActivityManager actvityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ApplicationInfo> appInfos = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (appInfos == null) {
            return appList;
        }

        //it is a running service or activity that is on foreground
        //proc_name||foreground||service --> o.w. it is a cached process
        //HashSet<String> set = new HashSet<String>();
        for (int i = 0; i < appInfos.size(); i++) {
            //print those with location permissions
            String app = appInfos.get(i).packageName;//.processName;
            boolean loc1 = pm.checkPermission("android.permission.ACCESS_FINE_LOCATION", app) == PackageManager.PERMISSION_GRANTED;
            boolean loc2 = true;//pm.checkPermission("android.permission.ACCESS_COARSE_LOCATION",app)==PackageManager.PERMISSION_GRANTED;
            if (loc1 || loc2) {

                if (!isSystemPackage(appInfos.get(i), app)) {
                    //System.out.println(procName);
                    appList.add(app);
                }
                //appList.add(procName);
            }
        }
        //appList.add("android");
        return appList;
    }

    //have to do extra check here; google apps + LP Doctor must be excluded.
    static private boolean isSystemPackage(ApplicationInfo pkgInfo, String app) {
        if (app.equals(App.PACKAGE_NAME)) {
            return true;
        }

        if (app.equals(LAUNCHER_PACKNAME)) {
            return true;
        }


        return ((pkgInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true
                : false;
    }

    static public boolean isSystemApp(Context context, String app) {
        if (systemApps.containsKey(app)) {
            return systemApps.get(app);
        }

        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(app, PackageManager.GET_META_DATA);
            boolean systemApp = isSystemPackage(ai, app);
            systemApps.put(app, systemApp);
            return systemApp;
        } catch (PackageManager.NameNotFoundException e) {
            systemApps.put(app, false);
            return false;
        }
    }
}
