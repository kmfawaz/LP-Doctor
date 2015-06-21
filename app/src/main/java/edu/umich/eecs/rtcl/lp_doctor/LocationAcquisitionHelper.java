/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.Random;

import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.CachedPlace;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationClusterer;

/**
 * Created by kmfawaz on 1/13/2015.
 */

//we have to ignore the fake locations when we receive them :)
public class LocationAcquisitionHelper implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    Context context;

    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    MonitoringService mainService;
    LocationClusterer locClust;
    Random rand;
    boolean isFakingLocation = false;

    //this will improved on as we go. Essentially, implementing another version of LP-Guardian
    //must set a time to update

    //also need a timer over here + a bit of randomized location
    private Handler handler = new Handler();
    private Runnable runnable;
    private int interval = 5000;

    public LocationAcquisitionHelper(Context context, MonitoringService mainService) {
        this.context = context;
        this.mainService = mainService;

        locClust = new LocationClusterer(context);
        locClust.dumpPlacesDb();
        createLocationRequest();
        buildGoogleApiClient();
        mGoogleApiClient.connect();

        rand = new Random();
    }

    //must have an async task to control the anonymization
    public void initiateAnonymization(final LatLng fakeLocation) {
        //what if is enabled?-- disable
        //handler.postDelayed(runnable, interval);
        //have to set the fake location here.
        //latlng + accuracy
        if (isFakingLocation) {
            removeGMSFakeLocation();
        }

        double latitude = 0;
        double longitude = 0;
        double accuracy = 0;


        runnable = new Runnable() {
            public void run() {
                setGMSFakeLocation(fakeLocation);
                //System.out.println("service working ...");
                //i++;
                //send intent to UI to update the text view
                //UpdateUI();
                Log.v("RTCL", "setting fake location for GMS");
                handler.postDelayed(runnable, interval);
            }
        };
        setGMSFakeLocation(fakeLocation);
        new FakeLocationSetter().execute(fakeLocation); //here is where the execution happens
    }

    private void setGMSFakeLocation(LatLng fakeLocation) {

        isFakingLocation = true;
        Location location = new Location(LocationManager.NETWORK_PROVIDER);

        //41.854955,	-87.692871
        location.setLatitude(fakeLocation.latitude);//+rand.nextDouble()/100);
        location.setLongitude(fakeLocation.longitude);//+rand.nextDouble()/100);

        location.setAccuracy(20); //need to study this further


        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(System.nanoTime());
        Log.v("RTCL", "setting fake location for GMS");
        try {
            LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, true);
            LocationServices.FusedLocationApi.setMockLocation(mGoogleApiClient, location);

            Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            Util.Log("rule", loc.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void removeGMSFakeLocation() {
        Log.v("RTCL", "removing fake location for GMS");
        handler.removeCallbacks(runnable);
        LocationServices.FusedLocationApi.setMockMode(mGoogleApiClient, false);
        isFakingLocation = false;
    }

    @Override
    public void onConnected(Bundle bundle) {
        //do things related to location access.
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        //get place id here and send the location to the service
        new PlaceGet().execute(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Util.LOCATION_POLLING_INTERVAL);
        mLocationRequest.setFastestInterval(Util.LOCATION_POLLING_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    class PlaceGet extends AsyncTask<Location, Void, Void> {
        PlaceGet() {
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //code executed when a new place is detected
        @Override
        protected Void doInBackground(Location... location) {

            LatLng currentLoc = new LatLng(location[0].getLatitude(), location[0].getLongitude());
            float accuracy = location[0].getAccuracy();
            if (accuracy > 100 || location[0].isFromMockProvider() || isFakingLocation) {
                //if (accuracy > 100 ) {
                if (Util.DEBUG) {
                    Log.v(Util.TAG, "inside condition!");
                }

                return null;
            }

            if (Util.DEBUG) {
                Log.v(Util.TAG, "real location !\t" + location[0]);
            }

            CachedPlace updatedCurrentPlace = locClust.getPlace(currentLoc, accuracy, mainService.getCurrentPlace());
            mainService.updatePlace(updatedCurrentPlace); //set the current place, also update time spent in the current place

            return null;
        }
    }


    //have this code run in an independent thread
    class FakeLocationSetter extends AsyncTask<LatLng, Void, Void> {
        FakeLocationSetter() {
        }

        @Override
        protected Void doInBackground(LatLng... fakeLocation) {
            handler.postDelayed(runnable, interval);
            //can we have the timers implemented here?
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

    }
}
