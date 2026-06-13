# TruckTrack Android App — Build Instructions

## Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Java 17

## Steps to build APK

### 1. Open project in Android Studio
- Open Android Studio
- Click "Open" → select the `trucktrack/android/` folder
- Wait for Gradle sync to complete (2-3 minutes first time)

### 2. Build debug APK (for testing)
- Menu: Build → Build Bundle(s) / APK(s) → Build APK(s)
- APK location: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Install on driver's phone
Option A — USB:
- Enable Developer Options on Android phone
- Enable USB Debugging
- Connect phone via USB
- Click Run ▶ in Android Studio

Option B — Copy APK:
- Copy app-debug.apk to phone
- Enable "Install from unknown sources" in Settings
- Open the APK file to install

## How the driver uses it
1. Open TruckTrack app
2. Enter Trip ID (given by dispatcher from dashboard)
3. Tap "Load Trip"
4. Tap "Confirm materials loaded"
5. Tap "Start trip" — GPS tracking begins automatically
6. Drive to destination
7. Tap "Mark arrived"
8. Tap "Confirm materials unloaded"
9. Tap "Complete trip"

## Backend URL
Currently pointing to: https://trucktrack-backend.onrender.com
To change: edit ApiClient.kt → BASE_URL constant
