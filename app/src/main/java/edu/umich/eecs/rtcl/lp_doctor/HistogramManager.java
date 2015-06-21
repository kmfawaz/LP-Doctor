/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.AppHistogram;

public class HistogramManager {

    //behavior depends on actual or reported value on the histogram
    //this refers to the old way where we didn't know how to manage the sessions correctly
    //previously we assumed a list of places per session, we are being more restrictive now?
    //assume a start and end place for the app for actual, but one for reported which has not been implemented yet
    public static final int REPORTED_FLAG = 0;
    public static final int ACTUAL_FLAG = 1;

    Context context;
    //MonitoringService mainService; //not needed for the time being
    //HistogramDbHelper mDbHelper;

    public HistogramManager(Context context) {
        this.context = context;
        // this.mainService = mainService;
        //mDbHelper = new HistogramDbHelper (context);
    }


    public void updateHistogram(String app, int place, int flag) {
        if (app.equals("") || place == -1) {
            return;
        }

        //flag = 1 --> actual
        //flag = 2 --> reported

        int id = getID(app, place); //id of app-place combination
        //SQLiteDatabase db = mDbHelper.getWritableDatabase();
        SQLiteDatabase db = App.getWritableDB();

        //System.out.println("updating record...." + id);

        if (id == -1) {
            //insert new element and return new place id
            ContentValues values = new ContentValues();
            values.put(AppHistogram.COLUMN_NAME_PACKNAME, app);
            values.put(AppHistogram.COLUMN_NAME_PLACE_ID, place);

            if (flag == ACTUAL_FLAG) {
                values.put(AppHistogram.COLUMN_NAME_REPORTED, 0);
                values.put(AppHistogram.COLUMN_NAME_ACTUAL, 1);
            } else if (flag == REPORTED_FLAG) {
                values.put(AppHistogram.COLUMN_NAME_REPORTED, 1);
                values.put(AppHistogram.COLUMN_NAME_ACTUAL, 0);
            } else {
                //nothing to do return
                //db.close();
                return;
            }

            db.insert(AppHistogram.TABLE_NAME, null, values);
        }

        //System.out.println("updating record");
        //o.w. return existing ID after updating record
        String selection = AppHistogram._ID + " = " + id;
        String strUpdate = "";
        if (flag == ACTUAL_FLAG) {
            strUpdate = "UPDATE " + AppHistogram.TABLE_NAME + " SET actual = actual+1 WHERE " + selection;
        } else if (flag == REPORTED_FLAG) {
            strUpdate = "UPDATE " + AppHistogram.TABLE_NAME + " SET reported = reported+1 WHERE " + selection;
        } //there is not else.
        db.execSQL(strUpdate);
        //db.close();
        dumpHistograms();

    }

    //print the content of the histograms
    private void dumpHistograms() {
        SQLiteDatabase db = App.getReadableDB();

        String sortOrder = AppHistogram.COLUMN_NAME_PACKNAME + " DESC"; //favor clusters with higher support over ones with less

        String[] projection = {AppHistogram._ID,
                AppHistogram.COLUMN_NAME_PACKNAME,
                AppHistogram.COLUMN_NAME_PLACE_ID,
                AppHistogram.COLUMN_NAME_ACTUAL,
                AppHistogram.COLUMN_NAME_REPORTED
        };

        Cursor c = db.query(AppHistogram.TABLE_NAME, projection, null, null, null, null, sortOrder, null);
        while (c.moveToNext()) {
            String packName = c.getString(c.getColumnIndex(AppHistogram.COLUMN_NAME_PACKNAME));
            int placeID = c.getInt(c.getColumnIndex(AppHistogram.COLUMN_NAME_PLACE_ID));
            int actual = c.getInt(c.getColumnIndex(AppHistogram.COLUMN_NAME_ACTUAL));
            int reported = c.getInt(c.getColumnIndex(AppHistogram.COLUMN_NAME_REPORTED));
            Util.Log(Util.DB_TAG, packName + "\t" + placeID + "\t" + actual + "\t" + reported);
        }
        Util.Log(Util.DB_TAG, "****************************");

        c.close();
    }

    public SparseArray<Integer> getHistogram(String app) {
        SparseArray<Integer> histogram = new SparseArray<Integer>();

        String strWhere = AppHistogram.COLUMN_NAME_PACKNAME + " = \'" + app + "\'";
        SQLiteDatabase db = App.getReadableDB();
        String[] projection = {AppHistogram.COLUMN_NAME_PLACE_ID, AppHistogram.COLUMN_NAME_REPORTED};
        Cursor c = db.query(AppHistogram.TABLE_NAME, projection, strWhere, null, null, null, null, null);
        while (c.moveToNext()) {
            int place = c.getInt(c.getColumnIndex(AppHistogram.COLUMN_NAME_PLACE_ID));
            int reported = c.getInt(c.getColumnIndex(AppHistogram.COLUMN_NAME_REPORTED));
            histogram.put(place, reported);
        }
        return histogram;
    }

    // need to get the ID -- of what???? -- UI related?
    private int getID(String app, int place) {

        String strWhere = AppHistogram.COLUMN_NAME_PACKNAME + " = \'" + app + "\' AND " +
                AppHistogram.COLUMN_NAME_PLACE_ID + " = " + place;

        ////System.out.println(strWhere);
        //SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteDatabase db = App.getReadableDB();

        //columns we are interested in
        String[] projection = {AppHistogram._ID,
                AppHistogram.COLUMN_NAME_PACKNAME,
                AppHistogram.COLUMN_NAME_PLACE_ID
        };

        Cursor c = db.query(AppHistogram.TABLE_NAME, projection, strWhere, null, null, null, null, null);
        c.moveToFirst();
        // no matching record
        if (c.getCount() == 0) {
            c.close();
            return -1;
        }
        int id = c.getInt(c.getColumnIndex(AppHistogram._ID));
        c.close();
        return id;
    }
}