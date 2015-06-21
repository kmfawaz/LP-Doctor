/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.ui;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import edu.umich.eecs.rtcl.lp_doctor.App;
import edu.umich.eecs.rtcl.lp_doctor.R;
import edu.umich.eecs.rtcl.lp_doctor.Util;

public class AppPane extends RelativeLayout {


    String app;
    ImageView icon;
    Context context;
    TextView appLabel;
    TextView packLabel;
    View separator;

    public AppPane(Context context) {
        super(context);
        this.context = context;
    }

    public AppPane(Context context, String app) {
        super(context);
        this.context = context;
        this.app = app;
        icon = new ImageView(context);
        icon.setId(R.id.ICON_ID); //image is 1
        appLabel = new TextView(context);
        appLabel.setId(R.id.APPNAME_ID); //label is 2
        separator = new View(context);
        separator.setId(R.id.SEPARATOR_ID); //separator is 3
        packLabel = new TextView(context);
        packLabel.setId(R.id.PACKNAME_ID); //package name is 4

        addImage();
        addAppLabel();
        addPackLabel();
        addSeparator();

        setClickable(true);
        setFocusable(true);
        setBackground(getResources().getDrawable(android.R.drawable.list_selector_background));
    }

    private Drawable scaleImage(Drawable image) throws NameNotFoundException {

        if ((image == null) || !(image instanceof BitmapDrawable)) {
            return image;
        }


        BitmapDrawable iconTemp = (BitmapDrawable) context.getPackageManager().getApplicationIcon(App.PACKAGE_NAME);
        int height = iconTemp.getIntrinsicHeight();
        int width = iconTemp.getIntrinsicWidth();

        Bitmap b = ((BitmapDrawable) image).getBitmap();
        Bitmap bitmapResized = Bitmap.createScaledBitmap(b, width, height, false);
        return new BitmapDrawable(getResources(), bitmapResized);
    }

    private void addImage() {
        Drawable iconTemp;
        try {
            iconTemp = context.getPackageManager().getApplicationIcon(app);
            icon.setImageDrawable(scaleImage(iconTemp));
            LayoutParams vp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            vp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            vp.topMargin = 10;
            icon.setLayoutParams(vp);
            //icons[i].setVisibility(ImageView.VISIBLE);
            addView(icon, vp);

        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void addAppLabel() {

        String appName = Util.getAppName(app, context);
        appLabel.setText(appName);
        appLabel.setTextSize(20);
        appLabel.setTextColor(android.graphics.Color.WHITE);
        LayoutParams vp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        vp.addRule(RelativeLayout.CENTER_VERTICAL, R.id.ICON_ID);
        vp.addRule(RelativeLayout.RIGHT_OF, R.id.ICON_ID);
        vp.leftMargin = 60;
        appLabel.setLayoutParams(vp);
        addView(appLabel, vp);
    }

    private void addPackLabel() {

        //String appName = Util.getAppName(app, context);
        packLabel.setText(app);
        packLabel.setTextSize(10);
        packLabel.setTextColor(android.graphics.Color.WHITE);
        LayoutParams vp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        vp.addRule(RelativeLayout.BELOW, R.id.APPNAME_ID);
        vp.addRule(RelativeLayout.ALIGN_LEFT, R.id.APPNAME_ID);
        vp.topMargin = 25;
        packLabel.setLayoutParams(vp);
        addView(packLabel, vp);
    }

    private void addSeparator() {
        LayoutParams vp = new LayoutParams(LayoutParams.MATCH_PARENT, 2);
        vp.addRule(RelativeLayout.BELOW, R.id.PACKNAME_ID);
        vp.topMargin = 10;
        separator.setLayoutParams(vp);
        separator.setBackgroundColor(android.graphics.Color.LTGRAY);
        addView(separator, vp);
    }


}
