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
				Log.debug("There was an error registering Places Monitoring Extension: %s", extensionError.getErrorName());
			}
		});
	}

	/**
	 * Start tracking the device's location and monitoring corresponding nearby POI's
	 *
	 */
	public static void start() {
		dispatchMonitorEvent(PlacesMonitorConstants.EVENTNAME_START);
	}

	/**
	 * Stop tracking the device's location and nearby POI's
	 */
	public static void stop() {
		dispatchMonitorEvent(PlacesMonitorConstants.EVENTNAME_STOP);
	}

	/**
	 * Immediately gets an update for the device's location
	 */
	public static void updateLocation() {
		dispatchMonitorEvent(PlacesMonitorConstants.EVENTNAME_UPDATE);
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
	private static void dispatchMonitorEvent(final String eventName) {

		final Event monitorEvent = new Event.Builder(eventName,
				PlacesMonitorConstants.EventType.MONITOR,
				PlacesMonitorConstants.EventSource.REQUEST_CONTENT).build();


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
}
