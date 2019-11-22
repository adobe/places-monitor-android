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
// PlacesMonitorOnBootReceiverTests.java
//
package com.adobe.marketing.mobile;

import android.content.Context;
import android.content.Intent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PlacesMonitor.class})
public class PlacesMonitorOnBootReceiverTests {
	private PlacesMonitorOnBootReceiver receiver;

	@Mock
	Context mockContext;

	@Mock
	Intent mockIntent;

	@Before
	public void before() {
		receiver = new PlacesMonitorOnBootReceiver();
		PowerMockito.mockStatic(PlacesMonitor.class);
	}

	@Test
	public void test_OnReceive() {
		// test
		receiver.onReceive(mockContext, mockIntent);

		// verify
		verifyStatic(PlacesMonitor.class, Mockito.times(1));
		PlacesMonitor.updateLocation();
	}
}
