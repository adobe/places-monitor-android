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
// PlacesLocationBroadcastReceiver.java
//

package com.adobe.marketing.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import com.google.android.gms.location.LocationResult;
import java.util.HashMap;
import java.util.List;

/**
 * Broadcast receiver for the location updates.
 * <p>
 *  Receive broadcast messages from Android OS about the device current location.
 */
public class PlacesLocationBroadcastReceiver extends BroadcastReceiver {
	static final String ACTION_LOCATION_UPDATE =
		"com.adobe.marketing.mobile.PlacesLocationBroadcastReceiver.locationUpdates";

	/**
	 * This method is called when the {@link PlacesLocationBroadcastReceiver} is receiving an intent broadcast with current location.
	 * <p>
	 *  Dispatches an event with EventType {@link PlacesMonitorConstants.EventType#OS} and EventSource {@link PlacesMonitorConstants.EventSource#RESPONSE_CONTENT}
	 * 	with the obtained location.
	 *  No action is taken if the passed intent is null.
	 *  No action is taken if the actionName of the intent is not same as {@link #ACTION_LOCATION_UPDATE}.
	 *  No action is performed if the received {@code LocationResult} is null.
	 *  No action is performed if the location array or the location is null.
	 *
	 * @param context 	the application's {@link Context}
	 * @param intent 	the broadcasted location message wrapped in an intent
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationBroadcastReceiver : Unable to process the location update, the received intent is null");
			return;
		}

		final String action = intent.getAction();

		if (!ACTION_LOCATION_UPDATE.equals(action)) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationBroadcastReceiver : Unable to process the location update, invalid action type received");
			return;
		}


		LocationResult result = LocationResult.extractResult(intent);

		if (result == null) {
			return;
		}

		List<Location> locations = result.getLocations();

		if (locations == null || locations.isEmpty()) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationBroadcastReceiver : Cannot process the location update, Received location array is null");
			return;
		}

		Location location = locations.get(0);

		if (location == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationBroadcastReceiver : Cannot process the location update, Received location is null");
			return;
		}

		String locationLog = "PlacesLocationBroadcastReceiver : A new location received with accuracy: " +
							 location.getAccuracy() + " lat: " + location.getLatitude() +
							 " lon: " + location.getLongitude();
		Log.debug(PlacesMonitorConstants.LOG_TAG, locationLog);
		dispatchOSLocationUpdateEvent(location.getLatitude(), location.getLongitude());
	}


	/**
	 * Creates and dispatches {@link PlacesMonitorConstants.EventType#OS} {@link PlacesMonitorConstants.EventSource#RESPONSE_CONTENT} event with
	 * obtained latitude and longitude to the eventHub.
	 *
	 * @param latitude 		{@code double} indicating latitude value
	 * @param longitude		{@code double} indicating longitude value
	 */
	private void dispatchOSLocationUpdateEvent(final double latitude, final double longitude) {
		HashMap<String, Object> eventData = new HashMap<>();
		eventData.put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE,
					  PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_UPDATE);
		eventData.put(PlacesMonitorConstants.EventDataKey.LATITUDE, latitude);
		eventData.put(PlacesMonitorConstants.EventDataKey.LONGITUDE, longitude);

		Event event = new Event.Builder(PlacesMonitorConstants.EVENTNAME_OS_LOCATION_UPDATE,
										PlacesMonitorConstants.EventType.OS, PlacesMonitorConstants.EventSource.RESPONSE_CONTENT).
		setEventData(eventData).build();

		if (MobileCore.dispatchEvent(event, null)) {
			Log.debug(PlacesMonitorConstants.LOG_TAG,
					  "PlacesLocationBroadcastReceiver : Successfully dispatched OS Response event with new location");
		} else {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						String.format("PlacesLocationBroadcastReceiver : Unable to dispatch the OS Response event with new location %s",
									  eventData));
		}

	}

}
