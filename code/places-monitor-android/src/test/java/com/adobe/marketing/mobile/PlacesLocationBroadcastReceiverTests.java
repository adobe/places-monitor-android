/* **************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2019 Adobe Inc.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Inc. and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Inc..
 **************************************************************************/

package com.adobe.marketing.mobile;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.LocationResult;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.*;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({LocationResult.class, LocalBroadcastManager.class})
public class PlacesLocationBroadcastReceiverTests {

	static final String ACTION_LOCATION_UPDATE =
		"com.adobe.marketing.mobile.PlacesLocationBroadcastReceiver.locationUpdates";
	private PlacesLocationBroadcastReceiver receiver;

	@Mock
	Context mockContext;

	@Mock
	Intent mockIntent;

	@Mock
	LocalBroadcastManager mockBroadcastManager;

	@Before
	public void before() throws Exception {
		receiver = new PlacesLocationBroadcastReceiver();
	}

	// ========================================================================================
	// onReceive - Location Update
	// ========================================================================================

	@Test
	public void test_OnReceive() throws Exception {
		// setup
		initiateMocking();

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify
		verify(mockBroadcastManager, times(1)).sendBroadcast(mockIntent);
		verify(mockIntent, times(1)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION);
	}

	@Test
	public void test_OnReceive_when_intentIsNull() throws Exception {
		// setup
		initiateMocking();

		// test
		receiver.onReceive(mockContext, null);

		// verify
		verify(mockBroadcastManager, times(0)).sendBroadcast(mockIntent);
		verify(mockIntent, times(0)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION);
	}

	@Test
	public void test_OnReceive_when_intentHasDifferentAction() throws Exception {
		// setup
		initiateMocking();
		when(mockIntent.getAction()).thenReturn("unknownAction");

		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify
		verify(mockBroadcastManager, times(0)).sendBroadcast(mockIntent);
		verify(mockIntent, times(0)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION);
	}


	@Test
	public void test_OnReceive_when_contextIsNull() throws Exception {
		// setup
		initiateMocking();

		// test
		receiver.onReceive(null, mockIntent);

		// verify
		verify(mockBroadcastManager, times(0)).sendBroadcast(mockIntent);
		verify(mockIntent, times(0)).setAction(PlacesMonitorConstants.INTERNAL_INTENT_ACTION_LOCATION);
	}

	private void initiateMocking() throws Exception {
		// static mocks
		PowerMockito.mockStatic(LocalBroadcastManager.class);
		PowerMockito.mockStatic(LocationResult.class);
		PowerMockito.when(LocalBroadcastManager.class, "getInstance", any(Context.class)).thenReturn(mockBroadcastManager);

		when(mockIntent.getAction()).thenReturn(ACTION_LOCATION_UPDATE);
	}

}
