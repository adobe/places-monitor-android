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
// PlacesMonitorConstants.java
//

package com.adobe.marketing.mobile;

final class PlacesMonitorConstants {
	static final int MONITOR_PERMISSION_MASK = 0xAD00;
	static final int MONITOR_LOCATION_PERMISSION_REQUEST_CODE = MONITOR_PERMISSION_MASK | 1;

	static final String LOG_TAG = PlacesMonitor.class.getSimpleName();
	static final String EXTENSION_NAME = "com.adobe.placesMonitor";

	static final String EXTENSION_VERSION = "2.2.3";

	// event names for places monitor request content
	static final String EVENTNAME_START = "start monitoring";
	static final String EVENTNAME_STOP = "stop monitoring";
	static final String EVENTNAME_UPDATE = "update location now";
	static final String EVENTNAME_SET_LOCATION_PERMISSION = "set location permission";
	static final String EVENTNAME_OS_PERMISSION_CHANGE = "OS Permission change";
	static final String EVENTNAME_OS_GEOFENCE_TRIGGER = "OS Geofence Trigger";
	static final String EVENTNAME_OS_LOCATION_UPDATE = "OS Location update";

	static final int NEARBY_GEOFENCES_COUNT = 20;

	static final class Location {
		static final int REQUEST_INTERVAL = 3600;				// 1 hour
		static final int REQUEST_FASTEST_INTERVAL = 1800;    	// 30 minutes
		static final int REQUEST_SMALLEST_DISPLACEMENT = 1000;   // 1 kilometer

		private Location() {
		}
	}

	static final class EventSource {
		static final String RESPONSE_CONTENT = "com.adobe.eventsource.responsecontent";
		static final String REQUEST_CONTENT = "com.adobe.eventsource.requestcontent";
		static final String SHARED_STATE = "com.adobe.eventsource.sharedstate";


		private EventSource() {
		}
	}

	static final class EventDataKey {
		static final String CLEAR = "clearclientdata";
		static final String LOCATION_PERMISSION = "locationpermission";

		static final String OS_EVENT_TYPE = "oseventtype";
		static final String LATITUDE = "latitude";
		static final String LONGITUDE = "longitude";
		static final String GEOFENCE_IDS = "geofenceIds";
		static final String GEOFENCE_TRANSITION_TYPE = "transitiontype";
		static final String LOCATION_PERMISSION_STATUS = "locationpermissionstatus";
		private EventDataKey() {
		}
	}


	static final class EventDataValue {
		static final String OS_EVENT_TYPE_LOCATION_UPDATE = "locationupdate";
		static final String OS_EVENT_TYPE_GEOFENCE_TRIGGER = "geofencetrigger";
		static final String OS_EVENT_TYPE_LOCATION_PERMISSION_CHANGE = "locationpermissionchange";
		static final String OS_LOCATION_PERMISSION_STATUS_GRANTED = "granted";
		static final String OS_LOCATION_PERMISSION_STATUS_DENIED = "denied";
		private EventDataValue() {
		}
	}


	static final class EventType {
		static final String HUB = "com.adobe.eventtype.hub";
		static final String OS = "com.adobe.eventtype.os";
		static final String MONITOR = "com.adobe.eventtype.placesmonitor";

		private EventType() {
		}
	}

	static final class SharedState {
		static final String STATEOWNER = "stateowner";
		static final String CONFIGURATION = "com.adobe.module.configuration";
		private SharedState() {
		}
	}

	static final class  SharedPreference {
		static final String MASTER_KEY = "com.adobe.placesMonitor";
		static final String HAS_LOCATION_DIALOG_PROMPTED = "adb_hasLocationDialogPrompted";
		static final String USERWITHIN_GEOFENCES_KEY = "adb_userWithinGeofences";
		static final String HAS_MONITORING_STARTED_KEY = "adb_hasMonitoringStarted";
		static final String LOCATION_PERMISSION_KEY = "adb_locationPermission";
		private SharedPreference() {
		}
	}

	static final class DocLinks {
		static final String REGISTER_PLACES_MONITOR = "https://docs.adobe.com/content/help/en/places/using/places-ext-aep-sdks/places-monitor-extension/places-monitor-api-reference.html#registerextension-android";
		static final String SET_LOCATION_PERMISSION = "https://docs.adobe.com/help/en/places/using/configure-places-with-sdk/places-monitor-extension/places-monitor-api-reference.html#setlocationpermission-android";
		static final String START_MONITOR = "https://docs.adobe.com/help/en/places/using/configure-places-with-sdk/places-monitor-extension/places-monitor-api-reference.html#start-android";
	}

	private PlacesMonitorConstants() {
	}
}
