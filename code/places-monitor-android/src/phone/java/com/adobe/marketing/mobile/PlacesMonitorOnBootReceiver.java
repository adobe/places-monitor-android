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

package com.adobe.marketing.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * <p>
 * PlacesOnBootReceiver is used to receive the boot broadcast from the OS after the user has finished booting.
 * </p>
 */

public class PlacesMonitorOnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.debug(PlacesMonitorConstants.LOG_TAG, "PlacesOnBootReceiver#onReceive -> Calling updateLocation");
        PlacesMonitor.updateLocation();
    }
}
