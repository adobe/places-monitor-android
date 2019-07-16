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
import android.support.v4.content.LocalBroadcastManager;


public class PlacesGeofenceBroadcastReceiver extends BroadcastReceiver {
	static final String ACTION_GEOFENCE_UPDATE =
		"com.adobe.marketing.mobile.PlacesGeofenceBroadcastReceiver.geofenceUpdates";

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

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesGeofenceBroadcastReceiver : Unable to process the geofence trigger, context is null");
			return;
		}

		// change the action name of the intent to broadcast it to the internal class
		intent.setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
		Log.debug(PlacesMonitorConstants.LOG_TAG,
				  "PlacesGeofenceBroadcastReceiver : Broadcasting the obtained geofence trigger to the PlacesMonitorInternal class");
		manager.sendBroadcast(intent);
	}

}