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
// PlacesMonitor.java
//

package com.adobe.marketing.mobile;

public class PlacesMonitor {

	/**
	 * Returns the current version of the PlacesMonitor extension.
	 *
	 * @return A {@link String} representing the PlacesMonitor extension version
	 */
	public static String extensionVersion() {
		return PlacesMonitorConstants.EXTENSION_VERSION;
	}

	/**
	 * Registers the PlacesMonitor extension with the {@code MobileCore}.
	 * <p>
	 * This will allow the extension to send and receive events to and from the SDK.
	 */
	public static void registerExtension() {
		MobileCore.registerExtension(PlacesMonitorInternal.class, new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(ExtensionError extensionError) {
				Log.debug(PlacesMonitorConstants.LOG_TAG,"There was an error registering Places Monitoring Extension: %s. For more details refer to %s",
						extensionError.getErrorName(), PlacesMonitorConstants.DocLinks.REGISTER_PLACES_MONITOR);
			}
		});
	}

	/**
	 * This API sets the type of location permission request for which user will be prompted for PlacesMonitor.start().
	 * <p>
	 * Tip: This API is effective only for devices that are on Android 10 and above.
	 *
	 * To set the appropriate authorization prompt to be shown to the user, call this API before the PlacesMonitor.start()
	 * Calling this method, while actively monitoring will upgrade the location permission level to the requested permission value.
	 * If the requested authorization level is either already provided or denied by the application user,
	 * or if you attempt to downgrade permission from ALWAYS_ALLOW to WHILE_USING_APP, this method has no effect.
	 *
	 * {@link PlacesMonitorLocationPermission#ALWAYS_ALLOW} is the default location permission value.
	 *
	 * Location permission can be set to one of the following values:
	 * <ul>
	 *     <li>{@link PlacesMonitorLocationPermission#WHILE_USING_APP}:
	 *     This value prompts the user to access device location only while using the application.
	 *     An app is considered to be in use when the user is looking at the app on their device screen, for example an activity is running in the foreground.
	 *     Make sure ACCESS_FINE_LOCATION permission is set in the App's Manifest file.
	 *
	 *     Important: Geofences will not be registered with the operating system if the app user has granted the {@link PlacesMonitorLocationPermission#WHILE_USING_APP} permission.
	 *     As a result, the Places Monitor extension will not trigger entry/exit events on regions that are happening in the background.
	 *
	 *     <li>{@link PlacesMonitorLocationPermission#ALWAYS_ALLOW}:
	 *     This value prompts the user to access device location even when the application is backgrounded.
	 *     Make sure ACCESS_BACKGROUND_LOCATION and ACCESS_FINE_LOCATION permission is set in the App's Manifest file.
	 *
	 *     <li>{@link PlacesMonitorLocationPermission#NONE}:
	 * 	   Setting to NONE will not prompt the user to access device location. StartMonitoring API will have no effect until the prompt to access device location is made by
	 * 	   the application.
	 * </ul>
	 * @param placesMonitorLocationPermission the location permission value
	 * @deprecated use {@link #setRequestLocationPermission(PlacesMonitorLocationPermission)}()} instead.
	 */
	@Deprecated
	public static void setLocationPermission(final PlacesMonitorLocationPermission placesMonitorLocationPermission) {
		setRequestLocationPermission(placesMonitorLocationPermission);
	}


	/**
	 * This API sets the type of location permission request for which user will be prompted for PlacesMonitor.start().
	 * <p>
	 * Tip: This API is effective only for devices that are on Android 10 and above.
	 *
	 * To set the appropriate authorization prompt to be shown to the user, call this API before the PlacesMonitor.start()
	 * Calling this method, while actively monitoring will upgrade the location permission level to the requested permission value.
	 * If the requested authorization level is either already provided or denied by the application user,
	 * or if you attempt to downgrade permission from ALWAYS_ALLOW to WHILE_USING_APP, this method has no effect.
	 *
	 * {@link PlacesMonitorLocationPermission#ALWAYS_ALLOW} is the default location permission value.
	 *
	 * Location permission can be set to one of the following values:
	 * <ul>
	 *     <li>{@link PlacesMonitorLocationPermission#WHILE_USING_APP}:
	 *     This value prompts the user to access device location only while using the application.
	 *     An app is considered to be in use when the user is looking at the app on their device screen, for example an activity is running in the foreground.
	 *     Make sure ACCESS_FINE_LOCATION permission is set in the App's Manifest file.
	 *
	 *     Important: Geofences will not be registered with the operating system if the app user has granted the {@link PlacesMonitorLocationPermission#WHILE_USING_APP} permission.
	 *     As a result, the Places Monitor extension will not trigger entry/exit events on regions that are happening in the background.
	 *
	 *     <li>{@link PlacesMonitorLocationPermission#ALWAYS_ALLOW}:
	 *     This value prompts the user to access device location even when the application is backgrounded.
	 *     Make sure ACCESS_BACKGROUND_LOCATION and ACCESS_FINE_LOCATION permission is set in the App's Manifest file.
	 *
	 *     <li>{@link PlacesMonitorLocationPermission#NONE}:
	 * 	   Setting to NONE will not prompt the user to access device location. StartMonitoring API will have no effect until the prompt to access device location is made by
	 * 	   the application.
	 * </ul>
	 * @param placesMonitorLocationPermission the location permission value
	 *
	 */
	public static void setRequestLocationPermission(final PlacesMonitorLocationPermission placesMonitorLocationPermission) {
		EventData data = new EventData();
		String locationPermissionString = placesMonitorLocationPermission == null ? null :
				placesMonitorLocationPermission.getValue();
		data.putString(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION, locationPermissionString);
		dispatchMonitorEvent(PlacesMonitorConstants.EVENTNAME_SET_LOCATION_PERMISSION, data);
	}

	/**
	 * Start tracking the device's location and monitoring corresponding nearby POI's
	 *
	 */
	public static void start() {
		dispatchMonitorEvent(PlacesMonitorConstants.EVENTNAME_START, new EventData());
	}

	/**
	 * Stop tracking the device's location and nearby POI's.
	 * <p>
	 * Calling this method will stop tracking the customer's location.  Additionally, it will unregister
	 * all previously registered regions.  Optionally, you may purge client-side data by passing in YES for the clearData
	 * parameter.
	 *
	 * Calling this method with YES for clearData will purge the data even if the monitor is not actively tracking
	 * the device's location.
	 *
	 * @param clearData pass YES to clear all client-side Places data from the device
	 */
	public static void stop(final boolean clearData) {
		dispatchStopEvent(clearData);
	}

	/**
	 * Immediately gets an update for the device's location
	 */
	public static void updateLocation() {
		dispatchMonitorEvent(PlacesMonitorConstants.EVENTNAME_UPDATE, new EventData());
	}

	/**
	 * Dispatches an {@link Event} to {@link EventHub} for the places monitor extension to process.
	 * <ul>
	 * 		<li> EventType : {@link PlacesMonitorConstants.EventType#MONITOR} </li>
	 * 		<li> EventSource : {@link PlacesMonitorConstants.EventSource#REQUEST_CONTENT} </li>
	 * </ul>
	 *
	 * @param eventName The name of the {@link Event} being dispatched
	 */
	private static void dispatchMonitorEvent(final String eventName, final EventData eventData) {

		final Event monitorEvent = new Event.Builder(eventName,
				PlacesMonitorConstants.EventType.MONITOR,
				PlacesMonitorConstants.EventSource.REQUEST_CONTENT).setData(eventData).build();


		ExtensionErrorCallback<ExtensionError> extensionErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(final ExtensionError extensionError) {
				Log.error(PlacesMonitorConstants.LOG_TAG, String.format("An error occurred dispatching event '%s', %s",
						  monitorEvent.getName(), extensionError.getErrorName()));
			}
		};

		if (MobileCore.dispatchEvent(monitorEvent, extensionErrorCallback)) {
			Log.debug(PlacesMonitorConstants.LOG_TAG, String.format("Places Monitor dispatched an event '%s'",
					  monitorEvent.getName()));
		}
	}

	/**
	 * Dispatches an {@link Event} to {@link EventHub} for the places monitor extension to stop processing further location updates.
	 * <ul>
	 * 		<li> EventType : {@link PlacesMonitorConstants.EventType#MONITOR} </li>
	 * 		<li> EventSource : {@link PlacesMonitorConstants.EventSource#REQUEST_CONTENT} </li>
	 * </ul>
	 *
	 * @param clearData a boolean representing whether to clear all client-side Places data from the device
	 */
	private static void dispatchStopEvent(final boolean clearData) {
		EventData data = new EventData();
		data.putBoolean(PlacesMonitorConstants.EventDataKey.CLEAR, clearData);
		final Event stopEvent = new Event.Builder(PlacesMonitorConstants.EVENTNAME_STOP,
				PlacesMonitorConstants.EventType.MONITOR,
		PlacesMonitorConstants.EventSource.REQUEST_CONTENT).setData(data).build();


		ExtensionErrorCallback<ExtensionError> extensionErrorCallback = new ExtensionErrorCallback<ExtensionError>() {
			@Override
			public void error(final ExtensionError extensionError) {
				Log.error(PlacesMonitorConstants.LOG_TAG, String.format("An error occurred dispatching event '%s', %s",
						  stopEvent.getName(), extensionError.getErrorName()));
			}
		};

		if (MobileCore.dispatchEvent(stopEvent, extensionErrorCallback)) {
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Places Monitor dispatched stop event");
		}
	}
}
