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
