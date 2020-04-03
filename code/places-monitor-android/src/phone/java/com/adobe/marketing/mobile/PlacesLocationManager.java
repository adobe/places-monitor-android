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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;


/**
 * Class to manage location updates from Android OS
 */
class PlacesLocationManager {

	// Latitude/Longitude constants
	private static final double MAX_LAT		= 90d;
	private static final double MIN_LAT		= -90d;
	private static final double MAX_LON		= 180d;
	private static final double MIN_LON		= -180d;

	// permission constants
	private FusedLocationProviderClient fusedLocationClient;
	private PendingIntent locationPendingIntent;
	private boolean hasMonitoringStarted;
	private PlacesMonitorInternal placesMonitorInternal;
	private PlacesMonitorLocationPermission requestedLocationPermission;


	/**
	 * Constructor.
	 */
	PlacesLocationManager(PlacesMonitorInternal placesMonitorInternal) {
		this.placesMonitorInternal = placesMonitorInternal;
		loadPersistedData();
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
		if (requestedLocationPermission == PlacesMonitorLocationPermission.WHILE_USING_APP) {
			if (!PlacesActivity.isWhileInUsePermissionGranted()) {
				Log.debug(PlacesMonitorConstants.LOG_TAG, "Requesting while in use location permission");
				PlacesActivity.askPermission(requestedLocationPermission);
				return;
			}
		} else if (requestedLocationPermission == PlacesMonitorLocationPermission.ALWAYS_ALLOW) {
			if (!PlacesActivity.isBackgroundPermissionGranted()) {
				Log.debug(PlacesMonitorConstants.LOG_TAG, "Requesting allow always location permission");
				PlacesActivity.askPermission(requestedLocationPermission);
				return;
			}
		} else if(requestedLocationPermission == PlacesMonitorLocationPermission.NONE) {
			// if the location permission hasn't been already granted, do not begin location tracking.
			// log a message letting the developer know that the permission should be requested by the application.
			if(!(PlacesActivity.isWhileInUsePermissionGranted() || PlacesActivity.isBackgroundPermissionGranted())) {
				Log.warning(PlacesMonitorConstants.LOG_TAG, "Places Monitor doesn't have apps location permission to start monitoring. Please request for location permission " +
						"in your application or call startMonitoring by setting setLocationPermission API to WHILE_USING_APP or ALWAYS_ALLOW. For more details refer to %s",
						PlacesMonitorConstants.DocLinks.SET_LOCATION_PERMISSION);
				return;
			}
		}

		beginLocationTracking();
	}


	void beginLocationTracking() {

		Context context = App.getAppContext();

		if (context == null) {
			Log.debug(PlacesMonitorConstants.LOG_TAG,
					  "Unable to start monitoring places, App context is null");
			return;
		}

		Log.debug(PlacesMonitorConstants.LOG_TAG,
				  "Location permission is granted. Starting to monitor location updates");


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
								  "Failed to start location updates, status code : SETTINGS_CHANGE_UNAVAILABLE. " +
								  "Location settings can't be changed to meet the requirements, no dialog pops up");
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
					  "Location updates are stopped or never started. Please start monitoring to get the location update. For more details refer to %s",
					PlacesMonitorConstants.DocLinks.START_MONITOR);
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


	/**
	 * Handler for setting the location permission value to the location manager.
	 * <p>
	 *  This method saves the location permission value to persistence.
	 *  If the monitoring has already been started, calling this method will then attempt to prompt the upgraded permission level to the user.
	 *
	 * @param placesMonitorLocationPermission the location permission level
	 */
	void setLocationPermission(PlacesMonitorLocationPermission placesMonitorLocationPermission) {
		saveRequestedLocationPermission(placesMonitorLocationPermission);

		if (hasMonitoringStarted) {
			startMonitoring();
		}
	}

	// ========================================================================================
	// Internal Location Processor
	// ========================================================================================

	/**
	 * Handler for processing the received location event.
	 * <p>
	 * This method will be called when the OS event on location update is received.
	 * This method attempts to fetch and monitor 20 near by POIs around the given location.
	 *
	 * @param eventData {@link EventData} from the location update OS event.
	 * @see Places#getNearbyPointsOfInterest(Location, int, AdobeCallback)
	 */
	void onLocationReceived(final EventData eventData) {
		double latitude;
		double longitude;

		try {
			latitude = eventData.getDouble(PlacesMonitorConstants.EventDataKey.LATITUDE);
			longitude = eventData.getDouble(PlacesMonitorConstants.EventDataKey.LONGITUDE);
		} catch (VariantException exception) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						String.format("PlacesLocationManager : Exception occurred while extracting latitude/longitude from the OS event. Ignoring location update event. Error message - %s",
									  exception.getMessage()));
			return;
		}


		if (!isValidLat(latitude) || !isValidLon(longitude)) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationManager : Invalid Latitude: (" + latitude + ") or Longitude (" + longitude +
						") obtained from the OS event. Ignoring location update event.");
			return;
		}


		Location location = new Location("Places Monitor location");
		location.setLatitude(latitude);
		location.setLongitude(longitude);
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


	/**
	 * Persists the {@link #hasMonitoringStarted} in-memory variable to persistence
	 * <p>
	 * Saving of data will fail if the {@link SharedPreferences} or App's {@link Context} is null.
	 *
	 * @param hasMonitoringStarted value to be persisted
	 */
	void setHasMonitoringStarted(final boolean hasMonitoringStarted) {
		this.hasMonitoringStarted = hasMonitoringStarted;
		SharedPreferences sharedPreferences = PlacesMonitorUtil.getSharedPreferences();

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
	 * Persists the {@link #requestedLocationPermission} in-memory variable to persistence
	 * <p>
	 * Saving of data will fail if the {@link SharedPreferences} or App's {@link Context} is null.
	 *
	 * @param locationPermission value to be persisted
	 */
	void saveRequestedLocationPermission(final PlacesMonitorLocationPermission locationPermission) {
		this.requestedLocationPermission = locationPermission;
		SharedPreferences sharedPreferences = PlacesMonitorUtil.getSharedPreferences();

		if (sharedPreferences == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to save location permission value to persistence, sharedPreference is null");
			return;
		}

		SharedPreferences.Editor editor = sharedPreferences.edit();

		if (editor == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to save location permission value to persistence, shared preference editor is null");
			return;
		}

		editor.putString(PlacesMonitorConstants.SharedPreference.LOCATION_PERMISSION_KEY, locationPermission.getValue());
		editor.commit();
	}

	/**
	 * Loads the persisted data into the in-memory variables.
	 * <p>
	 * This method is called during the boot time of the SDK.
	 * Loading of persisted data fails if the {@link SharedPreferences} or App's {@link Context} is null.
	 */
	void loadPersistedData() {
		SharedPreferences sharedPreferences = PlacesMonitorUtil.getSharedPreferences();

		if (sharedPreferences == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to load hasMonitoringStarted from persistence, sharedPreference is null");
			return;
		}

		hasMonitoringStarted = sharedPreferences.getBoolean(PlacesMonitorConstants.SharedPreference.HAS_MONITORING_STARTED_KEY,
							   false);
		Log.trace(PlacesMonitorConstants.LOG_TAG,
				  "PlacesLocationManager has loaded " + hasMonitoringStarted +  " for hasMonitoringStarted from persistence");

		String locationPermissionString = sharedPreferences.getString(
											  PlacesMonitorConstants.SharedPreference.LOCATION_PERMISSION_KEY, "");
		this.requestedLocationPermission = PlacesMonitorLocationPermission.fromString(locationPermissionString);
	}


	/**
	 * Verifies if the provided latitude is valid.
	 *
	 * @param latitude the latitude
	 * @return true if latitude is in the range [-90,90]
	 */
	private boolean isValidLat(final double latitude) {
		return latitude >= MIN_LAT && latitude <= MAX_LAT;
	}


	/**
	 * Verifies if the provided longitude is valid.
	 *
	 * @param longitude the longitude
	 * @return true if longitude is in the range [-180,180]
	 */
	private boolean isValidLon(final double longitude) {
		return longitude >= MIN_LON && longitude <= MAX_LON;
	}



}
