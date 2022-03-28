# Notice of deprecation

On **August 31, 2021**, the **Places Monitor** extension for the Adobe Experience Platform Mobile SDKs will be **deprecated**. The Places Monitor extension will not receive further updates or support beyond August 31st.

Customers that currently use the Places Monitor extension can continue usage of this extension with the understanding that no additional updates or support will be available through Adobe.

The deprecation of the Places Monitor extension has no bearing or negative impact on the Places Service extension which will continue to be supported with enhancements and updates.

Customers that are looking to transition away from the Places Monitor extension to their own monitoring solution should review the documentation for: [Use Places Service with your own monitoring solution](https://experienceleague.adobe.com/docs/places/using/using-your-own-monitor.html?lang=en). This document explains how to interact with the Places Service by implementing [Core Location](https://developer.apple.com/documentation/corelocation) services on iOS or [Location Services](https://developers.google.com/android/reference/com/google/android/gms/location/package-summary) from Google Play.

# Getting started with the Places Monitor for Android

Table of Contents

1. [About this project](#about-this-project)
2. [Current version](#current-version)
3. [Contributing to the project](#contributing-to-the-project)
4. [Environment setup](#environment-setup)
    - [Open the Android Studio project](#open-the-android-studio-project)
    <!-- - [Command line integration](#command-line-integration) -->
5. [Tips for Location testing on Android](#tips-for-location-testing-on-android)
6. [Licensing](#licensing)

## About this project

The Places Monitor for Android is used to manage the integration between Android's Geo Location services and the Android [Places extension](https://mvnrepository.com/artifact/com.adobe.marketing.mobile/places) for the [Adobe Experience Platform SDK](https://github.com/Adobe-Marketing-Cloud/acp-sdks).

## Current version

[![Maven Central](https://img.shields.io/maven-central/v/com.adobe.marketing.mobile/places-monitor.svg?logo=android&logoColor=white&label=places-monitor)](https://mvnrepository.com/artifact/com.adobe.marketing.mobile/places-monitor)
[![CircleCI](https://img.shields.io/circleci/project/github/adobe/places-monitor-android/dev.svg?logo=circleci)](https://circleci.com/gh/adobe/workflows/places-monitor-android)

Integrate the Places Monitor into your app by including the following in your gradle file's `dependencies`:

```implementation 'com.adobe.marketing.mobile:places-monitor:2.+'```

<!--
[![CircleCI](https://img.shields.io/circleci/project/github/adobe/places-monitor-android/master.svg?logo=circleci)](https://circleci.com/gh/adobe/workflows/places-monitor-android)
[![Code Coverage](https://img.shields.io/codecov/c/github/adobe/places-monitor-android/master.svg?logo=codecov)](https://codecov.io/gh/adobe/places-monitor-android/branch/master)
-->

## Contributing to the project

Adobe is not currently accepting external contributions for this project, as it is still in the process of being fully set up. It is our desire to open this project to external contributions in the near future.

We look forward to working with you!

## Environment setup

Android Studio 3.4 or newer is required to open this project.  If necessary, please [update here](https://developer.android.com/studio).

#### Open the Android Studio project

In Android Studio, open the `code/build.gradle` file

<!--
#### Command line integration

From command line you can build the project by running the following command:

~~~~
make build
~~~~

You can also run the unit test suite from command line:

~~~~
make test
~~~~
-->

## Tips for Location testing on Android

#### Running the app on Android API 26 and above

To improve user experience, Android 8.0 (Oreo - API level 26) imposes background execution limits, a mechanism which limits certain behaviors by apps that are not running in the foreground (for more information, read the [Android documentation here](https://developer.android.com/about/versions/oreo/background-location-limits)). The result of this is a considerably lower chance of triggering a location update while testing a backgrounded app on an emulator. One way of making sure you get a location update / Geofence trigger while your app is in the background is by opening the Google maps app in the emulator and start faking the location. This usually results in a geofence event being triggered within a few seconds.

#### Running the app on Android API 25 and below

 - In Android Nougat and other previous versions, users must both give permission for an app to receive  Location updates and enable Google Play's Location Services. The easiest way to do this is by opening the "Google Map" app and then granting the permission for google services thorough that app.

- If you are running in an Android Emulator older than API 24, there is a chance that the Google Play Services for providing location is not updated. In such case you will be presented with the prompt to update Google Play Services within your application.

## Licensing
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
