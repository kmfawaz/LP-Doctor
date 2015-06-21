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
import android.util.Log;
import android.widget.Toast;

import edu.umich.eecs.rtcl.lp_doctor.MonitoringService;
import edu.umich.eecs.rtcl.lp_doctor.Util;


//signals that the device completed boot, so we can start the service
//actually we have to start all services !!!!!!!!!!!!!!!!!!!!!!
public class RTCLStartupIntentReceiver extends BroadcastReceiver {

    //start all services here
    @Override
    public void onReceive(Context context, Intent intent) {

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            context.startService(new Intent(context, MonitoringService.class));
            Log.v("RTCL", "booting the app launcher");
            if (Util.DEBUG) {
                Toast.makeText(context, "Received the boot event", Toast.LENGTH_LONG).show();
            }

        } else if ("android.intent.action.PACKAGE_ADDED".equals(intent.getAction())) {
            //update util
            //get package name and update
            Uri uri = intent.getData();
            String app = uri != null ? uri.getSchemeSpecificPart() : null;
            Util.removeAppFromPermMap(app);

        } else if ("android.intent.action.PACKAGE_REPLACED".equals(intent.getAction())) {
            //update util
            //get package name and update
            Uri uri = intent.getData();
            String app = uri != null ? uri.getSchemeSpecificPart() : null;
            Util.removeAppFromPermMap(app);
        }
    }

}
