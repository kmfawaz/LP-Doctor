/*
 * LP-Doctor Copyright 2015 Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See LICENSE for the specific language governing permissions and limitations under the License
 */

package edu.umich.eecs.rtcl.lp_doctor.utilities;

import android.util.SparseArray;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.math3.stat.inference.ChiSquareTest;

import java.util.Arrays;
import java.util.Random;

import edu.umich.eecs.rtcl.lp_doctor.Util;
import edu.umich.eecs.rtcl.lp_doctor.anonymization.LambertW;
import edu.umich.eecs.rtcl.lp_doctor.rule.RuleData;


public class MathTools {

    //privacy level dictates the value pof alpha: 1%, 5%, 10%: low  medium  high
    //perform the chi-square test
    public static boolean fitsMobModel(SparseArray<Double> mobility, SparseArray<Integer> appHistogram, int currentPlace, int privacyLevel) {
        double pValue = getPValue(mobility, appHistogram, currentPlace);
        return pValue >= getAlphaFromLevel(privacyLevel); //fits the mobility model --> null hypothesis not rejected --> there is potential threat
    }

    // get alpha from level
    private static double getAlphaFromLevel(int privacyLevel) {
        switch (privacyLevel) {
            case RuleData.ANON_LOW:
                return 0.1;
            case RuleData.ANON_MEDIUM:
                return 0.05;
            case RuleData.ANON_HIGH:
                return 0.01;
        }
        return 0.05;
    }

    // probably use kl divergence to see if thisd place leaks information
    // see what happens if place is released, we have some locations with 0-visited time --easy
    private static double getPValue(SparseArray<Double> mobility, SparseArray<Integer> appHistogram, int currentPlace) {

        double[] expected = new double[mobility.size()];
        long[] observed = new long[mobility.size()];
        long[] toBeObserved = new long[mobility.size()];

        //nothing there, for bootstrapping
        if (mobility.size() < 2) {
            return 1;
        }


        for (int index = 0; index < mobility.size(); index++) {
            int placeID = mobility.keyAt(index);
            double probability = mobility.get(placeID);
            int numVisits = appHistogram.get(placeID, 0);// no visits if place not in histogram
            expected[index] = probability;
            observed[index] = numVisits;
            toBeObserved[index] = numVisits;
            if (placeID == currentPlace) {
                toBeObserved[index]++; //to be observed?
            }
            Util.Log(Util.SESSION_TAG, "place:\t" + placeID + "\texp:\t" + probability + "\tobs:\t" + numVisits);
        }
        double pValueOld = new ChiSquareTest().chiSquareTest(expected, observed);
        double pValueNew = new ChiSquareTest().chiSquareTest(expected, toBeObserved); //automatic normalization

        Util.Log(Util.SESSION_TAG, pValueOld + "\t" + Arrays.toString(expected) + "\t" + Arrays.toString(observed));
        Util.Log(Util.SESSION_TAG, pValueNew + "\t" + Arrays.toString(expected) + "\t" + Arrays.toString(observed));
        return pValueNew;
    }

    //use the KL divergence = D_{\mathrm{KL}}(P\|Q) = \sum_i P(i) \, \ln\frac{P(i)}{Q(i)}.
    //P is the observation, Q is the mobility model

    //there is a minute problem with the first access, as distance will always decrease. --> fixed
    // has been tested
    public static boolean isDistanceIncreased(SparseArray<Double> mobility, SparseArray<Integer> appHistogram, int currentPlace) {

        double[] expected = new double[mobility.size()];
        long[] observed = new long[mobility.size()];
        long[] toBeObserved = new long[mobility.size()];

        //we need the total number of visits
        int totalVisits = 0;
        for (int index = 0; index < mobility.size(); index++) {
            int placeID = mobility.keyAt(index);
            double probability = mobility.get(placeID);
            int numVisits = appHistogram.get(placeID, 0);// no visits if place not in histogram
            expected[index] = probability;
            observed[index] = numVisits;
            toBeObserved[index] = numVisits;
            if (placeID == currentPlace) {
                toBeObserved[index]++; //to be observed?
            }
            totalVisits += numVisits; // num visits is per place Id in mobility pattern


        }

        if (totalVisits == 0) {
            //no location access recorded, information leak is inevitable from location leak
            return false;
        }
        double KLValueOld = 0;
        double KLValueNew = 0;
        //what happens if totalVisits is 0?
        //also, what happens id the obsPr is 0?

        for (int i = 0; i < expected.length; i++) {
            double expPr = expected[i]; //larger than 0 by definition
            double obsPr = totalVisits > 0 ? observed[i] * 1.0 / totalVisits : 0;
            double obsPrNew = toBeObserved[i] * 1.0 / (totalVisits + 1);

            KLValueOld += obsPr <= 1e-6 ? 0 : obsPr * Math.log(obsPr / expPr);
            KLValueNew += obsPrNew <= 1e-6 ? 0 : obsPrNew * Math.log(obsPrNew / expPr);

            Util.Log(Util.SESSION_TAG, "exp:\t" + expPr + "\tobs:\t" + obsPr + "temp:\t" + (obsPr <= 1e-6 ? 0 : obsPr * Math.log(obsPr / expPr)));
        }


        //Util.Log(Util.SESSION_TAG,KLValueOld+"\t"+expected+"\t"+observed);
        //Util.Log(Util.SESSION_TAG,KLValueNew+"\t"+expected+"\t"+toBeObserved);
        return KLValueNew > KLValueOld;
    }

    //both functions require adequate testing


    //one per location, shouldn't recompute for the same visited place
    public static LatLng getFakeLocation(LatLng currentLoc, int trackLevel) {
        double radius = getRadius(trackLevel);
        double privacyLevel = Math.log(4);
        Random rand = new Random();//need good source of randomness
        double theta = rand.nextDouble() * 360;
        double p = rand.nextDouble();
        double eps = privacyLevel / radius; //some thing around 1000 m

        double lam = LambertW.branchNeg1((p - 1) / Math.exp(1));
        double r = (-1 / eps) * (lam + 1);

        return Util.calculateDerivedPosition(currentLoc, r, theta);

    }

    private static double getRadius(int trackLevel) {
        /*
        switch (trackLevel) {
            case RuleData.ANON_LOW:
                return 100;
            case RuleData.ANON_MEDIUM:
                return 1000;
            case RuleData.ANON_HIGH:
                return 5000;
            default:
                return 1000;
        }*/
        return trackLevel * 300;
    }
}

