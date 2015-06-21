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

import android.provider.BaseColumns;

public final class LocationContract {

    public LocationContract() {
    }

    /* Inner class that defines the table contents */
    public static abstract class PlaceEntry implements BaseColumns {
        public static final String TABLE_NAME = "place";
        public static final String COLUMN_NAME_PLACE_ID = "placeid";
        public static final String COLUMN_NAME_LAT = "latitude";
        public static final String COLUMN_NAME_LON = "longitude";
        public static final String COLUMN_NAME_COUNT = "count";
        public static final String COLUMN_NAME_TOTAL_TIME = "totaltime";
        public static final String COLUMN_NAME_LAST_TIME = "lasttime";
    }

    public static abstract class LogEntry implements BaseColumns {
        public static final String TABLE_NAME = "log";
        public static final String COLUMN_NAME_LOG_ENTRY = "entry";
    }

    public static abstract class CityEntry implements BaseColumns {
        public static final String TABLE_NAME = "cities";
        public static final String COLUMN_NAME_COUNTRY = "country";
        public static final String COLUMN_NAME_STATE = "state";
        public static final String COLUMN_NAME_CITY = "city";
        public static final String COLUMN_NAME_ZIPCODE = "zipcode";
        public static final String COLUMN_NAME_LAT = "latitude";
        public static final String COLUMN_NAME_LON = "longitude";
    }

    public static abstract class AppHistogram implements BaseColumns {
        public static final String TABLE_NAME = "histogram";
        public static final String COLUMN_NAME_PACKNAME = "app";
        public static final String COLUMN_NAME_PLACE_ID = "placeid";
        public static final String COLUMN_NAME_ACTUAL = "actual"; //real # of sessions from place detector
        //increment if app ran while anonymization was disabled, otherwise keep as is?
        public static final String COLUMN_NAME_REPORTED = "reported"; //reported # of sessions
    }

    //only persistent rules make it here
    public static abstract class RuleEntry implements BaseColumns {
        public static final String TABLE_NAME = "rules";
        public static final String COLUMN_NAME_PACKNAME = "app";
        public static final String COLUMN_NAME_PLACE_ID = "placeid";
        public static final String COLUMN_NAME_FORE_RULE = "forerule";
        public static final String COLUMN_NAME_BACK_RULE = "backrule";
        public static final String COLUMN_NAME_PERS_RULE = "persrule";
        public static final String COLUMN_NAME_TRACK_LEVEL = "tracklevel";
        public static final String COLUMN_NAME_PROF_LEVEL = "proflevel";
        public static final String COLUMN_NAME_LAT = "latitude"; //if forcing a specific translation
        public static final String COLUMN_NAME_LON = "longitude"; //if forcing a specific translation
    }

}
