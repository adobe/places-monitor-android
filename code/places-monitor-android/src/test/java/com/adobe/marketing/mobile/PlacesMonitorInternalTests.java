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

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExtensionApi.class, PlacesLocationManager.class, PlacesGeofenceManager.class, PlacesMonitorInternal.class})
public class PlacesMonitorInternalTests {
    private PlacesMonitorInternal monitorInternal;

    private Event startMonitoringEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_START,
            PlacesMonitorTestConstants.EventType.MONITOR,
            PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

    private Event stopMonitoringEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_STOP,
            PlacesMonitorTestConstants.EventType.MONITOR,
            PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

    private Event updateLocationEvent = new Event.Builder(PlacesMonitorTestConstants.EVENTNAME_UPDATE,
            PlacesMonitorTestConstants.EventType.MONITOR,
            PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();

    private Event invalidMonitorRequestEvent = new Event.Builder("Invalid API",
            PlacesMonitorTestConstants.EventType.MONITOR,
            PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT).build();



    @Mock
    ExtensionApi extensionApi;

    @Mock
    PlacesLocationManager locationManager;

    @Mock
    PlacesGeofenceManager geofenceManager;

    @Before
    public void before() throws Exception {
        PowerMockito.whenNew(PlacesGeofenceManager.class).withNoArguments().thenReturn(geofenceManager);
        PowerMockito.whenNew(PlacesLocationManager.class).withNoArguments().thenReturn(locationManager);
        monitorInternal = new PlacesMonitorInternal(extensionApi);
    }

    // ========================================================================================
    // constructor
    // ========================================================================================
    @Test
    public void test_Constructor() {
        // capture arguments
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor1 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor2 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor3 = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // verify 3 listeners are registered
        verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorConstants.EventType.HUB), eq(PlacesMonitorConstants.EventSource.SHARED_STATE), eq(PlacesMonitorListenerHubSharedState.class), callbackCaptor1.capture());
        verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorConstants.EventType.PLACES), eq(PlacesMonitorConstants.EventSource.RESPONSE_CONTENT), eq(PlacesMonitorListenerPlacesResponseContent.class), callbackCaptor2.capture());
        verify(extensionApi, times(1)).registerEventListener(eq(PlacesMonitorTestConstants.EventType.MONITOR), eq(PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT), eq(PlacesMonitorListenerMonitorRequestContent.class), callbackCaptor3.capture());

        // verify that loadFences is called
        verify(geofenceManager, times(1)).loadMonitoringFences();

        // verify register listener error callback are not null
        assertNotNull("The register listener error callback should not be null", callbackCaptor1.getValue());
        assertNotNull("The register listener error callback should not be null", callbackCaptor2.getValue());
        assertNotNull("The register listener error callback should not be null", callbackCaptor3.getValue());

        // calling the callback should not crash
        callbackCaptor1.getValue().error(ExtensionError.UNEXPECTED_ERROR);
        callbackCaptor2.getValue().error(ExtensionError.UNEXPECTED_ERROR);
        callbackCaptor3.getValue().error(ExtensionError.UNEXPECTED_ERROR);
    }

    // ========================================================================================
    // getName
    // ========================================================================================
    @Test
    public void test_getName() {
        // test
        String moduleName = monitorInternal.getName();
        assertEquals("getName should return the correct module name",PlacesMonitorTestConstants.EXTENSION_NAME, moduleName);
    }

    // ========================================================================================
    // getVersion
    // ========================================================================================
    @Test
    public void test_getVersion() {
        // test
        String moduleVersion = monitorInternal.getVersion();
        assertEquals("getVesion should return the correct module version",PlacesMonitorTestConstants.EXTENSION_VERSION, moduleVersion);
    }

    // ========================================================================================
    // onUnregistered
    // ========================================================================================
    @Test
    public void test_onUnregistered() {
        // test
        monitorInternal.onUnregistered();
        verify(extensionApi,times(1)).clearSharedEventStates(null);
    }

    // ========================================================================================
    // queueEvent
    // ========================================================================================
    @Test
    public void test_QueueEvent() {
        // test 1
        assertNotNull("EventQueue instance is should never be null",monitorInternal.getEventQueue());

        // test 2
        Event sampleEvent = new Event.Builder("event 1", "eventType", "eventSource").build();
        monitorInternal.queueEvent(sampleEvent);
        assertEquals("The size of the eventQueue should be correct", 1,monitorInternal.getEventQueue().size());

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
        // test
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(0)).startMonitoring();
        verify(locationManager, times(0)).stopMonitoring();
        verify(locationManager, times(0)).updateLocation();
        verify(geofenceManager, times(0)).stopMonitoringFences();
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_startEvent() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(),any(Event.class),any(ExtensionErrorCallback.class))).thenReturn(configData);

        // test
        monitorInternal.queueEvent(startMonitoringEvent);
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(1)).startMonitoring();
        verify(locationManager, times(0)).stopMonitoring();
        verify(locationManager, times(0)).updateLocation();
        verify(geofenceManager, times(0)).stopMonitoringFences();
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_stopEvent() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(),any(Event.class),any(ExtensionErrorCallback.class))).thenReturn(configData);

        // test
        monitorInternal.queueEvent(stopMonitoringEvent);
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(0)).startMonitoring();
        verify(locationManager, times(1)).stopMonitoring();
        verify(locationManager, times(0)).updateLocation();
        verify(geofenceManager, times(1)).stopMonitoringFences();
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_updateEvent() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(),any(Event.class),any(ExtensionErrorCallback.class))).thenReturn(configData);

        // test
        monitorInternal.queueEvent(updateLocationEvent);
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(0)).startMonitoring();
        verify(locationManager, times(0)).stopMonitoring();
        verify(locationManager, times(1)).updateLocation();
        verify(geofenceManager, times(0)).stopMonitoringFences();
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_InvalidMonitorRequestEvent() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(),any(Event.class),any(ExtensionErrorCallback.class))).thenReturn(configData);

        // test
        monitorInternal.queueEvent(invalidMonitorRequestEvent);
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(0)).startMonitoring();
        verify(locationManager, times(0)).stopMonitoring();
        verify(locationManager, times(0)).updateLocation();
        verify(geofenceManager, times(0)).stopMonitoringFences();
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_multipleEvents() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(),any(Event.class),any(ExtensionErrorCallback.class))).thenReturn(configData);

        // test
        monitorInternal.queueEvent(updateLocationEvent);
        monitorInternal.queueEvent(stopMonitoringEvent);
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(0)).startMonitoring();
        verify(locationManager, times(1)).stopMonitoring();
        verify(locationManager, times(1)).updateLocation();
        verify(geofenceManager, times(1)).stopMonitoringFences();
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_configurationNotAvailable() {
        // setup
        when(extensionApi.getSharedEventState(anyString(),any(Event.class),any(ExtensionErrorCallback.class))).thenReturn(null);

        // test
        monitorInternal.queueEvent(startMonitoringEvent);
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(0)).startMonitoring();

        // make configuration available
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(),any(Event.class),any(ExtensionErrorCallback.class))).thenReturn(configData);

        // now process the queued event
        monitorInternal.processEvents();

        // verify
        verify(locationManager, times(1)).startMonitoring();
    }

    @Test
    public void test_processEvents_when_configurationRetrievalError() {
        // setup
        when(extensionApi.getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(null);

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
    public void test_processEvents_when_nearByPlacesResponse_withEmptyEventData() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(configData);

        // test
        monitorInternal.queueEvent(nearByPlacesEvent(new EventData()));
        monitorInternal.processEvents();

        // verify
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_nearByPlacesResponse_withNullEventData() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(configData);

        // test
        monitorInternal.queueEvent(nearByPlacesEvent(null));
        monitorInternal.processEvents();

        // verify
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }

    @Test
    public void test_processEvents_when_nearByPlacesResponse_withPOIS() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(configData);
        // setup argument captors
        final ArgumentCaptor<List> callbackCaptor = ArgumentCaptor.forClass(List.class);

        EventData eventData = new EventData();
        eventData.putTypedList(PlacesMonitorConstants.EventDataKeys.NEAR_BY_PLACES_LIST,samplePOIList(), new PlacesMonitorPOIVariantSerializer());

        // test
        monitorInternal.queueEvent(nearByPlacesEvent(eventData));
        monitorInternal.processEvents();

        // verify
        verify(geofenceManager, times(1)).startMonitoringFences(callbackCaptor.capture());

        List<PlacesMonitorPOI> poisPassed = callbackCaptor.getValue();
        assertNotNull("The pois passed should not be null", poisPassed);
        assertEquals("The pois passed should have the correct size", 3,poisPassed.size());
    }

    @Test
    public void test_processEvents_when_nearByPlacesResponse_withFewInvalidPOIS() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(configData);
        // setup argument captors
        final ArgumentCaptor<List> callbackCaptor = ArgumentCaptor.forClass(List.class);

        EventData eventData = new EventData();
        List pois = samplePOIList();
        pois.add("BadPOI");
        pois.add(null);
        eventData.putTypedList(PlacesMonitorConstants.EventDataKeys.NEAR_BY_PLACES_LIST, pois, new PlacesMonitorPOIVariantSerializer());

        // test
        monitorInternal.queueEvent(nearByPlacesEvent(eventData));
        monitorInternal.processEvents();

        // verify
        verify(geofenceManager, times(1)).startMonitoringFences(callbackCaptor.capture());

        List<PlacesMonitorPOI> poisPassed = callbackCaptor.getValue();
        assertNotNull("The pois passed should not be null", poisPassed);
        assertEquals("The pois passed should have the correct size", 4, poisPassed.size());
    }

    @Test
    public void test_processEvents_when_nearByPlacesResponse_withNullPOIS() {
        // setup configuration
        Map<String,Object> configData = new HashMap<>();
        when(extensionApi.getSharedEventState(anyString(), any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(configData);

        EventData eventData = new EventData();
        eventData.putTypedList(PlacesMonitorConstants.EventDataKeys.NEAR_BY_PLACES_LIST,null, new PlacesMonitorPOIVariantSerializer());

        // test
        monitorInternal.queueEvent(nearByPlacesEvent(eventData));
        monitorInternal.processEvents();

        // verify
        verify(geofenceManager, times(0)).startMonitoringFences(ArgumentMatchers.<PlacesMonitorPOI>anyList());
    }


    // ========================================================================================
    // getExecutor
    // ========================================================================================
    @Test
    public void test_getExecutor_NeverReturnsNull() {
        // test
        ExecutorService executorService= monitorInternal.getExecutor();
        assertNotNull("The executor should not return null", executorService);

        // verify
        assertEquals("Gets the same excecutor instance on the next get", executorService , monitorInternal.getExecutor());
    }



    private Event nearByPlacesEvent (final EventData eventData) {
        return new Event.Builder("Near by Event",
                PlacesMonitorTestConstants.EventType.PLACES,
                PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT).setData(eventData).build();
    }

    private List<PlacesMonitorPOI> samplePOIList() {
        PlacesMonitorPOI poi1 = new PlacesMonitorPOI("poiID1","Brazil", 22.22, 33.33, 40);
        PlacesMonitorPOI poi2 = new PlacesMonitorPOI("poiID2","Australia", 44.44, -55.55, 80);
        PlacesMonitorPOI poi3 = new PlacesMonitorPOI("poiID3","Netherland", 66.66, -77.77, 100);
        List<PlacesMonitorPOI> pois = new ArrayList<PlacesMonitorPOI>();
        pois.add(poi1);
        pois.add(poi2);
        pois.add(poi3);
        return pois;
    }

}
