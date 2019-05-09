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

class PlacesMonitorListenerMonitorRequestContent extends ExtensionListener {

	protected PlacesMonitorListenerMonitorRequestContent(final ExtensionApi extension, final String type,
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
				// handle places monitor request event
				parentExtension.queueEvent(event);
				parentExtension.processEvents();
			}
		});

	}

}
