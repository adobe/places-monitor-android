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
import android.support.v4.content.LocalBroadcastManager;


public class PlacesLocationBroadcastReceiver extends BroadcastReceiver {
	static final String ACTION_LOCATION_UPDATE =
		"com.adobe.marketing.mobile.PlacesLocationBroadcastReceiver.locationUpdates";

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

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"PlacesLocationBroadcastReceiver : Unable to process the location, context is null");
			return;
		}

		// change the action name of the intent to broadcast it to the internal class
		intent.setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION);
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
		Log.debug(PlacesMonitorConstants.LOG_TAG,
				  "PlacesLocationBroadcastReceiver : Broadcasting the obtained location to the PlacesMonitorInternal class");
		manager.sendBroadcast(intent);
	}

}
