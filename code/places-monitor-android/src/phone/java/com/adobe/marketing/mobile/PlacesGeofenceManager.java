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
import com.google.android.gms.location.GeofencingEvent;
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
 * Class to manage and monitor geofences around the given device location
 */
class PlacesGeofenceManager {

	private final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
	private PendingIntent geofencePendingIntent;
	private Set<String> userWithinGeofences;
	private GeofencingClient geofencingClient;

	PlacesGeofenceManager() {
		userWithinGeofences = new HashSet<String>();
	}

	/**
	 * Starts monitoring the entry and exit events around the given nearByPOIs by registering with the Geofences with the Android OS.
	 * <p>
	 * This method is called by {@link PlacesMonitorInternal} when new set of POIs are available for monitoring.
	 * No action will be performed if the {@link GeofencingClient} required for the monitoring the POIs is null.
	 *
	 * @param nearByPOIs A {@link List} of n nearBy {@link PlacesPOI} objects
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
	 * {@link PlacesPOI} whose entry has not been already registered.
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

			// if the user is not withIn the poi and userWithinGeofences list contains the poi, remove it
			if (!poi.containsUser() && userWithinGeofences.contains(poi.getIdentifier())) {
				userWithinGeofences.remove(poi.getIdentifier());
			}
		}

		saveUserWithinGeofences();
		return newlyEnteredPois;
	}

	/**
	 * Stops monitoring for entry and exit event for the near by places of interest.
	 */
	void stopMonitoringFences() {
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

		unregisterPOIS(onSuccess, onFailiure);
	}

	// ========================================================================================
	// Internal Geofence Processor
	// ========================================================================================
	/**
	 * Handler for processing the received geofence event.
	 *
	 * <p>
	 * This method is called by the internal BroadcastReceiver on receiving an intent with {@link GeofencingEvent}.
	 * Calls the {@link PlacesExtension} to process the obtained {@link Geofence} triggers.
	 *
	 * No action is performed if the intents action is not same as {@link PlacesMonitorConstants#INTERNAL_INTENT_ACTION_GEOFENCE}.
	 * No action is performed if the received {@code GeofencingEvent} has error.
	 * No action is performed if the list of obtained {@code Geofences} is empty.
	 *
	 * @param intent the broadcasted geofence event message wrapped in an intent
	 * @see Places#processGeofence(Geofence, int)
	 */
	void onGeofenceReceived(final Intent intent) {
		if (intent == null) {
			Log.error(PlacesMonitorConstants.LOG_TAG,
					"Cannot process the geofence trigger, The received intent from the geofence broadcast receiver is null.");
			return;
		}

		final String action = intent.getAction();

		if (!PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE.equals(action)) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
					"Cannot process the geofence trigger, Invalid action type received from geofence broadcast receiver.");
			return;
		}

		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

		if (geofencingEvent.hasError()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
					"Cannot process the geofence trigger, Geofencing event has error. Ignoring region event.");
			return;
		}

		List<Geofence> obtainedGeofences = geofencingEvent.getTriggeringGeofences();

		if (obtainedGeofences == null || obtainedGeofences.isEmpty()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
					"Cannot process the geofence trigger, null or empty geofence obtained from the geofence trigger");

			return;
		}

		// curate the obtained geofence list
		List<Geofence> curatedGeofences  = getCuratedGeofencesList(obtainedGeofences, geofencingEvent.getGeofenceTransition());

		// dispatch a region event for the places list
		for (Geofence geofence : curatedGeofences) {
			Places.processGeofence(geofence, geofencingEvent.getGeofenceTransition());
		}
	}

	// ================================================================================================================================
	// getCuratedGeofencesList
	// ================================================================================================================================

	/**
	 * Compares with the existing in-memory {@code #userWithinGeofences} list and get the to be processed {@link Geofence} list.
	 *
	 * @param obtainedGeofences A {@link List} of {@code Geofence} obtained from the {@link GeofencingEvent}
	 * @param transitionType {@code int} representing the transition type of the provided list of geofences
	 *
	 * @return the curated list of {@code Geofence}'s that needs to be processed by {@link Places} extension
	 */
	List<Geofence> getCuratedGeofencesList(final List<Geofence> obtainedGeofences, final int transitionType) {
		List<Geofence> curatedGeofenceList = new ArrayList<Geofence>();

		// if entry event, add geofence to the userWithinGeofence
		if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
			for (Geofence geofence : obtainedGeofences) {
				if (!userWithinGeofences.contains(geofence.getRequestId())) {
					curatedGeofenceList.add(geofence);
					userWithinGeofences.add(geofence.getRequestId());
				} else {
					Log.debug(PlacesMonitorConstants.LOG_TAG,
							"Ignoring to process the entry of geofence" + geofence.getRequestId() + ".Because an entry was already recorded");
				}
			}
		}

		// if exit event, remove from the userWithinGeofence
		else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
			for (Geofence geofence : obtainedGeofences) {
				if (userWithinGeofences.contains(geofence.getRequestId())) {
					userWithinGeofences.remove(geofence.getRequestId());
				}

				curatedGeofenceList.add(geofence);
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
		SharedPreferences sharedPreferences = getSharedPreference();

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
		SharedPreferences sharedPreferences = getSharedPreference();

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
				Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to unregister old nearByPois," + message);
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
					"Unable to monitor geofences, App permission to use FINE_LOCATION is not granted.");
			return;
		}

		PendingIntent geofenceIntent = getGeofencePendingIntent();

		if (geofenceIntent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
					"Unable to stop monitoring geofences, Places Geofence Broadcast Receiver was never initialized");
			return;
		}


		for (PlacesPOI poi : nearByPOIs) {

			/**
			 * If a geofence was previously registered, reading them will just replace the old one, which
			 * in our case is a no-op. We therefore don't really need to keep track which geofence was
			 * registered before, which can go out of sync anyway with the OS. Furthermore, android
			 * does not provide any API to query which geofences are currenty monitored, so it's safer
			 * to re-register previously registered geofences.
			 */
			final Geofence fence = new Geofence.Builder()
					.setRequestId(poi.getIdentifier())
					.setCircularRegion(poi.getLatitude(), poi.getLongitude(), poi.getRadius())
					.setExpirationDuration(Geofence.NEVER_EXPIRE)
					.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
							Geofence.GEOFENCE_TRANSITION_EXIT)
					.build();
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Attempting to Monitor location with id " + poi.getIdentifier() +
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

		/*	by default initial trigger is set to INITIAL_TRIGGER_ENTER | INITIAL_TRIGGER_DWELL
		 *   This is not what we want since it will result in duplicate triggers if we are already
		 *   inside POI(s).
		 * */
		builder.setInitialTrigger(0);
		builder.addGeofences(geofences);

		try {
			Task<Void> task = geofencingClient.addGeofences(builder.build(), getGeofencePendingIntent());
			task.addOnSuccessListener(new OnSuccessListener<Void>() {
				@Override
				public void onSuccess(Void aVoid) {
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


	/**
	 * Getter for the applications {@link SharedPreferences}
	 * <p>
	 * Returns null if the app context is not available
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


	// ========================================================================================
	// private methods - Permission Handling
	// ========================================================================================
	/**
	 * Helper method to verify the FINE_LOCATION permission for the application
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
