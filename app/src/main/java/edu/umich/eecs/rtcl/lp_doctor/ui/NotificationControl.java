/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.app.NotificationCompat;

import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.anonymization.LocationAnonymizer;

/**
 * Created by kmfawaz on 2/5/2015.
 */
public class NotificationControl {

    //rule of thumb: One notification at a time
    //issue notification

    //remove notification
    //each notification associated: ID, app, action, state
    //action has to enable if state is to disable
    //or disable if the state is to enable

    //constructor needs a reference to the anonymizer service and context

    Context context;
    LocationAnonymizer locAnon;
    int notID = 11111;

    public NotificationControl(Context context, LocationAnonymizer locAnon) {
        this.context = context;
        this.locAnon = locAnon;
    }

    public void issueNotification(String action, String app, int place) {

        String message = getMessageFromAction(action);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.

        Notification notif = buildNotification(message, action, app, place);

        mNotificationManager.notify(notID, notif);
    }

    private String getMessageFromAction(String action) {
        String message = "";
        if (action.equals(LocationAnonymizer.ACTION_ANON)) {
            message = "Do you want to anonymize location?";
        } else if (action.equals(LocationAnonymizer.ACTION_DEANON)) {
            message = "Do you want to allow location access";
        }
        return message;
    }

    public void cancelNotification() {
        Util.Log(Util.SESSION_TAG, "cancel notification");
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notID);
    }

    private PendingIntent generateReduceIntent(String packName, int place) {
        Intent cancelIntent = new Intent(LocationAnonymizer.INTENT_ACTION); //define the intent to receive the notif. action
        cancelIntent.putExtra("action", LocationAnonymizer.ACTION_ANON_REDUCE);
        cancelIntent.putExtra("appName", packName);
        cancelIntent.putExtra("placeID", place);
        PendingIntent cancelIntentPending = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return cancelIntentPending;
    }

    private Notification buildNotification(String message, String action, String packName, int place) {
        //change notification there?
        String appName = Util.getAppName(packName, context);

        Intent cancelIntent = new Intent(LocationAnonymizer.INTENT_ACTION); //define the intent to receive the notif. action
        cancelIntent.putExtra("action", action);
        cancelIntent.putExtra("appName", packName);
        cancelIntent.putExtra("placeID", place);
        PendingIntent cancelIntentPending = PendingIntent.getBroadcast(context, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Bitmap myLogo = null;
        try {

            Drawable iconTemp = context.getPackageManager().getApplicationIcon(packName);
            myLogo = ((BitmapDrawable) iconTemp).getBitmap();

        } catch (PackageManager.NameNotFoundException e) {
            if (Util.DEBUG) {
                e.printStackTrace();
            }

        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(myLogo)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(appName)
                        .setContentText(message);

        if (action.equals(LocationAnonymizer.ACTION_DEANON)) {
            mBuilder = mBuilder.addAction(R.drawable.ic_cancel, "Disable Anonymization", cancelIntentPending);
            mBuilder.addAction(R.drawable.ic_reduce, "Reduce Anonymization", generateReduceIntent(packName, place));
            mBuilder.setTicker("LP-Doctor is protecting you!");
            //also add action to reduce anon
        } else if (action.equals(LocationAnonymizer.ACTION_ANON)) {
            mBuilder = mBuilder.addAction(R.drawable.ic_enable, "Enable Anonymization", cancelIntentPending);
            mBuilder.setTicker("Do you want to enable protection????");
        }


        //and we need the below because ???? -- don't think it's needed
        // Creates an explicit intent for an Activity in your app
        /*
        Intent resultIntent = new Intent(this, GlobalSettingsActivity.class);
        resultIntent.putExtra("appName", packName);
        resultIntent.putExtra("placeID", place); //global rule



        // The stack builder object will contain an artificial back stack for the started Activity.
        // This ensures that navigating backward from the Activity leads out of your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MyMainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
    */
        return mBuilder.build();
    }

}
