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
// PlacesMonitorListenerPlacesResponseContent.java
//

package com.adobe.marketing.mobile;

class PlacesMonitorListenerPlacesResponseContent extends ExtensionListener {

	protected PlacesMonitorListenerPlacesResponseContent(final ExtensionApi extension, final String type,
			final String source) {
		super(extension, type, source);
	}

	@Override
	public void hear(final Event event) {
		if (event.getEventData() == null) {
			return;
		}

		final PlacesMonitorInternal parentExtension = (PlacesMonitorInternal) super.getParentExtension();

		if (parentExtension == null) {
			return;
		}

		parentExtension.getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				// handle places response event
				parentExtension.queueEvent(event);
				parentExtension.processEvents();
			}
		});

	}
}
