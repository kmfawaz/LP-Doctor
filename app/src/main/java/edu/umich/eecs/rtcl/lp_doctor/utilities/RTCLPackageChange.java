/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.utilities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.ui.AppInstalledActivity;

/**
 * Created by kmfawaz on 1/21/2015.
 */
public class RTCLPackageChange extends BroadcastReceiver {

    //start all services here
    @Override
    public void onReceive(Context context, Intent intent) {


        //includes updated app?
        if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction())) {
            //update util
            //get package name and update
            Uri uri = intent.getData();
            String app = uri != null ? uri.getSchemeSpecificPart() : null;
            Util.removeAppFromPermMap(app);


            //only apps that require fine permissions and are not system apps
            if (Util.doesAppHasFinePerms(context, app) && !Util.isSystemApp(context, app)) {
                loadInstalledActivity(context, app);
            }

        } else if ("android.intent.action.PACKAGE_REPLACED".equals(intent.getAction())) {
            //update util
            //get package name and update
            Uri uri = intent.getData();
            String app = uri != null ? uri.getSchemeSpecificPart() : null;
            Util.removeAppFromPermMap(app);
        }
        //prompt the user to put in some settings, and back to the main menu, and close play store
    }

    private void loadInstalledActivity(Context context, String appInstalled) {
        //app must have fine location permission
        Intent intent = new Intent(context, AppInstalledActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("app", appInstalled);
        //intent.putExtra("placeID", -1); //global rule -- don't think it is needed
        //intent.putExtra("option", SecondarySettingsActivity.DISPLAY_PLACE_POLICIES); -- not yet
        context.startActivity(intent);
    }

}
