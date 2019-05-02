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

import com.google.android.gms.location.GeofencingEvent;

public class PlacesGeofenceBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION_GEOFENCE_UPDATE =
            "com.adobe.marketing.mobile.PlacesGeofenceBroadcastReceiver.geofenceUpdates";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.error(PlacesMonitorConstants.LOG_TAG,"Cannot process the geofence trigger, The received intent from the geofence broadcast receiver is null.");
            return;
        }

        final String action = intent.getAction();
        if (!ACTION_GEOFENCE_UPDATE.equals(action)) {
            Log.error(PlacesMonitorConstants.LOG_TAG,"Cannot process the geofence trigger, Invalid action type received from geofence broadcast receiver.");
            return;
        }

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.error(PlacesMonitorConstants.LOG_TAG, "Cannot process the geofence trigger, Geofencing event has error. Ignoring region event.");
            return;
        }

        String regionEventType = getRegionTransitionType(geofencingEvent.getGeofenceTransition());
        if (PlacesMonitorConstants.EventDataKeys.GEOFENCE_TYPE_NONE.equals(regionEventType)) {
            Log.warning(PlacesMonitorConstants.LOG_TAG,
                    "Cannot process the geofence trigger, unknown Transition type.");
            return;
        }

        PlacesMonitorDispatcher.dispatchRegionEvent(geofencingEvent.getTriggeringGeofences(), regionEventType);
    }


    private static String getRegionTransitionType(int geofenceTransitionType) {
        switch (geofenceTransitionType) {
            case 1:
                return "entry";

            case 2:
                return "exit";

            default:
                return "none";
        }
    }

}
