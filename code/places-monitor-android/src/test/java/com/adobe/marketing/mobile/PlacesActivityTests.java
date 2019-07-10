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
// PlacesActivityTests.java
//

package com.adobe.marketing.mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.location.LocationSettingsStates;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static com.google.android.gms.common.ConnectionResult.RESOLUTION_REQUIRED;
import static org.junit.Assert.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.mockito.ArgumentMatchers.*;


@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class, App.class, ActivityCompat.class, Build.class, PlacesMonitor.class, LocationSettingsStates.class})
public class PlacesActivityTests {
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;

    @Mock
    Context context;

    @Mock
    Bundle mockBundle;

    @Mock
    Window window;

    @Mock
    PlacesActivity placesActivity;

    @Mock
    Intent mockIntent;

    @Mock
    LocationSettingsStates locationSettingsStates;


    @Before
    public void before() throws Exception {
        PowerMockito.mockStatic(PlacesMonitor.class);
        PowerMockito.mockStatic(Build.class);
        PowerMockito.mockStatic(App.class);
        PowerMockito.mockStatic(ActivityCompat.class);
        PowerMockito.mockStatic(LocationSettingsStates.class);

        // set static variables
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 25);

        // mock static methods
        Mockito.when(App.getAppContext()).thenReturn(context);
        Mockito.when(ActivityCompat.shouldShowRequestPermissionRationale(placesActivity, FINE_LOCATION)).thenReturn(false);

        // mock other methods
        Mockito.when(placesActivity.getWindow()).thenReturn(window);
    }


    // ========================================================================================
    // isFineLocationPermissionGranted
    // ========================================================================================

    @Test
    public void test_isFineLocationPermissionGranted_for_OSLessThanAndroidM() throws Exception {
        // setup
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 22);

        // test
        boolean isGranted = PlacesActivity.isFineLocationPermissionGranted();

        // verify
        assertTrue(isGranted);
    }


    @Test
    public void test_isFineLocationPermissionGranted_when_AppContextNull() throws Exception {
        // setup
        Mockito.when(App.getAppContext()).thenReturn(null);

        // test
        boolean isGranted = PlacesActivity.isFineLocationPermissionGranted();

        // verify
        assertFalse(isGranted);
    }


    @Test
    public void test_isFineLocationPermissionGranted_when_PermissionGranted() throws Exception {
        // setup
        Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);

        // test
        boolean isGranted = PlacesActivity.isFineLocationPermissionGranted();

        // verify
        assertTrue(isGranted);
    }

    @Test
    public void test_isFineLocationPermissionGranted_when_PermissionDenied() throws Exception {
        // setup
        Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);

        // test
        boolean isGranted = PlacesActivity.isFineLocationPermissionGranted();

        // verify
        assertFalse(isGranted);
    }


    // ========================================================================================
    // askPermission
    // ========================================================================================
    @Test
    public void test_askPermission_startsActivity() {
        // setup
        final ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        // test
        PlacesActivity.askPermission();

        // verify
        verify(context, times(1)).startActivity(intentArgumentCaptor.capture());
        Intent capturedIntent = intentArgumentCaptor.getValue();
        assertNotNull(capturedIntent);
    }

    @Test
    public void test_askPermission_for_OSLessThanAndroidM_will_notStartActivity() throws Exception {
        // setup
        setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 22);

        // test
        PlacesActivity.askPermission();

        // verify
        verify(context, times(0)).startActivity(any(Intent.class));
    }

    @Test
    public void test_askPermission_when_nullContext_will_notStartActivity() throws Exception {
        // setup
        Mockito.when(App.getAppContext()).thenReturn(null);

        // test
        PlacesActivity.askPermission();

        // verify
        verify(context, times(0)).startActivity(any(Intent.class));
    }

    // ========================================================================================
    // onCreate - OverriddenMethod
    // ========================================================================================
    @Test
    public void test_onCreate_willRequestPermission() {
        // test
        Mockito.doCallRealMethod().when(placesActivity).onCreate(mockBundle);
        placesActivity.onCreate(mockBundle);

        // verify
        verify(window, times(1)).addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        verifyStatic(ActivityCompat.class, Mockito.times(1));
        ActivityCompat.requestPermissions(eq(placesActivity), any(String[].class), anyInt());
    }

    @Test
    public void test_onCreate_onShowRationaleTrue_willRequestPermission() {
        // setup
        Mockito.when(ActivityCompat.shouldShowRequestPermissionRationale(placesActivity, FINE_LOCATION)).thenReturn(true);

        // test
        Mockito.doCallRealMethod().when(placesActivity).onCreate(mockBundle);
        placesActivity.onCreate(mockBundle);

        // verify
        verifyStatic(ActivityCompat.class, Mockito.times(1));
        ActivityCompat.requestPermissions(eq(placesActivity), any(String[].class), anyInt());
    }

    // ========================================================================================
    // onRequestPermissionsResult - OverriddenMethod
    // ========================================================================================
    @Test
    public void test_onRequestPermissionsResult_when_InvalidRequestCode() {
        // setup
        int randomRequestCode = 33;

        // test
        Mockito.doCallRealMethod().when(placesActivity).onRequestPermissionsResult(anyInt(),  any(String[].class),
                any(int[].class));
        placesActivity.onRequestPermissionsResult(randomRequestCode,  new String[] {"permissionForFineLocation"}, new int[] {0});

        // verify
        verifyOnInvalidRequestCode();
    }

    @Test
    public void test_onRequestPermissionsResult_when_PermissionGranted() {
        // test
        Mockito.doCallRealMethod().when(placesActivity).onRequestPermissionsResult(anyInt(),  any(String[].class),
                any(int[].class));
        placesActivity.onRequestPermissionsResult(PlacesMonitorTestConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE,
                new String[] {"permissionForFineLocation"},
                new int[] {PackageManager.PERMISSION_GRANTED});

        // verify
        verifyOnPermissionGranted();
    }

    @Test
    public void test_onRequestPermissionsResult_when_PermissionDenied() {
        Mockito.doCallRealMethod().when(placesActivity).onRequestPermissionsResult(anyInt(),  any(String[].class),
                any(int[].class));
        placesActivity.onRequestPermissionsResult(PlacesMonitorTestConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE,
                new String[] {"permissionForFineLocation"},
                new int[] {PackageManager.PERMISSION_DENIED});

        // verify
        verifyOnPermissionDenied();
    }

    // ========================================================================================
    // onActivityResult - OverriddenMethod
    // ========================================================================================
    @Test
    public void test_onActivityResult_when_isLocationUsableFalse() throws Exception {
        // setup
        PowerMockito.when(LocationSettingsStates.class, "fromIntent", any(Intent.class)).thenReturn(locationSettingsStates);
        Mockito.when(locationSettingsStates.isLocationUsable()).thenReturn(false);

        // test
        Mockito.doCallRealMethod().when(placesActivity).onActivityResult(anyInt(), anyInt(), any(Intent.class));
        placesActivity.onActivityResult(RESOLUTION_REQUIRED, Activity.RESULT_OK, mockIntent);

        // verify
        verifyOnPermissionDenied();
    }

    @Test
    public void test_onActivityResult_when_ResultOK() throws Exception {
        // setup
        PowerMockito.when(LocationSettingsStates.class, "fromIntent", any(Intent.class)).thenReturn(locationSettingsStates);
        Mockito.when(locationSettingsStates.isLocationUsable()).thenReturn(true);

        // test
        Mockito.doCallRealMethod().when(placesActivity).onActivityResult(anyInt(), anyInt(), any(Intent.class));
        placesActivity.onActivityResult(RESOLUTION_REQUIRED, Activity.RESULT_OK, mockIntent);

        // verify
        verifyOnPermissionGranted();
    }


    @Test
    public void test_onActivityResult_when_ResultCancelled() throws Exception {
        // setup
        PowerMockito.when(LocationSettingsStates.class, "fromIntent", any(Intent.class)).thenReturn(locationSettingsStates);
        Mockito.when(locationSettingsStates.isLocationUsable()).thenReturn(true);

        // test
        Mockito.doCallRealMethod().when(placesActivity).onActivityResult(anyInt(), anyInt(), any(Intent.class));
        placesActivity.onActivityResult(RESOLUTION_REQUIRED, Activity.RESULT_CANCELED, mockIntent);

        // verify
        verifyOnPermissionDenied();
    }

    @Test
    public void test_onActivityResult_when_anyOtherResult() throws Exception {
        // setup
        PowerMockito.when(LocationSettingsStates.class, "fromIntent", any(Intent.class)).thenReturn(locationSettingsStates);
        Mockito.when(locationSettingsStates.isLocationUsable()).thenReturn(true);

        // test
        Mockito.doCallRealMethod().when(placesActivity).onActivityResult(anyInt(), anyInt(), any(Intent.class));
        placesActivity.onActivityResult(RESOLUTION_REQUIRED, Activity.RESULT_FIRST_USER, mockIntent);

        // verify
        verifyOnPermissionDenied();
    }



    private static void setFinalStatic(final Field field, final Object newValue) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, newValue);
    }

    private void verifyOnPermissionDenied() {
        // verify start is not called
        verifyStatic(PlacesMonitor.class, Mockito.times(0));
        PlacesMonitor.start();

        // verify stop monitoring is called
        verifyStatic(PlacesMonitor.class, Mockito.times(1));
        PlacesMonitor.stop();

        // verify the activity is removed from UI
        verify(placesActivity, times(1)).finish();
    }


    private void verifyOnPermissionGranted() {
        // verify start is called
        verifyStatic(PlacesMonitor.class, Mockito.times(1));
        PlacesMonitor.start();

        // verify stop monitoring is not called
        verifyStatic(PlacesMonitor.class, Mockito.times(0));
        PlacesMonitor.stop();

        // verify the activity is removed from UI
        verify(placesActivity, times(1)).finish();
    }


    private void verifyOnInvalidRequestCode() {
        // verify start and stop monitoring not called
        verifyStatic(PlacesMonitor.class, Mockito.times(0));
        PlacesMonitor.start();
        verifyStatic(PlacesMonitor.class, Mockito.times(0));
        PlacesMonitor.stop();

        // verify the activity is removed from UI
        verify(placesActivity, times(1)).finish();
    }
}
