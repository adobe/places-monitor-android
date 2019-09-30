/*
 * ***********************************************************************
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 * Copyright 2018 Adobe Systems Incorporated
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 *************************************************************************
 */

package com.adobe.marketing.mobile;

/**
 * Represents the possible location permission settings for Android.
 *<p>
 * Apps that use location services must request location permissions. On a device that runs Android 10 (API level 29) or higher
 * users see the dialog to indicate that your app is requesting a location permission.  Android Q have added the ability for users to control which apps can access their device location when they're not using the app.
 * A new permission "Allow only while using the app" is added.
 *
 *
 * <p>
 *
 */
public enum PlacesMonitorLocationPermission {
    /**
     * Permission for Places Monitor to access location while using application.
     */
    WHILE_USING_APP("whileusingapp"),

    /**
     * Permission for Places Monitor to access location in foreground and background.
     */
    ALLOW_ALL_TIME("allowalltime");

    private final String value;

    PlacesMonitorLocationPermission(final String value) {
        this.value = value;
    }

    /**
     * Returns the string value for this enum type.
     * @return the string name for this enum type.
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns a {@link PlacesMonitorLocationPermission} object based on the provided {@code text}.
     * <p>
     * If the text provided is not valid, {@link #ALLOW_ALL_TIME} will be returned.
     *
     * @param text {@link String} to be converted to a {@code PlacesMonitorLocationPermission} object
     * @return {@code PlacesMonitorLocationPermission} object equivalent to the provided text
     */
    static PlacesMonitorLocationPermission fromString(final String text) {
        for (PlacesMonitorLocationPermission b : PlacesMonitorLocationPermission.values()) {
            if (b.value.equalsIgnoreCase(text)) {
                return b;
            }
        }

        return ALLOW_ALL_TIME;
    }
}
