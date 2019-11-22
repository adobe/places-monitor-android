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
// PlacesLocationBroadcastReceiverTests.java
//


package com.adobe.marketing.mobile;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;


@RunWith(PowerMockRunner.class)
@PrepareForTest({LocationResult.class,  MobileCore.class})
public class PlacesLocationBroadcastReceiverTests {

	static final String ACTION_LOCATION_UPDATE =
		"com.adobe.marketing.mobile.PlacesLocationBroadcastReceiver.locationUpdates";

	// argument captors
	final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
	final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

	private PlacesLocationBroadcastReceiver receiver;
	private LocationResult locationResult;

	@Mock
	Context mockContext;

	@Mock
	Intent mockIntent;

	@Mock
	Location mockLocation1, mockLocation2;

	@Before
	public void before() throws Exception {
		PowerMockito.mockStatic(MobileCore.class);
		when(mockIntent.getAction()).thenReturn(ACTION_LOCATION_UPDATE);
		receiver = new PlacesLocationBroadcastReceiver();
	}

	// ========================================================================================
	// onReceive - Location Update
	// ========================================================================================

	@Test
	public void test_OnReceive_sendsOSEvent() throws Exception {
		// setup
		initiateLocationMocking();

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify the OS event dispatch
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_OS_LOCATION_UPDATE,
					 event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.OS, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT,
					 event.getSource());
		// evaluate the eventData
		EventData eventData = event.getData();
		assertEquals("the event data should contain two element", 3, eventData.size());
		assertEquals("the event data should contain the correct  event type",
					 PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_UPDATE,
					 eventData.getString2(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE));
		assertEquals("the event data should contain the correct latitude", 33.33,
					 eventData.getDouble(PlacesMonitorConstants.EventDataKey.LATITUDE), 0);
		assertEquals("the event data should contain the correct longitude", 22.22,
					 eventData.getDouble(PlacesMonitorConstants.EventDataKey.LONGITUDE), 0);
	}

	@Test
	public void test_OnReceive_when_intentIsNull() throws Exception {
		// setup
		initiateLocationMocking();

		// test
		receiver.onReceive(mockContext, null);

		// verify OS event is not dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}

	@Test
	public void test_OnReceive_when_intentHasDifferentAction() throws Exception {
		// setup
		initiateLocationMocking();
		when(mockIntent.getAction()).thenReturn("unknownAction");

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify OS event is not dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}


	@Test
	public void test_OnReceive_when_locationListIsEmpty() throws Exception {
		// setup
		List<Location> locationList = new ArrayList<>();
		locationResult = LocationResult.create(locationList);
		PowerMockito.mockStatic(LocationResult.class);
		PowerMockito.when(LocationResult.class, "extractResult", any(Intent.class)).thenReturn(locationResult);

		// test
		receiver.onReceive(null, mockIntent);

		// verify OS event is not dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}

	@Test
	public void test_OnReceive_when_locationIsNull() throws Exception {
		// setup
		List<Location> locationList = new ArrayList<>();
		locationList.add(null);
		locationResult = LocationResult.create(locationList);

		PowerMockito.mockStatic(LocationResult.class);
		PowerMockito.when(LocationResult.class, "extractResult", any(Intent.class)).thenReturn(locationResult);

		// test
		receiver.onReceive(null, mockIntent);

		// verify OS event is not dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}


	@Test
	public void test_OnReceive_when_locationListIsNull() throws Exception {
		// setup
		locationResult = LocationResult.create(null);

		// test
		receiver.onReceive(null, mockIntent);

		// verify OS event is not dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}

	private void initiateLocationMocking() throws Exception {
		// static mocks
		when(mockLocation1.getLatitude()).thenReturn(33.33);
		when(mockLocation1.getLongitude()).thenReturn(22.22);
		when(mockLocation2.getLatitude()).thenReturn(44.44);
		when(mockLocation2.getLongitude()).thenReturn(55.55);

		List<Location> locationList = new ArrayList<>();
		locationList.add(mockLocation1);
		locationList.add(mockLocation2);
		locationResult = LocationResult.create(locationList);

		PowerMockito.mockStatic(LocationResult.class);
		PowerMockito.when(LocationResult.class, "extractResult", any(Intent.class)).thenReturn(locationResult);
	}

}
