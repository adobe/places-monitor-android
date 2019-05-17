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

final class PlacesMonitorConstants {
	static final int MONITOR_LOCATION_PERMISSION_REQUEST_CODE = 92847;

	static final String LOG_TAG = PlacesMonitor.class.getSimpleName();
	static final String EXTENSION_NAME = "com.adobe.placesMonitor";

	static final String EXTENSION_VERSION = "1.0.0";
	static final String CONTINUOUS_MONITORING = "continuousmonitoring";

	// event names for places monitor request content
	static final String EVENTNAME_START = "start monitoring";
	static final String EVENTNAME_STOP = "stop monitoring";
	static final String EVENTNAME_UPDATE = "update location";
	static final int NEARBY_GEOFENCES_COUNT = 20;

	static final String INTERNAL_INTENT_ACTION_LOCATION = "intentactionlocation";
	static final String INTERNAL_INTENT_ACTION_GEOFENCE = "intentactiongeofence";

	static final class Location {
		static final int REQUEST_INTERVAL = 3600;				// 1 hour
		static final int REQUEST_FASTEST_INTERVAL = 1800;    	// 30 minutes
		static final int REQUEST_SMALLEST_DISPLACEMENT = 1000;   // 1 kilometer

		private Location() {
		}
	}

	static final class EventSource {
		static final String RESPONSE_CONTENT 	= "com.adobe.eventsource.responsecontent";
		static final String REQUEST_CONTENT = "com.adobe.eventsource.requestcontent";
		static final String SHARED_STATE = "com.adobe.eventsource.sharedstate";

		private EventSource() {
		}
	}

	static final class EventDataKeys {

		static final String NEAR_BY_PLACES_LIST = "nearbyplaceslist";
		static final String REQUEST_TYPE = "requesttype";
		static final String REQUEST_TYPE_GET_NEARBY_PLACES = "requestgetnearbyplaces";
		static final String REQUEST_TYPE_PROCESS_REGION_EVENT = "requestprocessregionevent";

		static final String PLACES_COUNT = "count";
		static final String LATITUDE = "latitude";
		static final String LONGITUDE = "longitude";

		static final String GEOFENCE_TYPE_NONE  = "none";
		static final String GEOFENCE_TYPE_ENTRY = "entry";
		static final String GEOFENCE_TYPE_EXIT  = "exit";

		static final String REGION_ID = "regionid";
		static final String REGION_EVENT_TYPE = "regioneventtype";

		private EventDataKeys() {
		}
	}


	static final class EventType {
		static final String HUB = "com.adobe.eventtype.hub";
		static final String MONITOR = "com.adobe.eventtype.placesmonitor";
		static final String PLACES = "com.adobe.eventtype.places";

		private EventType() {
		}
	}

	static final class SharedState {
		static final String STATEOWNER = "stateowner";
		static final String CONFIGURATION = "com.adobe.module.configuration";
		static final String PLACES = "com.adobe.module.places";

		private SharedState() {
		}
	}

	static final class SharedPreference {
		static final String MONITORING_FENCES_KEY = "adb_monitoringFences";
		static final String USERWITHIN_GEOFENCES_KEY = "adb_userWithinGeofences";
		private SharedPreference() {
		}
	}

	private PlacesMonitorConstants() {
	}
}
