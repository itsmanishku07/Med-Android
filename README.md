# MedReport AI — Android App

Java Android app consuming the MedReport AI backend API.

## Setup

### 1. Firebase
- Go to [Firebase Console](https://console.firebase.google.com)
- Add an Android app with package `com.medreport.ai`
- Download `google-services.json` → place in `android/app/`
- Enable **Email/Password** auth in Firebase Console

### 2. Backend URL
In `app/build.gradle`, update `BASE_URL`:
```
// Emulator → your machine
buildConfigField "String", "BASE_URL", '"http://10.0.2.2:8081/api/"'

// Real device on same WiFi → your machine's local IP
buildConfigField "String", "BASE_URL", '"http://192.168.x.x:8081/api/"'
```

### 3. Vector Drawables needed
Add these to `res/drawable/` (use Android Studio's Vector Asset tool):
- `ic_home.xml`, `ic_file.xml`, `ic_chat.xml`, `ic_notification.xml`
- `ic_pill.xml`, `ic_admin.xml`, `ic_add.xml`, `ic_edit.xml`
- `ic_delete.xml`, `ic_stop.xml`

### 4. Build
```
cd android
./gradlew assembleDebug
```

## Features
| Feature | Status |
|---|---|
| Login / Register (Firebase Auth) | ✅ |
| Dashboard with stats | ✅ |
| Upload medical reports (PDF/image) | ✅ |
| View report + AI analysis | ✅ |
| Trigger AI re-analysis | ✅ |
| Doctor assignment & review | ✅ |
| Real-time chat (Socket.IO) | ✅ |
| Notifications (mark read/delete) | ✅ |
| Medicine reminders + exact alarms | ✅ |
| Alarm rings even when app is closed | ✅ (AlarmManager) |
| Continuous alarm until user stops | ✅ |
| Admin dashboard | ✅ |
| Role-based UI (Patient/Doctor/Admin) | ✅ |

## Alarm Architecture
- `ReminderScheduler` — schedules `AlarmManager.setExactAndAllowWhileIdle` for each reminder
- `AlarmReceiver` — wakes device, plays looping ringtone + vibration, shows full-screen notification
- `BootReceiver` — re-schedules all alarms after device reboot
- Alarm persists until user taps **Stop Alarm** in the notification action or opens the app
