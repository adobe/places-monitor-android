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

import java.util.List;

public class PlacesLocationBroadcastReceiver extends BroadcastReceiver {
	static final String ACTION_LOCATION_UPDATE =
		"com.adobe.marketing.mobile.PlacesLocationBroadcastReceiver.locationUpdates";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) {
			Log.error(PlacesMonitorConstants.LOG_TAG,"Cannot process the location update, The received intent from the location broadcast receiver  is null");
			return;
		}

		final String action = intent.getAction();
		if (!ACTION_LOCATION_UPDATE.equals(action)) {
			Log.error(PlacesMonitorConstants.LOG_TAG,"Cannot process the location update, Invalid action type received from location broadcast receiver");
			return;
		}

		LocationResult result = LocationResult.extractResult(intent);
		if (result == null) {
			Log.error(PlacesMonitorConstants.LOG_TAG,"Cannot process the location update, Received location result is null");
			return;
		}

		List<Location> locations = result.getLocations();
		if (locations == null || locations.isEmpty()) {
			Log.error(PlacesMonitorConstants.LOG_TAG,"Cannot process the location update, Received location result is null");
			return;
		}

		Location location = locations.get(0);
		if (location == null) {
			Log.error(PlacesMonitorConstants.LOG_TAG, "Cannot process the location update, Received location is null");
			return;
		}

		String locationLog = "Location Received: Accuracy: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " +
									location.getLongitude();
		Log.debug(PlacesMonitorConstants.LOG_TAG, locationLog);


		// Dispatch the given location to Places Extension
		LocalNotification.sendNotification("Monitor Location Update Received", locationLog);
		PlacesMonitorDispatcher.dispatchLocation(location);

	}

}
