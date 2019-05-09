/* **************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2019 Adobe Inc.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Inc. and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Inc..
 **************************************************************************/

package com.adobe.marketing.mobile;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;


import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.List;


class PlacesLocationManager {


	private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
	// permission constants
	private final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
	private FusedLocationProviderClient fusedLocationClient;
	private Boolean isRequestingLocationUpdates = false;
	private PendingIntent locationPendingIntent;
	private PlacesMonitorInternal placesMonitorInternal;

	PlacesLocationManager(PlacesMonitorInternal placesMonitorInternal) {
		this.placesMonitorInternal = placesMonitorInternal;
	}

	void startMonitoring() {
		if (!checkPermissions()) {
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Requesting permission to monitor fine location");
			requestPermissions();
			return;
		}

		Log.debug(PlacesMonitorConstants.LOG_TAG,
				  "Location permission is already granted. Starting to monitor location updates");

		// Begin by checking if the device has the necessary location settings.
		Context context = App.getAppContext();
		final LocationRequest locationRequest = getLocationRequest();
		LocationSettingsRequest settingsRequest = new LocationSettingsRequest.Builder()
		.addLocationRequest(locationRequest).build();
		SettingsClient settingsClient = LocationServices.getSettingsClient(context);
		Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(settingsRequest);
		task.addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
			@Override
			public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

				FusedLocationProviderClient fusedLocationProviderClient = getFusedLocationClient();

				if (fusedLocationProviderClient == null) {
					Log.warning(PlacesMonitorConstants.LOG_TAG,
								"Unable to start monitoring location, fusedLocationProviderClient instance is null");
					return;
				}

				PendingIntent locationIntent = getPendingIntent();

				if (locationIntent == null) {
					Log.warning(PlacesMonitorConstants.LOG_TAG,
								"Unable to start monitoring location, Places Location Broadcast Receiver cannot be initialized");
					return;
				}


				isRequestingLocationUpdates = true;
				Log.debug(PlacesMonitorConstants.LOG_TAG, "All location settings are satisfied to monitor location");
				fusedLocationProviderClient.requestLocationUpdates(locationRequest,
						locationIntent);

			}
		});
		task.addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(Exception e) {
				int statusCode = ((ApiException) e).getStatusCode();
				isRequestingLocationUpdates = false;

				switch (statusCode) {
					case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
						Log.debug(PlacesMonitorConstants.LOG_TAG, "Failed to start location updates, status code : RESOLUTION_REQUIRED");
						break;

					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
						Log.error(PlacesMonitorConstants.LOG_TAG,
								  "Failed to start location updates, status code : SETTINGS_CHANGE_UNAVAILABLE");
				}

			}
		});
	}


	void stopMonitoring() {
		stopLocationUpdates();
	}


	void updateLocation() {
		if (!isRequestingLocationUpdates) {
			Log.debug(PlacesMonitorConstants.LOG_TAG,
					  "Location updates are stopped or never started. Please start monitoring to get the location update");
			return;
		}

		FusedLocationProviderClient fusedLocationProviderClient = getFusedLocationClient();

		if (fusedLocationProviderClient == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to update location, fusedLocationProviderClient instance is null");
			return;
		}


		Task<Location> task = fusedLocationProviderClient.getLastLocation();
		task.addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(Exception e) {
				Log.debug(PlacesMonitorConstants.LOG_TAG, "Failed to get location" + e.getLocalizedMessage());
			}
		});
		task.addOnSuccessListener(new OnSuccessListener<Location>() {
			@Override
			public void onSuccess(Location location) {
				placesMonitorInternal.getPOIsForLocation(location);
			}
		});
	}

	// ========================================================================================
	// Internal Location Processor
	// ========================================================================================

	void onLocationReceived(final Intent intent) {

		if (intent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
					  "Cannot process the location update, The received intent from the location broadcast receiver  is null");
			return;
		}

		final String action = intent.getAction();

		if (!PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION.equals(action)) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
					  "Cannot process the location update, Invalid action type received from location broadcast receiver");
			return;
		}

		LocationResult result = LocationResult.extractResult(intent);

		if (result == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Cannot process the location update, Received location result is null");
			return;
		}

		List<Location> locations = result.getLocations();

		if (locations == null || locations.isEmpty()) {
			Log.error(PlacesMonitorConstants.LOG_TAG, "Cannot process the location update, Received location result is null");
			return;
		}

		Location location = locations.get(0);

		if (location == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Cannot process the location update, Received location is null");
			return;
		}

		String locationLog = "Location Received: Accuracy: " + location.getAccuracy() + " lat: " + location.getLatitude() +
							 " lon: " +
							 location.getLongitude();
		Log.debug(PlacesMonitorConstants.LOG_TAG, locationLog);
		placesMonitorInternal.getPOIsForLocation(location);
	}


	/**
	 * Checks the current location permission state of the device.
	 * <p>
	 * Returns true if the permission for using fine location is granted.
	 * Returns false if the permission for using fine location is not granted or if the app context is null.
	 *
	 * @return Returns {@code boolean} representing the permission to monitor fine location
	 */
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


	/**
	 * Request the permission to monitor fine location.
	 */

	private void requestPermissions() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return;
		}

		Context context = App.getAppContext();

		if (context == null) {
			return;
		}

		Activity activity = App.getCurrentActivity();

		if (activity == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to request permission, current activity is null");
			return;
		}

		boolean shouldProvideRationale =
			ActivityCompat.shouldShowRequestPermissionRationale(activity,
					FINE_LOCATION);

		// Show the pop-up to the user. This would happen if the user denied the
		// request previously, but didn't check the "Don't ask again" checkbox.
		if (shouldProvideRationale) {
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Permission not granted to provide location");
		} else {
			// Request permission. It's possible this can be auto answered if device policy
			// sets the permission in a given state or the user denied the permission
			// previously and checked "Never ask again".
			ActivityCompat.requestPermissions(activity,
											  new String[] {FINE_LOCATION},
											  REQUEST_PERMISSIONS_REQUEST_CODE);
		}
	}

	private void stopLocationUpdates() {

		FusedLocationProviderClient fusedLocationProviderClient = getFusedLocationClient();

		if (fusedLocationProviderClient == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to stop monitoring location, fusedLocationProviderClient instance is null");
			return;
		}

		PendingIntent locationPendingIntent = getPendingIntent();

		if (locationPendingIntent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationManager : Unable to stop monitoring location, locationPendingIntent is null");
			return;
		}


		Task<Void> task = fusedLocationClient.removeLocationUpdates(getPendingIntent());
		task.addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> task) {
				isRequestingLocationUpdates = false;
				Log.debug(PlacesMonitorConstants.LOG_TAG, "Places Monitor has successfully stopped further location updates");
			}
		});
	}


	// ========================================================================================
	// Getters for intent, fusedLocationClient and locationRequest
	// ========================================================================================

	/**
	 * Returns a {@code PendingIntent} instance for getting the location updates
	 * <p>
	 * Returns the existing {@link #locationPendingIntent} instance if its not null.
	 * Else attempts to create a new instance of {@link FusedLocationProviderClient}.
	 * Returns null if the app context is not available.
	 *
	 * @return a {@code FusedLocationProviderClient} instance
	 */
	private PendingIntent getPendingIntent() {
		// Reuse the PendingIntent if we already have it.
		if (locationPendingIntent != null) {
			return locationPendingIntent;
		}

		Context context = App.getAppContext();

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationManager : Unable to create an intent to receive location updates, App Context not available");
			return null;
		}

		Intent intent = new Intent(context, PlacesLocationBroadcastReceiver.class);
		intent.setAction(PlacesLocationBroadcastReceiver.ACTION_LOCATION_UPDATE);
		locationPendingIntent = PendingIntent.getBroadcast(App.getAppContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		return locationPendingIntent;
	}

	/**
	 * Returns the {@code FusedLocationProviderClient} instance
	 * <p>
	 * Returns the existing {@link #fusedLocationClient} instance if it's not null.
	 * Else attempts to create a new instance of {@link FusedLocationProviderClient}.
	 * Returns null if the app context is not available or the google's getFusedLocationProviderClient API return null.
	 *
	 * @return a {@code FusedLocationProviderClient} instance
	 */
	private FusedLocationProviderClient getFusedLocationClient() {
		if (fusedLocationClient != null) {
			return fusedLocationClient;
		}

		Context context = App.getAppContext();

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Places location Services not initialized, App Context not available");
			return null;
		}

		fusedLocationClient = LocationServices.getFusedLocationProviderClient(App.getAppContext());
		return fusedLocationClient;
	}

	/**
	 * Return the {@code LocationRequest} instance with indicating the distance and time frequency of the
	 * location request.
	 *
	 * @return A valid {@link LocationRequest} instance
	 */
	private LocationRequest getLocationRequest() {
		LocationRequest locationRequest = LocationRequest.create();
		locationRequest.setInterval(PlacesMonitorConstants.Location.REQUEST_INTERVAL);
		locationRequest.setFastestInterval(PlacesMonitorConstants.Location.REQUEST_FASTEST_INTERVAL);
		locationRequest.setSmallestDisplacement(PlacesMonitorConstants.Location.REQUEST_SMALLEST_DISPLACEMENT);
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		return locationRequest;
	}

}
