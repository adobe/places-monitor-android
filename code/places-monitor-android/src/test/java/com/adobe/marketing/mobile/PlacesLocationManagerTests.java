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
// PlacesLocationManagerTests.java
//


package com.adobe.marketing.mobile;


import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;


import java.util.HashMap;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class, App.class, LocationServices.class, PendingIntent.class, LocationResult.class, Build.class, PlacesActivity.class})
public class PlacesLocationManagerTests {
	private PlacesLocationManager locationManager;

	@Mock
	Context context;

	@Mock
	FusedLocationProviderClient locationProviderClient;

	@Mock
	PendingIntent pendingIntent;

	@Mock
	Activity activity;

	@Mock
	SettingsClient mockSettingsClient;

	@Mock
	Task<Void> mockTask;

	@Mock
	LocationSettingsResponse mockLocationSettingsResponse;

	@Mock
	Task<Location> mockTaskLocation;

	@Mock
	Task<LocationSettingsResponse> mockTaskSettingsResponse;

	@Mock
	Location mockLocation;

	@Mock
	PlacesMonitorInternal mockPlacesMonitorInternal;

	@Mock
	SharedPreferences mockSharedPreference;

	@Mock
	SharedPreferences.Editor mockSharedPreferenceEditor;

	@Before
	public void before() {
		PowerMockito.mockStatic(App.class);
		PowerMockito.mockStatic(Build.class);
		PowerMockito.mockStatic(LocationServices.class);
		PowerMockito.mockStatic(PendingIntent.class);
		PowerMockito.mockStatic(PlacesActivity.class);

		// mock static methods
		Mockito.when(App.getAppContext()).thenReturn(context);
		Mockito.when(App.getCurrentActivity()).thenReturn(activity);
		Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(locationProviderClient);
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(pendingIntent);
		Mockito.when(App.getAppContext()).thenReturn(context);
		Mockito.when(LocationServices.getSettingsClient(context)).thenReturn(mockSettingsClient);
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(true);
		Mockito.when(PlacesActivity.isBackgroundPermissionGranted()).thenReturn(true);

		// mock instance methods
		Mockito.when(context.getSharedPreferences(PlacesMonitorTestConstants.SharedPreference.MASTER_KEY,
					 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
		Mockito.when(locationProviderClient.removeLocationUpdates(pendingIntent)).thenReturn(mockTask);
		Mockito.when(mockSettingsClient.checkLocationSettings(any(LocationSettingsRequest.class))).thenReturn(
			mockTaskSettingsResponse);
		Mockito.when(locationProviderClient.getLastLocation()).thenReturn(mockTaskLocation);

		locationManager = Mockito.spy(new PlacesLocationManager(mockPlacesMonitorInternal));
	}


	// ========================================================================================
	// beginLocationTracking
	// ========================================================================================

	@Test
	public void test_beginLocationTracking() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor = ArgumentCaptor.forClass(LocationRequest.class);

		// test
		locationManager.beginLocationTracking();

		// verify
		verify(mockTaskSettingsResponse, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(mockTaskSettingsResponse, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger the success callback
		onSuccessCallback.getValue().onSuccess(mockLocationSettingsResponse);

		// verify if location updates are requested
		verify(locationProviderClient, times(1)).requestLocationUpdates(locationRequestArgumentCaptor.capture(),
				eq(pendingIntent));
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertTrue("The location update flag should be set to true", hasMonitoringStarted);

		// verify the location request parameters
		assertEquals("the location request interval should be correct", PlacesMonitorTestConstants.Location.REQUEST_INTERVAL,
					 locationRequestArgumentCaptor.getValue().getInterval());
		assertEquals("the location fastest request interval should be correct",
					 PlacesMonitorTestConstants.Location.REQUEST_FASTEST_INTERVAL,
					 locationRequestArgumentCaptor.getValue().getFastestInterval());
		assertEquals("the location small displacement should be correct",
					 PlacesMonitorTestConstants.Location.REQUEST_SMALLEST_DISPLACEMENT,
					 locationRequestArgumentCaptor.getValue().getSmallestDisplacement(), 0.0);
	}


	@Test
	public void test_beginLocationTracking_when_contextIsNull() {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		locationManager.beginLocationTracking();

		// verify if location updates are not requested
		verify(mockSettingsClient, times(0)).checkLocationSettings(any(LocationSettingsRequest.class));
		verify(locationProviderClient, times(0)).requestLocationUpdates(any(LocationRequest.class), eq(pendingIntent));
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertFalse("The location update flag should be set to false", hasMonitoringStarted);
	}


	@Test
	public void test_beginLocationTracking_when_fusedLocationProviderClient_null() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor = ArgumentCaptor.forClass(LocationRequest.class);
		Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(null);

		// test
		locationManager.beginLocationTracking();

		// verify
		verify(mockTaskSettingsResponse, times(1)).addOnSuccessListener(onSuccessCallback.capture());

		// trigger the success callback
		onSuccessCallback.getValue().onSuccess(mockLocationSettingsResponse);

		// verify that the location updates are not requested
		verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(),
				eq(pendingIntent));
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertFalse("The location update flag should be set to false", hasMonitoringStarted);
	}

	@Test
	public void test_beginLocationTracking_when_pendingIntent_null() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor = ArgumentCaptor.forClass(LocationRequest.class);
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

		// test
		locationManager.beginLocationTracking();

		// verify
		verify(mockTaskSettingsResponse, times(1)).addOnSuccessListener(onSuccessCallback.capture());

		// trigger the success callback
		onSuccessCallback.getValue().onSuccess(mockLocationSettingsResponse);

		// verify that the location updates are not requested
		verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(),
				eq(pendingIntent));
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertFalse("The location update flag should be set to false", hasMonitoringStarted);
	}

	@Test
	public void test_beginLocationTracking_when_failure_withStatusCode_ResolutionRequired() {
		// setup
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor = ArgumentCaptor.forClass(LocationRequest.class);
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

		// test
		locationManager.beginLocationTracking();

		// verify
		verify(mockTaskSettingsResponse, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger that success callback
		Status status = new Status(LocationSettingsStatusCodes.RESOLUTION_REQUIRED);
		ApiException exception = new ApiException(status);
		onFailureCallback.getValue().onFailure(exception);

		// verify that the location updates are not requested
		verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(),
				eq(pendingIntent));
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertFalse("The location update flag should be set to false", hasMonitoringStarted);
	}

	@Test
	public void test_beginLocationTracking_when_failure_withStatusCode_settingsChangeUnavailable() {
		// setup
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		final ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor = ArgumentCaptor.forClass(LocationRequest.class);
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

		// test
		locationManager.beginLocationTracking();

		// verify
		verify(mockTaskSettingsResponse, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger that success callback
		Status status = new Status(LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE);
		ApiException exception = new ApiException(status);
		onFailureCallback.getValue().onFailure(exception);

		// verify that the location updates are not requested
		verify(locationProviderClient, times(0)).requestLocationUpdates(locationRequestArgumentCaptor.capture(),
				eq(pendingIntent));
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertFalse("The location update flag should be set to false", hasMonitoringStarted);
		assertFalse("The location update flag should be set to false", hasMonitoringStarted);
	}

	// ========================================================================================
	// startMonitoring
	// ========================================================================================
	@Test
	public void test_startMonitoring_when_WhileInUsePermissionRequested_WhileInUsePermissionNotGranted() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(false);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission",
								  PlacesMonitorLocationPermission.WHILE_USING_APP);

		// test
		locationManager.startMonitoring();

		// verify the SDK askPermissions
		verifyStatic(PlacesActivity.class, Mockito.times(1));
		PlacesActivity.askPermission(PlacesMonitorLocationPermission.WHILE_USING_APP);

		// verify if location updates are not requested
		verify(locationManager, times(0)).beginLocationTracking();
	}

	@Test
	public void test_startMonitoring_when_WhileInUsePermissionRequested_WhileInUsePermissionGranted() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(true);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission",
								  PlacesMonitorLocationPermission.WHILE_USING_APP);

		// test
		locationManager.startMonitoring();

		// verify the SDK does not ask anymore permissions
		verifyStatic(PlacesActivity.class, Mockito.times(0));
		PlacesActivity.askPermission(any(PlacesMonitorLocationPermission.class));

		// verify if location updates are requested
		verify(locationManager, times(1)).beginLocationTracking();
	}

	@Test
	public void test_startMonitoring_when_AlwaysAllow_Requested_WhileInUsePermissionGranted() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(true);
		Mockito.when(PlacesActivity.isBackgroundPermissionGranted()).thenReturn(false);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission", PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// test
		locationManager.startMonitoring();

		// verify the SDK asks from background permission
		verifyStatic(PlacesActivity.class, Mockito.times(1));
		PlacesActivity.askPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify if location updates are not requested
		verify(locationManager, times(0)).beginLocationTracking();
	}

	@Test
	public void test_startMonitoring_when_AlwaysAllow_Requested_BackgroundPermissionGranted() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(true);
		Mockito.when(PlacesActivity.isBackgroundPermissionGranted()).thenReturn(true);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission", PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// test
		locationManager.startMonitoring();

		// verify the SDK does not ask anymore permissions
		verifyStatic(PlacesActivity.class, Mockito.times(0));
		PlacesActivity.askPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify if location updates are requested
		verify(locationManager, times(1)).beginLocationTracking();
	}


	@Test
	public void test_startMonitoring_when_WhileInUsePermissionRequested_BackgroundPermissionGranted() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(true);
		Mockito.when(PlacesActivity.isBackgroundPermissionGranted()).thenReturn(true);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission",
								  PlacesMonitorLocationPermission.WHILE_USING_APP);

		// test
		locationManager.startMonitoring();

		// verify the SDK does not ask anymore permissions
		verifyStatic(PlacesActivity.class, Mockito.times(0));
		PlacesActivity.askPermission(any(PlacesMonitorLocationPermission.class));

		// verify if location updates are requested
		verify(locationManager, times(1)).beginLocationTracking();
	}

	@Test
	public void test_startMonitoring_when_SetRequestPermissionNONE_and_noLocationPermissionGrantedByApp() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(false);
		Mockito.when(PlacesActivity.isBackgroundPermissionGranted()).thenReturn(false);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission",
				PlacesMonitorLocationPermission.NONE);

		// test
		locationManager.startMonitoring();

		// verify if location updates are not requested
		verify(locationManager, times(0)).beginLocationTracking();
	}

	@Test
	public void test_startMonitoring_when_SetRequestPermissionNONE_and_whileInUsePermissionGrantedByApp() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(true);
		Mockito.when(PlacesActivity.isBackgroundPermissionGranted()).thenReturn(false);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission",
				PlacesMonitorLocationPermission.NONE);

		// test
		locationManager.startMonitoring();

		// verify if location updates are requested
		verify(locationManager, times(1)).beginLocationTracking();
	}

	@Test
	public void test_startMonitoring_when_SetRequestPermissionNONE_and_AlwaysPermissionGrantedByApp() {
		// setup
		Mockito.when(PlacesActivity.isWhileInUsePermissionGranted()).thenReturn(false);
		Mockito.when(PlacesActivity.isBackgroundPermissionGranted()).thenReturn(true);
		Whitebox.setInternalState(locationManager, "requestedLocationPermission",
				PlacesMonitorLocationPermission.NONE);

		// test
		locationManager.startMonitoring();

		// verify if location updates are requested
		verify(locationManager, times(1)).beginLocationTracking();
	}


	// ========================================================================================
	// stopMonitoring
	// ========================================================================================
	@Test
	public void test_stopMonitoring() {
		// setup
		final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);

		// test
		locationManager.stopMonitoring();

		// verify
		verify(mockTask, times(1)).addOnCompleteListener(onCompleteCallback.capture());
		verify(locationProviderClient, times(1)).removeLocationUpdates(pendingIntent);

		// trigger that callback
		onCompleteCallback.getValue().onComplete(mockTask);
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertFalse("The location update flag should be reset to false", hasMonitoringStarted);
	}

	@Test
	public void test_stopMonitoring_when_fusedLocationProviderClient_null() {
		// setup
		final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);
		Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(null);

		// test
		locationManager.stopMonitoring();

		// verify
		verify(mockTask, times(0)).addOnCompleteListener(onCompleteCallback.capture());
		verify(locationProviderClient, times(0)).removeLocationUpdates(pendingIntent);
	}

	@Test
	public void test_stopMonitoring_when_context_null() {
		// setup
		final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		locationManager.stopMonitoring();

		// verify
		verify(mockTask, times(0)).addOnCompleteListener(onCompleteCallback.capture());
		verify(locationProviderClient, times(0)).removeLocationUpdates(pendingIntent);
	}


	@Test
	public void test_stopMonitoring_when_pendingIntent_null() {
		// setup
		final ArgumentCaptor<OnCompleteListener> onCompleteCallback = ArgumentCaptor.forClass(OnCompleteListener.class);
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

		// test
		locationManager.stopMonitoring();

		// verify
		verify(mockTask, times(0)).addOnCompleteListener(onCompleteCallback.capture());
		verify(locationProviderClient, times(0)).removeLocationUpdates(pendingIntent);
	}

	// ========================================================================================
	// updateLocation
	// ========================================================================================

	@Test
	public void test_updateLocation() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		Whitebox.setInternalState(locationManager, "hasMonitoringStarted", true);
		Whitebox.setInternalState(locationManager, "fusedLocationClient", locationProviderClient);

		// test
		locationManager.updateLocation();

		// verify
		verify(mockTaskLocation, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(mockTaskLocation, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger the success callback
		onSuccessCallback.getValue().onSuccess(mockLocation);

		// verify
		verify(mockPlacesMonitorInternal, times(1)).getPOIsForLocation(mockLocation);
	}


	@Test
	public void test_updateLocation_when_successButNullLocation() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		Whitebox.setInternalState(locationManager, "hasMonitoringStarted", true);

		// test
		locationManager.updateLocation();

		// verify
		verify(mockTaskLocation, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(mockTaskLocation, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger the success callback
		onSuccessCallback.getValue().onSuccess(null);

		// verify
		verify(mockPlacesMonitorInternal, times(1)).getPOIsForLocation(null);
	}

	@Test
	public void test_updateLocation_when_monitoringNotStarted() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		Whitebox.setInternalState(locationManager, "hasMonitoringStarted", false);

		// test
		locationManager.updateLocation();

		// verify
		verify(mockTaskLocation, times(0)).addOnSuccessListener(onSuccessCallback.capture());

		// verify
		verify(mockPlacesMonitorInternal, times(0)).getPOIsForLocation(mockLocation);
	}

	@Test
	public void test_updateLocation_when_locationClient_null() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		Whitebox.setInternalState(locationManager, "hasMonitoringStarted", true);
		Mockito.when(LocationServices.getFusedLocationProviderClient(context)).thenReturn(null);

		// test
		locationManager.updateLocation();

		// verify
		verify(mockTaskLocation, times(0)).addOnSuccessListener(onSuccessCallback.capture());

		// verify
		verify(mockPlacesMonitorInternal, times(0)).getPOIsForLocation(mockLocation);
	}

	@Test
	public void test_updateLocation_when_FailureToReceiveLocation() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		Whitebox.setInternalState(locationManager, "hasMonitoringStarted", true);

		// test
		locationManager.updateLocation();

		// verify
		verify(mockTaskLocation, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(mockTaskLocation, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger the failure callback
		onFailureCallback.getValue().onFailure(new Exception());

		// verify
		verify(mockPlacesMonitorInternal, times(0)).getPOIsForLocation(mockLocation);
	}

	// ========================================================================================
	// setLocationPermission
	// ========================================================================================
	@Test
	public void test_setLocationPermission_whenMonitoringNotStarted() throws Exception {
		// setup
		Whitebox.setInternalState(locationManager, "hasMonitoringStarted", false);

		// test
		locationManager.setLocationPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify
		verify(locationManager, times(1)).saveRequestedLocationPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);
		verify(locationManager, times(0)).startMonitoring();
	}

	@Test
	public void test_setLocationPermission_whenMonitoringStarted() throws Exception {
		// setup
		Whitebox.setInternalState(locationManager, "hasMonitoringStarted", true);

		// test
		locationManager.setLocationPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify
		verify(locationManager, times(1)).saveRequestedLocationPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);
		verify(locationManager, times(1)).startMonitoring();
	}


	// ========================================================================================
	// GetPendingIntent
	// ========================================================================================
	// testing private method to get code coverage to centum.
	@Test
	public void test_getPendingIntent_when_context_isNull() throws Exception {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		PendingIntent intent = Whitebox.invokeMethod(locationManager, "getPendingIntent");

		// verify
		assertNull(intent);
	}

	// ========================================================================================
	// setHasMonitoringStarted
	// ========================================================================================
	@Test
	public void test_setHasMonitoringStarted() {
		// test
		locationManager.setHasMonitoringStarted(true);

		// verify
		verify(mockSharedPreferenceEditor, times(1)).putBoolean(
			PlacesMonitorTestConstants.SharedPreference.HAS_MONITORING_STARTED_KEY, true);
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertTrue(hasMonitoringStarted);
	}

	@Test
	public void test_setHasMonitoringStarted_whenSharedPreferenceNull() {
		// setup
		Mockito.when(context.getSharedPreferences(PlacesMonitorTestConstants.SharedPreference.MASTER_KEY,
					 0)).thenReturn(null);
		// test
		locationManager.setHasMonitoringStarted(true);

		// verify
		verify(mockSharedPreferenceEditor, times(0)).putBoolean(
			PlacesMonitorTestConstants.SharedPreference.HAS_MONITORING_STARTED_KEY, true);
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertTrue(hasMonitoringStarted);
	}

	@Test
	public void test_setHasMonitoringStarted_whenEditorNull() {
		// setup
		Mockito.when(mockSharedPreference.edit()).thenReturn(null);

		// test
		locationManager.setHasMonitoringStarted(true);

		// verify
		verify(mockSharedPreferenceEditor, times(0)).putBoolean(
			PlacesMonitorTestConstants.SharedPreference.HAS_MONITORING_STARTED_KEY, true);
		Boolean hasMonitoringStarted = Whitebox.getInternalState(locationManager, "hasMonitoringStarted");
		assertTrue(hasMonitoringStarted);
	}



	// ========================================================================================
	// saveRequestedLocationPermission
	// ========================================================================================
	@Test
	public void test_saveRequestedLocationPermission() {
		// test
		locationManager.saveRequestedLocationPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify
		verify(mockSharedPreferenceEditor, times(1)).putString(
			PlacesMonitorTestConstants.SharedPreference.LOCATION_PERMISSION_KEY,
			PlacesMonitorLocationPermission.ALWAYS_ALLOW.getValue());
		PlacesMonitorLocationPermission locationPermission = Whitebox.getInternalState(locationManager,
				"requestedLocationPermission");
		assertEquals(PlacesMonitorLocationPermission.ALWAYS_ALLOW, locationPermission);
	}

	@Test
	public void test_saveRequestedLocationPermission_whenSharedPreferenceNull() {
		// setup
		Mockito.when(context.getSharedPreferences(PlacesMonitorTestConstants.SharedPreference.MASTER_KEY,
					 0)).thenReturn(null);
		// test
		locationManager.saveRequestedLocationPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify
		verify(mockSharedPreferenceEditor, times(0)).putString(
			PlacesMonitorTestConstants.SharedPreference.LOCATION_PERMISSION_KEY,
			PlacesMonitorLocationPermission.ALWAYS_ALLOW.getValue());
		PlacesMonitorLocationPermission locationPermission = Whitebox.getInternalState(locationManager,
				"requestedLocationPermission");
		assertEquals(PlacesMonitorLocationPermission.ALWAYS_ALLOW, locationPermission);
	}


	@Test
	public void test_saveRequestedLocationPermission_whenEditorNull() {
		// setup
		Mockito.when(mockSharedPreference.edit()).thenReturn(null);

		// test
		locationManager.saveRequestedLocationPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify
		verify(mockSharedPreferenceEditor, times(0)).putString(
			PlacesMonitorTestConstants.SharedPreference.LOCATION_PERMISSION_KEY,
			PlacesMonitorLocationPermission.ALWAYS_ALLOW.getValue());
		PlacesMonitorLocationPermission locationPermission = Whitebox.getInternalState(locationManager,
				"requestedLocationPermission");
		assertEquals(PlacesMonitorLocationPermission.ALWAYS_ALLOW, locationPermission);
	}

	// ========================================================================================
	// onLocationReceived
	// ========================================================================================

	@Test
	public void test_onLocationReceived() {
		// setup
		final ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);

		// test
		locationManager.onLocationReceived(locationUpdateEventData(22.22, 33.33));

		// verify
		verify(mockPlacesMonitorInternal, times(1)).getPOIsForLocation(locationCaptor.capture());
		assertNotNull(locationCaptor.getValue());
	}

	@Test
	public void test_onLocationReceived_InvalidLatitude() {
		// test
		locationManager.onLocationReceived(locationUpdateEventData(222.22, 33.33));

		// verify
		verify(mockPlacesMonitorInternal, times(0)).getPOIsForLocation(any(Location.class));
	}

	@Test
	public void test_onLocationReceived_InvalidLongitude() {
		// test
		locationManager.onLocationReceived(locationUpdateEventData(22.22, 333.33));

		// verify
		verify(mockPlacesMonitorInternal, times(0)).getPOIsForLocation(any(Location.class));
	}

	@Test
	public void test_onLocationReceived_EventDataWithInvalidLatitudeDataType() {
		// setup
		EventData eventData = new EventData(new HashMap<String, Variant>() {
			{
				put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE,
					Variant.fromString(PlacesMonitorTestConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_UPDATE));
				put(PlacesMonitorConstants.EventDataKey.LATITUDE, Variant.fromString("invalidTypeInLatitudeKey"));
				put(PlacesMonitorConstants.EventDataKey.LONGITUDE, Variant.fromDouble(22.3));
			}
		});

		// test
		locationManager.onLocationReceived(eventData);

		// verify
		verify(mockPlacesMonitorInternal, times(0)).getPOIsForLocation(any(Location.class));
	}


	// ========================================================================================
	// private helper methods
	// ========================================================================================

	private EventData locationUpdateEventData(final double latitude, final double longitude) {
		return new EventData(new HashMap<String, Variant>() {
			{
				put(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE,
					Variant.fromString(PlacesMonitorTestConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_UPDATE));
				put(PlacesMonitorConstants.EventDataKey.LATITUDE, Variant.fromDouble(latitude));
				put(PlacesMonitorConstants.EventDataKey.LONGITUDE, Variant.fromDouble(longitude));
			}
		});
	}


}
