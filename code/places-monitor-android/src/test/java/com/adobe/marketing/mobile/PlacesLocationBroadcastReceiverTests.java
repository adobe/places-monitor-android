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

import com.google.android.gms.location.LocationResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.*;

import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PlacesMonitorDispatcher.class, LocationResult.class})
public class PlacesLocationBroadcastReceiverTests {

    static final String ACTION_LOCATION_UPDATE =
            "com.adobe.marketing.mobile.PlacesLocationBroadcastReceiver.locationUpdates";
    private PlacesLocationBroadcastReceiver receiver;
    private LocationResult locationResult;

    @Mock
    Context mockContext;

    @Mock
    Intent mockIntent;

    @Mock
    Location location1,location2;

    @Before
    public void before() throws Exception {
        List<Location> locationList = new ArrayList<>();
        locationList.add(location1);
        locationList.add(location2);
        locationResult = LocationResult.create(locationList);
        receiver = new PlacesLocationBroadcastReceiver();
    }

    // ========================================================================================
    // onReceive - Location Update
    // ========================================================================================

    @Test
    public void test_OnReceive() throws Exception {
        // setup
        List<Location> locationList = new ArrayList<>();
        locationList.add(location1);
        locationList.add(location2);
        locationResult = LocationResult.create(locationList);
        initiateMocking();

        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(1));
        PlacesMonitorDispatcher.dispatchLocation(location1);
    }


    @Test
    public void test_OnReceive_when_intentIsNull() throws Exception {
        // setup
        initiateMocking();

        // test
        receiver.onReceive(mockContext, null);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(any(Location.class));
    }

    @Test
    public void test_OnReceive_when_intentHasDifferentAction() throws Exception {
        // setup
        initiateMocking();
        when(mockIntent.getAction()).thenReturn("unknownAction");

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(any(Location.class));
    }

    @Test
    public void test_OnReceive_when_intentLocationResultIsNull() throws Exception {
        // setup
        locationResult = null;
        initiateMocking();
        Mockito.when(LocationResult.extractResult(any(Intent.class))).thenReturn(null);

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(any(Location.class));
    }


    @Test
    public void test_OnReceive_when_intentWithNoLocation() throws Exception {
        // setup
        List<Location> locationList = new ArrayList<>();
        locationResult = LocationResult.create(locationList);
        initiateMocking();

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(any(Location.class));
    }

    @Test
    public void test_OnReceive_when_intentWithLocations() throws Exception {
        // setup
        List<Location> locations = new ArrayList<Location>();
        locations.add(null);
        locations.add(location2);
        locationResult = LocationResult.create(locations);
        initiateMocking();

        // test
        receiver.onReceive(mockContext, mockIntent);

        // verify
        verifyStatic(PlacesMonitorDispatcher.class, Mockito.times(0));
        PlacesMonitorDispatcher.dispatchLocation(any(Location.class));
    }

    private void initiateMocking() throws Exception {
        // static mocks
        PowerMockito.mockStatic(PlacesMonitorDispatcher.class);
        PowerMockito.mockStatic(LocationResult.class);
        PowerMockito.when(LocationResult.class, "extractResult", any(Intent.class)).thenReturn(locationResult);

        when(mockIntent.getAction()).thenReturn(ACTION_LOCATION_UPDATE);
    }


}
