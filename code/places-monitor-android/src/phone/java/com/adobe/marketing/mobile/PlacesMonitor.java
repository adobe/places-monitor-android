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

public class PlacesMonitor {

	/**
	 * Returns the current version of the PlacesMonitor Extension
	 *
	 * @return A {@link String} representing the Places Monitor Extension version
	 */
	public static String extensionVersion() {
		return PlacesMonitorConstants.EXTENSION_VERSION;
	}

	/**
	 * Registers the Places Monitor extension with the {@code MobileCore}
	 * <p>
	 * This will allow the extension to send and receive events to and from the SDK.
	 *
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
	 * Start tracking the device's location and monitoring their nearby Places
	 *
	 */
	public static void start() {
		dispatchMonitorEvent(PlacesMonitorConstants.EVENTNAME_START);
	}

	/**
	 * Stop tracking the device's location
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
