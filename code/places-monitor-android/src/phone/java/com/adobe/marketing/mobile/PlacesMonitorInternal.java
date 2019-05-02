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

	protected PlacesMonitorInternal(final ExtensionApi extensionApi) {
		super(extensionApi);

		// register a listener for shared state changes
		extensionApi.registerEventListener(
				PlacesMonitorConstants.EventType.HUB,
				PlacesMonitorConstants.EventSource.SHARED_STATE,
				PlacesMonitorListenerHubSharedState.class, new ExtensionErrorCallback<ExtensionError>() {
					@Override
					public void error(ExtensionError extensionError) {
						if(extensionError != null) {
							Log.debug("There was an error registering PlacesMonitorListenerHubSharedState for Event Hub shared state events: %s",
									extensionError.getErrorName());
						}
					}
				});

		// register a listener for places response events
		extensionApi.registerEventListener(
				PlacesMonitorConstants.EventType.PLACES,
				PlacesMonitorConstants.EventSource.RESPONSE_CONTENT,
				PlacesMonitorListenerPlacesResponseContent.class,  new ExtensionErrorCallback<ExtensionError>() {
					@Override
					public void error(ExtensionError extensionError) {
						if(extensionError != null) {
							Log.debug("There was an error registering PlacesMonitorListenerPlacesResponseContent for Places response events: %s",
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
							Log.debug("There was an error registering PlacesMonitorListenerPlacesResponseContent for Places Monitor request events: %s",
									extensionError.getErrorName());
						}
					}
				});

		// initialize location, geofence Manager and the events queue
		locationManager = new PlacesLocationManager();
		geofenceManager = new PlacesGeofenceManager();
		geofenceManager.loadMonitoringFences();
		eventQueue = new ConcurrentLinkedQueue<>();
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
					if (extensionError != null){
						Log.error(PlacesMonitorConstants.LOG_TAG,
								String.format("Could not process event, an error occurred while retrieving configuration shared state: %s",
										extensionError.getErrorName()));
					}
				}
			};
			Map<String, Object> configSharedState = getApi().getSharedEventState(PlacesMonitorConstants.SharedState.CONFIGURATION,
					eventToProcess, extensionErrorCallback);

			// NOTE: configuration is mandatory processing the event, so if shared state is null (pending) stop processing events
			if (configSharedState == null) {
				Log.debug(PlacesMonitorConstants.LOG_TAG, "Could not process event, configuration shared state is pending");
				return;
			}

			if (PlacesMonitorConstants.EventType.MONITOR.equalsIgnoreCase(eventToProcess.getType()) &&
					PlacesMonitorConstants.EventSource.REQUEST_CONTENT.equalsIgnoreCase(eventToProcess.getSource())) {
				// handle the places monitor request event
				processMonitorRequestEvent(eventToProcess);
			}

			else if (PlacesMonitorConstants.EventType.PLACES.equalsIgnoreCase(eventToProcess.getType()) &&
					PlacesMonitorConstants.EventSource.RESPONSE_CONTENT.equalsIgnoreCase(eventToProcess.getSource())) {
				processPlacesResponseEvent(eventToProcess);
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
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Could not process places monitor request event, Invalid/Unknown event name");
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
	// Location, Geofence updates handler method
	// ========================================================================================

	private void processPlacesResponseEvent(final Event event) {
		EventData eventData = event.getData();

		if (eventData == null || eventData.isEmpty()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Places Response has invalid event data");
			return;
		}

		try {
			List<PlacesMonitorPOI> pois = eventData.getTypedList(PlacesMonitorConstants.EventDataKeys.NEAR_BY_PLACES_LIST,
					new PlacesMonitorPOIVariantSerializer());
			geofenceManager.startMonitoringFences(pois);
		} catch (VariantException exp) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Places Response has invalid event data");
		}
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
