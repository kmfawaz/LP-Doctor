/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.placeIdentification;

import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by kmfawaz on 1/16/2015.
 */
public class CachedPlace {
    public static LatLng currentLoc;
    private static SparseArray<LatLng> cache; //serves a cache for faster lookup between location and coordinates
    //results from the latest lookup
    private int currentPlace = -1;
    //private int timeInCurrentPlace; // continuously in this place, not the total time spent in the place since the start of logging
    private long lastUpdated;
    //private static String currentCity = "";
    //private static LatLng cityLoc = null;

    public CachedPlace() {
        if (cache == null) {
            cache = new SparseArray<LatLng>();
        }
        currentPlace = -1;
        currentLoc = new LatLng(0, 0);
        lastUpdated = 0;
    }

    //what if not cached?
    public static LatLng getCachedLoc(int place) {
        return cache.get(place);
    }

    public void setCachedPlace(int pl, double lat, double lon, long timeOfLocation) {
        if (cache == null) {
            cache = new SparseArray<LatLng>();
        }
        cache.put(pl, new LatLng(lat, lon));
        currentPlace = pl;
        currentLoc = new LatLng(lat, lon);
        lastUpdated = timeOfLocation;
    }

    public void setCachedPlace(CachedPlace updatedPlace) {
        if (updatedPlace == null) {
            return;
        }
        currentPlace = updatedPlace.currentPlace;
        currentLoc = currentLoc;
        lastUpdated = updatedPlace.lastUpdated;
    }

    public LatLng getCurrentLoc() {
        return currentLoc;
    }

    public int getPlaceID() {
        return currentPlace;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    //here we can detect that the user visited new location
    public long getTimeSpentSinceLastUpdate(int newPlaceID, long currentTime) {
        //newly visited place, time spent is initialized to 0.
        if (newPlaceID != currentPlace) {
            return 0;
        }

        long timeDiff = currentTime - lastUpdated;

        //some error resulted in negative time diff, or very long time since last update
        if (timeDiff > 10 * 60 * 1000 || timeDiff < 0) {
            return 0;
        }

        return timeDiff;
    }

    //public static LatLng getCityLoc() {
    //    return cityLoc;
    //}

    //public static String getCityName() {
    //    return currentCity;
    //}


    // public int getTimeSpent() {
    //     return timeInCurrentPlace;
    // }
}
