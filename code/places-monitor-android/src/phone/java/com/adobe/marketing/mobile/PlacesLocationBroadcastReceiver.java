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
