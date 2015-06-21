/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.placeIdentification;

/**
 * Created by kmfawaz on 1/16/2015.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.AppHistogram;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.CityEntry;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.LogEntry;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.PlaceEntry;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.RuleEntry;

public class PlaceReaderDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 15;
    public static final String DATABASE_NAME = "PlaceReader.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_PLACES =
            "CREATE TABLE " + PlaceEntry.TABLE_NAME + " (" +
                    PlaceEntry.COLUMN_NAME_PLACE_ID + " INTEGER PRIMARY KEY," +
                    PlaceEntry.COLUMN_NAME_LAT + REAL_TYPE + COMMA_SEP +
                    PlaceEntry.COLUMN_NAME_LON + REAL_TYPE + COMMA_SEP +
                    PlaceEntry.COLUMN_NAME_COUNT + INT_TYPE + COMMA_SEP +
                    PlaceEntry.COLUMN_NAME_TOTAL_TIME + INT_TYPE + COMMA_SEP +
                    PlaceEntry.COLUMN_NAME_LAST_TIME + INT_TYPE +
                    " )";
    private static final String SQL_CREATE_CITIES =
            "CREATE TABLE " + CityEntry.TABLE_NAME + " (" +
                    CityEntry._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                    CityEntry.COLUMN_NAME_COUNTRY + TEXT_TYPE + COMMA_SEP +
                    CityEntry.COLUMN_NAME_STATE + TEXT_TYPE + COMMA_SEP +
                    CityEntry.COLUMN_NAME_CITY + TEXT_TYPE + COMMA_SEP +
                    CityEntry.COLUMN_NAME_ZIPCODE + INT_TYPE + COMMA_SEP +
                    CityEntry.COLUMN_NAME_LAT + REAL_TYPE + COMMA_SEP +
                    CityEntry.COLUMN_NAME_LON + REAL_TYPE +
                    " )";
    private static final String SQL_CREATE_LOG =
            "CREATE TABLE " + LogEntry.TABLE_NAME + " (" +
                    LogEntry._ID + " INTEGER PRIMARY KEY," +
                    LogEntry.COLUMN_NAME_LOG_ENTRY +
                    " )";
    private static final String SQL_CREATE_RULES =
            "CREATE TABLE " + RuleEntry.TABLE_NAME + " (" +
                    RuleEntry.COLUMN_NAME_PACKNAME + TEXT_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_PLACE_ID + INT_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_FORE_RULE + INT_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_BACK_RULE + INT_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_PERS_RULE + INT_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_TRACK_LEVEL + INT_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_PROF_LEVEL + INT_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_LAT + REAL_TYPE + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_LON + REAL_TYPE + COMMA_SEP +
                    "PRIMARY KEY (" + RuleEntry.COLUMN_NAME_PACKNAME + COMMA_SEP +
                    RuleEntry.COLUMN_NAME_PLACE_ID +
                    " ))";
    private static final String SQL_DELETE_PLACES =
            "DROP TABLE IF EXISTS " + PlaceEntry.TABLE_NAME;
    // If you change the database schema, you must increment the database version.
/*
    private static final String SQL_DELETE_LOGS =
            "DROP TABLE IF EXISTS " + LogEntry.TABLE_NAME;

    private static final String SQL_DELETE_CITIES =
            "DROP TABLE IF EXISTS " + CityEntry.TABLE_NAME;

    */
    private static final String SQL_DELETE_RULES =
            "DROP TABLE IF EXISTS " + RuleEntry.TABLE_NAME;
    private static final String SQL_CREATE_HISTOGRAM =
            "CREATE TABLE " + LocationContract.AppHistogram.TABLE_NAME + " (" +
                    LocationContract.AppHistogram._ID + " INTEGER PRIMARY KEY" + COMMA_SEP +
                    LocationContract.AppHistogram.COLUMN_NAME_PACKNAME + TEXT_TYPE + COMMA_SEP +
                    LocationContract.AppHistogram.COLUMN_NAME_PLACE_ID + TEXT_TYPE + COMMA_SEP +
                    LocationContract.AppHistogram.COLUMN_NAME_ACTUAL + INT_TYPE + COMMA_SEP +
                    LocationContract.AppHistogram.COLUMN_NAME_REPORTED + INT_TYPE +
                    " )";
    private static final String SQL_DELETE_HISTOGRAMS =
            "DROP TABLE IF EXISTS " + LocationContract.AppHistogram.TABLE_NAME;
    Context context;

    public PlaceReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_PLACES);
        db.execSQL("Create Index PlaceTable_latitude_idx ON " + PlaceEntry.TABLE_NAME + "(" + PlaceEntry.COLUMN_NAME_LAT +
                "," + PlaceEntry.COLUMN_NAME_LON + ");");

        db.execSQL(SQL_CREATE_LOG);

        db.execSQL(SQL_CREATE_HISTOGRAM);
        db.execSQL("Create Index HistogramTable_latitude_idx ON " + AppHistogram.TABLE_NAME + "(" +
                AppHistogram.COLUMN_NAME_PACKNAME + ");");

        db.execSQL(SQL_CREATE_RULES);// need index on place and app?
    }

    public void flushRules() {
        this.getWritableDatabase().execSQL("DELETE FROM " + RuleEntry.TABLE_NAME);
    }



    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        // db.execSQL(SQL_DELETE_PLACES);
        // db.execSQL(SQL_CREATE_PLACES);
        //db.execSQL(SQL_DELETE_LOGS);
        //db.execSQL(SQL_DELETE_CITIES);
        //db.execSQL(SQL_DELETE_RULES);
        db.execSQL(SQL_CREATE_RULES);
        //db.execSQL(SQL_DELETE_HISTOGRAMS);
        //db.execSQL(SQL_CREATE_HISTOGRAM);
        //db.execSQL( "Create Index HistogramTable_latitude_idx ON " + AppHistogram.TABLE_NAME+"("+
        //        AppHistogram.COLUMN_NAME_PACKNAME+ ");");

        //onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) { //very smart I must say !!!!!!!!!!!! (being sarcastic)
        onUpgrade(db, oldVersion, newVersion);
    }

}
