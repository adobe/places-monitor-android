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
// PlacesGeofenceManagerTests.java
//

package com.adobe.marketing.mobile;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class, App.class, LocationServices.class, PendingIntent.class, ActivityCompat.class, Intent.class, Places.class, GeofencingEvent.class})
public class PlacesGeofenceManagerTests {
	static private String MONITOR_SHARED_PREFERENCE_KEY = "com.adobe.placesMonitor";
	private final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
	private PlacesGeofenceManager geofenceManager;


	@Mock
	Context context;

	@Mock
	GeofencingEvent mockGeofencingEvent;

	@Mock
	Intent intent;

	@Mock
	GeofencingClient geofencingClient;

	@Mock
	PendingIntent geofencePendingIntent;

	@Mock
	Task<Void> addTask;

	@Mock
	Task<Void> removeTask;

	@Mock
	Void mockVoid;

	@Mock
	SharedPreferences mockSharedPreference;

	@Mock
	SharedPreferences.Editor mockSharedPreferenceEditor;


	@Before
	public void before() throws Exception {
		PowerMockito.mockStatic(App.class);
		PowerMockito.mockStatic(Places.class);
		PowerMockito.mockStatic(LocationServices.class);
		PowerMockito.mockStatic(GeofencingEvent.class);
		PowerMockito.mockStatic(PendingIntent.class);
		PowerMockito.mockStatic(ActivityCompat.class);

		geofenceManager = new PlacesGeofenceManager();

		// mock static methods
		Mockito.when(App.getAppContext()).thenReturn(context);
		Mockito.when(LocationServices.getGeofencingClient(context)).thenReturn(geofencingClient);
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(geofencePendingIntent);
		Mockito.when(App.getAppContext()).thenReturn(context);
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_GRANTED);

		// mock instance methods
		Mockito.when(context.getSharedPreferences(MONITOR_SHARED_PREFERENCE_KEY, 0)).thenReturn(mockSharedPreference);
		Mockito.when(mockSharedPreference.edit()).thenReturn(mockSharedPreferenceEditor);
		Mockito.when(geofencingClient.removeGeofences(geofencePendingIntent)).thenReturn(removeTask);
		Mockito.when(geofencingClient.addGeofences(any(GeofencingRequest.class),
					 eq(geofencePendingIntent))).thenReturn(addTask);
		Mockito.when(geofencingClient.removeGeofences(ArgumentMatchers.<String>anyList())).thenReturn(removeTask);
	}


	// ========================================================================================
	// startMonitoringFences
	// ========================================================================================

	@Test
	public void test_startMonitoringFences_Happy() {

		// setup other captors
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		final ArgumentCaptor<GeofencingRequest> addedFences = ArgumentCaptor.forClass(GeofencingRequest.class);
		final ArgumentCaptor<OnSuccessListener> onSuccessCallbackRemoveFences = ArgumentCaptor.forClass(
					OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallbackRemoveFences = ArgumentCaptor.forClass(
					OnFailureListener.class);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// verify the removal of all old pois
		verify(geofencingClient, times(1)).removeGeofences(any(PendingIntent.class));
		verify(removeTask, times(1)).addOnSuccessListener(onSuccessCallbackRemoveFences.capture());
		verify(removeTask, times(1)).addOnFailureListener(onFailureCallbackRemoveFences.capture());

		// trigger the success callback for removal
		onSuccessCallbackRemoveFences.getValue().onSuccess(mockVoid);

		// verify the addition of new pois
		verify(geofencingClient, times(1)).addGeofences(addedFences.capture(), eq(geofencePendingIntent));
		verify(addTask, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(addTask, times(1)).addOnFailureListener(onFailureCallback.capture());

		// verify the added pois are correct
		assertEquals("pois added for monitoring should be correct", 4, addedFences.getValue().getGeofences().size());

		// trigger success callback
		onSuccessCallback.getValue().onSuccess(mockVoid);

		// verify process geofence is called twice for the newly entered poi
		verifyStatic(Places.class, Mockito.times(2));
		Places.processGeofence(any(Geofence.class), eq(Geofence.GEOFENCE_TRANSITION_ENTER));
	}

	@Test
	public void test_startMonitoringFences_onFailureToRemoveOldFences_stillAddsNewPOIs() {
		// setup other captors
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		final ArgumentCaptor<GeofencingRequest> addedFences = ArgumentCaptor.forClass(GeofencingRequest.class);
		final ArgumentCaptor<OnSuccessListener> onSuccessCallbackRemoveFences = ArgumentCaptor.forClass(
					OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallbackRemoveFences = ArgumentCaptor.forClass(
					OnFailureListener.class);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// verify the removal of all old pois
		verify(geofencingClient, times(1)).removeGeofences(any(PendingIntent.class));
		verify(removeTask, times(1)).addOnSuccessListener(onSuccessCallbackRemoveFences.capture());
		verify(removeTask, times(1)).addOnFailureListener(onFailureCallbackRemoveFences.capture());

		// trigger the failure callback for removal
		onFailureCallbackRemoveFences.getValue().onFailure(new Exception());

		// verify the addition of new pois
		verify(geofencingClient, times(1)).addGeofences(addedFences.capture(), eq(geofencePendingIntent));
		verify(addTask, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(addTask, times(1)).addOnFailureListener(onFailureCallback.capture());

		// verify the added pois are correct
		assertEquals("pois added for monitoring should be correct", 4, addedFences.getValue().getGeofences().size());

		// trigger success callback
		onSuccessCallback.getValue().onSuccess(mockVoid);
	}

	@Test
	public void test_startMonitoringFences_when_FailedToAddFences() {

		// setup other captors
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		final ArgumentCaptor<GeofencingRequest> addedFences = ArgumentCaptor.forClass(GeofencingRequest.class);
		final ArgumentCaptor<OnSuccessListener> onSuccessCallbackRemoveFences = ArgumentCaptor.forClass(
					OnSuccessListener.class);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// capture the removal callback
		verify(removeTask, times(1)).addOnSuccessListener(onSuccessCallbackRemoveFences.capture());

		// trigger the success callback for removal
		onSuccessCallbackRemoveFences.getValue().onSuccess(mockVoid);

		// verify the addition of new pois
		verify(geofencingClient, times(1)).addGeofences(addedFences.capture(), eq(geofencePendingIntent));
		verify(addTask, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(addTask, times(1)).addOnFailureListener(onFailureCallback.capture());

		// verify the added pois are correct
		assertEquals("pois added for monitoring should be correct", 4, addedFences.getValue().getGeofences().size());

		// trigger failure callback
		onFailureCallback.getValue().onFailure(new Exception());

		// verify processGeofence is called twice for the newly entered poi
		verifyStatic(Places.class, Mockito.times(2));
		Places.processGeofence(any(Geofence.class), eq(Geofence.GEOFENCE_TRANSITION_ENTER));
	}


	@Test
	public void test_startMonitoringFences_addFence_throwsSecurityException() {
		Mockito.when(geofencingClient.addGeofences(any(GeofencingRequest.class),
					 eq(geofencePendingIntent))).thenThrow(SecurityException.class);

		// setup other captors
		final ArgumentCaptor<GeofencingRequest> addedFences = ArgumentCaptor.forClass(GeofencingRequest.class);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// verify method calls
		verify(geofencingClient, times(1)).removeGeofences(any(PendingIntent.class));
		verify(geofencingClient, times(0)).addGeofences(addedFences.capture(), eq(geofencePendingIntent));
		verify(addTask, times(0)).addOnSuccessListener(any(OnSuccessListener.class));
	}

	@Test
	public void test_startMonitoringFences_when_geoFencingClient_isNull() {
		// initial setup with no pois being monitored
		Mockito.when(LocationServices.getGeofencingClient(context)).thenReturn(null);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// verify method calls
		verify(geofencingClient, times(0)).addGeofences(any(GeofencingRequest.class), eq(geofencePendingIntent));
		verify(geofencingClient, times(0)).removeGeofences(any(PendingIntent.class));
	}

	@Test
	public void test_startMonitoringFences_when_permissionDenied() {
		// initial setup with no pois being monitored
		Mockito.when(ActivityCompat.checkSelfPermission(context, FINE_LOCATION)).thenReturn(PackageManager.PERMISSION_DENIED);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// verify method calls
		verify(geofencingClient, times(0)).addGeofences(any(GeofencingRequest.class), eq(geofencePendingIntent));
		verify(geofencingClient, times(1)).removeGeofences(any(PendingIntent.class));

	}

	@Test
	public void test_startMonitoringFences_when_contextNull() {
		// initial setup with no pois being monitored
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// verify method calls
		verify(geofencingClient, times(0)).addGeofences(any(GeofencingRequest.class), eq(geofencePendingIntent));
		verify(geofencingClient, times(0)).removeGeofences(any(PendingIntent.class));
	}

	@Test
	public void test_startMonitoringFences_when_nullPendingIntent() {
		// initial setup with no pois being monitored
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

		// test
		geofenceManager.startMonitoringFences(poiListA());

		// verify method calls
		verify(geofencingClient, times(0)).addGeofences(any(GeofencingRequest.class), eq(geofencePendingIntent));
		verify(geofencingClient, times(0)).removeGeofences(any(PendingIntent.class));

	}

	// ========================================================================================
	// stopMonitoringFences
	// ========================================================================================

	@Test
	public void test_stopMonitoringFences() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);
		final ArgumentCaptor<Set<String>> persistedPOICaptor = ArgumentCaptor.forClass(Set.class);
		Whitebox.setInternalState(geofenceManager, "geofencingClient", geofencingClient);
		Whitebox.setInternalState(geofenceManager, "geofencePendingIntent", geofencePendingIntent);

		// test
		geofenceManager.stopMonitoringFences();

		// verify
		verify(geofencingClient, times(1)).removeGeofences(geofencePendingIntent);
		verify(removeTask, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(removeTask, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger the success callback
		onSuccessCallback.getValue().onSuccess(mockVoid);
	}


	@Test
	public void test_stopMonitoringFences_then_FailedToStopMonitor() {
		// setup
		final ArgumentCaptor<OnSuccessListener> onSuccessCallback = ArgumentCaptor.forClass(OnSuccessListener.class);
		final ArgumentCaptor<OnFailureListener> onFailureCallback = ArgumentCaptor.forClass(OnFailureListener.class);

		// test
		geofenceManager.stopMonitoringFences();

		// verify
		verify(geofencingClient, times(1)).removeGeofences(geofencePendingIntent);
		verify(removeTask, times(1)).addOnSuccessListener(onSuccessCallback.capture());
		verify(removeTask, times(1)).addOnFailureListener(onFailureCallback.capture());

		// trigger the failure callback
		onFailureCallback.getValue().onFailure(new Exception());
	}


	@Test
	public void test_stopMonitoringFences_when_geofencingClient_isNull() {
		// setup
		Mockito.when(LocationServices.getGeofencingClient(context)).thenReturn(null);

		// test
		geofenceManager.stopMonitoringFences();

		// verify
		verify(geofencingClient, times(0)).removeGeofences(geofencePendingIntent);
	}

	@Test
	public void test_stopMonitoringFences_when_geofencingIntent_isNull() {
		// setup
		Mockito.when(PendingIntent.getBroadcast(eq(context), eq(0), any(Intent.class),
												eq(PendingIntent.FLAG_UPDATE_CURRENT))).thenReturn(null);

		// test
		geofenceManager.stopMonitoringFences();

		// verify
		verify(geofencingClient, times(0)).removeGeofences(geofencePendingIntent);
	}


	@Test
	public void test_stopMonitoringFences_when_context_isNull() {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		geofenceManager.stopMonitoringFences();

		// verify
		verify(geofencingClient, times(0)).removeGeofences(geofencePendingIntent);
	}

	// ========================================================================================
	// onGeofenceReceived
	// ========================================================================================

	@Test
	public void test_onGeofenceReceived() throws Exception {
		// setup
		List<Geofence> obtainedGeofence = new ArrayList<>();
		Geofence geofence = new Geofence.Builder().setRequestId("id1").setTransitionTypes(
			Geofence.GEOFENCE_TRANSITION_ENTER).setCircularRegion(22.33, -33.33,
					100).setExpirationDuration(Geofence.NEVER_EXPIRE).build();
		obtainedGeofence.add(geofence);

		Mockito.when(mockGeofencingEvent.getTriggeringGeofences()).thenReturn(obtainedGeofence);
		Mockito.when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER);
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
		when(intent.getAction()).thenReturn(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);


		// test
		geofenceManager.onGeofenceReceived(intent);

		// verify
		verifyStatic(Places.class, Mockito.times(1));
		Places.processGeofence(geofence, Geofence.GEOFENCE_TRANSITION_ENTER);
	}

	@Test
	public void test_onGeofenceReceived_ForEntry_whenPOIAlreadyEntered() throws Exception {
		// setup
		HashSet<String> initialUserWithinGeofenceSet = new HashSet<String>();
		initialUserWithinGeofenceSet.add("id1");
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", initialUserWithinGeofenceSet);

		List<Geofence> obtainedGeofence = new ArrayList<>();
		Geofence geofence = new Geofence.Builder().setRequestId("id1").setTransitionTypes(
			Geofence.GEOFENCE_TRANSITION_ENTER).setCircularRegion(22.33, -33.33,
					100).setExpirationDuration(Geofence.NEVER_EXPIRE).build();
		obtainedGeofence.add(geofence);

		Mockito.when(mockGeofencingEvent.getTriggeringGeofences()).thenReturn(obtainedGeofence);
		Mockito.when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER);
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
		when(intent.getAction()).thenReturn(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);


		// test
		geofenceManager.onGeofenceReceived(intent);

		// verify
		verifyStatic(Places.class, Mockito.times(0));
		Places.processGeofence(any(Geofence.class), anyInt());
	}

	@Test
	public void test_onGeofenceReceived_ForExit_whenPOIAlreadyEntered() throws Exception {
		// setup
		HashSet<String> initialUserWithinGeofenceSet = new HashSet<String>();
		initialUserWithinGeofenceSet.add("id1");
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", initialUserWithinGeofenceSet);

		List<Geofence> obtainedGeofence = new ArrayList<>();
		Geofence geofence = new Geofence.Builder().setRequestId("id1").setTransitionTypes(
			Geofence.GEOFENCE_TRANSITION_EXIT).setCircularRegion(22.33, -33.33,
					100).setExpirationDuration(Geofence.NEVER_EXPIRE).build();
		obtainedGeofence.add(geofence);

		Mockito.when(mockGeofencingEvent.getTriggeringGeofences()).thenReturn(obtainedGeofence);
		Mockito.when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
		when(intent.getAction()).thenReturn(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);


		// test
		geofenceManager.onGeofenceReceived(intent);

		// verify
		verifyStatic(Places.class, Mockito.times(1));
		Places.processGeofence(geofence, Geofence.GEOFENCE_TRANSITION_EXIT);

		// verify result
		HashSet<String> resultUserWithInGeofences = Whitebox.getInternalState(geofenceManager, "userWithinGeofences");
		assertEquals(0, resultUserWithInGeofences.size());
	}

	@Test
	public void test_onGeofenceReceived_ForExit_whenPOINotAlreadyEntered() throws Exception {
		// setup
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", new HashSet<String>());

		List<Geofence> obtainedGeofence = new ArrayList<>();
		Geofence geofence = new Geofence.Builder().setRequestId("id1").setTransitionTypes(
			Geofence.GEOFENCE_TRANSITION_EXIT).setCircularRegion(22.33, -33.33,
					100).setExpirationDuration(Geofence.NEVER_EXPIRE).build();
		obtainedGeofence.add(geofence);

		Mockito.when(mockGeofencingEvent.getTriggeringGeofences()).thenReturn(obtainedGeofence);
		Mockito.when(mockGeofencingEvent.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
		when(intent.getAction()).thenReturn(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);

		// test
		geofenceManager.onGeofenceReceived(intent);

		// verify that exit event goes out, if the poi is not already entered.
		verifyStatic(Places.class, Mockito.times(1));
		Places.processGeofence(geofence, Geofence.GEOFENCE_TRANSITION_EXIT);

		// verify result
		HashSet<String> resultUserWithInGeofences = Whitebox.getInternalState(geofenceManager, "userWithinGeofences");
		assertEquals(0, resultUserWithInGeofences.size());
	}



	@Test
	public void test_onGeofenceReceived_when_nullIntent() throws Exception {
		// setup
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
		when(intent.getAction()).thenReturn(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);

		// test
		geofenceManager.onGeofenceReceived(null);

		// verify
		verifyStatic(Places.class, Mockito.times(0));
		Places.processGeofenceEvent(mockGeofencingEvent);
	}

	// ========================================================================================
	// findNewlyEnteredPOIs
	// ========================================================================================
	@Test
	public void test_findNewlyEnteredPOIs_when_noInitiallyEnteredPOIs() {
		// setup
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", new HashSet<String>());

		List<PlacesPOI> nearByPOIs = new ArrayList<PlacesPOI>();
		PlacesPOI poi2 = new PlacesPOI("id2", "name2", 22.22, 33.33, 100, "libraryID", 200, null);
		PlacesPOI poi3 = new PlacesPOI("id3", "name3", 22.22, 33.33, 100, "libraryID", 200, null);
		PlacesPOI poi4 = new PlacesPOI("id4", "name3", 22.22, 33.33, 100, "libraryID", 200, null);
		poi2.setContainsUser(true);
		poi3.setContainsUser(false);
		poi4.setContainsUser(true);
		nearByPOIs.add(poi2);
		nearByPOIs.add(poi3);
		nearByPOIs.add(poi4);

		// test
		List<PlacesPOI> newlyEnteredPOI = geofenceManager.findNewlyEnteredPOIs(nearByPOIs);

		// verify the stored in memory userWithinGeofences variable
		HashSet<String> resultUserWithInGeofences = Whitebox.getInternalState(geofenceManager, "userWithinGeofences");
		assertEquals(2, resultUserWithInGeofences.size());
		assertTrue(resultUserWithInGeofences.contains("id2"));
		assertTrue(resultUserWithInGeofences.contains("id4"));

		// verify newlyEntered POI
		assertEquals(2, newlyEnteredPOI.size());
		assertEquals("id2", newlyEnteredPOI.get(0).getIdentifier());
		assertEquals("id4", newlyEnteredPOI.get(1).getIdentifier());
	}

	@Test
	public void test_findNewlyEnteredPOIs_when_allPOIsAlreadyEntered() {
		// setup
		HashSet<String> initialUserWithinGeofenceSet = new HashSet<String>();
		initialUserWithinGeofenceSet.add("id1");
		initialUserWithinGeofenceSet.add("id2");
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", initialUserWithinGeofenceSet);

		List<PlacesPOI> nearByPOIs = new ArrayList<PlacesPOI>();
		PlacesPOI poi2 = new PlacesPOI("id2", "name2", 22.22, 33.33, 100, "libraryID", 200, null);
		PlacesPOI poi3 = new PlacesPOI("id3", "name3", 22.22, 33.33, 100, "libraryID", 200, null);
		PlacesPOI poi4 = new PlacesPOI("id4", "name3", 22.22, 33.33, 100, "libraryID", 200, null);
		poi2.setContainsUser(true);
		poi3.setContainsUser(false);
		poi4.setContainsUser(true);
		nearByPOIs.add(poi2);
		nearByPOIs.add(poi3);
		nearByPOIs.add(poi4);

		// test
		List<PlacesPOI> newlyEnteredPOI = geofenceManager.findNewlyEnteredPOIs(nearByPOIs);

		// verify the stored in memory userWithinGeofences variable
		HashSet<String> resultUserWithInGeofences = Whitebox.getInternalState(geofenceManager, "userWithinGeofences");
		assertEquals(2, resultUserWithInGeofences.size());
		assertTrue(resultUserWithInGeofences.contains("id2"));
		assertTrue(resultUserWithInGeofences.contains("id4"));

		// verify newlyEntered POI
		assertEquals(1, newlyEnteredPOI.size());
		assertEquals("id4", newlyEnteredPOI.get(0).getIdentifier());
	}


	@Test
	public void test_findNewlyEnteredPOIs_when_removesAllPOIsIfthereAreNoNearByPOIS() {
		// setup
		HashSet<String> initialUserWithinGeofenceSet = new HashSet<String>();
		initialUserWithinGeofenceSet.add("id1");
		initialUserWithinGeofenceSet.add("id2");
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", initialUserWithinGeofenceSet);

		// test
		List<PlacesPOI> newlyEnteredPOI = geofenceManager.findNewlyEnteredPOIs(new ArrayList<PlacesPOI>());

		// verify the stored in memory userWithinGeofences variable
		HashSet<String> resultUserWithInGeofences = Whitebox.getInternalState(geofenceManager, "userWithinGeofences");
		assertEquals(0, resultUserWithInGeofences.size());

		// verify newlyEntered POI
		assertEquals(0, newlyEnteredPOI.size());
	}

	@Test
	public void test_onGeofenceReceived_when_unknownAction() throws Exception {
		// setup
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
		when(intent.getAction()).thenReturn("unknown action");

		// test
		geofenceManager.onGeofenceReceived(intent);

		// verify
		verifyStatic(Places.class, Mockito.times(0));
		Places.processGeofenceEvent(mockGeofencingEvent);
	}

	@Test
	public void test_onGeofenceReceived_when_GeofenceEventHasError() throws Exception {
		// setup
		PowerMockito.when(GeofencingEvent.class, "fromIntent", any(Intent.class)).thenReturn(mockGeofencingEvent);
		when(intent.getAction()).thenReturn(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_GEOFENCE);
		when(mockGeofencingEvent.hasError()).thenReturn(true);

		// test
		geofenceManager.onGeofenceReceived(intent);

		// verify
		verifyStatic(Places.class, Mockito.times(0));
		Places.processGeofenceEvent(mockGeofencingEvent);
	}

	// ========================================================================================
	// loadMonitoringFences
	// ========================================================================================

	@Test
	public void test_loadPersistedData() {
		// setup
		Set<String> savedMonitoringPois = poiSetA();
		Set<String> savedUserWithinPois = poiSetB();
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", new HashSet<>());

		when(mockSharedPreference.getStringSet(eq(PlacesMonitorTestConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY),
											   any(Set.class))).thenReturn(savedUserWithinPois);

		// test
		geofenceManager.loadPersistedData();

		// verify
		assertEquals(savedUserWithinPois, Whitebox.getInternalState(geofenceManager, "userWithinGeofences"));
	}

	@Test
	public void test_loadPersistedData_whenSharedPreference_isNull() {
		// setup
		Set<String> savedMonitoringPois = poiSetA();
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", new HashSet<>());
		Mockito.when(context.getSharedPreferences(MONITOR_SHARED_PREFERENCE_KEY, 0)).thenReturn(null);

		when(mockSharedPreference.getStringSet(eq(PlacesMonitorTestConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY),
											   any(Set.class))).thenReturn(savedMonitoringPois);

		// test
		geofenceManager.loadPersistedData();


		// verify
		verify(mockSharedPreference, times(0)).getStringSet(eq(
					PlacesMonitorTestConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY), any(Set.class));
		HashSet<String> loadedUserWithinFences = Whitebox.getInternalState(geofenceManager, "userWithinGeofences");
		assertEquals(0, loadedUserWithinFences.size());
	}


	// ========================================================================================
	// saveUserWithinGeofences
	// ========================================================================================

	@Test
	public void test_saveUserWithinGeofences() {
		// setup
		Set<String> pois = poiSetA();
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", pois);
		final ArgumentCaptor<Set<String>> persistedPOICaptor = ArgumentCaptor.forClass(Set.class);

		// test
		geofenceManager.saveUserWithinGeofences();

		// verify
		verify(mockSharedPreference, times(1)).edit();
		verify(mockSharedPreferenceEditor, times(1)).putStringSet(eq(
					PlacesMonitorTestConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY), persistedPOICaptor.capture());
		verify(mockSharedPreferenceEditor, times(1)).commit();
		assertEquals(pois, persistedPOICaptor.getValue());
	}


	@Test
	public void test_saveUserWithinGeofences_when_sharedPreference_isNull() {
		// setup
		Set<String> pois = poiSetA();
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", pois);
		Mockito.when(context.getSharedPreferences(MONITOR_SHARED_PREFERENCE_KEY, 0)).thenReturn(null);

		// test
		geofenceManager.saveUserWithinGeofences();

		// verify
		verify(mockSharedPreference, times(0)).edit();
		verify(mockSharedPreferenceEditor, times(0)).putStringSet(eq(
					PlacesMonitorTestConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY),
				ArgumentMatchers.<String>anySet());
		verify(mockSharedPreferenceEditor, times(0)).commit();
	}

	@Test
	public void test_saveUserWithinGeofences_when_sharedPreferenceEditor_isNull() {
		// setup
		Set<String> pois = poiSetA();
		Whitebox.setInternalState(geofenceManager, "userWithinGeofences", pois);
		Mockito.when(mockSharedPreference.edit()).thenReturn(null);

		// test
		geofenceManager.saveUserWithinGeofences();

		// verify
		verify(mockSharedPreferenceEditor, times(0)).putStringSet(eq(
					PlacesMonitorTestConstants.SharedPreference.USERWITHIN_GEOFENCES_KEY),
				ArgumentMatchers.<String>anySet());
		verify(mockSharedPreferenceEditor, times(0)).commit();
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
		PendingIntent intent = Whitebox.invokeMethod(geofenceManager, "getGeofencePendingIntent");

		// verify
		assertNull(intent);
	}


	@Test
	public void test_getSharedPreference_when_context_isNull() throws Exception {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		SharedPreferences sharedPreferences = Whitebox.invokeMethod(geofenceManager, "getSharedPreference");

		// verify
		assertNull(sharedPreferences);
	}

	@Test
	public void test_checkPermissions_when_context_isNull() throws Exception {
		// setup
		Mockito.when(App.getAppContext()).thenReturn(null);

		// test
		Boolean permission = Whitebox.invokeMethod(geofenceManager, "checkPermissions");

		// verify
		assertFalse(permission);
	}

	// ========================================================================================
	// POI Set A
	// ========================================================================================

	private List<PlacesPOI> poiListA() {
		List<PlacesPOI> pois = new ArrayList<>();
		PlacesPOI poi1 = new PlacesPOI("id1", "name1", 22.22, 33.33, 100, "libraryID", 200, null);
		poi1.setContainsUser(true);
		pois.add(poi1);
		PlacesPOI poi2 = new PlacesPOI("id2", "name2", 22.22, 33.33, 100, "libraryID", 200, null);
		poi2.setContainsUser(true);
		pois.add(poi2);
		pois.add(new PlacesPOI("id3", "name3", 22.22, 33.33, 100, "libraryID", 200, null));
		pois.add(new PlacesPOI("id4", "name4", 22.22, 33.33, 100, "libraryID", 200, null));
		return pois;
	}

	private Set<String> poiSetA() {
		Set<String> pois = new HashSet<>();
		pois.add("id1");
		pois.add("id2");
		pois.add("id3");
		pois.add("id4");
		return pois;
	}

	// ========================================================================================
	// POI Set B
	// ========================================================================================

	private List<PlacesPOI> poiListB() {
		List<PlacesPOI> pois = new ArrayList<>();
		pois.add(new PlacesPOI("id3", "name3", 22.22, 33.33, 100, "libraryID", 200, null));
		pois.add(new PlacesPOI("id5", "name1", 22.22, 33.33, 100, "libraryID", 200, null));
		pois.add(new PlacesPOI("id6", "name2", 22.22, 33.33, 100, "libraryID", 200, null));
		return pois;
	}

	private Set<String> poiSetB() {
		Set<String> pois = new HashSet<>();
		pois.add("id3");
		pois.add("id5");
		pois.add("id6");
		return pois;
	}

	// ========================================================================================
	// POI Set C
	// ========================================================================================

	private List<PlacesPOI> poiListC() {
		List<PlacesPOI> pois = new ArrayList<>();
		pois.add(new PlacesPOI("id9", "name1", 22.22, 33.33, 100, "libraryID", 200, null));
		pois.add(new PlacesPOI("id10", "name2", 22.22, 33.33, 100, "libraryID", 200, null));
		return pois;
	}

	private Set<String> poiSetC() {
		Set<String> pois = new HashSet<>();
		pois.add("id9");
		pois.add("id10");
		return pois;
	}


}
