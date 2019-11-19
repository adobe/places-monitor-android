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
// PlacesMonitorOnBootReceiver.java
//

package com.adobe.marketing.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Broadcast receiver for the device boot event.
 * <p>
 * The {@link #onReceive(Context, Intent)} method of this class is called when the device has finished booting.
 * This broadcast receiver attempts to restore the geofences after the Android device is rebooted.
 * Geofence tracking or the location tracking will not be restarted, if the PlacesMonitor hasn't been started
 * or if the privacy status is opted out.
 *
 * @see PlacesMonitor#start()
 */

public class PlacesMonitorOnBootReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.debug(PlacesMonitorConstants.LOG_TAG, "Places monitor received OS boot event attempting to update location.");
		PlacesMonitor.updateLocation();
	}
}
