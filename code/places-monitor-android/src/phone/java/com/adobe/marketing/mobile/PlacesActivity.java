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
