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

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.google.android.gms.location.LocationSettingsStates;

import static com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED;

public abstract class PlacesActivity extends Activity {

	// API >= 23 (Marshmallow)
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (requestCode == PlacesMonitorConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				// Permission granted
				PlacesMonitor.start();
			} else {
				// Permission denied.
				PlacesMonitor.stop();
			}
		}
	}

	// API < 23 (up to Lollipop)
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		final LocationSettingsStates states = LocationSettingsStates.fromIntent(intent);
		if (!states.isLocationUsable()) {
			// we don't have any way to get location data, so we should stop and return
			PlacesMonitor.stop();
			return;
		}

		if (requestCode == RESOLUTION_REQUIRED) {
			switch (resultCode) {
				case Activity.RESULT_OK:
					PlacesMonitor.start();
					break;
				case Activity.RESULT_CANCELED:
					PlacesMonitor.stop();
					break;
				default:
					PlacesMonitor.stop();
					break;
			}
		}
	}
}
