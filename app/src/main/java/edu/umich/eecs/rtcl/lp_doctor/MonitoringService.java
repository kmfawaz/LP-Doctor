/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import edu.umich.eecs.rtcl.lp_doctor.anonymization.LocationAnonymizer;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.CachedPlace;

//import com.android.launcher3.R;

public class MonitoringService extends Service {


    public static final String INTENT_FILTER = "edu.umich.eecs.rtcl.phonelab.MonitoringService";
    public HistogramManager histHelper;
    LocationAcquisitionHelper locHelper;
    LocationAnonymizer locAnonHelper;
    long timeStart = 0;
    private CachedPlace currentPlace = new CachedPlace();
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent locationIntent) {
            //System.out.println(locationIntent);
            // Extract data included in the Intent
            String action = locationIntent.getStringExtra("action");

            Log.v(Util.TAG, action);
            if (action.equals("stopService")) {
                stopService();
            } else if (action.equals("LPPM")) {
                //main entry point!!!!!!! --> we have to start session
                //received from app launcher -- app execution is blocked
                //instruct app monitor that a new session is about to begin, and another about to finish
                timeStart = System.nanoTime();
                locAnonHelper.processAppLaunch(locationIntent, currentPlace.getPlaceID());
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("creating service");

        registerReceiver(mMessageReceiver, new IntentFilter(INTENT_FILTER));
        locHelper = new LocationAcquisitionHelper(this, this);
        histHelper = new HistogramManager(this);
        locAnonHelper = new LocationAnonymizer(this, this);
    }

    public void updatePlace(CachedPlace updatedCurrentPlace) {
        currentPlace.setCachedPlace(updatedCurrentPlace);
    }

    public CachedPlace getCurrentPlace() {
        return currentPlace;
    }

    // called after the service has been created
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        System.out.println("the intent is:   " + intent);
        showNotif();
        Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show();

        //start the other service from here?
        return Service.START_STICKY;
    }

    //implement a handler that runs every second and invokes an async task to perform a task.
    //int i=0;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {

        unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private void showNotif() {

        Intent intent = new Intent(INTENT_FILTER);
        intent.putExtra("action", "stopService");

        PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification note = new Notification.Builder(this)
                .setContentTitle("RTCLMon")
                .setSmallIcon(R.drawable.ic_launcher)
                .addAction(R.drawable.ic_stat_name, "Stop Service", contentIntent)
                .build();


        note.flags |= Notification.FLAG_NO_CLEAR; //to disallow user from clearing notification

        startForeground(1337, note);
    }

    private void stopService() {
        this.stopSelf();
    }

    public void instructApptoLaunch(String appLPPM, String result, String source, int decision) {
        //send the intent back to the
        Util.Log("lp_time", "time inside:\t" + (System.nanoTime() - timeStart));
        //myService.doSomethingPrivate();
        Util.Log("source", source);
        Intent intent = new Intent(source);
        //intent.setAction(MainActivity.INTENT_ACT_FILTER);
        intent.putExtra("data", result);
        sendBroadcast(intent);
    }

    public void startLocationAnon(LatLng fakeLocation) {
        locHelper.initiateAnonymization(fakeLocation);
        //Log.v(Util.TAG,"\t"+interval);
    }

    public void stopLocationAnon() {
        locHelper.removeGMSFakeLocation();
        //Log.v(Util.TAG,"\t"+interval);
    }


}
