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

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
//import com.google.android.gms.location.LocationCallback;


public class Geolocation extends CordovaPlugin {

    String TAG = "GeolocationPlugin";
    CallbackContext context;


    String[] highAccuracyPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
    String[] lowAccuracyPermissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
    String[] permissionsToRequest;
    String[] permissionsToCheck;


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
        } else if (action.equals("watchPosition")) {
            LOG.d(TAG, "Executing watchPosition");
//            FusedLocationProviderClient fusedLocationClient;
//            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            try {
                Log.i(TAG, "execute: 1");
                LocationManager lm = (LocationManager) this.webView.getContext().getSystemService(Context.LOCATION_SERVICE);
                Log.i(TAG, "execute: 2");
                permissionsToCheck = highAccuracyPermissions;
                Log.i(TAG, "execute: 3");
                permissionsToRequest = Build.VERSION.SDK_INT <= 31 ? highAccuracyPermissions : permissionsToCheck;
                if (hasPermisssion(permissionsToCheck)) {
                    if (ActivityCompat.checkSelfPermission(this.webView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.webView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
//                        return false;

                    }
                }
                Log.i(TAG, "execute: 4");
//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    // TODO: Consider calling
//                    //    ActivityCompat#requestPermissions
//                    // here to request the missing permissions, and then overriding
//                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                    //                                          int[] grantResults)
//                    // to handle the case where the user grants the permission. See the documentation
//                    // for ActivityCompat#requestPermissions for more details.
////                    return TODO;
//                }

//                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                JSONObject lastLocationJsonObject = locationtoposition(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                callbackContext.success(lastLocationJsonObject.toString());
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                       JSONObject positionJsonObject = locationtoposition(location);
                        Log.i(TAG, "onLocationChanged: location.tostring " + positionJsonObject);
                        callbackContext.success(positionJsonObject.toString());
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        Log.i(TAG, "onProviderEnabled: provider " + provider);
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        Log.i(TAG, "onProviderDisabled: provider " + provider);
                    }
                });
//                }
                PluginResult r = new PluginResult(PluginResult.Status.OK, Build.VERSION.SDK_INT);
                r.setKeepCallback(true);
                context.sendPluginResult(r);
                return true;
            } catch (Exception e) {
                Log.i(TAG, "execute: error");
                e.printStackTrace();
                callbackContext.error(e.getMessage());
                return false;
            }
//            LocationCallback locationCallback = new LocationCallback() {
//                @Override
//                public void onLocationResult(LocationResult locationResult) {
//                    if (locationResult == null) {
//                        return;
//                    }
//                    for (Location location : locationResult.getLocations()) {
//                        // Update UI with location data
//                        // ...
//                    }
//                }
//            };


        }

        return false;
    }
    JSONObject locationtoposition (Location location) {
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
