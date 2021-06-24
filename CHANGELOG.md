### 2.2.3 (24 Jun, 2021)
- Updating README.md with notice of deprecation on August 31, 2021.

### 2.2.2 (10 May, 2021)
- Fixed a crash that was caused when application did not find the required activity for requesting location permission.

### 2.2.1 (6 May, 2020)
- Improvements to logging.

### 2.2.0 (27 Jan, 2020)
- Call new Places API to collect location authorization status when the app launches and when authorization changes while the app is running.
- Added setRequestLocationPermission API and deprecated setLocationPermission API.

### 2.1.1 (22 Nov, 2019)
- Now recognizes boot of an Android device and, if required, re-registers geofences with the OS according to the device's current location.
- Fixed a race condition that sometimes caused entry/exit events to be discarded.

### 2.1.0 (9 Oct, 2019)
- Added a new API setLocationPermission to set the type of location permission request for which the user will be prompted.
- Support for Android 10.

### 2.0.0 (6 Aug, 2019)
- Monitoring status is now persisted between launches.
- Handling of the callback resulting from a location permission request no longer requires you to extend PlacesActivity.
- Changed an existing API, allowing developers to clear all Places data from the device
    - Old API: public static void stop();
    - New API: public static void stop(final boolean clearData);
- Updated the use of the Places getNearbyPointsOfInterest API to handle error scenarios more effectively.

### 1.0.1 (30 May, 2019)
- Fixed an issue that prevented an entry event for POIs when the Places monitoring is started.

### 1.0.0 (17 May, 2019)
- Initial release of Places Monitor for Android.
