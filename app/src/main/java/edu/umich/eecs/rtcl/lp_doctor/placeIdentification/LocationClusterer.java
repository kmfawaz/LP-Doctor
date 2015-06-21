/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.placeIdentification;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;
import java.util.LinkedList;

import edu.umich.eecs.rtcl.lp_doctor.App;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.PlaceEntry;

public class LocationClusterer {

    //maps a location to anonymous visited place
    //test on an existing trace

    final static private double RADIUS = 100;
    private static final int MIN_STAY = 7; //5 minutes minimum
    //basic data structure
    //map id to location
    LinkedList<placeRecord> locationDB;
    //PlaceReaderDbHelper mDbHelper; // ---> should be one object?
    int currentID;

    public LocationClusterer(Context context) {
        currentID = 0;
        //mDbHelper = new PlaceReaderDbHelper(context);
    }

    //just to find if we can use the cached location; also timeSpent is crucial -- will be evident later hopefully
    /*
    public static int getPlace(LatLng loc, CachedPlace currentPlace) {
        //if within the last known location (100m)
        //report place
        //o.w. go to DB to do lookup
        try {
            double cachedLat = currentPlace.getCurrentLoc().latitude;
            double cachedLon = currentPlace.getCurrentLoc().longitude;
            int placeCached = currentPlace.getPlaceID();

            float results[] = {0.0f};
            Location.distanceBetween(reportedLat, reportedLon, cachedLat, cachedLon, results);
            if (results[0] <= 100) {
                return placeCached;
            }

            return getPlaceFromDb(new LatLng(reportedLat, reportedLon)).placeID; //assuming it is in.
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
    */

    //not clear why is it needed, will come up later
    /*
    //return time in minutes, no entry, return 0
    public boolean getTimeSpent(int place, PlaceReaderDbHelper mDbHelper) {
        //if within the last known location (100m)
        //report place
        //o.w. go to DB to do lookup

        int placeCached = CachedPlace.getPlace();
        int timeSpent = CachedPlace.getTimeSpent() / 60;
        if (placeCached == place && timeSpent > MIN_STAY) {
            return true;
        }


        return getTimespentFromDb(place, mDbHelper) > MIN_STAY; //assuming it is in.
    }
*/

    //get location given a place ID
    public static LatLng getLoc(int place) {
        LatLng retResult;
        retResult = CachedPlace.getCachedLoc(place);
        if (retResult != null) {
            return retResult;
        }

        String where = PlaceEntry.COLUMN_NAME_PLACE_ID + " = " + place;

        //SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteDatabase db = App.getReadableDB();

        String[] proj = {PlaceEntry.COLUMN_NAME_LAT, PlaceEntry.COLUMN_NAME_LON};

        Cursor c = db.query(PlaceEntry.TABLE_NAME, proj, where, null, null, null, null, null);
        c.moveToFirst();
        if (c.getCount() == 0) {
            return null;
        }
        double latitude = c.getDouble(c.getColumnIndex(PlaceEntry.COLUMN_NAME_LAT));
        double longitude = c.getDouble(c.getColumnIndex(PlaceEntry.COLUMN_NAME_LON));
        c.close();
        return new LatLng(latitude, longitude);
    }

    //called whenever there is a new location update to get the place ID and update the time spent at the particular place
    public static int getPlaceForUI(LatLng location, float accuracy) {

        if (accuracy > 100) {
            return -2;
        }

        double lat = location.latitude;
        double lon = location.longitude;

        placeRecord record = getPlaceFromDb(location);
        SQLiteDatabase db = App.getWritableDB();

        //only insert new location if total time spent is > 10 minutes, disregards moving locations
        if (record == null) {
            //insert new element and return new place id
            ContentValues values = new ContentValues();
            values.put(PlaceEntry.COLUMN_NAME_LAT, lat);
            values.put(PlaceEntry.COLUMN_NAME_LON, lon);
            values.put(PlaceEntry.COLUMN_NAME_COUNT, 1);
            values.put(PlaceEntry.COLUMN_NAME_TOTAL_TIME, 0);
            values.put(PlaceEntry.COLUMN_NAME_LAST_TIME, 0);
            long newRowId;
            newRowId = db.insert(PlaceEntry.TABLE_NAME, null, values);

            return (int) newRowId;
        }

        //o.w. return existing ID after updating record with new spent time
        record.updateRecord(location);

        //time is measured always in ms

        //System.out.println("total time spent at place: "+ (timeSpent+record.timeTotal));
        ContentValues values = new ContentValues();
        values.put(PlaceEntry.COLUMN_NAME_LAT, record.latitude);
        values.put(PlaceEntry.COLUMN_NAME_LON, record.longitude);
        values.put(PlaceEntry.COLUMN_NAME_COUNT, record.count);
        values.put(PlaceEntry.COLUMN_NAME_TOTAL_TIME, 0);
        values.put(PlaceEntry.COLUMN_NAME_LAST_TIME, 0);
        String selection = PlaceEntry.COLUMN_NAME_PLACE_ID + " = " + record.placeID;
        db.update(PlaceEntry.TABLE_NAME, values, selection, null);


        return record.placeID;
    }

    //return a list of all place ids that the user has visited
    public static LinkedList<Integer> getAllPlaces() {


        LinkedList<Integer> places = new LinkedList<Integer>();

        //SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteDatabase db = App.getReadableDB();

        String strWhere = PlaceEntry.COLUMN_NAME_TOTAL_TIME + " >" + (5 * 60 * 1000); //interested in places with more than 1 minute
        String[] projection = {PlaceEntry.COLUMN_NAME_PLACE_ID};
        Cursor c = db.query(PlaceEntry.TABLE_NAME, projection, strWhere, null, null, null, null, null);

        // return matching record
        while (c.moveToNext()) {
            int placeID = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_PLACE_ID));
            places.add(placeID);
        }
        c.close();
        return places;
    }

    private static placeRecord getPlaceFromDb(LatLng location) {
        try {
            String latCol = PlaceEntry.COLUMN_NAME_LAT;
            String lonCol = PlaceEntry.COLUMN_NAME_LON;
            String strWhere = Util.genWhereRange(location, RADIUS, latCol, lonCol);

            ////System.out.println(strWhere);
            //SQLiteDatabase db = mDbHelper.getReadableDatabase();
            SQLiteDatabase db = App.getReadableDB();
            //columns we are interested in
            String[] projection = {
                    PlaceEntry.COLUMN_NAME_PLACE_ID,
                    PlaceEntry.COLUMN_NAME_LAT,
                    PlaceEntry.COLUMN_NAME_LON,
                    PlaceEntry.COLUMN_NAME_COUNT,
                    PlaceEntry.COLUMN_NAME_TOTAL_TIME
            };

            String sortOrder = PlaceEntry.COLUMN_NAME_COUNT + " DESC"; //favor clusters with higher support over ones with less
            String limit = "1"; //favor older clusters over new ones

            Cursor c = db.query(
                    PlaceEntry.TABLE_NAME,  // The table to query
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
            // return matching record
            int placeID = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_PLACE_ID));
            double latitude = c.getDouble(c.getColumnIndex(PlaceEntry.COLUMN_NAME_LAT));
            double longitude = c.getDouble(c.getColumnIndex(PlaceEntry.COLUMN_NAME_LON));
            int count = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_COUNT));
            int totalTime = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_TOTAL_TIME));
            placeRecord record = new placeRecord(latitude, longitude, count, placeID, totalTime);
            c.close();
            return record;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new placeRecord(0, 0, 0, -1, 0);
    }

    //called whenever there is a new location update to get the place ID and update the time spent at the particular place
    public CachedPlace getPlace(LatLng location, float accuracy, CachedPlace currentPlace) {
        //iterate overall records in DB
        //if matching --> fine
        //o.w. --> add new one

        long currentTime = new Date().getTime();

        if (accuracy > 100) {
            return null;
        }

        double lat = location.latitude;
        double lon = location.longitude;

        placeRecord record = getPlaceFromDb(location);
        //SQLiteDatabase db = mDbHelper.getWritableDatabase();
        SQLiteDatabase db = App.getWritableDB();

        //only insert new location if total time spent is > 10 minutes, disregards moving locations
        if (record == null) {
            //insert new element and return new place id
            ContentValues values = new ContentValues();
            values.put(PlaceEntry.COLUMN_NAME_LAT, lat);
            values.put(PlaceEntry.COLUMN_NAME_LON, lon);
            values.put(PlaceEntry.COLUMN_NAME_COUNT, 1);
            values.put(PlaceEntry.COLUMN_NAME_TOTAL_TIME, 0);
            values.put(PlaceEntry.COLUMN_NAME_LAST_TIME, new Date().getTime());
            long newRowId;
            newRowId = db.insert(PlaceEntry.TABLE_NAME, null, values);

            currentPlace.setCachedPlace((int) newRowId, lat, lon, currentTime);

            if (Util.DEBUG) {
                Log.v(Util.TAG, "place: " + (int) newRowId + " <" + lat + "," + lon + "> time: 0");
            }

            return currentPlace;
        }

        //o.w. return existing ID after updating record with new spent time
        record.updateRecord(location);

        //time is measured always in ms
        int timeSpent = (int) currentPlace.getTimeSpentSinceLastUpdate(record.placeID, currentTime);

        //System.out.println("total time spent at place: "+ (timeSpent+record.timeTotal));
        ContentValues values = new ContentValues();
        values.put(PlaceEntry.COLUMN_NAME_LAT, record.latitude);
        values.put(PlaceEntry.COLUMN_NAME_LON, record.longitude);
        values.put(PlaceEntry.COLUMN_NAME_COUNT, record.count);
        values.put(PlaceEntry.COLUMN_NAME_TOTAL_TIME, timeSpent + record.timeTotal);
        values.put(PlaceEntry.COLUMN_NAME_LAST_TIME, currentTime);
        String selection = PlaceEntry.COLUMN_NAME_PLACE_ID + " = " + record.placeID;
        db.update(PlaceEntry.TABLE_NAME, values, selection, null);

        //replace the current location by the cluster ID
        currentPlace.setCachedPlace(record.placeID, record.latitude, record.longitude, currentTime);

        if (Util.DEBUG) {
            Log.v(Util.TAG, "place: " + record.placeID + " <" + record.latitude + "," + record.longitude + "> time: " + (timeSpent + record.timeTotal) / 60000);
        }

        return currentPlace;
    }

    public void dumpPlacesDb() {

        try {
            SQLiteDatabase db = App.getReadableDB();
            //columns we are interested in
            String[] projection = {
                    PlaceEntry.COLUMN_NAME_PLACE_ID,
                    PlaceEntry.COLUMN_NAME_LAT,
                    PlaceEntry.COLUMN_NAME_LON,
                    PlaceEntry.COLUMN_NAME_COUNT,
                    PlaceEntry.COLUMN_NAME_TOTAL_TIME
            };

            String sortOrder = PlaceEntry.COLUMN_NAME_COUNT + " DESC"; //favor clusters with higher support over ones with less
            String strWhere = PlaceEntry.COLUMN_NAME_TOTAL_TIME + " >" + (5 * 60 * 1000);
            Cursor c = db.query(
                    PlaceEntry.TABLE_NAME,  // The table to query
                    projection,                               // The columns to return
                    strWhere,                                // The columns for the WHERE clause
                    null,                                    // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder,                                // The sort order
                    null                                     //limit string
            );
            while (c.moveToNext()) {
                int placeID = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_PLACE_ID));
                double latitude = c.getDouble(c.getColumnIndex(PlaceEntry.COLUMN_NAME_LAT));
                double longitude = c.getDouble(c.getColumnIndex(PlaceEntry.COLUMN_NAME_LON));
                int count = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_COUNT));
                int totalTime = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_TOTAL_TIME));
                placeRecord record = new placeRecord(latitude, longitude, count, placeID, totalTime);
                Util.Log(Util.DB_TAG, record.toGPSVString());

            }
            Util.Log(Util.DB_TAG, "***********************************");
            c.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getTimespentFromDb(int place) {

        String strWhere = PlaceEntry.COLUMN_NAME_PLACE_ID + "=" + place;

        ////System.out.println(strWhere);
        //SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteDatabase db = App.getReadableDB();

        //columns we are interested in
        String[] projection = {
                PlaceEntry.COLUMN_NAME_PLACE_ID,
                PlaceEntry.COLUMN_NAME_TOTAL_TIME
        };

        String sortOrder = PlaceEntry.COLUMN_NAME_COUNT + " DESC"; //favor clusters with higher support over ones with less
        String limit = "1"; //favor older clusters over new ones

        Cursor c = db.query(
                PlaceEntry.TABLE_NAME,  // The table to query
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
            return 0;
        }
        // return matching record
        int placeID = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_PLACE_ID));
        int totalTime = c.getInt(c.getColumnIndex(PlaceEntry.COLUMN_NAME_TOTAL_TIME));
        c.close();
        return totalTime;
    }

    static class placeRecord {
        private double latitude;
        private double longitude;
        private int count;
        private int placeID;
        private int timeTotal;

        public placeRecord(double latitude, double longitude, int count, int placeID, int timeTotal) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.count = count;
            this.placeID = placeID;
            this.timeTotal = timeTotal;
        }

        public void updateRecord(LatLng loc) {
            count++;
            latitude = (latitude * (count - 1) + loc.latitude) / count;
            longitude = (longitude * (count - 1) + loc.longitude) / count;
        }


        public String toString() {
            return placeID + ":" + latitude + ":" + longitude + ":" + count + ":" + timeTotal / (60 * 1000);
        }

        public String toGPSVString() {
            return placeID + "," + +timeTotal / (60 * 1000) + "," + latitude + "," + longitude;
        }

    }


}