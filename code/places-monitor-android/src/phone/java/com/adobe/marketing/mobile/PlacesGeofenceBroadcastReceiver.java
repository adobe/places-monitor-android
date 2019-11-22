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
// PlacesGeofenceBroadcastReceiver.java
//

package com.adobe.marketing.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Broadcast receiver for the geofence updates.
 * <p>
 * Receives broadcast messages from the Android OS about the geofence entry/exit events.
 */
public class PlacesGeofenceBroadcastReceiver extends BroadcastReceiver {
	static final String ACTION_GEOFENCE_UPDATE =
		"com.adobe.marketing.mobile.PlacesGeofenceBroadcastReceiver.geofenceUpdates";

	/**
	 * This method is called when the {@link PlacesGeofenceBroadcastReceiver} is receiving an intent with geofence event.
	 * <p>
	 *  Dispatches an event with EventType {@link PlacesMonitorConstants.EventType#OS} and EventSource {@link PlacesMonitorConstants.EventSource#RESPONSE_CONTENT}
	 *  with the obtained geofence triggers.
	 *  No action is taken if received intent is null.
	 *  No action is taken if actionName of the intent is not equal to {@link #ACTION_GEOFENCE_UPDATE}.
	 *  No action is taken if {@link GeofencingEvent} has error or if no geofences associated with the event.
	 *
	 * @param context 	the application's {@link Context}
	 * @param intent 	the broadcasted geofence event message wrapped in an intent
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesGeofenceBroadcastReceiver : Cannot process the geofence trigger, the received intent is null.");
			return;
		}

		final String action = intent.getAction();

		if (!ACTION_GEOFENCE_UPDATE.equals(action)) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesGeofenceBroadcastReceiver : Unable to process the geofence trigger, invalid action type received");
			return;
		}

		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

		if (geofencingEvent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesGeofenceBroadcastReceiver : Unable to process the geofence trigger, GeofencingEvent is null");
			return;
		}

		if (geofencingEvent.hasError()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesGeofenceBroadcastReceiver : Cannot process the geofence trigger, Geofencing event has error. Ignoring region event.");
			return;
		}

		List<Geofence> obtainedGeofences = geofencingEvent.getTriggeringGeofences();

		if (obtainedGeofences == null || obtainedGeofences.isEmpty()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesGeofenceBroadcastReceiver : Cannot process the geofence trigger, null or empty geofence obtained from the geofence trigger");

			return;
		}


		// create a list of geofence ID's
		List<String> geofenceIDs = new ArrayList<String>();

		for (Geofence geofence : obtainedGeofences) {
			geofenceIDs.add(geofence.getRequestId());
		}

		dispatchOSGeofenceTriggerEvent(geofenceIDs, geofencingEvent.getGeofenceTransition());
	}


	/**
	 * Creates and dispatches {@link PlacesMonitorConstants.EventType#OS} {@link PlacesMonitorConstants.EventSource#RESPONSE_CONTENT} event with
	 * obtained list of geofenceIDs and transitionType to the eventHub.
	 *
	 * @param geofenceIDs		A {@link List} of geofenceIDs
	 * @param transitionType	An {@code int} representing the type of geofence transition
	 */
	private void dispatchOSGeofenceTriggerEvent(final List<String> geofenceIDs, final int transitionType) {
		// create eventData
		HashMap<String, Object> eventData = new HashMap<>();

		eventData.put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE,
					  PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_GEOFENCE_TRIGGER);
		eventData.put(PlacesMonitorConstants.EventDataKey.GEOFENCE_IDS, geofenceIDs);
		eventData.put(PlacesMonitorConstants.EventDataKey.GEOFENCE_TRANSITION_TYPE, transitionType);

		// dispatch OS event
		Event event = new Event.Builder(PlacesMonitorConstants.EVENTNAME_OS_GEOFENCE_TRIGGER,
										PlacesMonitorConstants.EventType.OS, PlacesMonitorConstants.EventSource.RESPONSE_CONTENT).
		setEventData(eventData).build();

		if (MobileCore.dispatchEvent(event, null)) {
			Log.debug(PlacesMonitorConstants.LOG_TAG,
					  "PlacesGeofenceBroadcastReceiver : Successfully dispatched OS Response event with geofence transitions");
		} else {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						String.format("PlacesGeofenceBroadcastReceiver : Unable to dispatch the OS Response event with geofence transitions %s",
									  event.getEventData()));
		}
	}
}
