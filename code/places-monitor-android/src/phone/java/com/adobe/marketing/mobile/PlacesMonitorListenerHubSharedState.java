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
// PlacesMonitorListenerHubSharedState.java
//

package com.adobe.marketing.mobile;

/**
 * Listens for {@link PlacesMonitorConstants.EventType#HUB}, {@link PlacesMonitorConstants.EventSource#SHARED_STATE} events
 * and triggers the queued places monitor events waiting for valid configuration shared state
 * @see PlacesMonitorInternal
 */
class PlacesMonitorListenerHubSharedState extends ExtensionListener {

	/**
	 * Constructor.
	 *
	 * @param extensionApi an instance of  {@link ExtensionApi}
	 * @param type  {@link EventType} this listener is registered to handle
	 * @param source {@link EventSource} this listener is registered to handle
	 */
	protected PlacesMonitorListenerHubSharedState(final ExtensionApi extensionApi, final String type, final String source) {
		super(extensionApi, type, source);
	}

	/**
	 * Listens to {@code PlacesMonitorConstants.EventType#HUB}, {@code PlacesMonitorConstants.EventSource#SHARED_STATE} event.
	 * <p>
	 * Triggers the queued events which are waiting for valid configuration shared state.
	 *
	 * @param event the shared state update {@link Event}
	 * @see PlacesMonitorInternal#processEvents()
	 */
	@Override
	public void hear(final Event event) {
		if (event.getEventData() == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG, "EventData is null, ignoring the share state change event.");
			return;
		}

		final PlacesMonitorInternal parentExtension = (PlacesMonitorInternal) super.getParentExtension();

		if (parentExtension == null) {
			Log.warning(PlacesMonitorConstants.LOG_TAG,
						"The parent extension, associated with the PlacesMonitorListenerHubSharedState is null, ignoring the share state change event.");
			return;
		}

		String sharedStateOwner = event.getData().optString(PlacesMonitorConstants.SharedState.STATEOWNER,
								  null);

		if (PlacesMonitorConstants.SharedState.CONFIGURATION.equals(sharedStateOwner)) {
			parentExtension.getExecutor().execute(new Runnable() {
				@Override
				public void run() {
					parentExtension.processEvents();;
				}
			});
		}
	}

}
