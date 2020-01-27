/*
 Copyright 2020 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
*/

//
// PlacesMonitorUtil.java
//

package com.adobe.marketing.mobile;

import android.content.Context;
import android.content.SharedPreferences;

class PlacesMonitorUtil {

    /**
     * Getter for the applications {@link SharedPreferences}
     * <p>
     * Returns null if the app context is not available
     *
     * @return a {@code SharedPreferences} instance
     */
    static SharedPreferences getSharedPreferences() {
        Context appContext = App.getAppContext();

        if (appContext == null) {
            return null;
        }

        return appContext.getSharedPreferences(PlacesMonitorConstants.SharedPreference.MASTER_KEY, 0);
    }

}
