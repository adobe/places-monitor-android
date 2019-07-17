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
// PlacesGeofenceBroadcastReceiverTests.java
//

package com.adobe.marketing.mobile;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;


import com.google.android.gms.location.GeofencingEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GeofencingEvent.class, LocalBroadcastManager.class})
public class PlacesGeofenceBroadcastReceiverTests {

	static final String ACTION_GEOFENCE_UPDATE =
		"com.adobe.marketing.mobile.PlacesGeofenceBroadcastReceiver.geofenceUpdates";
	private PlacesGeofenceBroadcastReceiver receiver;

	@Mock
	Context mockContext;

	@Mock
	Intent mockIntent;

	@Mock
	LocalBroadcastManager mockBroadcastManager;

	@Before
	public void before() throws Exception {
		receiver = new PlacesGeofenceBroadcastReceiver();
		when(mockIntent.getAction()).thenReturn(ACTION_GEOFENCE_UPDATE);
	}

	// ========================================================================================
	// onReceive
	// ========================================================================================


	@Test
	public void test_OnReceive() throws Exception {
		// setup
		initiateMocking();

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify
		verify(mockBroadcastManager, times(1)).sendBroadcast(mockIntent);
		verify(mockIntent, times(1)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);
	}

	@Test
	public void test_OnReceive_when_intentIsNull() throws Exception {
		// setup
		initiateMocking();

		// test
		receiver.onReceive(mockContext, null);

		// verify
		verify(mockBroadcastManager, times(0)).sendBroadcast(mockIntent);
		verify(mockIntent, times(0)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);
	}

	@Test
	public void test_OnReceive_when_intentHasDifferentAction() throws Exception {
		// setup
		initiateMocking();
		when(mockIntent.getAction()).thenReturn("unknownAction");

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify
		verify(mockBroadcastManager, times(0)).sendBroadcast(mockIntent);
		verify(mockIntent, times(0)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);
	}


	@Test
	public void test_OnReceive_when_contextIsNull() throws Exception {
		// setup
		initiateMocking();

		// test
		receiver.onReceive(null, mockIntent);

		// verify
		verify(mockBroadcastManager, times(0)).sendBroadcast(mockIntent);
		verify(mockIntent, times(0)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);
	}

	private void initiateMocking() throws Exception {
		// static mocks
		PowerMockito.mockStatic(LocalBroadcastManager.class);
		PowerMockito.when(LocalBroadcastManager.class, "getInstance", any(Context.class)).thenReturn(mockBroadcastManager);
		when(mockIntent.getAction()).thenReturn(ACTION_GEOFENCE_UPDATE);
	}
}
