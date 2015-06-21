/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.PlaceReaderDbHelper;

/**
 * Created by kmfawaz on 1/22/2015.
 */
public class App extends Application {
    public static String PACKAGE_NAME;
    private static SQLiteDatabase dbWrite;
    private static SQLiteDatabase dbRead;

    public static SQLiteDatabase getWritableDB() {
        return dbWrite;
    }

    public static SQLiteDatabase getReadableDB() {
        return dbRead;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PlaceReaderDbHelper db = new PlaceReaderDbHelper(getApplicationContext());
        dbWrite = db.getWritableDatabase();
        dbRead = db.getReadableDatabase();
        PACKAGE_NAME = getApplicationContext().getPackageName();
    }

}