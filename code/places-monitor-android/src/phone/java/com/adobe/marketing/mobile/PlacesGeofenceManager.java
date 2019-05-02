/*
 Copyright 2019 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
*/

//
// PlacesGeofenceManager.java
//

package com.adobe.marketing.mobile;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class PlacesGeofenceManager {

    static private String MONITOR_SHARED_PREFERENCE_KEY = "com.adobe.placesMonitor";
    static private String MONITORING_FENCES_KEY = "monitoringFences";
    private final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private PendingIntent geofencePendingIntent;
    private Set<String> monitoringFences;
    private GeofencingClient geofencingClient;

    PlacesGeofenceManager() {
        monitoringFences = new HashSet<String>();
    }

    void startMonitoringFences(List<PlacesMonitorPOI> nearByPOIs) {
        if (nearByPOIs == null || nearByPOIs.isEmpty()) {
            Log.debug(PlacesMonitorConstants.LOG_TAG, "Places Extension responded with no regions around the current location to be monitored. Removing all the currently monitored geofence.");
            nearByPOIs = new ArrayList<PlacesMonitorPOI>();
        }

        GeofencingClient geofencingClient = getGeofencingClient();

        if (geofencingClient == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG,
                    "Unable to start monitoring geofences, geofencingClient instance is null");
            return;
        }

        addNearbyFences(nearByPOIs);
        removeNonNeabyFences(nearByPOIs);
    }

    void stopMonitoringFences() {

        GeofencingClient geofencingClient = getGeofencingClient();

        if (geofencingClient == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG,
                    "Unable to stop monitoring geofences, geofencingClient instance is null");
            return;
        }

        PendingIntent geofenceIntent = getGeofencePendingIntent();

        if (geofenceIntent == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG,
                    "Unable to stop monitoring geofences, Places Geofence Broadcast Receiver was never initialized");
            return;
        }


        Task<Void> task = geofencingClient.removeGeofences(geofenceIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                monitoringFences.clear();
                saveMonitoringFences();
                Log.debug(PlacesMonitorConstants.LOG_TAG, "Successfully stopped monitoring geofences");
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.debug(PlacesMonitorConstants.LOG_TAG, "Failed to stop monitoring geofences");
            }
        });
    }

    // ========================================================================================
    // Load/Save Monitored Fences to persistence
    // ========================================================================================

    void loadMonitoringFences() {
        SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to load monitoring geofences from persistence, sharedPreference is null");
            return;
        }

        monitoringFences = sharedPreferences.getStringSet(MONITORING_FENCES_KEY, new HashSet<String>());
    }

    void saveMonitoringFences() {
        SharedPreferences sharedPreferences = getSharedPreference();
        if (sharedPreferences == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to save monitoring geofences from persistence, sharedPreference is null");
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (editor == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to save monitoring geofences from persistence, shared preference editor is null");
            return;
        }

        editor.putStringSet(MONITORING_FENCES_KEY, monitoringFences);
        editor.commit();
    }

    // ========================================================================================
    // private methods
    // ========================================================================================

    private void addNearbyFences(final List<PlacesMonitorPOI> nearByPOIs) {
        // List of geofence to be added
        final List<Geofence> geofences = new ArrayList<>();

        if (!checkPermissions()) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to monitor geofences, App permission to use FINE_LOCATION is not granted.");
            return;
        }

        PendingIntent geofenceIntent = getGeofencePendingIntent();

        if (geofenceIntent == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG,
                    "Unable to stop monitoring geofences, Places Geofence Broadcast Receiver was never initialized");
            return;
        }


        for (PlacesMonitorPOI poi : nearByPOIs) {
            if (monitoringFences.contains(poi.getIdentifier())) {
                continue;
            }

            final Geofence fence = new Geofence.Builder()
                    .setRequestId(poi.getIdentifier())
                    .setCircularRegion(poi.getLatitude(), poi.getLongitude(), poi.getRadius())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            Log.debug(PlacesMonitorConstants.LOG_TAG, "Monitoring location with id " + poi.getIdentifier() +
                    " name " + poi.getName() +
                    " latitude " + poi.getLatitude() +
                    " longitude " + poi.getLongitude());
            geofences.add(fence);
        }

        if (geofences.isEmpty()) {
            Log.debug(PlacesMonitorConstants.LOG_TAG, "There are no new geofences that needs to be monitored");
            return;
        }



        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofences);

        try {
            Task<Void> task = geofencingClient.addGeofences(builder.build(), getGeofencePendingIntent());
            task.addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    for (Geofence eachgeofence : geofences) {
                        monitoringFences.add(eachgeofence.getRequestId());
                    }
                    saveMonitoringFences();
                    Log.debug(PlacesMonitorConstants.LOG_TAG, "Successfully added " + geofences.size() + " fences for monitoring");
                }
            });
            task.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.debug(PlacesMonitorConstants.LOG_TAG, "Error in adding fences for monitoring " + e.getMessage());

                }
            });
        } catch (SecurityException e) {
            Log.debug(PlacesMonitorConstants.LOG_TAG, "Add Geofence: SecurityException: " + e.getMessage());
        }
    }

    private void removeNonNeabyFences(final List<PlacesMonitorPOI> nearbyPOI) {
        // List of geofence be removed
        final List<String> toBeRemoved = new ArrayList<>();

        // convert list into a hashMap for convenience
        Map<String, PlacesMonitorPOI> poisMap = new HashMap<String, PlacesMonitorPOI>();
        for (PlacesMonitorPOI i : nearbyPOI) poisMap.put(i.getIdentifier(), i);

        for (String eachID : monitoringFences) {
            if (poisMap.containsKey(eachID)) {
                continue;
            }

            toBeRemoved.add(eachID);
        }

        if (toBeRemoved.isEmpty()) {
            Log.debug(PlacesMonitorConstants.LOG_TAG, "There are no geofences that needs to be removed");
            return;
        }

        Task<Void> task = geofencingClient.removeGeofences(toBeRemoved);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                for (String eachgeofence : toBeRemoved) {
                    monitoringFences.remove(eachgeofence);
                }
                saveMonitoringFences();
                Log.debug(PlacesMonitorConstants.LOG_TAG, "Successfully removed " + toBeRemoved.size() + " fences for monitoring");
            }
        });
        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                Log.debug(PlacesMonitorConstants.LOG_TAG, "Error in adding fences for monitoring " + e.getMessage());

            }
        });

    }


    // ========================================================================================
    // Getters for intent and geofencingClient
    // ========================================================================================

    /**
     * Returns a {@code PendingIntent} instance for getting the Geofence triggers
     * <p>
     * Returns the existing {@link #geofencePendingIntent} instance if its not null.
     * Else attempts to create a new Pending Intent
     * Returns null if the app context is not available.
     *
     * @return a {@code PendingIntent} instance
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Context context = App.getAppContext();

        if (context == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG,
                    "Unable to create an intent to receive location updates, App Context not available");
            return null;
        }

        Intent intent = new Intent(context, PlacesGeofenceBroadcastReceiver.class);
        intent.setAction(PlacesGeofenceBroadcastReceiver.ACTION_GEOFENCE_UPDATE);
        geofencePendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    /**
     * Returns the {@code GeofencingClient} instance
     * <p>
     * Returns the existing {@link #geofencingClient} instance if its not null.
     * Else attempts to create a new instance of {@link GeofencingClient}.
     * Returns null if the app context is not available
     *
     * @return a {@code GeofencingClient} instance
     */
    private GeofencingClient getGeofencingClient() {
        if (geofencingClient != null) {
            return geofencingClient;
        }

        Context context = App.getAppContext();

        if (context == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Places Geofence Services not initialized, App Context not available");
            return null;
        }

        geofencingClient = LocationServices.getGeofencingClient(context);
        return geofencingClient;
    }

    private SharedPreferences getSharedPreference() {
        Context appContext = App.getAppContext();

        if (appContext == null) {
            return null;
        }
        return appContext.getSharedPreferences(MONITOR_SHARED_PREFERENCE_KEY, 0);
    }


    private boolean checkPermissions() {
        Context context = App.getAppContext();

        if (context == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to check location permission, App context is not available");
            return false;
        }

        int permissionState = ActivityCompat.checkSelfPermission(context,
                FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }
}
