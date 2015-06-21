/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.rule;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.LatLng;

import edu.umich.eecs.rtcl.lp_doctor.App;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.RuleEntry;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData.SingleRule;

public class RuleInterface {

    public static final String APP_FLAG = "**_all_apps";
    public static final int PLACE_FLAG = 99999999; //hopefully no one will accumulate that many places
    private static final String FIXED_COOR_FILE = "fixed_coordinates";

    //PlaceReaderDbHelper mDbHelper;
    public RuleInterface() {
        //this.mDbHelper = mDbHelper;
    }


    //add rule
    //modify rule
    //remove rule
    //fetch rule
    //probably used for settings page
    public RuleData fetchAllRules(String app) {

        RuleData appRule = new RuleData(app);
        String strWhere = RuleEntry.COLUMN_NAME_PACKNAME + " = \'" + app + "\'";

        SQLiteDatabase db = App.getReadableDB();//.getReadableDatabase();

        // columns we are interested in
        String[] projection = {
                RuleEntry.COLUMN_NAME_PLACE_ID,
                RuleEntry.COLUMN_NAME_FORE_RULE,
                RuleEntry.COLUMN_NAME_BACK_RULE,
                RuleEntry.COLUMN_NAME_PERS_RULE,
                RuleEntry.COLUMN_NAME_TRACK_LEVEL,
                RuleEntry.COLUMN_NAME_PROF_LEVEL,
                RuleEntry.COLUMN_NAME_LAT,
                RuleEntry.COLUMN_NAME_LON
        };

        Cursor c = db.query(RuleEntry.TABLE_NAME, projection, strWhere, null, null, null, null, null);

        //c.moveToFirst();

        // no matching record
        if (c.getCount() == 0) {
            ////System.out.println("NO RULES FOUND!");
            return appRule;
        }
        ////System.out.println("found rules");
        while (c.moveToNext()) {
            ////System.out.println("getting subsequent rules");
            appRule.fetchSubRule(c);
        }

        return appRule;
    }

    public int getProtectionLevel(String app) {

        String strWhere = "(" + RuleEntry.COLUMN_NAME_PACKNAME + "='" + app + "' OR " +
                RuleEntry.COLUMN_NAME_PACKNAME + "= '" + RuleInterface.APP_FLAG + "')";

        Util.Log(Util.DB_TAG, strWhere);
        SQLiteDatabase db = App.getReadableDB();

        String[] projection = {RuleEntry.COLUMN_NAME_PROF_LEVEL};
        String sortOrder = RuleEntry.COLUMN_NAME_PLACE_ID + " DESC";
        String limit = "1"; //favor older clusters over new ones

        Cursor c = db.query(RuleEntry.TABLE_NAME, projection, strWhere, null, null, null, sortOrder, limit);
        c.moveToFirst();
        // no matching record
        if (c.getCount() == 0) {
            return 0; // does 0 have any significance?
        }
        return c.getInt(c.getColumnIndex(RuleEntry.COLUMN_NAME_PROF_LEVEL));
    }

    //implement rule caching over here (app & place)
    //very important to make the decision; place -1 --> global rule
    public SingleRule fetchUpToDateRule(String app, int place) {
        RuleData appRule = new RuleData(app);

        String strWhereFirstClause = "(" +
                RuleEntry.COLUMN_NAME_PLACE_ID + "=" + place + " OR " +
                RuleEntry.COLUMN_NAME_PLACE_ID + "=-1" + " OR " +
                RuleEntry.COLUMN_NAME_PLACE_ID + "=" + RuleInterface.PLACE_FLAG +
                ")";

        String strWhereSecondClause = "(" + RuleEntry.COLUMN_NAME_PACKNAME + "='" + app + "' OR " +
                RuleEntry.COLUMN_NAME_PACKNAME + "= '" + RuleInterface.APP_FLAG + "')";

        String strWhere = strWhereFirstClause + " AND " + strWhereSecondClause;

        Util.Log(Util.DB_TAG, strWhere);
        SQLiteDatabase db = App.getReadableDB();//mDbHelper.getReadableDatabase();

        //columns we are interested in
        String[] projection = {
                RuleEntry.COLUMN_NAME_PLACE_ID,
                RuleEntry.COLUMN_NAME_FORE_RULE,
                RuleEntry.COLUMN_NAME_BACK_RULE,
                RuleEntry.COLUMN_NAME_PERS_RULE,
                RuleEntry.COLUMN_NAME_TRACK_LEVEL,
                RuleEntry.COLUMN_NAME_PROF_LEVEL, //that's what we are going to rely on
                RuleEntry.COLUMN_NAME_LAT,
                RuleEntry.COLUMN_NAME_LON
        };

        String sortOrder = RuleEntry.COLUMN_NAME_PLACE_ID + " DESC, " +
                RuleEntry.COLUMN_NAME_PACKNAME + " DESC";

        //favor per-place rules
        String limit = "1"; //favor older clusters over new ones

        Cursor c = db.query(
                RuleEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                strWhere,                                // The columns for the WHERE clause
                null,                                    // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder,                                // The sort order
                limit                                     //limit string
        );
        c.moveToFirst();
        // no matching record
        if (c.getCount() == 0) {
            return null;
        }
        return appRule.fetchSubRule(c);
    }

    public boolean removeRule(String app, int place) {
        String strWhere = RuleEntry.COLUMN_NAME_PACKNAME + "='" + app + "' AND " +
                RuleEntry.COLUMN_NAME_PLACE_ID + "=" + place;

        ////System.out.println(strWhere);
        SQLiteDatabase db = App.getWritableDB();//mDbHelper.getWritableDatabase();
        int numRows = db.delete(RuleEntry.TABLE_NAME, strWhere, null);
        return numRows > 0;
    }

    //I guess rule
    public boolean addRule(String app, int place, SingleRule rule) {
        String strInsert = "insert or replace into " + RuleEntry.TABLE_NAME + "(" +
                RuleEntry.COLUMN_NAME_PACKNAME + "," +
                RuleEntry.COLUMN_NAME_PLACE_ID + "," +
                RuleEntry.COLUMN_NAME_FORE_RULE + "," +
                RuleEntry.COLUMN_NAME_BACK_RULE + "," +
                RuleEntry.COLUMN_NAME_PERS_RULE + "," +
                RuleEntry.COLUMN_NAME_TRACK_LEVEL + "," +
                RuleEntry.COLUMN_NAME_PROF_LEVEL + "," +
                RuleEntry.COLUMN_NAME_LAT + "," +
                RuleEntry.COLUMN_NAME_LON + ") values ('" +
                app + "'," + rule.toString() + ")";
        ////System.out.println(strInsert);
        SQLiteDatabase db = App.getWritableDB();//mDbHelper.getWritableDatabase();
        db.execSQL(strInsert);
        //insert or replace into Book (Name, TypeID, Level, Seen) values ( ... )
        return true;
    }

    // Populate from the prompts and settings menu later
    public boolean updateForeRule(String app, int place, SingleRule rule) {
        ContentValues cv = new ContentValues();
        cv.put(RuleEntry.COLUMN_NAME_FORE_RULE, rule.foreRule);
        cv.put(RuleEntry.COLUMN_NAME_PROF_LEVEL, rule.profLevel);
        cv.put(RuleEntry.COLUMN_NAME_TRACK_LEVEL, rule.trackLevel);

        SQLiteDatabase db = App.getWritableDB();//mDbHelper.getWritableDatabase();
        String strWhere = RuleEntry.COLUMN_NAME_PACKNAME + "='" + app + "' AND " +
                RuleEntry.COLUMN_NAME_PLACE_ID + "=" + place;
        int rows = db.update(RuleEntry.TABLE_NAME, cv, strWhere, null);

        if (rows == 0) { //no global rule set
            addRule(app, place, rule); //the old way
        }

        return (rows > 0);
    }

    //I don't think we will be using this
    //insert new rule if there is none to update
    public boolean updatePersRule(String app, int place, int value) {
        ContentValues cv = new ContentValues();
        cv.put(RuleEntry.COLUMN_NAME_PERS_RULE, value);
        SQLiteDatabase db = App.getWritableDB();//mDbHelper.getWritableDatabase();
        String strWhere = RuleEntry.COLUMN_NAME_PACKNAME + "='" + app + "' AND " +
                RuleEntry.COLUMN_NAME_PLACE_ID + "=" + place;
        int rows = db.update(RuleEntry.TABLE_NAME, cv, strWhere, null);

        if (rows == 0) { //no global rule set
            SingleRule rule = new SingleRule(place, RuleData.NONE, RuleData.CITY_LEVEL, value,
                    RuleData.ANON_NONE, RuleData.ANON_NONE, 0, 0);
            addRule(app, place, rule);
        }

        return (rows > 0);
    }

    public void setFixedLocation(String app, int place, LatLng fakeLocation, int privacyLevel, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(FIXED_COOR_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putFloat(getFixedRuleKeyLat(app, place, privacyLevel), (float) fakeLocation.latitude);
        editor.putFloat(getFixedRuleKeyLon(app, place, privacyLevel), (float) fakeLocation.longitude);

        editor.commit();
    }

    public LatLng getFixedLocation(String app, int place, int privacyLevel, Context context) {
        //shared preferences get the app's lat and lon
        SharedPreferences sharedPref = context.getSharedPreferences(FIXED_COOR_FILE, Context.MODE_PRIVATE);
        double latitude = sharedPref.getFloat(getFixedRuleKeyLat(app, place, privacyLevel), 10000);
        double longitude = sharedPref.getFloat(getFixedRuleKeyLon(app, place, privacyLevel), 10000);

        if (latitude > 9999 || longitude > 9999) {
            return null; //if nothing found
        }
        return new LatLng(latitude, longitude);
    }

    private String getFixedRuleKeyLat(String app, int place, int privacyLevel) {
        return app + ":" + place + ":" + privacyLevel + ":lat";
    }

    private String getFixedRuleKeyLon(String app, int place, int privacyLevel) {
        return app + ":" + place + ":" + privacyLevel + ":lon";
    }

}
