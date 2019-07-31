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
// PlacesLocationManager.java
//

package com.adobe.marketing.mobile;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;


import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
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


/**
 * Class to manage location updates from Android OS
 */
class PlacesLocationManager {

	// permission constants
	private FusedLocationProviderClient fusedLocationClient;
	private PendingIntent locationPendingIntent;
	private boolean hasMonitoringStarted;
	private PlacesMonitorInternal placesMonitorInternal;

	/**
	 * Constructor.
	 */
	PlacesLocationManager(PlacesMonitorInternal placesMonitorInternal) {
		this.placesMonitorInternal = placesMonitorInternal;
		loadHasMonitoringStarted();
	}

	/**
	 *  Call this method to start getting location updated from Android OS.
	 *  <p>
	 *  If the permission to access fine location is granted, then request is made against {@link FusedLocationProviderClient}
	 *  to get location for every 2 kilometer of user movement or every 30 mins (whichever happens first).
	 *  If the permission to access fine location is not provided. A prompt is made to ask for monitoring fine location.
	 *
	 *  No action if the applications context is null.
	 *  No action is taken if permission to access the fine location is denied by user.
	 */
	void startMonitoring() {
		Context context = App.getAppContext();

		if (context == null) {
			Log.debug(PlacesMonitorConstants.LOG_TAG,
					  "Unable to start monitoring places, App context is null");
			return;
		}

		if (!PlacesActivity.isFineLocationPermissionGranted()) {
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Requesting permission to monitor fine location");
			PlacesActivity.askPermission();
			return;
		}

		Log.debug(PlacesMonitorConstants.LOG_TAG,
				  "Location permission is already granted. Starting to monitor location updates");

		// Begin by checking if the device has the necessary location settings.

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


				setHasMonitoringStarted(true);
				Log.debug(PlacesMonitorConstants.LOG_TAG, "All location settings are satisfied to monitor location");
				fusedLocationProviderClient.requestLocationUpdates(locationRequest,
						locationIntent);

			}
		});
		task.addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(Exception e) {
				int statusCode = ((ApiException) e).getStatusCode();
				setHasMonitoringStarted(false);

				switch (statusCode) {
					case LocationSettingsStatusCodes.RESOLUTION_REQUIRED: {
						Log.debug(PlacesMonitorConstants.LOG_TAG,
								  "Failed to start location updates, status code : RESOLUTION_REQUIRED.  Attempting to get permission.");

						// Location settings are not satisfied. But could be fixed by showing the
						// user a dialog.
						try {
							final Activity currentActivity = App.getCurrentActivity();

							if (currentActivity == null) {
								break;
							}

							final ResolvableApiException resolvable = (ResolvableApiException) e;
							resolvable.startResolutionForResult(currentActivity, LocationSettingsStatusCodes.RESOLUTION_REQUIRED);
						} catch (IntentSender.SendIntentException ex) {
							// Ignore the error.
						} catch (ClassCastException ex) {
							// Ignore, should be an impossible error.
						}

						break;
					}

					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
						Log.error(PlacesMonitorConstants.LOG_TAG,
								  "Failed to start location updates, status code : SETTINGS_CHANGE_UNAVAILABLE");
						break;
					}

					default: {
						break;
					}
				}

			}
		});
	}

	/**
	 *  Call this method to stop getting any further location updates from Android OS.
	 */
	void stopMonitoring() {
		stopLocationUpdates();
	}


	/**
	 *  Requests a immediate location update.
	 *  <p>
	 *  Once the location updates are received, {@link Places} extension is called to grab nearbyPOIs around the obtained location.
	 *  No action is taken if the {@link FusedLocationProviderClient} instance is null.
	 */
	void updateLocation() {
		if (!hasMonitoringStarted) {
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

	/**
	 * Handler for processing the received location event.
	 *
	 * <p>
	 * This method is called by the internal {@link android.content.BroadcastReceiver} on receiving an intent with {@link Location}.
	 * Calls the {@link PlacesExtension} to get the closest POIs around the given location.
	 *
	 * No action is performed if the intents action is not same as {@link PlacesMonitorConstants#INTERNAL_INTENT_ACTION_LOCATION}.
	 * No action is performed if the received {@code LocationResult} is null.
	 * No action is performed if the location array or the location is null.
	 *
	 * @param intent broadcasted geofence event message wrapped in an intent
	 * @see Places#getNearbyPointsOfInterest(Location, int, AdobeCallback)
	 */
	void onLocationReceived(final Intent intent) {

		if (intent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Cannot process the location update, The received intent from the location broadcast receiver  is null");
			return;
		}

		final String action = intent.getAction();

		if (!PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION.equals(action)) {
			Log.trace(PlacesMonitorConstants.LOG_TAG,
					  "Cannot process the location update, Invalid action type received from location broadcast receiver");
			return;
		}

		LocationResult result = LocationResult.extractResult(intent);

		if (result == null) {
			return;
		}

		List<Location> locations = result.getLocations();

		if (locations == null || locations.isEmpty()) {
			Log.trace(PlacesMonitorConstants.LOG_TAG, "Cannot process the location update, Received location array is null");
			return;
		}

		Location location = locations.get(0);

		if (location == null) {
			Log.trace(PlacesMonitorConstants.LOG_TAG, "Cannot process the location update, Received location is null");
			return;
		}

		String locationLog = "Location Received: Accuracy: " + location.getAccuracy() + " lat: " + location.getLatitude() +
							 " lon: " +
							 location.getLongitude();
		Log.debug(PlacesMonitorConstants.LOG_TAG, locationLog);
		placesMonitorInternal.getPOIsForLocation(location);
	}

	/**
	 * Call to stop getting location updates from Android OS.
	 * <p>
	 * On successful execution, will stop getting any further location updates from the {@link FusedLocationProviderClient}.
	 *
	 * No action is performed if the FusedLocationProviderClient instance is null.
	 * No action is performed if the PendingIntent for getting location update is null.
	 *
	 * @see FusedLocationProviderClient
	 */
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
				setHasMonitoringStarted(false);
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
	 * Returns existing {@link #locationPendingIntent} instance if its not null.
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
	 * Returns {@code FusedLocationProviderClient} instance
	 * <p>
	 * Returns existing {@link #fusedLocationClient} instance if it's not null.
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
	 * Returns {@code LocationRequest} instance with distance and time frequency of the
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


	void setHasMonitoringStarted(final boolean hasMonitoringStarted) {
		this.hasMonitoringStarted = hasMonitoringStarted;
		SharedPreferences sharedPreferences = getSharedPreference();

		if (sharedPreferences == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to save monitoring geofences from persistence, sharedPreference is null");
			return;
		}

		SharedPreferences.Editor editor = sharedPreferences.edit();

		if (editor == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to save monitoring geofences from persistence, shared preference editor is null");
			return;
		}

		editor.putBoolean(PlacesMonitorConstants.SharedPreference.HAS_MONITORING_STARTED_KEY, hasMonitoringStarted);
		editor.commit();
	}


	/**
	 * Loads previously persisted data for {@link #hasMonitoringStarted} into memory.
	 * <p>
	 * This method is called during the boot time of SDK.
	 * Loading of persisted data fails if the {@link SharedPreferences} or App's {@link Context} is null.
	 */
	void loadHasMonitoringStarted() {
		SharedPreferences sharedPreferences = getSharedPreference();

		if (sharedPreferences == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to load hasMonitoringStarted from persistence, sharedPreference is null");
			return;
		}

		hasMonitoringStarted = sharedPreferences.getBoolean(PlacesMonitorConstants.SharedPreference.HAS_MONITORING_STARTED_KEY,
							   false);
		Log.trace(PlacesMonitorConstants.LOG_TAG,
				  "PlacesLocationManager has loaded " + hasMonitoringStarted +  " for hasMonitoringStarted from persistence");
	}

	/**
	 * Getter for applications {@link SharedPreferences}
	 * <p>
	 * Returns null if app context is not available
	 *
	 * @return a {@code SharedPreferences} instance
	 */
	private SharedPreferences getSharedPreference() {
		Context appContext = App.getAppContext();

		if (appContext == null) {
			return null;
		}

		return appContext.getSharedPreferences(PlacesMonitorConstants.SharedPreference.MASTER_KEY, 0);
	}

}
