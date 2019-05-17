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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.support.v4.content.LocalBroadcastManager;


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
	private BroadcastReceiver internalLocationReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			locationManager.onLocationReceived(intent);
		}
	};

	private BroadcastReceiver internalGeofenceReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			geofenceManager.onGeofenceReceived(intent);
		}
	};

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
					Log.warning("PlacesMonitorInternal : There was an error registering PlacesMonitorListenerHubSharedState for Event Hub shared state events: %s",
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
					Log.warning("PlacesMonitorInternal : There was an error registering PlacesMonitorListenerPlacesResponseContent for Places Monitor request events: %s",
								extensionError.getErrorName());
				}
			}
		});

		// initialize location, geofence Manager and the events queue
		locationManager = new PlacesLocationManager(this);
		geofenceManager = new PlacesGeofenceManager();
		geofenceManager.loadPersistedData();
		eventQueue = new ConcurrentLinkedQueue<>();

		Context context = App.getAppContext();

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesMonitorInternal : Context is null, Internal Broadcast receivers not initialized");
			return;
		}

		LocalBroadcastManager.getInstance(context).registerReceiver(internalLocationReceiver,
				new IntentFilter(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION));

		LocalBroadcastManager.getInstance(context).registerReceiver(internalGeofenceReceiver,
				new IntentFilter(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE));
	}


	@Override
	protected String getName() {
		return PlacesMonitorConstants.EXTENSION_NAME;
	}

	@Override
	protected String getVersion() {
		return PlacesMonitorConstants.EXTENSION_VERSION;
	}

	@Override
	protected void onUnregistered() {
		super.onUnregistered();
		getApi().clearSharedEventStates(null);
	}


	void getPOIsForLocation(final Location location) {
		if (location == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesMonitorInternal : Null location is obtained from OS, Ignoring to get near by pois");
			return;
		}

		Log.debug(PlacesMonitorConstants.LOG_TAG,
				  "PlacesMonitorInternal : New location obtained: " + location.getLatitude() + location.getLongitude() +
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
				// TODO : Read error and recover if possible
			}
		});
	}

	void queueEvent(final Event event) {
		if (event == null) {
			return;
		}

		eventQueue.add(event);
	}


	void processEvents() {
		while (!eventQueue.isEmpty()) {
			Event eventToProcess = eventQueue.peek();

			ExtensionErrorCallback<ExtensionError> extensionErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
				@Override
				public void error(final ExtensionError extensionError) {
					if (extensionError != null) {
						Log.warning(PlacesMonitorConstants.LOG_TAG,
									String.format("PlacesMonitorInternal : Could not process event, an error occurred while retrieving configuration shared state: %s",
												  extensionError.getErrorName()));
					}
				}
			};
			Map<String, Object> configSharedState = getApi().getSharedEventState(PlacesMonitorConstants.SharedState.CONFIGURATION,
													eventToProcess, extensionErrorCallback);

			// NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
			if (configSharedState == null) {
				Log.warning(PlacesMonitorConstants.LOG_TAG,
							"PlacesMonitorInternal : Could not process event, configuration shared state is pending");
				return;
			}

			if (PlacesMonitorConstants.EventType.MONITOR.equalsIgnoreCase(eventToProcess.getType()) &&
					PlacesMonitorConstants.EventSource.REQUEST_CONTENT.equalsIgnoreCase(eventToProcess.getSource())) {
				// handle the places monitor request event
				processMonitorRequestEvent(eventToProcess);
			}

			// event processed, remove it from the queue
			eventQueue.poll();
		}
	}


	private void processMonitorRequestEvent(final Event event) {
		final String eventName = event.getName();

		if (PlacesMonitorConstants.EVENTNAME_START.equals(eventName)) {
			startMonitoring();
		} else if (PlacesMonitorConstants.EVENTNAME_STOP.equals(eventName)) {
			stopMonitoring();
		} else if (PlacesMonitorConstants.EVENTNAME_UPDATE.equals(eventName)) {
			updateLocation();
		} else {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesMonitorInternal : Could not process places monitor request event, Invalid/Unknown event name");
		}
	}

	// ========================================================================================
	// Public API handlers
	// ========================================================================================

	private void startMonitoring() {
		locationManager.startMonitoring();
	}

	private void stopMonitoring() {
		locationManager.stopMonitoring();
		geofenceManager.stopMonitoringFences();
	}

	private void updateLocation() {
		locationManager.updateLocation();
	}




	// ========================================================================================
	// Getters for private members
	// ========================================================================================

	ExecutorService getExecutor() {
		synchronized (executorMutex) {
			if (executorService == null) {
				executorService = Executors.newSingleThreadExecutor();
			}

			return executorService;
		}
	}

	ConcurrentLinkedQueue<Event> getEventQueue() {
		return eventQueue;
	}
}
