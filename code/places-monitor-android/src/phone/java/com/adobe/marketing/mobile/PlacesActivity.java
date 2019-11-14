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
// PlacesActivity.java
//

package com.adobe.marketing.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.WindowManager;
import com.google.android.gms.location.LocationSettingsStates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED;

public class PlacesActivity extends Activity {

	private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
	private static final String BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
	private static final String INTENT_PERMISSION_KEY = "intent permission key";

	/**
	 * Checks if permission to access fine location while app is in use has been granted.
	 * <ol>
	 *   <li> Returns true for the devices running on versions below Android M, Since Runtime permission not required.</li>
	 *   <li> Returns true if the permission for using fine location is already granted. </li>
	 *   <li> Returns false if the permission for using fine location is not granted or if the app context is null.</li>
	 * </ol>
	 *
	 * @return Returns {@code boolean} representing the permission to monitor fine location when app is in use
	 */
	public static boolean isWhileInUsePermissionGranted() {
		// for version below API 23, need not check permissions
		if (!isRuntimePermissionRequired()) {
			return true;
		}

		// get hold of the app context. bail out if null
		Context context = App.getAppContext();

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to check location permission, App context is not available");
			return false;
		}

		// verify the permission for fine location
		int permissionState = ActivityCompat.checkSelfPermission(context, FINE_LOCATION);
		return permissionState == PackageManager.PERMISSION_GRANTED;
	}

	/**
	 * Checks if permission to access fine location while the app is in background has been granted.
	 * <ol>
	 *   <li> Returns true for the devices running on versions below Android M, Since Runtime permission not required.</li>
	 *   <li> Returns true if the permission for using fine location in background is already granted. </li>
	 *   <li> Returns false if the permission for using fine location in background is not granted or if the app context is null.</li>
	 * </ol>
	 *
	 * @return Returns {@code boolean} representing the permission to monitor fine location in background
	 */
	public static boolean isBackgroundPermissionGranted() {
		// for version below API 23, need not check permissions
		if (!isRuntimePermissionRequired()) {
			return true;
		}

		// get hold of the app context. bail out if null
		Context context = App.getAppContext();

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to check location permission, App context is not available");
			return false;
		}

		// verify the permission for fine location
		int permissionState = ActivityCompat.checkSelfPermission(context, BACKGROUND_LOCATION);
		return permissionState == PackageManager.PERMISSION_GRANTED;
	}


	/**
	 * Request permission to access fine location from the user.
	 * <ol>
	 *   <li> Call to this method does nothing for devices running on versions below Android M, Since Runtime permission not required.</li>
	 *   <li> Does not invoke the permission dialog if the app context is null</li>
	 * </ol>
	 *
	 * @param locationPermission The location permission setting that the user will be prompted for, could be {@link PlacesMonitorLocationPermission#WHILE_USING_APP} or {@link PlacesMonitorLocationPermission#ALWAYS_ALLOW}
	 */
	public static void askPermission(final PlacesMonitorLocationPermission locationPermission) {
		// for version below API 23, need not ask permission
		if (!isRuntimePermissionRequired()) {
			return;
		}

		// get hold of the app context. bail out if null
		Context context = App.getAppContext();

		if (context == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "Unable to request permission, App context is null");
			return;
		}

		// start a new task activity
		Intent intent = new Intent(context, PlacesActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(INTENT_PERMISSION_KEY, locationPermission);
		context.startActivity(intent);
	}

	// ========================================================================================
	// Activity class overridden methods
	// ========================================================================================

	/**
	 * Lifecycle method for {@link PlacesActivity}
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// make the activity not interactable
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

		if(getIntent() == null || getIntent().getExtras() == null){
			Log.debug(PlacesMonitorConstants.LOG_TAG, "Cannot request permission. PlacesActivity Intent is null");
		}
		PlacesMonitorLocationPermission locationPermission = (PlacesMonitorLocationPermission) getIntent().getExtras().get(INTENT_PERMISSION_KEY);

		boolean shouldProvideRationale =
			ActivityCompat.shouldShowRequestPermissionRationale(this,
					FINE_LOCATION);

		if (shouldProvideRationale) {
			onShowRationale(locationPermission);
		} else {
			ActivityCompat.requestPermissions(this, getPermissionArray(locationPermission),
											  PlacesMonitorConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE);
		}
	}

	/**
	 * Permission request callback for devices running on Android M and above versions.
	 */
	// API >= 23 (Marshmallow)
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode != PlacesMonitorConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE) {
			onInvalidRequestCode();
			return;
		}

		if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			onPermissionGranted();
		} else {
			onPermissionDenied();
		}
	}


	/**
	 * Permission request callback for devices running below Android M versions.
	 */
	// API < 23 (up to Lollipop)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		final LocationSettingsStates states = LocationSettingsStates.fromIntent(intent);

		if (!states.isLocationUsable()) {
			// we don't have any way to get location data, so we should stop and return
			onPermissionDenied();
			return;
		}

		if (requestCode == RESOLUTION_REQUIRED) {
			switch (resultCode) {
				case Activity.RESULT_OK:
					onPermissionGranted();
					break;

				case Activity.RESULT_CANCELED:
					onPermissionDenied();
					break;

				default:
					onPermissionDenied();
					break;
			}
		}
	}



	// ========================================================================================
	// Helper Method
	// ========================================================================================

	/**
	 * Helper method to verify if the device supports runtime permission.
	 *
	 */
	private static boolean isRuntimePermissionRequired() {
		return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
	}

	private static String[] getPermissionArray(PlacesMonitorLocationPermission placesMonitorLocationPermission) {
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(FINE_LOCATION);
		if (PlacesMonitorLocationPermission.ALWAYS_ALLOW == placesMonitorLocationPermission) {
			permissionList.add(BACKGROUND_LOCATION);
		}
		return permissionList.toArray(new String[permissionList.size()]);
	}

	// ========================================================================================
	// Permission Result Handlers
	// ========================================================================================

	/**
	 * Permission handler method called when user has given permission to access fine location.
	 */
	private void onPermissionGranted() {
		dispatchPermissionChangeOSEvent(PlacesMonitorConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_GRANTED);
		finish();
	}

	/**
	 * Permission handler method called when user has denied permission to access fine location.
	 */
	private void onPermissionDenied() {
		dispatchPermissionChangeOSEvent(PlacesMonitorConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_DENIED);
		finish();
	}

	/**
	 * Permission handler method called when permission request code obtained doesn't match with permission request code
	 * provided while requesting permission.
	 *
	 */
	private void onInvalidRequestCode() {
		finish();
	}

	/**
	 * Permission handler method for showing rationale.
	 * <p>
	 * This method is called when {@link ActivityCompat#shouldShowRequestPermissionRationale(Activity, String)} returns "true",
	 * which mean when the application was launched earlier and user had "denied" the permission in last launch WITHOUT checking "never show again".
	 */
	private void onShowRationale(PlacesMonitorLocationPermission locationPermission) {
		Log.debug(PlacesMonitorConstants.LOG_TAG, "Permission not granted on the first attempt. PlacesMonitor extension " +
				  "doesn't support showing rationale. Requesting permission again");
		ActivityCompat.requestPermissions(this,
				  						  getPermissionArray(locationPermission),
										  PlacesMonitorConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE);
	}

	/**
	 * Dispatch an OS event to eventHub, indicating the change in location permission
	 *
	 * @param status A {@link String} value representing the status of the changed permission.
	 */
	private void dispatchPermissionChangeOSEvent(final String status) {
		// create eventData
		HashMap<String,Object> eventData = new HashMap<>();
		eventData.put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE, PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_PERMISSION_CHANGE);
		eventData.put(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION_STATUS, status);

		// dispatch OS event
		Event event = new Event.Builder(PlacesMonitorConstants.EVENTNAME_OS_PERMISSION_CHANGE,PlacesMonitorConstants.EventType.OS, PlacesMonitorConstants.EventSource.RESPONSE_CONTENT).
				setEventData(eventData).build();
		MobileCore.dispatchEvent(event, null);
	}

}

