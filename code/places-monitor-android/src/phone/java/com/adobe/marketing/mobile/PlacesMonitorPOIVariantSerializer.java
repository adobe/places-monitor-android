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
// PlacesMonitorPOIVariantSerializer.java
//

package com.adobe.marketing.mobile;

import java.util.HashMap;
import java.util.Map;

class PlacesMonitorPOIVariantSerializer  implements VariantSerializer<PlacesMonitorPOI> {

    static final String IDENTIFIER = "regionid";
    static final String NAME = "regionname";
    static final String LATITUDE = "latitude";
    static final String LONGITUDE = "longitude";
    static final String RADIUS = "radius";

    @Override
    public Variant serialize(final PlacesMonitorPOI poi) throws VariantException {
        if (poi == null) {
            return Variant.fromNull();
        }

        HashMap<String, Variant> poiMap = new HashMap<String, Variant>();
        poiMap.put(IDENTIFIER, Variant.fromString(poi.getIdentifier()));
        poiMap.put(NAME, Variant.fromString(poi.getName()));
        poiMap.put(LATITUDE, Variant.fromDouble(poi.getLatitude()));
        poiMap.put(LONGITUDE, Variant.fromDouble(poi.getLongitude()));
        poiMap.put(RADIUS, Variant.fromInteger(poi.getRadius()));

        return Variant.fromVariantMap(poiMap);
    }

    @Override
    public PlacesMonitorPOI deserialize(final Variant serialized) throws VariantException {
        if (serialized == null) {
            throw new IllegalArgumentException();
        }

        if (serialized.getKind() == VariantKind.NULL) {
            return null;
        }

        final Map<String, Variant> variantMap = serialized.getVariantMap();

        final String identifier = Variant.optVariantFromMap(variantMap, IDENTIFIER).optString(null);
        final String name = Variant.optVariantFromMap(variantMap, NAME).optString(null);
        final double latitude = Variant.optVariantFromMap(variantMap, LATITUDE).optDouble(0.00);
        final double longitude = Variant.optVariantFromMap(variantMap, LONGITUDE).optDouble(0.00);
        final int radius = Variant.optVariantFromMap(variantMap, RADIUS).optInteger(0);

        PlacesMonitorPOI poi = new PlacesMonitorPOI(identifier, name, latitude, longitude, radius);
        return poi;
    }
}
