/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;

import java.util.Iterator;

import edu.umich.eecs.rtcl.lp_doctor.App;
import edu.umich.eecs.rtcl.lp_doctor.MonitoringService;
import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.placeIdentification.LocationContract;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public static final String INTENT_ACT_FILTER = "edu.umich.eecs.rtcl.phonelab.MainActivity";
    public final static String EXTRA_MESSAGE = "edu.umich.eecs.LPPM.MESSAGE";
    final String providerName = "my_provider";
    EditText latitude;
    EditText longitude;
    Button act;
    Button stop;
    Button test;
    Button flush;
    View.OnClickListener testListen = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            //Log.v("LP-Doctor", hasLocationService() + "");
            //send the intent to the service side
            Log.v(Util.TAG, "pressed the test button");

            startLocActivity();
        }
    };
    View.OnClickListener flushListen = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            //flush all rules from the rules DB
            App.getWritableDB().execSQL("DELETE FROM " + LocationContract.RuleEntry.TABLE_NAME);

        }
    };
    android.location.LocationListener myLocationListener = new android.location.LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            System.out.println("received fake location..." + location.getLatitude() + "\t" + location.getLongitude());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };
    int i = 0;
    View.OnClickListener actListen = new View.OnClickListener() {

        @Override
        public void onClick(View v) {

            i++;
            latitude.setText("latitude: " + i);
            longitude.setText("longitude: " + i);
            startService(new Intent(MainActivity.this, MonitoringService.class));


        }
    };
    View.OnClickListener stopListen = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            //stopService(new Intent(MainActivity.this, MyService.class));
            stopService(new Intent(MainActivity.this, MonitoringService.class));
        }
    };
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent locationIntent) {
            //System.out.println(locationIntent);
            // Extract data included in the Intent
            String action = locationIntent.getStringExtra("action");


            String data = locationIntent.getStringExtra("data");
            Log.v(Util.TAG, "finished execution with this decision:\t" + data);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        latitude = (EditText) findViewById(R.id.latitude);
        longitude = (EditText) findViewById(R.id.longitude);

        act = (Button) findViewById(R.id.act);
        act.setOnClickListener(actListen);

        stop = (Button) findViewById(R.id.stop_service);
        stop.setOnClickListener(stopListen);

        test = (Button) findViewById(R.id.test);
        test.setOnClickListener(testListen);

        flush = (Button) findViewById(R.id.flush);
        flush.setOnClickListener(flushListen);


        registerReceiver(mMessageReceiver, new IntentFilter(INTENT_ACT_FILTER));


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public boolean hasLocationService() {
        LocationManager localLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        localLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
        Iterator localIterator = localLocationManager.getAllProviders().iterator();
        while (localIterator.hasNext()) {
            String str = (String) localIterator.next();
            try {
                System.out.println(str);
                Location localLocation = localLocationManager.getLastKnownLocation(str);
                if (localLocation != null) {
                    return true;
                }
            } catch (SecurityException localSecurityException) {
            } catch (IllegalArgumentException localIllegalArgumentException) {
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        // LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        //MonitoringService.mGoogleApiClient = mGoogleApiClient;
    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println(i);
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        latitude.setText(lat + "");
        longitude.setText(lon + "");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "connection to gms failed", Toast.LENGTH_SHORT).show();
    }

    private void startLocActivity() {
        Intent intent = new Intent(this, AppListActivity.class);
        String message = "Get Location from GMS";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
