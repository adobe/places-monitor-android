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

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GeofencingEvent.class, MobileCore.class, GeofencingEvent.class})
public class PlacesGeofenceBroadcastReceiverTests {

	static final String ACTION_GEOFENCE_UPDATE =
		"com.adobe.marketing.mobile.PlacesGeofenceBroadcastReceiver.geofenceUpdates";
	private PlacesGeofenceBroadcastReceiver receiver;

	// argument captors
	final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
	final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

	@Mock
	Context mockContext;

	@Mock
	Intent mockIntent;

	@Mock
	GeofencingEvent mockGeofencingEvent;


	@Before
	public void before() throws Exception {
		// mock the static classes
		PowerMockito.mockStatic(MobileCore.class);
		PowerMockito.mockStatic(GeofencingEvent.class);

		when(mockIntent.getAction()).thenReturn(ACTION_GEOFENCE_UPDATE);

		receiver = new PlacesGeofenceBroadcastReceiver();

		when(mockIntent.getAction()).thenReturn(ACTION_GEOFENCE_UPDATE);
	}

	// ========================================================================================
	// onReceive
	// ========================================================================================


	@Test
	public void test_OnReceive_sendOSEvent() throws Exception {
		// setup
		mockGeofenceWithCount(2);

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify the OS event dispatch
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_OS_GEOFENCE_TRIGGER,
					 event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.OS, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT,
					 event.getSource());
		// evaluate the eventData
		EventData eventData = event.getData();
		assertEquals("the event data should contain two element", 3, eventData.size());
		assertEquals("the event data should contain the correct  event type",
					 PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_GEOFENCE_TRIGGER,
					 eventData.getString2(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE));
		assertEquals("the event data should contain the correct  geofence transition type", Geofence.GEOFENCE_TRANSITION_EXIT,
					 eventData.getInteger(PlacesMonitorConstants.EventDataKey.GEOFENCE_TRANSITION_TYPE));

		// evaluate the geofenceIds
		List<String> obtainedGeofenceIds = eventData.getStringList(PlacesMonitorConstants.EventDataKey.GEOFENCE_IDS);
		assertEquals("the event data should contain the correct geofence ids count", 2,  obtainedGeofenceIds.size());
		assertEquals("the event data should contain the correct geofence ids", "id0",  obtainedGeofenceIds.get(0));
		assertEquals("the event data should contain the correct geofence ids", "id1",  obtainedGeofenceIds.get(1));
	}


	@Test
	public void test_OnReceive_when_GeofenceEventHasError() throws Exception {
		// setup
		Mockito.when(mockGeofencingEvent.hasError()).thenReturn(true);
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify no event is dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}


	@Test
	public void test_OnReceive_when_noObtainedGeofence() throws Exception {
		// setup
		mockGeofenceWithCount(0);

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify no event is dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}

	@Test
	public void test_OnReceive_when_intentIsNull() throws Exception {
		// setup

		// test
		receiver.onReceive(mockContext, null);

		// verify no event is dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}

	@Test
	public void test_OnReceive_when_intentHasDifferentAction() throws Exception {
		// setup
		when(mockIntent.getAction()).thenReturn("unknownAction");

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify no event is dispatched
		verifyStatic(MobileCore.class, Mockito.times(0));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());
	}


	private void mockGeofenceWithCount(int count) throws Exception {
		List<Geofence> obtainedGeofence = new ArrayList<>();

		for (int i = 0; i < count; i++) {
			Geofence geofence = new Geofence.Builder().setRequestId("id" + i).setTransitionTypes(
				Geofence.GEOFENCE_TRANSITION_EXIT).setCircularRegion(22.33, -33.33,
						100).setExpirationDuration(Geofence.NEVER_EXPIRE).build();
			obtainedGeofence.add(geofence);
		}


		Mockito.when(mockGeofencingEvent.getTriggeringGeofences()).thenReturn(obtainedGeofence);
		Mockito.when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
	}

}
