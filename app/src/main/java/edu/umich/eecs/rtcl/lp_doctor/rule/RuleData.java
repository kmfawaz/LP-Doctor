/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.rule;

import android.database.Cursor;
import android.util.SparseArray;

import java.util.LinkedList;

import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract.RuleEntry;

/*public static abstract class RuleEntry implements BaseColumns {
        public static final String TABLE_NAME = "rules";
        public static final String COLUMN_NAME_PACKNAME = "app";
        public static final String COLUMN_NAME_PLACE_ID = "placeid";
        public static final String COLUMN_NAME_FORE_RULE = "forerule";
        public static final String COLUMN_NAME_BACK_RULE = "backrule";
        public static final String COLUMN_NAME_PERS_RULE = "persrule";
        public static final String COLUMN_NAME_FORE_LEVEL = "forelevel";
        public static final String COLUMN_NAME_LAT = "latitude"; //if forcing a specific translation
        public static final String COLUMN_NAME_LON = "longitude"; //if forcing a specific translation
    }
    */

public class RuleData {
    static public final int NO_ACTION = 0; // do nothing
    static public final int CITY_LEVEL = 1; //city-level anonymization
    static public final int BLOCK = 2; //block location access
    static public final int LOW_DIFF_PRIV = 3; //apply low level indistinguishability mechanism -- CCS paper
    static public final int HIGH_DIFF_PRIV = 4; //apply high level indistinguishability mechanism -- our mechanism
    static public final int SYN_ROUTE = 5; //synthetic route
    static public final int FIXED = 6; //fixed location
    static public final int NONE = -1; //fixed location

    //add more levels, 1-10; 10 being high anon
    static public final int ANON_NONE = 0; //no anonymization level set
    static public final int ANON_LOW = 1; //low tracking/profiling protection
    static public final int ANON_MEDIUM = 5; //medium
    static public final int ANON_HIGH = 10; //high

    String app;
    SparseArray<SingleRule> rulesByLocation; //every place id might be associated with a different rule

    public RuleData(String app) {
        this.app = app;
        rulesByLocation = new SparseArray<SingleRule>();
    }

    public SingleRule fetchSubRule(Cursor c) {
        try {

            int place = c.getInt(c.getColumnIndex(RuleEntry.COLUMN_NAME_PLACE_ID));
            int foreRule = c.getInt(c.getColumnIndex(RuleEntry.COLUMN_NAME_FORE_RULE));
            int backRule = c.getInt(c.getColumnIndex(RuleEntry.COLUMN_NAME_BACK_RULE));
            int persRule = c.getInt(c.getColumnIndex(RuleEntry.COLUMN_NAME_PERS_RULE));
            int trackLevel = c.getInt(c.getColumnIndex(RuleEntry.COLUMN_NAME_TRACK_LEVEL));
            int profLevel = c.getInt(c.getColumnIndex(RuleEntry.COLUMN_NAME_PROF_LEVEL));
            double lat = c.getDouble(c.getColumnIndex(RuleEntry.COLUMN_NAME_LAT));
            double lon = c.getDouble(c.getColumnIndex(RuleEntry.COLUMN_NAME_LON));
            SingleRule entry = new SingleRule(place, foreRule, backRule, persRule, trackLevel, profLevel, lat, lon);
            rulesByLocation.put(place, entry);
            return entry;
        } catch (Exception e) {
            //failed to add subrule
            e.printStackTrace();
        }
        return null;
    }

    public SingleRule getRuleByPlace(int place) {
        return rulesByLocation.get(place);
    }

    public LinkedList<Integer> getPlaces() {
        LinkedList<Integer> keys = new LinkedList<Integer>();
        //System.out.println("getting places...... ");
        for (int ind = 0; ind < rulesByLocation.size(); ind++) {
            int place = rulesByLocation.keyAt(ind);
            //System.out.println("getting place id: "+ place );
            if (place != -1) {
                keys.add(rulesByLocation.keyAt(ind));
                //place -1 must get special treatment, we will face the error later.
            }
        }
        return keys;
    }

    public void print() {
        //System.out.println(app);
        for (int ind = 0; ind < rulesByLocation.size(); ind++) {
            //System.out.println(rulesByLocation.valueAt(ind));
        }
    }

    public boolean isEmpty() {
        return rulesByLocation.size() == 0;
    }


    public static class SingleRule {
        public int placeid; //placeid -1 for all possibles
        public int foreRule;
        public int backRule; //no action, city level, block
        public int persRule;
        public int trackLevel; //from 1 to 100;
        public int profLevel; //from 1 to 100;
        public double lat; //
        public double lon;

        public SingleRule(int placeid, int foreRule, int backRule, int persRule, int trackLevel, int profLevel, double lat, double lon) {
            this.placeid = placeid;
            this.foreRule = foreRule;
            this.backRule = backRule;
            this.persRule = persRule;
            this.trackLevel = trackLevel;
            this.profLevel = profLevel;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() {
            return placeid + "," + foreRule + "," + backRule + "," + persRule + "," + trackLevel + "," + profLevel + "," + lat + "," + lon;
        }
    }
}
