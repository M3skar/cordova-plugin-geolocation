/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at
         http://www.apache.org/licenses/LICENSE-2.0
       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */


package org.apache.cordova.geolocation;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.Consumer;
//import com.google.android.gms.location.LocationCallback;


public class Geolocation extends CordovaPlugin {

    String TAG = "GeolocationPlugin";
    CallbackContext context;


    String[] highAccuracyPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    String[] lowAccuracyPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
    String[] permissionsToRequest;
    String[] permissionsToCheck;
    static LocationListener gLocationListener = null;
    static CallbackContext lastContext = null;
//    static Handler locationUpdateHandler = null;

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        LOG.d(TAG, "We are entering execute");
        LOG.d(TAG, action);
        context = callbackContext;
        if (action.equals("getPermission")) {

            boolean highAccuracy = args.getBoolean(0);
            permissionsToCheck = highAccuracy ? highAccuracyPermissions : lowAccuracyPermissions;

            // Always request both FINE & COARSE permissions on API <= 31 due to bug in WebView that manifests on these versions
            // See https://bugs.chromium.org/p/chromium/issues/detail?id=1269362
            permissionsToRequest = Build.VERSION.SDK_INT <= 31 ? highAccuracyPermissions : permissionsToCheck;

            if (hasPermisssion(permissionsToCheck)) {
                PluginResult r = new PluginResult(PluginResult.Status.OK, Build.VERSION.SDK_INT);
                context.sendPluginResult(r);
                return true;
            } else {
                PermissionHelper.requestPermissions(this, 0, permissionsToRequest);
            }
            return true;
        } else if (action.equals("getCurrentPosition")) {

            LocationManager lm = (LocationManager) this.webView.getContext().getSystemService(Context.LOCATION_SERVICE);
            Location lastLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            JSONObject lastLocationJsonObject = locationToJson(lastLocation);
            Log.i(TAG, "execute: lastlocation " + lastLocation.getAccuracy() + " _ " + lastLocation.toString());
            callbackContext.success(lastLocationJsonObject.toString());
            PluginResult r = new PluginResult(PluginResult.Status.ERROR, Build.VERSION.SDK_INT);
            context.sendPluginResult(r);
            return true;

        } else if (action.equals("watchPosition")) {
            LOG.d(TAG, "Executing watchPosition");
            try {

                LocationManager lm = (LocationManager) this.webView.getContext().getSystemService(Context.LOCATION_SERVICE);
                lastContext = callbackContext;

                if (gLocationListener != null) {
                    lm.removeUpdates(gLocationListener);
                }

                gLocationListener = new LocationListener() {

                    @Override
                    public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
                        Log.i(TAG, "onStatusChanged: provider " + provider + " status " + status);
                    }

                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        JSONObject positionJsonObject = locationToJson(location);
                        Log.i(TAG, "onLocationChanged: location " + location.toString());

                        PluginResult r = new PluginResult(PluginResult.Status.OK, positionJsonObject.toString());
                        r.setKeepCallback(true);
                        context.sendPluginResult(r);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        Log.i(TAG, "onProviderEnabled: provider " + provider);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        Log.i(TAG, "onProviderDisabled: provider " + provider);
                    }
                };

                lm.requestLocationUpdates("fused", 1000, 0F, gLocationListener);

                Location lastLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Log.i(TAG, "watch position, last location, " + lastLocation.toString());
                JSONObject lastLocationJsonObject = locationToJson(lastLocation);

                PluginResult r = new PluginResult(PluginResult.Status.OK, lastLocationJsonObject.toString());
                r.setKeepCallback(true);
                context.sendPluginResult(r);
                return true;
            } catch (Exception e) {
                Log.i(TAG, "execute: error");
                e.printStackTrace();
                callbackContext.error(e.getMessage());
                return false;
            }

        }

        return false;
    }

    JSONObject locationToJson(Location location) {
        JSONObject coordsJsonObject = new JSONObject();
        try {
            coordsJsonObject.put("latitude", location.getLatitude());
            coordsJsonObject.put("longitude", location.getLongitude());
            coordsJsonObject.put("altitude", location.getAltitude());
            coordsJsonObject.put("accuracy", location.getAccuracy());
            coordsJsonObject.put("velocity", location.getSpeed());
            coordsJsonObject.put("heading", location.getBearing());
            coordsJsonObject.put("altitudeAccuracy", location.getAccuracy());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject positionJsonObject = new JSONObject();
        try {
            positionJsonObject.put("coords", coordsJsonObject);
            positionJsonObject.put("timestamp", location.getTime());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return positionJsonObject;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;
        //This is important if we're using Cordova without using Cordova, but we have the geolocation plugin installed
        if (context != null) {
            for (int i = 0; i < grantResults.length; i++) {
                int r = grantResults[i];
                String p = permissions[i];
                if (r == PackageManager.PERMISSION_DENIED && arrayContains(permissionsToCheck, p)) {
                    LOG.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    context.sendPluginResult(result);
                    return;
                }

            }
            result = new PluginResult(PluginResult.Status.OK);
            context.sendPluginResult(result);
        }
    }

    public boolean hasPermisssion(String[] permissions) {
        for (String p : permissions) {
            if (!PermissionHelper.hasPermission(this, p)) {
                return false;
            }
        }
        return true;
    }

    /*
     * We override this so that we can access the permissions variable, which no longer exists in
     * the parent class, since we can't initialize it reliably in the constructor!
     */

    public void requestPermissions(int requestCode) {
        PermissionHelper.requestPermissions(this, requestCode, permissionsToRequest);
    }

    //https://stackoverflow.com/a/12635769/777265
    private <T> boolean arrayContains(final T[] array, final T v) {
        if (v == null) {
            for (final T e : array)
                if (e == null)
                    return true;
        } else {
            for (final T e : array)
                if (e == v || v.equals(e))
                    return true;
        }

        return false;
    }

}
