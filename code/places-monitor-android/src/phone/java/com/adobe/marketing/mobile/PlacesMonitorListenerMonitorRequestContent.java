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
// PlacesMonitorListenerMonitorRequestContent.java
//

package com.adobe.marketing.mobile;


/**
 * Listens for {@link PlacesMonitorConstants.EventType#MONITOR}, {@link PlacesMonitorConstants.EventSource#REQUEST_CONTENT} events.
 * <p>
 * Monitor request content events consist of following events
 * <ul>
 *     <li>Event to start monitoring</li>
 *     <li>Event to stop monitoring</li>
 *     <li>Event to update location</li>
 * </ul>
 * @see PlacesMonitorInternal
 */
class PlacesMonitorListenerMonitorRequestContent extends ExtensionListener {

	/**
	 * Constructor.
	 *
	 * @param extensionApi an instance of  {@link ExtensionApi}
	 * @param type  {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	protected PlacesMonitorListenerMonitorRequestContent(final ExtensionApi extensionApi, final String type,
			final String source) {
		super(extensionApi, type, source);
	}

	/**
	 * Method that gets called when {@link PlacesMonitorConstants.EventType#MONITOR},
	 * {@link PlacesMonitorConstants.EventSource#REQUEST_CONTENT} event is dispatched through eventHub.
	 * <p>
	 * {@link PlacesMonitorInternal} queues event and attempts to process them immediately.
	 *
	 * @param event placesmonitor requestContent {@link Event} to be processed
	 * @see PlacesMonitorInternal#processEvents()
	 */
	@Override
	public void hear(final Event event) {
		if (event.getEventData() == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "EventData is null, ignoring the monitor request content event.");
			return;
		}

		final PlacesMonitorInternal parentExtension = (PlacesMonitorInternal) super.getParentExtension();

		if (parentExtension == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"The parent extension, associated with the PlacesMonitorListenerMonitorRequestContent is null, ignoring the monitor request content event.");
			return;
		}

		parentExtension.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				// handle places monitor request event
				parentExtension.queueEvent(event);
				parentExtension.processEvents();
			}
		});

	}

}
