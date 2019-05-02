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
import android.location.Location;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationResult;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PlacesMonitorDispatcher.class, GeofencingEvent.class})
public class PlacesGeofenceBroadcastReceiverTests {

    static final String ACTION_GEOFENCE_UPDATE =
            "com.adobe.marketing.mobile.PlacesGeofenceBroadcastReceiver.geofenceUpdates";
    private PlacesGeofenceBroadcastReceiver receiver;
    List<Geofence> geofenceList;


    @Mock
    Context mockContext;

    @Mock
    Intent mockIntent;

    @Mock
    GeofencingEvent mockGeofencingEvent;

    @Mock
    Geofence geofence1, geofence2;

    @Before
    public void before() throws Exception {

        receiver = new PlacesGeofenceBroadcastReceiver();


        geofenceList = new ArrayList<>();
        geofenceList.add(geofence1);
        geofenceList.add(geofence2);


        PowerMockito.mockStatic(PlacesMonitorDispatcher.class);
        PowerMockito.mockStatic(GeofencingEvent.class);
        when(mockGeofencingEvent.hasError()).thenReturn(false);
        when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(1);
        when(mockGeofencingEvent.getTriggeringGeofences()).thenReturn(geofenceList);
        PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);

        when(mockIntent.getAction()).thenReturn(ACTION_GEOFENCE_UPDATE);
    }

    // ========================================================================================
    // onReceive
    // ========================================================================================

    @Test
    public void test_OnReceive_of_entryEvent() {
        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(1));
        PlacesMonitorDispatcher.dispatchRegionEvent(geofenceList, "entry");
    }

    @Test
    public void test_OnReceive_of_exitEvent() {
        // setup
        when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(2);

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(1));
        PlacesMonitorDispatcher.dispatchRegionEvent(geofenceList, "exit");
    }


    @Test
    public void test_OnReceive_of_otherEvent() {
        // setup
        when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(4);

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchRegionEvent(any(List.class), anyString());
    }

    @Test
    public void test_OnReceive_when_intentIsNull() {
        // test
        receiver.onReceive(mockContext, null);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchRegionEvent(any(List.class), anyString());
    }

    @Test
    public void test_OnReceive_when_intentHasDifferentAction() {
        // setup
        when(mockIntent.getAction()).thenReturn("unknownAction");

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(any(Location.class));
    }


    @Test
    public void test_OnReceive_when_geofencingEvent_hasError() {
        // setup
        when(mockGeofencingEvent.hasError()).thenReturn(true);

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(any(Location.class));
    }
}
