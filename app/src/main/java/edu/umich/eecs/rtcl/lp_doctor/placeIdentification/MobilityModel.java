/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.placeIdentification;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import edu.umich.eecs.rtcl.lp_doctor.App;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.PlaceEntry;

/**
 * Created by kmfawaz on 1/22/2015.
 */
public class MobilityModel {

    //very simple things going on here :)
    SparseArray<Double> placesPDF;


    public MobilityModel() {
        fillMobilityModel();
    }

    //how frequently to fill this model, at every session?
    //or every time the user visited new place
    private void fillMobilityModel() {
        placesPDF = new SparseArray<Double>();
        //for (int i=0;i<100;i++) {
        //    mobilityModel.put(i, i*1.253647616); // what is the reason for this ???? probably for testing
        // }
        //where the total time is larger than 0, right ???????
        String strWhere = " where " + PlaceEntry.COLUMN_NAME_TOTAL_TIME + " >" + (60 * 1000); //interested in places with more than 1 minute
        String strSelect = "select " + PlaceEntry.COLUMN_NAME_PLACE_ID + ", " + PlaceEntry.COLUMN_NAME_TOTAL_TIME +
                " *1.0/(select sum(" + PlaceEntry.COLUMN_NAME_TOTAL_TIME + ") from " + PlaceEntry.TABLE_NAME + ") from "
                + PlaceEntry.TABLE_NAME + strWhere; //gives you everything you need

        //SQLiteDatabase db = mDbHelper.getReadableDatabase();
        SQLiteDatabase db = App.getReadableDB();
        Cursor c = db.rawQuery(strSelect, null);

        while (c.moveToNext()) {
            int place = c.getInt(0);
            double probability = c.getDouble(1);
            placesPDF.put(place, probability);
        }
        c.close();
    }

    public SparseArray<Double> getMobilityPDF() {
        return placesPDF;
    }


    private void printMobilityModel() {
        int place = 0;
        for (int i = 0; i < placesPDF.size(); i++) {
            place = placesPDF.keyAt(i);
            double probability = placesPDF.get(place);
            Util.Log(Util.DB_TAG, place + "\t" + probability);
        }
    }

}
