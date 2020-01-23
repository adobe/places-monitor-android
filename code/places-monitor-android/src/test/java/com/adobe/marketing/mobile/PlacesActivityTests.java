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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
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
@PrepareForTest({Context.class, App.class, ActivityCompat.class, Build.class, PlacesMonitor.class, LocationSettingsStates.class, Intent.class, MobileCore.class, Places.class})
public class PlacesActivityTests {
	private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
	private static final String BACKGROUND_LOCATION = Manifest.permission.ACCESS_BACKGROUND_LOCATION;
	private static final String INTENT_PERMISSION_KEY = "intent permission key";


	// argument captors
	final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
	final ArgumentCaptor<ExtensionErrorCallback> callbackCaptor = ArgumentCaptor.forClass(ExtensionErrorCallback.class);

	@Mock
	Context context;

	@Mock
	Bundle mockBundle, mockExtra;

	@Mock
	Window window;

	@Mock
	PlacesActivity placesActivity;

	@Mock
	Intent mockIntent;

	@Mock
	LocationSettingsStates locationSettingsStates;

	@Mock
	SharedPreferences mockSharedPreference;

	@Mock
	SharedPreferences.Editor mockSharedPreferenceEditor;

	@Before
	public void before() throws Exception {
		PowerMockito.mockStatic(Places.class);
		PowerMockito.mockStatic(PlacesMonitor.class);
		PowerMockito.mockStatic(Build.class);
		PowerMockito.mockStatic(App.class);
		PowerMockito.mockStatic(ActivityCompat.class);
		PowerMockito.mockStatic(LocationSettingsStates.class);
		PowerMockito.mockStatic(MobileCore.class);

		// set static variables
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 25);

		// mock static methods
		Mockito.when(App.getAppContext()).thenReturn(context);
		Mockito.when(ActivityCompat.shouldShowRequestPermissionRationale(placesActivity, FINE_LOCATION)).thenReturn(false);

		// mock other methods
		Mockito.when(placesActivity.getWindow()).thenReturn(window);
		Mockito.when(placesActivity.getIntent()).thenReturn(mockIntent);
		Mockito.when(mockIntent.getExtras()).thenReturn(mockExtra);
		Mockito.when(context.getSharedPreferences(PlacesMonitorTestConstants.SharedPreference.MASTER_KEY,
				0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
	}

	// ========================================================================================
	// updateLocationAuthorizationStatus
	// ========================================================================================
	@Test
	public void test_UpdateLocationAuthorizationStatus_when_APILevel_below_AndroidM() throws Exception{
		// setup
		// API level for Android M is 23
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 19);

		// test
		PlacesActivity.updateLocationAuthorizationStatus();

		// verify
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.ALWAYS);
	}

	@Test
	public void test_UpdateLocationAuthorizationStatus_when_AppContextNull() throws Exception{
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		PlacesActivity.updateLocationAuthorizationStatus();

		// verify
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.UNKNOWN);
	}

	@Test
	public void test_UpdateLocationAuthorizationStatus_when_only_FineLocationGranted_APILevel_below_AndroidQ() throws Exception{
		// setup
		// API level for Android Qis 29
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 25);
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);

		// test
		PlacesActivity.updateLocationAuthorizationStatus();

		// verify
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.ALWAYS);
	}


	@Test
	public void test_UpdateLocationAuthorizationStatus_when_only_FineLocationGranted_APILevel_above_AndroidQ() throws Exception{
		// setup
		// API level for Android Qis 29
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 29);
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);
		Mockito.when(ActivityCompat.checkSelfPermission(context, BACKGROUND_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);

		// test
		PlacesActivity.updateLocationAuthorizationStatus();

		// verify
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.WHEN_IN_USE);
	}

	@Test
	public void test_UpdateLocationAuthorizationStatus_when_backgroundLocationGranted_APILevel_above_AndroidQ() throws Exception{
		// setup
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 29);
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);
		Mockito.when(ActivityCompat.checkSelfPermission(context, BACKGROUND_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);

		// test
		PlacesActivity.updateLocationAuthorizationStatus();

		// verify
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.ALWAYS);
	}


	@Test
	public void test_UpdateLocationAuthorizationStatus_when_permissionDenied() throws Exception{
		// setup
		// mockSharedPreference to respond with true for hasLocationDialogEverPrompted
		Mockito.when(mockSharedPreference.getBoolean(anyString(),anyBoolean())).thenReturn(true);
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 29);
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);

		// test
		PlacesActivity.updateLocationAuthorizationStatus();

		// verify
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.DENIED);
	}

	@Test
	public void test_UpdateLocationAuthorizationStatus_when_permissionNotAsked() throws Exception{
		// setup
		// mockSharedPreference to respond with false for hasLocationDialogEverPrompted
		Mockito.when(mockSharedPreference.getBoolean(anyString(),anyBoolean())).thenReturn(false);
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 29);
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);

		// test
		PlacesActivity.updateLocationAuthorizationStatus();

		// verify
		verifyStatic(Places.class, times(1));
		Places.setAuthorizationStatus(PlacesAuthorizationStatus.UNKNOWN);
	}

	// ========================================================================================
	// isFineLocationPermissionGranted
	// ========================================================================================

	@Test
	public void test_isWhileInUsePermissionGranted_for_OSLessThanAndroidM() throws Exception {
		// setup
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 22);

		// test
		boolean isGranted = PlacesActivity.isWhileInUsePermissionGranted();

		// verify
		assertTrue(isGranted);
	}


	@Test
	public void test_isWhileInUsePermissionGranted_when_AppContextNull() throws Exception {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		boolean isGranted = PlacesActivity.isWhileInUsePermissionGranted();

		// verify
		assertFalse(isGranted);
	}


	@Test
	public void test_isWhileInUsePermissionGranted_when_PermissionGranted() throws Exception {
		// setup
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);

		// test
		boolean isGranted = PlacesActivity.isWhileInUsePermissionGranted();

		// verify
		assertTrue(isGranted);
	}

	@Test
	public void test_isWhileInUsePermissionGranted_when_PermissionDenied() throws Exception {
		// setup
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);

		// test
		boolean isGranted = PlacesActivity.isWhileInUsePermissionGranted();

		// verify
		assertFalse(isGranted);
	}

	// ========================================================================================
	// isBackgroundPermissionGranted
	// ========================================================================================

	@Test
	public void test_isBackgroundPermissionGranted_for_OSLessThanAndroidM() throws Exception {
		// setup
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 22);

		// test
		boolean isGranted = PlacesActivity.isBackgroundPermissionGranted();

		// verify
		assertTrue(isGranted);
	}


	@Test
	public void test_isBackgroundPermissionGranted_when_AppContextNull() throws Exception {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		boolean isGranted = PlacesActivity.isBackgroundPermissionGranted();

		// verify
		assertFalse(isGranted);
	}


	@Test
	public void test_isBackgroundPermissionGranted_when_PermissionGranted() throws Exception {
		// setup
		Mockito.when(ActivityCompat.checkSelfPermission(context,
					 BACKGROUND_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);

		// test
		boolean isGranted = PlacesActivity.isBackgroundPermissionGranted();

		// verify
		assertTrue(isGranted);
	}

	@Test
	public void test_isBackgroundPermissionGranted_when_PermissionDenied() throws Exception {
		// setup
		setFinalStatic(Build.VERSION.class.getField("SDK_INT"), 29);
		Mockito.when(ActivityCompat.checkSelfPermission(context,
					 BACKGROUND_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);

		// test
		boolean isGranted = PlacesActivity.isBackgroundPermissionGranted();

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
		PlacesActivity.askPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

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
		PlacesActivity.askPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify
		verify(context, times(0)).startActivity(any(Intent.class));
	}

	@Test
	public void test_askPermission_when_nullContext_will_notStartActivity() throws Exception {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		PlacesActivity.askPermission(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// verify
		verify(context, times(0)).startActivity(any(Intent.class));
	}

	// ========================================================================================
	// onCreate - OverriddenMethod
	// ========================================================================================
	@Test
	public void test_onCreate_willRequestPermission_AlwaysAllow() {
		// setup
		Mockito.when(mockExtra.get(INTENT_PERMISSION_KEY)).thenReturn(PlacesMonitorLocationPermission.ALWAYS_ALLOW);

		// test
		Mockito.doCallRealMethod().when(placesActivity).onCreate(mockBundle);
		placesActivity.onCreate(mockBundle);

		// verify
		verify(window, times(1)).addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		verifyStatic(ActivityCompat.class, Mockito.times(1));
		ActivityCompat.requestPermissions(eq(placesActivity), eq(new String[] {FINE_LOCATION, BACKGROUND_LOCATION}),
										  eq(PlacesMonitorTestConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE));
	}

	@Test
	public void test_onCreate_willRequestPermission_WhenInUse() {
		// setup
		Mockito.when(mockExtra.get(INTENT_PERMISSION_KEY)).thenReturn(PlacesMonitorLocationPermission.WHILE_USING_APP);

		// test
		Mockito.doCallRealMethod().when(placesActivity).onCreate(mockBundle);
		placesActivity.onCreate(mockBundle);

		// verify
		verify(window, times(1)).addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
		verifyStatic(ActivityCompat.class, Mockito.times(1));
		ActivityCompat.requestPermissions(eq(placesActivity), eq(new String[] {FINE_LOCATION}),
										  eq(PlacesMonitorTestConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE));
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
	public void test_onRequestPermissionsResult_when_PermissionGranted() throws Exception {
		// test
		Mockito.when(MobileCore.dispatchEvent(any(Event.class), any(ExtensionErrorCallback.class))).thenReturn(true);

		Mockito.doCallRealMethod().when(placesActivity).onRequestPermissionsResult(anyInt(),  any(String[].class),
				any(int[].class));
		placesActivity.onRequestPermissionsResult(PlacesMonitorTestConstants.MONITOR_LOCATION_PERMISSION_REQUEST_CODE,
				new String[] {"permissionForFineLocation"},
				new int[] {PackageManager.PERMISSION_GRANTED});

		// verify
		verifyOnPermissionGranted();

		// verify the activity is removed from UI
		verify(placesActivity, times(1)).finish();
	}

	@Test
	public void test_onRequestPermissionsResult_when_PermissionDenied() throws Exception {
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

	private void verifyOnPermissionDenied() throws Exception {
		// The OS permission change event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_OS_PERMISSION_CHANGE,
					 event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.OS, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT,
					 event.getSource());
		EventData eventData = event.getData();
		assertEquals("the event data should contain two element", 2, eventData.size());
		assertEquals("the event data should contain the right event type",
					 PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_PERMISSION_CHANGE,
					 eventData.getString2(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE));
		assertEquals("the event data should contain the right permission status",
					 PlacesMonitorConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_DENIED,
					 eventData.getString2(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION_STATUS));

		// verify the activity is removed from UI
		verify(placesActivity, times(1)).finish();
	}


	private void verifyOnPermissionGranted() throws Exception {
		// The OS permission change event should be dispatched
		verifyStatic(MobileCore.class, Mockito.times(1));
		MobileCore.dispatchEvent(eventCaptor.capture(), callbackCaptor.capture());

		// verify dispatched event
		Event event = eventCaptor.getValue();
		assertNotNull("The dispatched event should not be null", event);
		assertEquals("the event name should be correct", PlacesMonitorTestConstants.EVENTNAME_OS_PERMISSION_CHANGE,
					 event.getName());
		assertEquals("the event type should be correct", PlacesMonitorTestConstants.EventType.OS, event.getType());
		assertEquals("the event source should be correct", PlacesMonitorTestConstants.EventSource.RESPONSE_CONTENT,
					 event.getSource());
		EventData eventData = event.getData();
		assertEquals("the event data should contain two element", 2, eventData.size());
		assertEquals("the event data should contain the right event type",
					 PlacesMonitorConstants.EventDataValue.OS_EVENT_TYPE_LOCATION_PERMISSION_CHANGE,
					 eventData.getString2(PlacesMonitorConstants.EventDataKey.OS_EVENT_TYPE));
		assertEquals("the event data should contain the right permission status",
					 PlacesMonitorConstants.EventDataValue.OS_LOCATION_PERMISSION_STATUS_GRANTED,
					 eventData.getString2(PlacesMonitorConstants.EventDataKey.LOCATION_PERMISSION_STATUS));

		// verify the activity is removed from UI
		verify(placesActivity, times(1)).finish();
	}


	private void verifyOnInvalidRequestCode() {
		// verify start and stop monitoring not called
		verifyStatic(PlacesMonitor.class, Mockito.times(0));
		PlacesMonitor.start();
		verifyStatic(PlacesMonitor.class, Mockito.times(0));
		PlacesMonitor.stop(true);

		// verify the activity is removed from UI
		verify(placesActivity, times(1)).finish();
	}
}
