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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PlacesMonitorInternal.class, ExtensionApi.class})
public class PlacesMonitorListenerHubSharedStateTests {

	@Mock
	PlacesMonitorInternal mockPlacesMonitorInternal;

	@Mock
	ExtensionApi extensionApi;


	private int EXECUTOR_TIMEOUT = 5;  // 5 milliseconds
	private PlacesMonitorListenerHubSharedState placesMonitorListenerHubSharedState;
	private ExecutorService executor = Executors.newSingleThreadExecutor();


	@Before
	public void beforeEach() {
		placesMonitorListenerHubSharedState = new PlacesMonitorListenerHubSharedState(extensionApi,
				PlacesMonitorTestConstants.EventType.HUB, PlacesMonitorTestConstants.EventSource.SHARED_STATE);
		when(mockPlacesMonitorInternal.getExecutor()).thenReturn(executor);
		when(extensionApi.getExtension()).thenReturn(mockPlacesMonitorInternal);
	}

	@Test
	public void testHear_WhenConfigurationSharedStateEvent() {
		// setup
		EventData eventData = new EventData();
		eventData.putString(PlacesMonitorConstants.SharedState.STATEOWNER, PlacesMonitorConstants.SharedState.CONFIGURATION);
		Event event = new Event.Builder("testEvent", "test source", "test type").setData(eventData).build();

		// test
		placesMonitorListenerHubSharedState.hear(event);
		waitForExecutor();

		// verify
		verify(mockPlacesMonitorInternal, times(1)).processEvents();
	}

	@Test
	public void testHear_WithNullEventData() {
		// setup
		Event event = new Event.Builder("testEvent", PlacesMonitorTestConstants.EventType.HUB,
										PlacesMonitorTestConstants.EventSource.SHARED_STATE).setData(null).build();

		// test
		placesMonitorListenerHubSharedState.hear(event);
		waitForExecutor();

		// verify
		verify(mockPlacesMonitorInternal, times(0)).processEvents();
	}

	@Test
	public void testHear_WithNullParentExtension() {
		// setup
		EventData eventData = new EventData();
		eventData.putString(PlacesMonitorConstants.SharedState.STATEOWNER, PlacesMonitorConstants.SharedState.CONFIGURATION);
		Event event = new Event.Builder("testEvent", PlacesMonitorTestConstants.EventType.HUB,
										PlacesMonitorTestConstants.EventSource.SHARED_STATE).setData(eventData).build();
		when(extensionApi.getExtension()).thenReturn(null);

		// test
		placesMonitorListenerHubSharedState.hear(event);
		waitForExecutor();

		// verify
		verify(mockPlacesMonitorInternal, times(0)).processEvents();
	}

	@Test
	public void testHear_WhenOtherSharedStateEvent() {
		// setup
		EventData eventData = new EventData();
		eventData.putString(PlacesMonitorConstants.SharedState.STATEOWNER, "OtherSharedState");
		Event event = new Event.Builder("testEvent", PlacesMonitorTestConstants.EventType.HUB,
										PlacesMonitorTestConstants.EventSource.SHARED_STATE).setData(eventData).build();

		// test
		placesMonitorListenerHubSharedState.hear(event);
		waitForExecutor();

		// verify
		verify(mockPlacesMonitorInternal, times(0)).processEvents();
	}

	void waitForExecutor() {
		Future<?> future = executor.submit(new Runnable() {
			@Override
			public void run() {
				// Fake task to check the execution termination
			}
		});

		try {
			future.get(EXECUTOR_TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			fail(String.format("Executor took longer than %s (sec)", EXECUTOR_TIMEOUT));
		}
	}

}
