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
// PlacesMonitorInternal.java
//

package com.adobe.marketing.mobile;

import android.location.Location;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PlacesMonitorInternal extends Extension {

	private ConcurrentLinkedQueue<Event> eventQueue;
	private PlacesLocationManager locationManager;
	private PlacesGeofenceManager geofenceManager;
	private ExecutorService executorService;
	private final Object executorMutex = new Object();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Called during places monitor extension's registration.
	 * The following listeners are registered during this extension's registration.
	 * <ul>
	 *     <li> {@link PlacesMonitorListenerHubSharedState} listening to event with eventType {@link PlacesMonitorConstants.EventType#HUB}
	 *     and EventSource {@link PlacesMonitorConstants.EventSource#SHARED_STATE}</li>
	 *     <li> {@link PlacesMonitorListenerMonitorRequestContent} listening to event with eventType {@link PlacesMonitorConstants.EventType#MONITOR}
	 *     and EventSource {@link PlacesMonitorConstants.EventSource#REQUEST_CONTENT}</li>
	 *      <li> {@link PlacesMonitorListenerOSResponseContent} listening to event with eventType {@link PlacesMonitorConstants.EventType#OS}
	 * 	 *  and EventSource {@link PlacesMonitorConstants.EventSource#RESPONSE_CONTENT}</li>
	 * </ul>
	 *
	 * @param extensionApi 	{@link ExtensionApi} instance
	 */
	protected PlacesMonitorInternal(final ExtensionApi extensionApi) {
		super(extensionApi);

		// register a listener for shared state changes
		extensionApi.registerEventListener(
			PlacesMonitorConstants.EventType.HUB,
			PlacesMonitorConstants.EventSource.SHARED_STATE,
		PlacesMonitorListenerHubSharedState.class, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				if (extensionError != null) {
					Log.error(PlacesMonitorConstants.LOG_TAG,"There was an error registering PlacesMonitorListenerHubSharedState for Event Hub shared state events: %s",
							  extensionError.getErrorName());
				}
			}
		});

		// register a listener for monitor request events
		extensionApi.registerEventListener(
			PlacesMonitorConstants.EventType.MONITOR,
			PlacesMonitorConstants.EventSource.REQUEST_CONTENT,
		PlacesMonitorListenerMonitorRequestContent.class, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				if (extensionError != null) {
					Log.error(PlacesMonitorConstants.LOG_TAG,"There was an error registering PlacesMonitorListenerPlacesResponseContent for Places Monitor request events: %s",
							  extensionError.getErrorName());
				}
			}
		});


		// register a listener for os response events
		extensionApi.registerEventListener(
			PlacesMonitorConstants.EventType.OS,
			PlacesMonitorConstants.EventSource.RESPONSE_CONTENT,
		PlacesMonitorListenerOSResponseContent.class, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				if (extensionError != null) {
					Log.error(PlacesMonitorConstants.LOG_TAG,"There was an error registering PlacesMonitorListenerOSResponseContent for OS response events: %s",
							  extensionError.getErrorName());
				}
			}
		});

		// initialize location, geofence Manager and the events queue
		locationManager = new PlacesLocationManager(this);
		geofenceManager = new PlacesGeofenceManager();
		geofenceManager.loadPersistedData();
		eventQueue = new ConcurrentLinkedQueue<>();

		// authorization status can change while the app is not running, so we must validate
		// that our current shared state value is still accurate
		PlacesActivity.updateLocationAuthorizationStatus();

		Log.debug(PlacesMonitorConstants.LOG_TAG, "Registering Places Monitoring extension - version %s",
				  PlacesMonitorConstants.EXTENSION_VERSION);
	}

	/**
	 * Overridden method of {@link Extension} class to handle error occurred during registration of the module.
	 *
	 * @param extensionUnexpectedError 	{@link ExtensionUnexpectedError} occurred exception
	 */
	@Override
	protected void onUnexpectedError(ExtensionUnexpectedError extensionUnexpectedError) {
		Log.error(PlacesMonitorConstants.LOG_TAG,
				  String.format("Unexpected error occurred while registering PlacesMonitor extension. Error message %s",
								extensionUnexpectedError.getMessage()));
		this.onUnregistered();
	}

	/**
	 * Overridden method of {@link Extension} class to provide a valid extension name to register with eventHub.
	 *
	 * @return A {@link String} extension name for Places Monitor
	 */
	@Override
	protected String getName() {
		return PlacesMonitorConstants.EXTENSION_NAME;
	}

	/**
	 * Overridden method of {@link Extension} class to provide the extension version.
	 *
	 * @return A {@link String} representing the extension version
	 */
	@Override
	protected String getVersion() {
		return PlacesMonitorConstants.EXTENSION_VERSION;
	}

	/**
	 * Overridden method of {@link Extension} class called when extension is unregistered by the core.
	 *
	 * <p>
	 * On unregister of places monitor extension, the shared states are cleared.
	 */
	@Override
	protected void onUnregistered() {
		super.onUnregistered();
		getApi().clearSharedEventStates(null);
	}

	/**
	 * Gets the nearbyPOIs for the given location.
	 *
	 * <p>
	 * This method is called by the {@link #locationManager} with the current device location to fetch the closest
	 * {@link PlacesMonitorConstants#NEARBY_GEOFENCES_COUNT} nearby points of interest around the given location.
	 * The obtained POIs are then passed to {@link #geofenceManager} to start monitoring for entry/exit events.
	 *
	 * @param location 	A {@link Location} instance representing device's current location
	 */
	void getPOIsForLocation(final Location location) {
		if (location == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Null location is obtained from OS, Ignoring to get near by pois");
			return;
		}

		Log.debug(PlacesMonitorConstants.LOG_TAG,
				  "New location obtained: " + location.getLatitude() + location.getLongitude() +
				  "Attempting to get the near by pois");
		Places.getNearbyPointsOfInterest(location, PlacesMonitorConstants.NEARBY_GEOFENCES_COUNT,
		new AdobeCallback<List<PlacesPOI>>() {
			@Override
			public void call(List<PlacesPOI> placesPOIS) {
				geofenceManager.startMonitoringFences(placesPOIS);
			}
		}, new AdobeCallback<PlacesRequestError>() {
			@Override
			public void call(PlacesRequestError placesRequestError) {
				handlePlacesRequestError(placesRequestError);
			}
		});
	}

	/**
	 * This method queues the provided event in {@link #eventQueue}.
	 *
	 * <p>
	 * The queued events are then processed in an orderly fashion.
	 * No action is taken if the provided event's value is null.
	 *
	 * @param event 	The {@link Event} thats needs to be queued
	 */
	void queueEvent(final Event event) {
		if (event == null) {
			return;
		}

		eventQueue.add(event);
	}

	/**
	 * Processes the queued event one by one until queue is empty.
	 *
	 * <p>
	 * Suspends processing of the events in the queue if the configuration shared state is not ready.
	 * Processed events are polled out of the {@link #eventQueue}.
	 */
	void processEvents() {
		while (!eventQueue.isEmpty()) {
			Event eventToProcess = eventQueue.peek();

			ExtensionErrorCallback<ExtensionError> extensionErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
				@Override
				public void error(final ExtensionError extensionError) {
					if (extensionError != null) {
						Log.warning(PlacesMonitorConstants.LOG_TAG,
									String.format("Could not process event, an error occurred while retrieving configuration shared state: %s",
												  extensionError.getErrorName()));
					}
				}
			};
			Map<String, Object> configSharedState = getApi().getSharedEventState(PlacesMonitorConstants.SharedState.CONFIGURATION,
													eventToProcess, extensionErrorCallback);

			// NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
			if (configSharedState == null) {
				Log.warning(PlacesMonitorConstants.LOG_TAG,
							"Could not process event, configuration shared state is pending");
				return;
			}

			if (PlacesMonitorConstants.EventType.MONITOR.equalsIgnoreCase(eventToProcess.getType()) &&
					PlacesMonitorConstants.EventSource.REQUEST_CONTENT.equalsIgnoreCase(eventToProcess.getSource())) {
				// handle the places monitor request event
				processMonitorRequestEvent(eventToProcess);
			}

			else if (PlacesMonitorConstants.EventType.OS.equalsIgnoreCase(eventToProcess.getType()) &&
					 PlacesMonitorConstants.EventSource.RESPONSE_CONTENT.equalsIgnoreCase(eventToProcess.getSource())) {
				// handle the places monitor request event
				processOSResponseEvent(eventToProcess);
			}

			// event processed, remove it from the queue
			eventQueue.poll();
		}
	}


	/**
	 * Method to process the places monitor request content {@link Event}'s.
	 *
	 * <p>
	 * Differentiates the event by the given name and processes them accordingly.
	 *
	 * @param event 	MonitorRequestContent {@link Event} to process
	 */
	private void processMonitorRequestEvent(final Event event) {
		final String eventName = event.getName();

		if (PlacesMonitorConstants.EVENTNAME_START.equals(eventName)) {
			startMonitoring();
		} else if (PlacesMonitorConstants.EVENTNAME_STOP.equals(eventName)) {

			boolean shouldClear = false;
			EventData data = event.getData();

			if (data != null && !data.isEmpty()) {
				shouldClear = data.optBoolean(PlacesMonitorConstants.EventDataKey.CLEAR, false);
			}

			stopMonitoring(shouldClear);
		} else if (PlacesMonitorConstants.EVENTNAME_UPDATE.equals(eventName)) {
			updateLocation();
		} else if (PlacesMonitorConstants.EVENTNAME_SET_LOCATION_PERMISSION.equals(eventName)) {
			setLocationPermission(event.getEventData());
		} else {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Could not process places monitor request event, Invalid/Unknown event name");
		}
	}


	/**
	 * Method to process OS response event.
	 *
	 * <p>
	 * This function looks for the appropriate eventData keys and processes the following OS Events:
	 * <ul>
	 *     <li> Location change event
	 *     <li> Geofence transition event
	 *     <li> Permission change event
	 * </ul>
	 * This method will not process the event if the eventData doesn't contain the required eventData keys.
	 *
	 * @param event 	An OS {@link Event} to be processed
	 */
	private void processOSResponseEvent(final Event event) {
		EventData eventData = event.getData();

		if (eventData == null || eventData.isEmpty()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Received empty eventData , Ignoring OS event.");
			return;
		}

		String eventType;

		try {
			eventType = eventData.getString2(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE);
		} catch (VariantException exception) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Invalid eventType for OS responseContent event, Ignoring OS event.");
			return;
		}

		if (StringUtils.isNullOrEmpty(eventType)) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Null/Empty eventType for OS responseContent event, Ignoring OS event.");
			return;
		}


		switch (eventType) {
			case PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_UPDATE: {
				locationManager.onLocationReceived(eventData);
				break;
			}

			case PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_GEOFENCE_TRIGGER: {
				geofenceManager.onGeofenceTriggerReceived(eventData);
				break;
			}

			case PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_PERMISSION_CHANGE: {
				handlePermissionChange(eventData);
				break;
			}

			default: {
				Log.warning(PlacesMonitorConstants.LOG_TAG,
							"Invalid eventType for OS responseContent event, Ignoring OS event.");
			}

		}
	}

	/**
	 * Method to handle the OS event for location permission change.
	 *
	 * @param eventData A {@link EventData} of the OS event containing the permission status
	 */
	private void handlePermissionChange(final EventData eventData) {
		String permissionStatus;

		try {
			permissionStatus = eventData.getString2(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION_STATUS);
		} catch (VariantException exp) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Unable to read permission status from the OS responseContent event. Ignoring Permission status change event.");
			return;
		}

		if (StringUtils.isNullOrEmpty(permissionStatus)) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Null/empty permission status value from the OS responseContent event. Ignoring Permission status change event.");
			return;
		}

		switch (permissionStatus) {
			case PlacesMonitorConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_GRANTED: {
				locationManager.beginLocationTracking();
				break;
			}

			case PlacesMonitorConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_DENIED: {
				locationManager.stopMonitoring();
				geofenceManager.stopMonitoringFences(true);
				break;
			}

			default: {
				Log.warning(PlacesMonitorConstants.LOG_TAG,
							"Invalid permission status value from the OS responseContent event. Ignoring Permission status change event.");
			}
		}

		PlacesActivity.updateLocationAuthorizationStatus();
	}

	/**
	 * Method to handle the error that occurred while getting the nearbyPointOfInterest.
	 *
	 * @param error 	A {@link PlacesRequestError} representing the type of error
	 */
	private void handlePlacesRequestError(final PlacesRequestError error) {
		String errorString = "Unknown error.";

		switch (error) {
			case CONNECTIVITY_ERROR:
				errorString = "No network connectivity.";
				break;

			case INVALID_LATLONG_ERROR:
				errorString =
					"An invalid latitude and/or longitude was provided.  Valid values are -90 to 90 (lat) and -180 to 180 (lon).";
				break;

			case QUERY_SERVICE_UNAVAILABLE:
				errorString = "The Places Query Service is unavailable. Try again later.";
				break;

			case SERVER_RESPONSE_ERROR:
				errorString = "There is an error in the response from the server.";
				break;

			case CONFIGURATION_ERROR:
				errorString = "Missing Places configuration.";
				stopMonitoring(true);
				break;

			default:
				break;
		}

		Log.warning(PlacesMonitorConstants.LOG_TAG,
					"An error occurred while attempting to retrieve nearby points of interest: " + errorString);
	}

	// ========================================================================================
	// Public API handlers
	// ========================================================================================


	/**
	 * Handler for places monitor extension's Start public api call.
	 *
	 * <p>
	 * This method requests the {@link #locationManager} to start monitoring for device location.
	 */
	private void startMonitoring() {
		locationManager.startMonitoring();
	}


	/**
	 * Handler for places monitor extension's Stop public api call.
	 *
	 * <p>
	 * This method requests the {@link #locationManager} to stop monitoring the device current location.
	 * It also requests the {@link #geofenceManager} to stop monitoring the fences that are currently being monitored.
	 *
	 * Calling this method with YES for clearData will purge the data even if the monitor is not actively tracking
	 * the device's location.
	 *
	 * @param clearData pass YES to clear all client-side Places data from the device.
	 */
	private void stopMonitoring(final boolean clearData) {
		locationManager.stopMonitoring();
		geofenceManager.stopMonitoringFences(clearData);

		if (clearData) {
			Places.clear();
		}
	}

	/**
	 * Handler for places monitor extension's updateLocation public api call.
	 * <p>
	 * This method requests the location manager to update the device current location immediately.
	 */
	private void updateLocation() {
		locationManager.updateLocation();
	}

	private void setLocationPermission(final Map<String, Object> eventData) {
		if (eventData == null || eventData.isEmpty()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"Invalid location permission value set. Ignoring setLocationPermission API call. For more details refer to %s",
					PlacesMonitorConstants.DocLinks.SET_LOCATION_PERMISSION);
			return;
		}

		String locationPermissionString = (String)eventData.get(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION);
		PlacesMonitorLocationPermission placesMonitorLocationPermission = PlacesMonitorLocationPermission.fromString(
					locationPermissionString);
		locationManager.setLocationPermission(placesMonitorLocationPermission);
	}



	// ========================================================================================
	// Getters for private members
	// ========================================================================================
	/**
	 * Getter for the {@link #executorService}. Access to which is mutex protected.
	 *
	 * @return A non-null {@link ExecutorService} instance
	 */
	ExecutorService getExecutor() {
		synchronized (executorMutex) {
			if (executorService == null) {
				executorService = Executors.newSingleThreadExecutor();
			}

			return executorService;
		}
	}

	/**
	 * Getter for the {@link #eventQueue}.
	 *
	 * @return A non-null {@link ConcurrentLinkedQueue} instance
	 */
	ConcurrentLinkedQueue<Event> getEventQueue() {
		return eventQueue;
	}
}
