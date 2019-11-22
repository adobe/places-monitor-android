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
// PlacesMonitorTests.java
//

package com.adobe.marketing.mobile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.*;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class PlacesMonitorTests {

	@Before
	public void before() {
		PowerMockito.mockStatic(MobileCore.class);
	}

	// ========================================================================================
	// extensionVersion
	// ========================================================================================

	@Test
	public void test_extensionVersionAPI() {
		// test
		String extensionVersion = PlacesMonitor.extensionVersion();
		assertEquals("The Extension version API returns the correct value", PlacesMonitorTestConstants.EXTENSION_VERSION,
					 extensionVersion);
	}

	// ========================================================================================
	// registerExtension
	// ========================================================================================

	@Test
	public void test_registerExtensionAPI() {
		// test
		PlacesMonitor.registerExtension();
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// The monitor extension should register with core
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.registerExtension(eq(PlacesMonitorInternal.class), callbackCaptor.capture());

		// verify the callback
		ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
		assertNotNull("The extension callback should not be null", extensionErrorCallback);

		// should not crash on calling the callback
		extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
	}


	// ========================================================================================
	// start
	// ========================================================================================

	@Test
	public void test_startAPI() {
		// setup
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		// setup argument captors
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		PlacesMonitor.start();

		// The start event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_START, event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.MONITOR, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT,
					 event.getSource());
	}

	// ========================================================================================
	// stop
	// ========================================================================================

	@Test
	public void test_stopAPI_withClearData() {
		// setup
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		// setup argument captors
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		PlacesMonitor.stop(true);

		// The start event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_STOP, event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.MONITOR, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT,
					 event.getSource());
		// verify eventData
		Map<String, Object> data = event.getEventData();
		assertEquals(true, data.get(PlacesMonitorTestConstants.EventDataKey.CLEAR));
	}


	@Test
	public void test_stopAPI_withOutClearData() {
		// setup
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		// setup argument captors
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		PlacesMonitor.stop(false);

		// The start event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_STOP, event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.MONITOR, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT,
					 event.getSource());
		// verify eventData
		Map<String, Object> data = event.getEventData();
		assertEquals(false, data.get(PlacesMonitorTestConstants.EventDataKey.CLEAR));
	}


	// ========================================================================================
	// updateLocation
	// ========================================================================================

	@Test
	public void test_updateLocationAPI() {
		// setup
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		// setup argument captors
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		PlacesMonitor.updateLocation();

		// The start event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_UPDATE, event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.MONITOR, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT,
					 event.getSource());
	}


	// ========================================================================================
	// setLocationPermission
	// ========================================================================================

	@Test
	public void test_setLocationPermission() {
		// setup
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		// setup argument captors
		final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		PlacesMonitor.setLocationPermission(PlacesMonitorLocationPermission.WHILE_USING_APP);

		// The start event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_SET_LOCATION_PERMISSION,
					 event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.MONITOR, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT,
					 event.getSource());
		assertEquals("the event data size should be correct", 1, event.getEventData().size());
		assertEquals("the event data should be correct", PlacesMonitorLocationPermission.WHILE_USING_APP.getValue(),
					 event.getEventData().get(PlacesMonitorTestConstants.EventDataKey.LOCATION_PERMISSION));
	}

	// ========================================================================================
	// dispatchEventCallback
	// ========================================================================================

	@Test
	public void test_dispatchEventCallback_When_validError() {
		// setup
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		// setup argument captors
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		PlacesMonitor.start();

		// The start event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(any(Event.class), callbackCaptor.capture());


		// verify dispatch event error callback
		ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
		assertNotNull("The dispatch event error callback should not be null", extensionErrorCallback);

		// should not crash or throw exception on calling the callback
		extensionErrorCallback.error(ExtensionError.BAD_NAME);
	}


	@Test
	public void test_dispatchStopEvent_When_validError() {
		// setup
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		// setup argument captors
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		PlacesMonitor.stop(false);

		// The start event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(any(Event.class), callbackCaptor.capture());


		// verify dispatch event error callback
		ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
		assertNotNull("The dispatch event error callback should not be null", extensionErrorCallback);

		// should not crash or throw exception on calling the callback
		extensionErrorCallback.error(ExtensionError.BAD_NAME);
	}

}
