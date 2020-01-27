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
// PlacesMonitorInternalTests.java
//

package com.adobe.marketing.mobile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.location.Location;

import com.google.android.gms.location.Geofence;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExtensionApi.class, PlacesLocationManager.class, PlacesGeofenceManager.class, PlacesMonitorInternal.class, App.class, Context.class, Intent.class, Places.class, Location.class, PlacesActivity.class})
public class PlacesMonitorInternalTests {
	private PlacesMonitorInternal monitorInternal;

	private Event startMonitoringEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_START,
			PlacesMonitorTestConstants.EventType.MONITOR,
			PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

	private Event OSLocationEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_OS_LOCATION_UPDATE,
			PlacesMonitorTestConstants.EventType.OS,
			PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT).setData(locationUpdateEventData()).build();

	private Event OSGeofenceEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_OS_GEOFENCE_TRIGGER,
			PlacesMonitorTestConstants.EventType.OS,
			PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT).setData(geofenceTransitionEventData()).build();

	private Event stopMonitoringEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_STOP,
			PlacesMonitorTestConstants.EventType.MONITOR,
			PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

	private Event stopMonitoringEventWithClearData = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_STOP,
			PlacesMonitorTestConstants.EventType.MONITOR,
	PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).setData(new EventData(new HashMap<String, Variant>() {
		{
			put(PlacesMonitorConstants.EventDataKey.CLEAR, Variant.fromBoolean(true));
		}
	})).build();

	private Event stopMonitoringEventWithOutClearData = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_STOP,
			PlacesMonitorTestConstants.EventType.MONITOR,
	PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).setData(new EventData(new HashMap<String, Variant>() {
		{
			put(PlacesMonitorConstants.EventDataKey.CLEAR, Variant.fromBoolean(false));
		}
	})).build();

	private Event updateLocationEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_UPDATE,
			PlacesMonitorTestConstants.EventType.MONITOR,
			PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

	private Event setLocationPermissionEvent = new Event.Builder(
		PlacesMonitorTestConstants.EVENTNAME_SET_LOCATION_PERMISSION,
		PlacesMonitorTestConstants.EventType.MONITOR,
	PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).setData(new EventData(new HashMap<String, Variant>() {
		{
			put(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION,
				Variant.fromString(PlacesMonitorLocationPermission.WHILE_USING_APP.getValue()));
		}
	})).build();

	private Event setLocationPermissionEventNoEventData = new Event.Builder(
		PlacesMonitorTestConstants.EVENTNAME_SET_LOCATION_PERMISSION,
		PlacesMonitorTestConstants.EventType.MONITOR,
		PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

	private Event invalidMonitorRequestEvent = new Event.Builder("Invalid API",
			PlacesMonitorTestConstants.EventType.MONITOR,
			PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

	@Mock
	Location location;

	@Mock
	Context context;

	@Mock
	ExtensionUnexpectedError extensionUnexpectedError;

	@Mock
	ExtensionApi extensionApi;

	@Mock
	PlacesLocationManager locationManager;

	@Mock
	PlacesGeofenceManager geofenceManager;

	@Before
	public void before() throws Exception {
		PowerMockito.mockStatic(App.class);
		PowerMockito.mockStatic(Places.class);
		PowerMockito.whenNew(PlacesGeofenceManager.class).withNoArguments().thenReturn(geofenceManager);
	}


	// ========================================================================================
	// constructor
	// ========================================================================================
	@Test
	public void test_Constructor() {
		// capture arguments
		initWithContext(context);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor1 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor2 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor3 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// verify 3 listeners are registered
		verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorConstants.EventType.HUB),
				eq(PlacesMonitorConstants.EventSource.SHARED_STATE), eq(PlacesMonitorListenerHubSharedState.class),
				callbackCaptor1.capture());
		verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorTestConstants.EventType.MONITOR),
				eq(PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT), eq(PlacesMonitorListenerMonitorRequestContent.class),
				callbackCaptor2.capture());

		verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorTestConstants.EventType.OS),
				eq(PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT), eq(PlacesMonitorListenerOSResponseContent.class),
				callbackCaptor3.capture());

		// verify that loadFences is called
		verify(geofenceManager, times(1)).loadPersistedData();

		// verify register listener error callback are not null
		assertNotNull("The register listener error callback should not be null", callbackCaptor1.getValue());
		assertNotNull("The register listener error callback should not be null", callbackCaptor2.getValue());
		assertNotNull("The register listener error callback should not be null", callbackCaptor3.getValue());


		// calling the callback should not crash
		callbackCaptor1.getValue().error(ExtensionError.UNEXPECTED_ERROR);
		callbackCaptor2.getValue().error(ExtensionError.UNEXPECTED_ERROR);
		callbackCaptor3.getValue().error(ExtensionError.UNEXPECTED_ERROR);

		// verify location auth status got initialized
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.ALWAYS);
	}

	@Test
	public void test_Constructor_when_noContext() {
		// setup
		initWithContext(null);

		// capture arguments
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor1 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor2 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// verify 3 listeners are registered
		verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorConstants.EventType.HUB),
				eq(PlacesMonitorConstants.EventSource.SHARED_STATE), eq(PlacesMonitorListenerHubSharedState.class),
				callbackCaptor1.capture());
		verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorTestConstants.EventType.MONITOR),
				eq(PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT), eq(PlacesMonitorListenerMonitorRequestContent.class),
				callbackCaptor2.capture());

		verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorTestConstants.EventType.MONITOR),
				eq(PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT), eq(PlacesMonitorListenerMonitorRequestContent.class),
				callbackCaptor2.capture());

		// verify that loadFences is called
		verify(geofenceManager, times(1)).loadPersistedData();
	}

	// ========================================================================================
	// getName
	// ========================================================================================
	@Test
	public void test_getName() {
		// setup
		initWithContext(context);

		// test
		String moduleName = monitorInternal.getName();
		assertEquals("getName should return the correct module name", PlacesMonitorTestConstants.EXTENSION_NAME, moduleName);
	}

	// ========================================================================================
	// getVersion
	// ========================================================================================
	@Test
	public void test_getVersion() {
		// setup
		initWithContext(context);

		// test
		String moduleVersion = monitorInternal.getVersion();
		assertEquals("getVesion should return the correct module version", PlacesMonitorTestConstants.EXTENSION_VERSION,
					 moduleVersion);
	}

	// ========================================================================================
	// onUnexpectedError
	// ========================================================================================
	@Test
	public void test_onUnexpectedError() {
		// setup
		initWithContext(context);

		// test
		monitorInternal.onUnexpectedError(extensionUnexpectedError);
		verify(extensionApi, times(1)).clearSharedEventStates(null);
	}

	// ========================================================================================
	// onUnregistered
	// ========================================================================================

	@Test
	public void test_onUnregistered() {
		// setup
		initWithContext(context);

		// test
		monitorInternal.onUnregistered();
		verify(extensionApi, times(1)).clearSharedEventStates(null);
	}

	// ========================================================================================
	// queueEvent
	// ========================================================================================
	@Test
	public void test_QueueEvent() {
		// setup
		initWithContext(context);

		// test 1
		assertNotNull("EventQueue instance is should never be null", monitorInternal.getEventQueue());

		// test 2
		Event sampleEvent = new Event.Builder("event 1", "eventType", "eventSource").build();
		monitorInternal.queueEvent(sampleEvent);
		assertEquals("The size of the eventQueue should be correct", 1, monitorInternal.getEventQueue().size());

		// test 3
		monitorInternal.queueEvent(null);
		assertEquals("The size of the eventQueue should be correct", 1, monitorInternal.getEventQueue().size());

		// test 4
		Event anotherEvent = new Event.Builder("event 2", "eventType", "eventSource").build();
		monitorInternal.queueEvent(anotherEvent);
		assertEquals("The size of the eventQueue should be correct", 2, monitorInternal.getEventQueue().size());

	}

	// ========================================================================================
	// processEvents
	// ========================================================================================
	@Test
	public void test_processEvents_when_noEventInQueue() {
		// setup
		initWithContext(context);

		// test
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(0)).stopMonitoring();
		verify(locationManager, times(0)).updateLocation();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
	}

	@Test
	public void test_processEvents_when_startEvent() {
		// setup
		initWithContext(context);

		// setup configuration
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Map<String, Object> configData = new HashMap<>();
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(startMonitoringEvent);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(1)).startMonitoring();
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
	}

	@Test
	public void test_processEvents_when_stopEventWithClearData() {
		// setup
		initWithContext(context);

		// setup configuration
		Map<String, Object> configData = new HashMap<>();
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(stopMonitoringEventWithClearData);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(1)).stopMonitoring();
		verify(locationManager, times(0)).updateLocation();
		verify(geofenceManager, times(1)).stopMonitoringFences(true);
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
		verify(locationManager, times(0)).setLocationPermission(any(PlacesMonitorLocationPermission.class));

		// verify places call
		verifyStatic(Places.class, Mockito.times(1));
		Places.clear();
	}

	@Test
	public void test_processEvents_when_stopEventWithOutClearData() {
		// setup
		initWithContext(context);

		// setup configuration
		Map<String, Object> configData = new HashMap<>();
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(stopMonitoringEventWithOutClearData);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(1)).stopMonitoring();
		verify(locationManager, times(0)).updateLocation();
		verify(geofenceManager, times(1)).stopMonitoringFences(false);
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
		verify(locationManager, times(0)).setLocationPermission(any(PlacesMonitorLocationPermission.class));

		// verify places call
		verifyStatic(Places.class, Mockito.times(0));
		Places.clear();
	}

	@Test
	public void test_processEvents_when_stopEventWithoutEventData() {
		// setup
		initWithContext(context);

		// setup configuration
		Map<String, Object> configData = new HashMap<>();
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(stopMonitoringEvent);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(1)).stopMonitoring();
		verify(locationManager, times(0)).updateLocation();
		verify(geofenceManager, times(1)).stopMonitoringFences(false);
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
		verify(locationManager, times(0)).setLocationPermission(any(PlacesMonitorLocationPermission.class));

		// verify places call
		verifyStatic(Places.class, Mockito.times(0));
		Places.clear();
	}

	@Test
	public void test_processEvents_when_updateEvent() {
		// setup
		initWithContext(context);

		// setup configuration
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Map<String, Object> configData = new HashMap<>();
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(updateLocationEvent);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(0)).stopMonitoring();
		verify(locationManager, times(1)).updateLocation();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
		verify(locationManager, times(0)).setLocationPermission(any(PlacesMonitorLocationPermission.class));
	}

	@Test
	public void test_processEvents_when_InvalidMonitorRequestEvent() {
		// setup
		initWithContext(context);

		// setup configuration
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Map<String, Object> configData = new HashMap<>();
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(invalidMonitorRequestEvent);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(0)).stopMonitoring();
		verify(locationManager, times(0)).updateLocation();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
		verify(locationManager, times(0)).setLocationPermission(any(PlacesMonitorLocationPermission.class));
	}

	@Test
	public void test_processEvents_when_SetLocationPermissionEvent() {
		// setup
		initWithContext(context);

		// setup configuration
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Map<String, Object> configData = new HashMap<>();
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(setLocationPermissionEvent);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(0)).stopMonitoring();
		verify(locationManager, times(0)).updateLocation();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
		verify(locationManager, times(1)).setLocationPermission(PlacesMonitorLocationPermission.WHILE_USING_APP);
	}

	@Test
	public void test_processEvents_when_SetLocationPermissionEventWithNoEventData() {
		// setup
		initWithContext(context);

		// setup configuration
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Map<String, Object> configData = new HashMap<>();
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(setLocationPermissionEventNoEventData);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(0)).stopMonitoring();
		verify(locationManager, times(0)).updateLocation();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
		verify(locationManager, times(0)).setLocationPermission(any(PlacesMonitorLocationPermission.class));
	}

	@Test
	public void test_processEvents_when_multipleEvents() {
		// setup
		initWithContext(context);

		// setup configuration
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Map<String, Object> configData = new HashMap<>();
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// test
		monitorInternal.queueEvent(updateLocationEvent);
		monitorInternal.queueEvent(stopMonitoringEventWithOutClearData);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();
		verify(locationManager, times(1)).stopMonitoring();
		verify(locationManager, times(1)).updateLocation();
		verify(geofenceManager, times(1)).stopMonitoringFences(false);
		verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesPOI>anyList());
	}

	@Test
	public void test_processEvents_when_configurationNotAvailable() {
		// setup
		initWithContext(context);

		// setup
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(null);

		// test
		monitorInternal.queueEvent(startMonitoringEvent);
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(0)).startMonitoring();

		// make configuration available
		Map<String, Object> configData = new HashMap<>();
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(configData);

		// now process the queued event
		monitorInternal.processEvents();

		// verify
		verify(locationManager, times(1)).startMonitoring();
	}

	@Test
	public void test_processEvents_when_configurationRetrievalError() {
		// setup
		initWithContext(context);
		when(extensionApi.getSharedEventState(anyString(), any(Event.class),
											  any(ExtensionErrorCallback.class))).thenReturn(null);

		// setup argument captors
		final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

		// test
		monitorInternal.queueEvent(startMonitoringEvent);
		monitorInternal.processEvents();

		// verify getSharedEventState callback
		verify(extensionApi, times(1)).getSharedEventState(anyString(), any(Event.class), callbackCaptor.capture());
		ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
		assertNotNull("the extension error callback should not be null", extensionErrorCallback);

		// invoking callback with error, should not crash
		callbackCaptor.getValue().error(ExtensionError.UNEXPECTED_ERROR);
	}

	@Test
	public void test_processEvents_when_OSLocationEvent() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);

		// test
		monitorInternal.queueEvent(OSLocationEvent);
		monitorInternal.processEvents();

		// verify locationManager is called with the correct data
		verify(locationManager, times(1)).onLocationReceived(locationUpdateEventData());
	}

	@Test
	public void test_processEvents_when_OSGeofenceEvent() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(OSGeofenceEvent);
		monitorInternal.processEvents();

		// verify geofenceManager is called with the correct data
		verify(geofenceManager, times(1)).onGeofenceTriggerReceived(geofenceTransitionEventData());
	}

	@Test
	public void test_processEvents_when_OSEventUnknownType() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(makeOSEvent("UnknownType"));
		monitorInternal.processEvents();

		// verify no internal methods are called
		verify(geofenceManager, times(0)).onGeofenceTriggerReceived(geofenceTransitionEventData());
		verify(locationManager, times(0)).onLocationReceived(locationUpdateEventData());
		verify(locationManager, times(0)).beginLocationTracking();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(true);
	}

	@Test
	public void test_processEvents_when_OSEventWithEmptyEventData() {
		// setup
		initWithContext(context);
		Event osEvent = new Event.Builder("OS Event",
										  PlacesMonitorTestConstants.EventType.OS,
										  PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT).setData(null).build();
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(osEvent);
		monitorInternal.processEvents();

		// verify no internal methods are called
		verify(geofenceManager, times(0)).onGeofenceTriggerReceived(geofenceTransitionEventData());
		verify(locationManager, times(0)).onLocationReceived(locationUpdateEventData());
		verify(locationManager, times(0)).beginLocationTracking();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(true);
	}

	@Test
	public void test_processEvents_when_OSEventWithEmptyEventType() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(makeOSEvent(""));
		monitorInternal.processEvents();

		// verify no internal methods are called
		verify(geofenceManager, times(0)).onGeofenceTriggerReceived(geofenceTransitionEventData());
		verify(locationManager, times(0)).onLocationReceived(locationUpdateEventData());
		verify(locationManager, times(0)).beginLocationTracking();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(true);
	}


	// ========================================================================================
	// process OS location permission change events
	// ========================================================================================

	@Test
	public void test_processEvents_when_LocationPermissionEvent_GrantedStatus() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(makePermissionChangeEvent(
									   PlacesMonitorTestConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_GRANTED));
		monitorInternal.processEvents();

		// verify if location tracking is started
		verify(locationManager, times(1)).beginLocationTracking();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(true);
	}

	@Test
	public void test_processEvents_when_LocationPermissionEvent_DeniedStatus() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(makePermissionChangeEvent(
									   PlacesMonitorTestConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_DENIED));
		monitorInternal.processEvents();

		// verify if the location tracking and geofence monitoring is stopped
		verify(locationManager, times(0)).beginLocationTracking();
		verify(locationManager, times(1)).stopMonitoring();
		verify(geofenceManager, times(1)).stopMonitoringFences(true);
	}

	@Test
	public void test_processEvents_when_LocationPermissionEvent_UnknownStatus() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(makePermissionChangeEvent("Unknown event"));
		monitorInternal.processEvents();

		// verify no action is taken
		verify(locationManager, times(0)).beginLocationTracking();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(true);
	}

	@Test
	public void test_processEvents_when_LocationPermissionEvent_EmptyStatus() {
		// setup
		initWithContext(context);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);
		Whitebox.setInternalState(monitorInternal, "geofenceManager", geofenceManager);

		// test
		monitorInternal.queueEvent(makePermissionChangeEvent(""));
		monitorInternal.processEvents();

		// verify no action is taken
		verify(locationManager, times(0)).beginLocationTracking();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(true);
	}


	// ========================================================================================
	// getExecutor
	// ========================================================================================
	@Test
	public void test_getExecutor_NeverReturnsNull() {
		// setup
		initWithContext(context);

		// test
		ExecutorService executorService = monitorInternal.getExecutor();
		assertNotNull("The executor should not return null", executorService);

		// verify
		assertEquals("Gets the same executor instance on the next get", executorService, monitorInternal.getExecutor());
	}


	// ========================================================================================
	// getPOIsForLocation
	// ========================================================================================
	@Test
	public void test_getPOIsForLocation_whenLocationNull() {
		// setup
		initWithContext(context);

		// test
		monitorInternal.getPOIsForLocation(null);

		// verify
		verifyStatic(Places.class, Mockito.times(0));
		Places.getNearbyPointsOfInterest(any(Location.class), anyInt(), any(AdobeCallback.class), any(AdobeCallback.class));
	}

	@Test
	public void test_getPOIsForLocation_when_success() {
		// setup
		initWithContext(context);
		final ArgumentCaptor<AdobeCallback> successCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
		final ArgumentCaptor<AdobeCallback> failurecallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);

		// test
		monitorInternal.getPOIsForLocation(location);

		// verify
		verifyStatic(Places.class, Mockito.times(1));
		Places.getNearbyPointsOfInterest(any(Location.class), anyInt(), successCallbackCaptor.capture(),
										 failurecallbackCaptor.capture());

		// call the success callback
		List<PlacesPOI> nearbyPois = samplePOIList();
		successCallbackCaptor.getValue().call(nearbyPois);

		// verify
		verify(geofenceManager, times(1)).startMonitoringFences(nearbyPois);
	}

	@Test
	public void test_getPOIsForLocation_when_failure_with_ConfigurationError() {
		// setup
		initWithContext(context);
		final ArgumentCaptor<AdobeCallback> successCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
		final ArgumentCaptor<AdobeCallback> failureCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);

		// test
		monitorInternal.getPOIsForLocation(location);

		// verify
		verifyStatic(Places.class, Mockito.times(1));
		Places.getNearbyPointsOfInterest(any(Location.class), anyInt(), successCallbackCaptor.capture(),
										 failureCallbackCaptor.capture());

		// call the failure callback
		failureCallbackCaptor.getValue().call(PlacesRequestError.CONFIGURATION_ERROR);

		// verify
		verifyStatic(Places.class, Mockito.times(1));
		Places.clear();
		verify(locationManager, times(1)).stopMonitoring();
		verify(geofenceManager, times(1)).stopMonitoringFences(true);
	}

	@Test
	public void test_getPOIsForLocation_when_failure_with_OtherErrors() {
		// setup
		initWithContext(context);
		final ArgumentCaptor<AdobeCallback> successCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
		final ArgumentCaptor<AdobeCallback> failureCallbackCaptor = ArgumentCaptor.forClass(AdobeCallback.class);
		Whitebox.setInternalState(monitorInternal, "locationManager", locationManager);

		// test
		monitorInternal.getPOIsForLocation(location);

		// verify
		verifyStatic(Places.class, Mockito.times(1));
		Places.getNearbyPointsOfInterest(any(Location.class), anyInt(), successCallbackCaptor.capture(),
										 failureCallbackCaptor.capture());

		// call the failure callback with CONNECTIVITY_ERROR
		failureCallbackCaptor.getValue().call(PlacesRequestError.CONNECTIVITY_ERROR);

		// verify;
		verifyStatic(Places.class, Mockito.times(0));
		Places.clear();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());


		// call the failure callback with INVALID_LATLONG_ERROR
		failureCallbackCaptor.getValue().call(PlacesRequestError.INVALID_LATLONG_ERROR);

		// verify;
		verifyStatic(Places.class, Mockito.times(0));
		Places.clear();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());


		// call the failure callback with QUERY_SERVICE_UNAVAILABLE
		failureCallbackCaptor.getValue().call(PlacesRequestError.QUERY_SERVICE_UNAVAILABLE);

		// verify;
		verifyStatic(Places.class, Mockito.times(0));
		Places.clear();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());


		// call the failure callback with SERVER_RESPONSE_ERROR
		failureCallbackCaptor.getValue().call(PlacesRequestError.SERVER_RESPONSE_ERROR);

		// verify;
		verifyStatic(Places.class, Mockito.times(0));
		Places.clear();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());


		// call the failure callback with UNKNOWN_ERROR
		failureCallbackCaptor.getValue().call(PlacesRequestError.UNKNOWN_ERROR);

		// verify;
		verifyStatic(Places.class, Mockito.times(0));
		Places.clear();
		verify(locationManager, times(0)).stopMonitoring();
		verify(geofenceManager, times(0)).stopMonitoringFences(anyBoolean());
	}

	// ========================================================================================
	// Private helper methods
	// ========================================================================================

	private void initWithContext(Context context) {
		Mockito.when(App.getAppContext()).thenReturn(context);
		monitorInternal = new PlacesMonitorInternal(extensionApi);
	}

	private List<PlacesPOI> samplePOIList() {
		PlacesPOI poi1 = new PlacesPOI("poiID1", "Brazil", 22.22, 33.33, 40, "libraryID", 200, null);
		PlacesPOI poi2 = new PlacesPOI("poiID2", "Australia", 44.44, -55.55, 80, "libraryID", 200, null);
		PlacesPOI poi3 = new PlacesPOI("poiID3", "Netherland", 66.66, -77.77, 100, "libraryID", 200, null);
		List<PlacesPOI> pois = new ArrayList<PlacesPOI>();
		pois.add(poi1);
		pois.add(poi2);
		pois.add(poi3);
		return pois;
	}

	private EventData locationUpdateEventData() {
		return new EventData(new HashMap<String, Variant>() {
			{
				put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE,
					Variant.fromString(PlacesMonitorTestConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_UPDATE));
				put(PlacesMonitorConstants.EventDataKey.LATITUDE, Variant.fromDouble(22.22));
				put(PlacesMonitorConstants.EventDataKey.LONGITUDE, Variant.fromDouble(33.33));
			}
		});
	}

	private EventData geofenceTransitionEventData() {
		return new EventData(new HashMap<String, Variant>() {
			{
				put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE,
					Variant.fromString(PlacesMonitorTestConstants.EventDataValue.OS_EVENT_TYPE_GEOFENCE_TRIGGER));
				put(PlacesMonitorConstants.EventDataKey.GEOFENCE_IDS, Variant.fromStringList(getGeofenceIds()));
				put(PlacesMonitorConstants.EventDataKey.GEOFENCE_TRANSITION_TYPE,
					Variant.fromInteger(Geofence.GEOFENCE_TRANSITION_ENTER));
			}
		});
	}

	private List<String> getGeofenceIds() {
		List<String> geofenceIds = new ArrayList<>();
		geofenceIds.add("id1");
		geofenceIds.add("id2");
		return geofenceIds;
	}

	private Event makePermissionChangeEvent(final String permissionStatus) {
		EventData data = new EventData(new HashMap<String, Variant>() {
			{
				put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE,
					Variant.fromString(PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_PERMISSION_CHANGE));
				put(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION_STATUS, Variant.fromString(permissionStatus));
			}
		});

		Event event = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_OS_PERMISSION_CHANGE,
										PlacesMonitorTestConstants.EventType.OS,
										PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT).setData(data).build();
		return event;
	}

	private Event makeOSEvent(final String eventType) {
		EventData data = new EventData(new HashMap<String, Variant>() {
			{
				put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE, Variant.fromString(eventType));
			}
		});

		Event event = new Event.Builder("OS Event",
										PlacesMonitorTestConstants.EventType.OS,
										PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT).setData(data).build();
		return event;
	}






}
