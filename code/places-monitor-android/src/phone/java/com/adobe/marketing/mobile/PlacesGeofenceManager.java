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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Class to manage and monitor geofences around the given device's current location
 */
class PlacesGeofenceManager {

	private final double INCONSEQUENTIAL_LATITUDE = 0.0;
	private final double INCONSEQUENTIAL_LONGITUDE =  0.0;
	private final float INCONSEQUENTIAL_RADIUS = 100.0f;
	private final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
	private PendingIntent geofencePendingIntent;
	private Set<String> userWithinGeofences;
	private GeofencingClient geofencingClient;

	PlacesGeofenceManager() {
		userWithinGeofences = new HashSet<String>();
	}

	/**
	 * Starts monitoring the entry/exit events around the given nearByPOIs by registering with the Geofences with the Android OS.
	 * <p>
	 * This method is called by {@link PlacesMonitorInternal} when new set of POIs are available for monitoring.
	 * No action will be performed if the {@link GeofencingClient} required for the monitoring the POIs is null.
	 *
	 * @param nearByPOIs 	A {@link List} of n nearBy {@link PlacesPOI} objects
	 * @see #getGeofencingClient()
	 */
	void startMonitoringFences(List<PlacesPOI> nearByPOIs) {
		if (nearByPOIs == null || nearByPOIs.isEmpty()) {
			Log.debug(PlacesMonitorConstants.LOG_TAG,
					  "Places Extension responded with no regions around the current location to be monitored. Removing all the currently monitored geofence.");
			nearByPOIs = new ArrayList<PlacesPOI>();
		}

		GeofencingClient geofencingClient = getGeofencingClient();

		if (geofencingClient == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to start monitoring geofences, geofencingClient instance is null");
			return;
		}


		refreshNearByPOIS(nearByPOIs);

		// identify the newly entered regions and dispatch an entry event
		List <PlacesPOI> newlyEnteredPois = findNewlyEnteredPOIs(nearByPOIs);

		for (PlacesPOI poi : newlyEnteredPois) {
			Geofence geofence = new Geofence.Builder()
			.setRequestId(poi.getIdentifier())
			.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
			.setCircularRegion(poi.getLatitude(), poi.getLongitude(), poi.getRadius())
			.setExpirationDuration(Geofence.NEVER_EXPIRE)
			.build();
			Places.processGeofence(geofence, Geofence.GEOFENCE_TRANSITION_ENTER);
		}
	}

	/**
	 * Compares the new set of nearByPOIs with the existing {@link #userWithinGeofences} and creates a list of
	 * {@link PlacesPOI} whose entry has not been already recorded.
	 *
	 * <p>
	 * This method,
	 * <ul>
	 *     <li> Remove's the pois from {@code #userWithinGeofences} which are not a part of nearbypois </li>
	 *     <li> Check for the newEntryPOI comparing the inmemory {@code #userWithinGeofences} list </li>
	 * </ul>
	 *
	 * @param nearbyPOIs a brand new {@link List} of nearByPOIs
	 * @return A {@code List} of newly entered POI
	 */
	List <PlacesPOI> findNewlyEnteredPOIs(List<PlacesPOI> nearbyPOIs) {
		// First, remove the userWithinGeofence poi that are not currently nearbypois

		// convert list into a hashMap for convenience
		Map<String, PlacesPOI> poisMap = new HashMap<String, PlacesPOI>();

		for (PlacesPOI i : nearbyPOIs) {
			poisMap.put(i.getIdentifier(), i);
		}

		// using iterator to remove the pois from userWithinGeofences which are not a part of nearbypois
		for (Iterator<String> iterator = userWithinGeofences.iterator(); iterator.hasNext();) {
			String eachID = iterator.next();

			if (!poisMap.containsKey(eachID)) {
				iterator.remove();
			}
		}


		// Second, check for the newEntryPOI comparing the inmemory userWithinGeofences list
		List <PlacesPOI> newlyEnteredPois = new ArrayList<PlacesPOI>();

		for (PlacesPOI poi : nearbyPOIs) {

			// if the user is withIn the poi and we haven't recorded that yet, then add them to newlyEnteredPois list
			if (poi.containsUser() && !userWithinGeofences.contains(poi.getIdentifier())) {
				userWithinGeofences.add(poi.getIdentifier());
				newlyEnteredPois.add(poi);
				continue;
			}

			// if the user is not within the poi and userWithinGeofences list contains the poi, remove it
			if (!poi.containsUser() && userWithinGeofences.contains(poi.getIdentifier())) {
				userWithinGeofences.remove(poi.getIdentifier());
			}
		}

		saveUserWithinGeofences();
		return newlyEnteredPois;
	}

	/**
	 * Stops monitoring for entry and exit event on nearby places of interest.
	 *
	 * Calling this method with YES for clearData will purge the {@link #userWithinGeofences} data in addition to stop monitoring
	 * for further geofence events.
	 *
	 * @param clearData a boolean indicating whether to clear the {@link #userWithinGeofences} from in-memory and persistence
	 */
	void stopMonitoringFences(final boolean clearData) {
		AdobeCallback<Void> onSuccess = new AdobeCallback<Void>() {
			@Override
			public void call(Void aVoid) {
				// on successful unregistration of all the pois register the new nearbypois
				Log.warning(PlacesMonitorConstants.LOG_TAG, "Successfully stopped monitoring all the fences");
			}
		};
		AdobeCallback<String> onFailiure = new AdobeCallback<String>() {
			@Override
			public void call(String message) {
				Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to stop monitoring all the fences," + message);
			}
		};

		if (clearData) {
			userWithinGeofences.clear();
			saveUserWithinGeofences();
		}

		unregisterPOIS(onSuccess, onFailiure);
	}

	// ========================================================================================
	// Internal Geofence Processor
	// ========================================================================================

	/**
	 * Handler for processing the received geofence event.
	 *
	 * <p>
	 * Make sure this method is called with non-null eventData.
	 * This method will be called when the OS event for Geofence transitions is received.
	 * This method curates the list of geofence transitions received to prevent duplicate entry/exits and then
	 * calls the {@link PlacesExtension} to process the obtained {@link Geofence} triggers.
	 *
	 * @param eventData the {@link EventData} from the OS Event containing geofence transition information
	 * @see Places#processGeofence(Geofence, int)
	 */
	void onGeofenceTriggerReceived(final EventData eventData) {

		List<String> geofenceIDs;
		int transitionType;

		try {

			geofenceIDs = eventData.getStringList(PlacesMonitorConstants.EventDataKey.GEOFENCE_IDS);
			transitionType = eventData.getInteger(PlacesMonitorConstants.EventDataKey.GEOFENCE_TRANSITION_TYPE);
		} catch (VariantException exp) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						String.format("Exception occurred while reading the geofenceIds from the OS event, ignoring the OS event. Exception message - ",
									  exp.getMessage()));
			return;
		}

		if (geofenceIDs == null || geofenceIDs.isEmpty()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"No geofenceId's are obtained from OS geofence event. Ignoring the OS event.");
			return;
		}

		// curate the obtained geofence list
		List<String> curatedGeofences  = getCuratedGeofencesList(geofenceIDs, transitionType);

		// dispatch a region event for the places list
		for (String geofenceID : curatedGeofences) {
			// Creating a geofence object.
			// To successfully create a geofence object, setting of latitude, longitude, radius, transition type and expiry duration are required.
			// Note : This geofence object is created with inconsequential latitude, longitude and radius.
			// Places API method processGeofence only reads the geofenceId of the triggered fences. Other data elements are not used by the Places.processGeofence API.
			// Moreover latitude, longitude and radius cannot be extracted from the geofence object. Unless its passed to android for monitoring.
			Geofence geofence = new Geofence.Builder()
			.setRequestId(geofenceID)
			.setExpirationDuration(Geofence.NEVER_EXPIRE)
			.setTransitionTypes(transitionType)
			.setCircularRegion(INCONSEQUENTIAL_LATITUDE, INCONSEQUENTIAL_LONGITUDE, INCONSEQUENTIAL_RADIUS)
			.build();
			Places.processGeofence(geofence, transitionType);
		}
	}

	// ================================================================================================================================
	// getCuratedGeofencesList
	// ================================================================================================================================

	/**
	 * Compares with the existing in-memory {@code #userWithinGeofences} list, ignores the duplicate entry event,
	 * updates the in-memory {@code #userWithinGeofences} variable with the obtained geofences and finally returns the curated list
	 * of GeofenceIDs that needs to be processed.
	 *
	 *
	 * @param obtainedGeofenceIds A {@link List} of {@code String} representing geofenceIDs obtained from the OS event
	 * @param transitionType {@code int} representing the transition type of the provided list of geofences
	 *
	 * @return the curated list of {@code Geofence}'s that needs to be processed by {@link Places} extension
	 */
	List<String> getCuratedGeofencesList(final List<String> obtainedGeofenceIds, final int transitionType) {
		List<String> curatedGeofenceList = new ArrayList<String>();

		// if entry event, add geofence to the userWithinGeofence
		if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
			for (String geofenceID : obtainedGeofenceIds) {
				if (!userWithinGeofences.contains(geofenceID)) {
					curatedGeofenceList.add(geofenceID);
					userWithinGeofences.add(geofenceID);
				} else {
					Log.debug(PlacesMonitorConstants.LOG_TAG,
							  String.format("Ignoring to process the entry of geofenceId %s. Because an entry was already recorded", geofenceID));
				}
			}
		}

		// if exit event, remove from the userWithinGeofence
		else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
			for (String geofenceID : obtainedGeofenceIds) {
				if (userWithinGeofences.contains(geofenceID)) {
					userWithinGeofences.remove(geofenceID);
				}

				curatedGeofenceList.add(geofenceID);
			}
		}

		return curatedGeofenceList;

	}

	// ========================================================================================
	// Load/Save Monitored Fences to persistence
	// ========================================================================================


	/**
	 * Loads the persisted data into the in-memory variables.
	 * <p>
	 * This method is called during the boot time of the SDK.
	 * Loading of persisted data fails if the {@link SharedPreferences} or App's {@link Context} is null.
	 *
	 */
	void loadPersistedData() {
		SharedPreferences sharedPreferences = PlacesMonitorUtil.getSharedPreferences();

		if (sharedPreferences == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to load monitoring geofences from persistence, sharedPreference is null");
			return;
		}

		userWithinGeofences = sharedPreferences.getStringSet(PlacesMonitorConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY,
							  new HashSet<String>());
		Log.trace(PlacesMonitorConstants.LOG_TAG,
				  "PlacesGeoFenceManager.loadPersistedData() userWithinGeofences: " + userWithinGeofences.toString());
	}

	/**
	 * Saves the in-memory variable {@link #userWithinGeofences} in persistence.
	 */
	void saveUserWithinGeofences() {
		SharedPreferences sharedPreferences = PlacesMonitorUtil.getSharedPreferences();

		if (sharedPreferences == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to save userWithIn geofences from persistence, sharedPreference is null");
			return;
		}

		SharedPreferences.Editor editor = sharedPreferences.edit();

		if (editor == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to save userWithIn geofences from persistence, shared preference editor is null");
			return;
		}

		if (userWithinGeofences == null || userWithinGeofences.isEmpty()) {
			editor.remove(PlacesMonitorConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY);
		} else {
			editor.putStringSet(PlacesMonitorConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY, userWithinGeofences);
		}

		editor.commit();
	}

	/**
	 * Unregisters the previously monitoring POIs and registers the new list of nearByPOIs passed
	 *
	 * @param nearByPOIs A {@link List} of {@link PlacesPOI} that needs to be registered for monitoring
	 */
	void refreshNearByPOIS(final List<PlacesPOI> nearByPOIs) {
		AdobeCallback<Void> onSuccess = new AdobeCallback<Void>() {
			@Override
			public void call(Void aVoid) {
				// on successful unregistration of all the pois register the new nearbypois
				Log.warning(PlacesMonitorConstants.LOG_TAG, "Successfully unregistered old nearByPois");
				registerPOIs(nearByPOIs);
			}
		};
		AdobeCallback<String> onFailiure = new AdobeCallback<String>() {
			@Override
			public void call(String message) {
				Log.warning(PlacesMonitorConstants.LOG_TAG, String.format("Unable to unregister old nearByPois. Error message %s.",
							message));
				registerPOIs(nearByPOIs);
			}
		};

		unregisterPOIS(onSuccess, onFailiure);
	}

	// ========================================================================================
	// private methods
	// ========================================================================================

	/**
	 * Unregisters all the pois that are currently being monitored by google's {@link GeofencingClient}.
	 * <p>
	 * The pois are registered to be monitored for entry and exit events.
	 * The registration will fail if,
	 * <ul>
	 *     <li> The permission for accessing the fine location is denied.</li>
	 *     <li> {@link PendingIntent} for receiving Geofencing events is null.</li>
	 *     <li> If the provided list of nearByPois is null/empty.</li>
	 * </ul>
	 *
	 * @param onSuccess A {@link AdobeCallback} called when the unregistering of all pois is successful
	 * @param onFailure A {@link AdobeCallback} called when the unregistering of all pois has failed
	 */
	private void unregisterPOIS(final AdobeCallback<Void> onSuccess, final AdobeCallback<String> onFailure) {
		GeofencingClient geofencingClient = getGeofencingClient();

		if (geofencingClient == null) {
			onFailure.call("geofencingClient instance is null");
			return;
		}

		PendingIntent geofenceIntent = getGeofencePendingIntent();

		if (geofenceIntent == null) {
			onFailure.call("geofence intent is null");
			return;
		}


		Task<Void> task = geofencingClient.removeGeofences(geofenceIntent);
		task.addOnSuccessListener(new OnSuccessListener<Void>() {
			@Override
			public void onSuccess(Void aVoid) {
				;

				if (onSuccess != null) {
					onSuccess.call(null);
				}
			}
		});
		task.addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(Exception e) {
				if (onFailure != null) {
					onFailure.call(e.getMessage());
				}
			}
		});
	}

	/**
	 * Registers the given list of {@link PlacesPOI} with the google's {@link GeofencingClient}
	 * <p>
	 * The pois are registered to be monitored for entry and exit events. The registration will fail if,
	 * <ul>
	 *     <li> The permission for accessing the fine location is denied.</li>
	 *     <li> {@link PendingIntent} for receiving Geofencing events is null.</li>
	 *     <li> If the provided list of nearByPois is null/empty.</li>
	 * </ul>
	 *
	 * @param nearByPOIs A {@link List} of nearbyPOIs obtained for the devices current location
	 */
	private void registerPOIs(final List<PlacesPOI> nearByPOIs) {
		// List of geofence to be added
		final List<Geofence> geofences = new ArrayList<>();

		if (!checkPermissions()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to register new geofences, App permission to use FINE_LOCATION is not granted.");
			return;
		}

		PendingIntent geofenceIntent = getGeofencePendingIntent();

		if (geofenceIntent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to register new geofences, Places Geofence Broadcast Receiver was never initialized");
			return;
		}


		for (PlacesPOI poi : nearByPOIs) {


			// If a geofence was previously registered, reading them will just replace the old one, which
			// in our case is a no-op. We therefore don't really need to keep track which geofence was
			// registered before, which can go out of sync anyway with the OS. Furthermore, android
			// does not provide any API to query which geofences are currently monitored, so it's safer
			// to re-register previously registered geofences.

			final Geofence fence = new Geofence.Builder()
			.setRequestId(poi.getIdentifier())
			.setCircularRegion(poi.getLatitude(), poi.getLongitude(), poi.getRadius())
			.setExpirationDuration(Geofence.NEVER_EXPIRE)
			.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
								Geofence.GEOFENCE_TRANSITION_EXIT)
			.build();
			Log.debug(PlacesMonitorConstants.LOG_TAG,
					  String.format("Attempting to Monitor POI with id %s name %s latitude %s longitude %s", poi.getIdentifier(),
									poi.getName(), poi.getLatitude(), poi.getLongitude()));
			geofences.add(fence);
		}

		if (geofences.isEmpty()) {
			Log.debug(PlacesMonitorConstants.LOG_TAG, "There are no new geofences that needs to be monitored");
			return;
		}

		GeofencingRequest.Builder builder = new GeofencingRequest.Builder();


		// By default initial trigger is set to INITIAL_TRIGGER_ENTER | INITIAL_TRIGGER_DWELL
		// This is not what we want since it will result in duplicate triggers if we are already
		// inside POI(s).

		builder.setInitialTrigger(0);
		builder.addGeofences(geofences);

		try {
			Task<Void> task = geofencingClient.addGeofences(builder.build(), getGeofencePendingIntent());
			task.addOnSuccessListener(new OnSuccessListener<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
					Log.debug(PlacesMonitorConstants.LOG_TAG, String.format("Successfully added %d fences for monitoring",
							  geofences.size()));
				}
			});
			task.addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(Exception e) {
					Log.debug(PlacesMonitorConstants.LOG_TAG, "Error in adding fences for monitoring " + e.getMessage());

				}
			});
		} catch (SecurityException e) {
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Add Geofence : SecurityException: " + e.getMessage());
		}
	}

	// ========================================================================================
	// private methods - Getters
	// ========================================================================================

	/**
	 * Returns a {@code PendingIntent} instance for getting the Geofence triggers
	 * <p>
	 * Returns the existing {@link #geofencePendingIntent} instance if its not null.
	 * Else attempts to create a new Pending Intent.
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


	// ========================================================================================
	// private methods - Permission Handling
	// ========================================================================================
	/**
	 * Helper method to verify if the permission to access FINE_LOCATION is granted for the application.
	 * <p>
	 * Returns true if the permission is granted, false otherwise.
	 *
	 * @return a {@code boolean} representing the permission for accessing FINE_LOCATION
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
}
