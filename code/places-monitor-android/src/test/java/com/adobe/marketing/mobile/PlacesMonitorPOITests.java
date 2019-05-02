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
// PlacesMonitorPOITests.java
//

package com.adobe.marketing.mobile;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PlacesMonitorPOITests {

    static final String KEY_IDENTIFIER = "regionid";
    static final String KEY_NAME = "regionname";
    static final String KEY_LATITUDE = "latitude";
    static final String KEY_LONGITUDE = "longitude";
    static final String KEY_RADIUS = "radius";

    private static String SAMPLE_IDENTIFIER = "identifier";
    private static String SAMPLE_NAME = "poname";
    private static double SAMPLE_LATITUDE = 33.44;
    private static double SAMPLE_LONGITUDE = -55.66;
    private static int SAMPLE_RADIUS = 300;

    // ========================================================================================
    // PlacesMonitorPOI Getters
    // ========================================================================================

    @Test
    public void test_Getters() throws Exception {
        // setup
        final PlacesMonitorPOI poi = new PlacesMonitorPOI(SAMPLE_IDENTIFIER, SAMPLE_NAME, SAMPLE_LATITUDE, SAMPLE_LONGITUDE, SAMPLE_RADIUS);

        // verify
        assertEquals(SAMPLE_IDENTIFIER, poi.getIdentifier());
        assertEquals(SAMPLE_NAME, poi.getName());
        assertEquals(SAMPLE_LATITUDE, poi.getLatitude(), 0.0);
        assertEquals(SAMPLE_LONGITUDE, poi.getLongitude(), 0.0);
        assertEquals(SAMPLE_RADIUS, poi.getRadius(), 0.0);
    }


    // ========================================================================================
    // PlacesMonitorPOI - deserialize
    // ========================================================================================

    @Test (expected = IllegalArgumentException.class)
    public void test_deserialize_when_null() throws VariantException {
        final PlacesMonitorPOIVariantSerializer serializer = new PlacesMonitorPOIVariantSerializer();
        serializer.deserialize(null);
    }

    @Test
    public void test_deserialize_when_nullVariant() throws VariantException {
        final PlacesMonitorPOIVariantSerializer serializer = new PlacesMonitorPOIVariantSerializer();
        assertNull("Null Variant on deserialize should return null",serializer.deserialize(Variant.fromNull()));
    }

    @Test (expected = VariantException.class)
    public void test_deserialize_when_notVariantMap() throws VariantException {
        final PlacesMonitorPOIVariantSerializer serializer = new PlacesMonitorPOIVariantSerializer();
        serializer.deserialize(Variant.fromInteger(2));
    }

    @Test
    public void test_deserialize_Happy() throws VariantException {
        // setup : create valid poi variant
        Variant identifier = Variant.fromString(SAMPLE_IDENTIFIER);
        Variant name = Variant.fromString(SAMPLE_NAME);
        Variant latitude = Variant.fromDouble(SAMPLE_LATITUDE);
        Variant longitude = Variant.fromDouble(SAMPLE_LONGITUDE);
        Variant radius = Variant.fromInteger(SAMPLE_RADIUS);

        HashMap<String, Variant> map = new HashMap<String, Variant>();
        map.put(KEY_IDENTIFIER, identifier);
        map.put(KEY_NAME, name);
        map.put(KEY_LATITUDE, latitude);
        map.put(KEY_LONGITUDE, longitude);
        map.put(KEY_RADIUS, radius);
        Variant poiVariant = Variant.fromVariantMap(map);

        // test
        final PlacesMonitorPOIVariantSerializer serializer = new PlacesMonitorPOIVariantSerializer();
        final PlacesMonitorPOI poi = serializer.deserialize(poiVariant);

        // verify
        assertNotNull("deserialize poi should not be null", poi);
        assertEquals("deserialize poi should have correct identifier", SAMPLE_IDENTIFIER, poi.getIdentifier());
        assertEquals("deserialize poi should have correct name", SAMPLE_NAME, poi.getName());
        assertEquals("deserialize poi should have correct latitude", SAMPLE_LATITUDE, poi.getLatitude(), 0.0);
        assertEquals("deserialize poi should have correct longitude", SAMPLE_LONGITUDE, poi.getLongitude(), 0.0);
        assertEquals("deserialize poi should have correct radius", SAMPLE_RADIUS, poi.getRadius(), 0.0);
    }

    // ========================================================================================
    // PlacesMonitorPOI - serialize
    // ========================================================================================

    @Test
    public void test_serialize_when_nullPOI() throws VariantException {

        // test
        final PlacesMonitorPOIVariantSerializer serializer = new PlacesMonitorPOIVariantSerializer();
        Variant poiVariant = serializer.serialize(null);

        // verify
        assertEquals("Variant should be of type Variant NULL", VariantKind.NULL, poiVariant.getKind());
    }
    
    @Test
    public void test_serialize_happy() throws VariantException{

        // setup
        final PlacesMonitorPOI poi = new PlacesMonitorPOI(SAMPLE_IDENTIFIER, SAMPLE_NAME, SAMPLE_LATITUDE, SAMPLE_LONGITUDE, SAMPLE_RADIUS);

        // test
        PlacesMonitorPOIVariantSerializer serializer = new PlacesMonitorPOIVariantSerializer();
        Variant poiVariant = serializer.serialize(poi);

        // verify
        final Map<String, Variant> variantMap = poiVariant.getVariantMap();
        assertEquals("serialize poi should have correct identifier", SAMPLE_IDENTIFIER, Variant.optVariantFromMap(variantMap, KEY_IDENTIFIER).optString(null));
        assertEquals("serialize poi should have correct name", SAMPLE_NAME, Variant.optVariantFromMap(variantMap, KEY_NAME).optString(null));
        assertEquals("serialize poi should have correct latitude", SAMPLE_LATITUDE, Variant.optVariantFromMap(variantMap, KEY_LATITUDE).optDouble(0.00), 0.0);
        assertEquals("serialize poi should have correct longitude", SAMPLE_LONGITUDE, Variant.optVariantFromMap(variantMap, KEY_LONGITUDE).optDouble(0.00), 0.0);
        assertEquals("serialize poi should have correct radius", SAMPLE_RADIUS, Variant.optVariantFromMap(variantMap, KEY_RADIUS).optInteger(0), 0.0);
    }



}
