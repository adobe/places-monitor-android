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
// LocalNotification.java
//

package com.adobe.marketing.mobile;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;


/**
 * TODO : Remove before release
 */
public class LocalNotification {

    private static final String CHANNEL_ID = "channel_01";


    static void sendNotification(final String notificationTitle, final String notificationMessage) {
        try {
            // Get an instance of the Notification manager
            Context context = App.getAppContext();

            if (context == null) {
                return;
            }

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            // Android O requires a Notification Channel.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the channel for the notification
                NotificationChannel mChannel =
                        new NotificationChannel(CHANNEL_ID, "Places Monitor Peaks", NotificationManager.IMPORTANCE_DEFAULT);

                // Set the Notification Channel for the Notification Manager.
                mNotificationManager.createNotificationChannel(mChannel);
            }

            // Get a notification builder that's compatible with platform versions >= 4
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

            // Define the notification settings.
            builder.setSmallIcon(getSmallIcon())
                    .setColor(Color.RED)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationMessage);

            // Set the Channel ID for Android O.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID); // Channel ID
            }

            // Dismiss notification once the user touches it.
            builder.setAutoCancel(true);

            // Issue the notification
            mNotificationManager.notify(0, builder.build());
        } catch (Exception excetion) {

        }

    }

    private static int getSmallIcon() {
        return App.getSmallIconResourceID() != -1 ? App.getSmallIconResourceID() :
                android.R.drawable.sym_def_app_icon;
    }
}
