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
// PlacesMonitorLocationPermission.java
//

package com.adobe.marketing.mobile;

/**
 * Represents the possible location permission settings for Android.
 * <p>
 * Apps that use location services must request location permissions. On a device that runs Android 10 (API level 29) or higher
 * users see the dialog to indicate that your app is requesting a location permission.
 * <p>
 * Before Android 10 location permission was a binary choice, either allow or deny access to their device location. If the user has provided the access location,
 * the app could get location information both in foreground as well as background.
 * <p>
 * Android 10 have added the ability for users to control which apps can access their device location when they're not using the app.
 * A new permission "Allow only while using the app" is added.
 */
public enum PlacesMonitorLocationPermission {

	/**
	 * Permission for Places Monitor to access location while using application.
	 * An app is considered to be in use when the user is looking at the app on their device screen (i.e) an activity is running in the foreground.
	 * <p>
	 * Important:  Geofences will not get registered with the Operating system if the app user has granted "While using app" permission.
	 * "Always Allow" permission is mandatory for registering and getting the entry/exit triggers on the geofence from the OS.
	 */
	WHILE_USING_APP("whileusingapp"),

	/**
	 * Permission for Places Monitor to access location in foreground and background.
	 */
	ALWAYS_ALLOW("alwaysAllow"),

	/**
	 * On Choosing NONE, Places Monitor extension will not attempt to request for location permissions when startMonitoring API is called for the first time.
	 */
	NONE("none");

	private final String value;

	PlacesMonitorLocationPermission(final String value) {
		this.value = value;
	}

	/**
	 * Returns the string value for this enum type.
	 * @return the string name for this enum type.
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Returns a {@link PlacesMonitorLocationPermission} object based on the provided {@code text}.
	 * <p>
	 * If the text provided is not valid, {@link #ALWAYS_ALLOW} will be returned.
	 *
	 * @param text {@link String} to be converted to a {@code PlacesMonitorLocationPermission} object
	 * @return {@code PlacesMonitorLocationPermission} object equivalent to the provided text
	 */
	static PlacesMonitorLocationPermission fromString(final String text) {
		for (PlacesMonitorLocationPermission b : PlacesMonitorLocationPermission.values()) {
			if (b.value.equalsIgnoreCase(text)) {
				return b;
			}
		}

		return ALWAYS_ALLOW;
	}
}
