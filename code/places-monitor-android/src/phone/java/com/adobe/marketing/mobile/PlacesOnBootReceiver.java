package com.adobe.marketing.mobile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * <p>
 * PlacesOnBootReceiver is used to receive the boot broadcast from the OS after the user has finished booting.
 * </p>
 */

public class PlacesOnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.debug(PlacesMonitorConstants.LOG_TAG, "PlacesOnBootReceiver#onReceive -> Calling updateLocation");
        PlacesMonitor.updateLocation();
    }
}
