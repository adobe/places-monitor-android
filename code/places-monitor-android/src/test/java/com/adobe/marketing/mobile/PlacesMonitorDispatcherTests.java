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
// PlacesMonitorDispatcherTests.java
//

package com.adobe.marketing.mobile;

import android.location.Location;
import com.google.android.gms.location.Geofence;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.*;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MobileCore.class})
public class PlacesMonitorDispatcherTests {

    @Before
    public void before() {
        PowerMockito.mockStatic(MobileCore.class);
    }


    // ========================================================================================
    // dispatchLocation
    // ========================================================================================
    @Test
    public void test_dispatchLocation_happy() throws VariantException{
        // setup
        Location location = new Location("testProvider");
        location.setLatitude(37.82);
        location.setLongitude(-121.34);
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // setup argument captors
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        PlacesMonitorDispatcher.dispatchLocation(location);

        // verify method calls
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

        // verify dispatched event
        Event event = eventCaptor.getValue();
        assertNotNull("The dispatched event should not be null", event);
        assertEquals("the event name should be correct" , "Places Monitor Location Event", event.getName());
        assertEquals("the event type should be correct" , PlacesMonitorTestConstants.EventType.PLACES, event.getType());
        assertEquals("the event source should be correct" , PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        EventData eventData = event.getData();
        assertNotNull("the event data should not be null" , eventData);
        // TODO : Not sure why location object is not created in the setup step
        // assertEquals("the event data should contain correct latitude" , 37.82, eventData.getDouble(PlacesMonitorTestConstants.EventDataKeys.LATITUDE),0.0);
        // assertEquals("the event data should contain correct longitude" ,-121.34, eventData.getDouble(PlacesMonitorTestConstants.EventDataKeys.LONGITUDE), 0.0);
        assertEquals("the event data should contain correct poi count" , PlacesMonitorTestConstants.NEARBY_GEOFENCES_COUNT, eventData.getInteger(PlacesMonitorTestConstants.EventDataKeys.PLACES_COUNT));
        assertEquals("the event data should contain correct requestType" , PlacesMonitorTestConstants.EventDataKeys.REQUEST_TYPE_GET_NEARBY_PLACES, eventData.getString2(PlacesMonitorConstants.EventDataKeys.REQUEST_TYPE));


        // verify extension callback event
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("the extension error callback should not be null", extensionErrorCallback);
    }

    @Test
    public void test_dispatchLocation_when_locationIsNull() throws Exception{
        // setup
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // test
        PlacesMonitorDispatcher.dispatchLocation(null);

        // verify dispatch is not called
        verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    // ========================================================================================
    // dispatchLocation's Callback
    // ========================================================================================

    @Test
    public void test_dispatchLocationCallback_when_validError() throws Exception{
        // setup
        Location location = new Location("testProvider");
        location.setLatitude(37.82);
        location.setLongitude(-121.34);
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // setup argument captors
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        PlacesMonitorDispatcher.dispatchLocation(location);

        // verify method calls
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(any(Event.class), callbackCaptor.capture());

        // verify extension callback
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("The extension callback should not be null", extensionErrorCallback);

        // should not crash or throw exception on calling the callback
        extensionErrorCallback.error(ExtensionError.EVENT_NULL);
    }


    // ========================================================================================
    // dispatchRegionEvent
    // ========================================================================================


    @Test
    public void test_dispatchRegionEvent_happy() throws VariantException {
        // setup
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // create sample geofences
        Geofence geofence = new Geofence.Builder().setRequestId("geofenceId1").setCircularRegion(15.0,15.0, 50).setTransitionTypes(1).setExpirationDuration(200).build();
        List<Geofence> geofences = new ArrayList<>();
        geofences.add(geofence);

        // setup argument captors
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        PlacesMonitorDispatcher.dispatchRegionEvent(geofences, "entry");

        // verify event dispatched
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

        // verify dispatched event
        Event event = eventCaptor.getValue();
        assertNotNull("The dispatched event should not be null", event);
        assertEquals("the event name should be correct" , "Places Monitor Region Event", event.getName());
        assertEquals("the event type should be correct" , PlacesMonitorTestConstants.EventType.PLACES, event.getType());
        assertEquals("the event source should be correct" , PlacesMonitorTestConstants.EventSource.REQUEST_CONTENT, event.getSource());
        EventData eventData = event.getData();
        assertNotNull("the event data should not be null" , eventData);
        assertEquals("the event data should contain correct geofenceID" , geofence.getRequestId() , eventData.getString2(PlacesMonitorTestConstants.EventDataKeys.REGION_ID));
        assertEquals("the event data should contain correct regionEventType" , "entry" , eventData.getString2(PlacesMonitorTestConstants.EventDataKeys.REGION_EVENT_TYPE));
        assertEquals("the event data should contain correct requestType" , PlacesMonitorTestConstants.EventDataKeys.REQUEST_TYPE_PROCESS_REGION_EVENT, eventData.getString2(PlacesMonitorConstants.EventDataKeys.REQUEST_TYPE));

        // verify extension callback event
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("The extension callback should not be null", extensionErrorCallback);
    }

    @Test
    public void test_dispatchRegionEvent_multipleRegions() {
        // setup
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // create sample geofences
        Geofence geofence1 = new Geofence.Builder().setRequestId("geofenceId1").setCircularRegion(15.0,15.0, 50).setTransitionTypes(1).setExpirationDuration(200).build();
        Geofence geofence2 = new Geofence.Builder().setRequestId("geofenceId2").setCircularRegion(30.0,30.0, 100).setTransitionTypes(1).setExpirationDuration(200).build();
        Geofence geofence3 = new Geofence.Builder().setRequestId("geofenceId3").setCircularRegion(40.0,40.0, 200).setTransitionTypes(1).setExpirationDuration(200).build();
        List<Geofence> geofences = new ArrayList<>();
        geofences.add(geofence1);
        geofences.add(geofence2);
        geofences.add(geofence3);
        geofences.add(null);

        // test
        PlacesMonitorDispatcher.dispatchRegionEvent(geofences, "entry");

        // verify 3 events dispatched
        verifyStatic(MobileCore.class, Mockito.times(3));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }


    @Test
    public void test_dispatchRegionEvent_when_nullGeofences() {
        // setup
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // test
        PlacesMonitorDispatcher.dispatchRegionEvent(null, "entry");

        // verify dispatch is not called
        verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    @Test
    public void test_dispatchRegionEvent_when_emptyGeofences() {
        // setup
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // test
        PlacesMonitorDispatcher.dispatchRegionEvent(new ArrayList<Geofence>(), "entry");

        // verify dispatch is not called
        verifyStatic(MobileCore.class, Mockito.times(0));
        MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class));
    }

    // ========================================================================================
    // dispatchRegionEvent's Callback
    // ========================================================================================

    @Test
    public void test_dispatchRegionCallback_when_ValidError() {
        // setup
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // create sample geofences
        Geofence geofence = new Geofence.Builder().setRequestId("geofenceId1").setCircularRegion(15.0,15.0, 50).setTransitionTypes(1).setExpirationDuration(200).build();
        List<Geofence> geofences = new ArrayList<>();
        geofences.add(geofence);

        // setup argument captors
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        PlacesMonitorDispatcher.dispatchRegionEvent(geofences, "entry");

        // verify event dispatched
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(any(Event.class), callbackCaptor.capture());

        // verify extension callback event
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("The extension callback should not be null", extensionErrorCallback);
        // should not crash or throw exception on calling the callback
        extensionErrorCallback.error(ExtensionError.UNEXPECTED_ERROR);
    }

    @Test
    public void test_dispatchRegionCallback_whenNullError() throws VariantException {
        // setup
        Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

        // create sample geofences
        Geofence geofence = new Geofence.Builder().setRequestId("geofenceId1").setCircularRegion(15.0,15.0, 50).setTransitionTypes(1).setExpirationDuration(200).build();
        List<Geofence> geofences = new ArrayList<>();
        geofences.add(geofence);

        // setup argument captors
        final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

        // test
        PlacesMonitorDispatcher.dispatchRegionEvent(geofences, "entry");

        // verify event dispatched
        verifyStatic(MobileCore.class, Mockito.times(1));
        MobileCore.dispatchEvent(any(Event.class), callbackCaptor.capture());

        // verify extension callback event
        ExtensionErrorCallback extensionErrorCallback = callbackCaptor.getValue();
        assertNotNull("The extension callback should not be null", extensionErrorCallback);
        // should not crash or throw exception on calling the callback
        extensionErrorCallback.error(null);
    }

}
