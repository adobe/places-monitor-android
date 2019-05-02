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
// PlacesMonitorDispatcher.java
//

package com.adobe.marketing.mobile;
import android.location.Location;
import com.google.android.gms.location.Geofence;

import java.util.List;

class PlacesMonitorDispatcher {

    static void dispatchLocation(final Location location) {
        if(location == null) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Location is null, Ignoring to dispatch Places Monitor Location event");
            return;
        }

        // create event data
        EventData eventData = new EventData();
        eventData.putDouble(PlacesMonitorConstants.EventDataKeys.LATITUDE, location.getLatitude());
        eventData.putDouble(PlacesMonitorConstants.EventDataKeys.LONGITUDE, location.getLongitude());
        eventData.putInteger(PlacesMonitorConstants.EventDataKeys.PLACES_COUNT, PlacesMonitorConstants.NEARBY_GEOFENCES_COUNT);
        eventData.putString(PlacesMonitorConstants.EventDataKeys.REQUEST_TYPE,
                PlacesMonitorConstants.EventDataKeys.REQUEST_TYPE_GET_NEARBY_PLACES);

        final Event event = new Event.Builder("Places Monitor Location Event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                .setData(eventData)
                .build();

        ExtensionErrorCallback<ExtensionError> extensionErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                if (extensionError != null) {
                    Log.error(PlacesMonitorConstants.LOG_TAG, String.format("An error occurred dispatching event '%s', %s", event.getName(),
                            extensionError.getErrorName()));
                }
            }
        };

        MobileCore.dispatchEvent(event, extensionErrorCallback);
    }

    static void dispatchRegionEvent(final List<Geofence> geofences, final String regionEventType) {
        if (geofences == null || geofences.isEmpty()) {
            Log.warning(PlacesMonitorConstants.LOG_TAG, "Geofences array is null/empty, Ignoring to dispatch Places Monitor Geofence event");
            return;
        }

        for (Geofence geofence : geofences) {
            if (geofence == null) {
                continue;
            }

            EventData eventData = new EventData();
            eventData.putString(PlacesMonitorConstants.EventDataKeys.REGION_ID, geofence.getRequestId());
            eventData.putString(PlacesMonitorConstants.EventDataKeys.REGION_EVENT_TYPE, regionEventType);
            eventData.putString(PlacesMonitorConstants.EventDataKeys.REQUEST_TYPE,
                    PlacesMonitorConstants.EventDataKeys.REQUEST_TYPE_PROCESS_REGION_EVENT);

            Log.debug(PlacesMonitorConstants.LOG_TAG, "Places Monitor recorded a geofence event " + regionEventType +
                    " for id " + geofence.getRequestId());
            // TODO : Remove before release
            LocalNotification.sendNotification("Places Monitor Geofence event","Recorded a geofence event " + regionEventType +
                    " for id " + geofence.getRequestId());
            final Event event = new Event.Builder("Places Monitor Region Event", EventType.PLACES, EventSource.REQUEST_CONTENT)
                    .setData(eventData)
                    .build();

            ExtensionErrorCallback<ExtensionError> extensionErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
                @Override
                public void error(final ExtensionError extensionError) {
                    if (extensionError != null){
                        Log.error(PlacesMonitorConstants.LOG_TAG, String.format("An error occurred dispatching event '%s', %s", event.getName(),
                                extensionError.getErrorName()));
                    }
                }
            };
            MobileCore.dispatchEvent(event, extensionErrorCallback);
        }
    }
}
