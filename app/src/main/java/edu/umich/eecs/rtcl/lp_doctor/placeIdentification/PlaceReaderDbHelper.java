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

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        //create new cities table, fill it with content from the csv file -- one time thing :)
        // db.execSQL(SQL_CREATE_CITIES);
        // db.execSQL( "Create Index CityTable_latitude_idx ON " + CityEntry.TABLE_NAME+"("+CityEntry.COLUMN_NAME_LAT+
        //         "," + CityEntry.COLUMN_NAME_LON+");");

        // db.execSQL(SQL_CREATE_RULES);
    }

    public void flushRules() {
        this.getWritableDatabase().execSQL("DELETE FROM " + RuleEntry.TABLE_NAME);
    }


    public boolean fillCityDB() {
        //read from US-cities-proc.csv
        //insert data row by row
        int numRows = (int) DatabaseUtils.queryNumEntries(getReadableDatabase(), CityEntry.TABLE_NAME);
        if (numRows > 1) { //table has some elements
            return true;
        }
        //otherwise fill table
        try {
            InputStream adFile = context.getAssets().open("US-cities-proc.csv");
            BufferedReader br = new BufferedReader(new InputStreamReader((adFile)));
            String line;
            while ((line = br.readLine()) != null) {

                String[] fields = line.split(COMMA_SEP);
                String country = fields[1];
                String state = fields[2];
                String city = fields[3];
                int zipcode = 0;
                try {
                    zipcode = Integer.parseInt(fields[4]);
                } catch (Exception e) {
                }

                double lat = 0;
                try {
                    lat = Double.parseDouble(fields[5]);
                } catch (Exception e) {
                }

                double lon = 0;
                try {
                    lon = Double.parseDouble(fields[6]);
                } catch (Exception e) {
                }

                ContentValues values = new ContentValues();
                values.put(CityEntry.COLUMN_NAME_COUNTRY, country);
                values.put(CityEntry.COLUMN_NAME_STATE, state);
                values.put(CityEntry.COLUMN_NAME_CITY, city);
                values.put(CityEntry.COLUMN_NAME_ZIPCODE, zipcode);
                values.put(CityEntry.COLUMN_NAME_LAT, lat);
                values.put(CityEntry.COLUMN_NAME_LON, lon);

                this.getWritableDatabase().insert(CityEntry.TABLE_NAME, null, values);
            }
        } catch (Exception e) {
            System.err.println("Failed to read ad File");
            return false;
        }
        return true;
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
