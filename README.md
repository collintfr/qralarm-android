

<p align="center">
   <img src="./fastlane/metadata/android/en-US/images/icon.png" width="192" height="192"/>
</p>

<h1 align="center"><b>QRAlarm</b></h1>

Say goodbye to oversleeping with QRAlarm! 🚀

QRAlarm - QR Code Alarm Clock is an effective and smart alarm clock that gets you out of bed by making you scan the QR or Barcode to turn off the alarm! This makes it perfect for both heavy sleepers and productive early risers! 🔝

Have you ever...

⏰ ...found yourself caught in the “snooze loop” where you kept snoozing the alarm while never getting up?

😴 ...turned off your alarm giving yourself those mythical “5 more minutes” just to fall back asleep again and oversleep?

📝 ...wanted to start off the day earlier to get your tasks done but you simply couldn’t leave your bed in the morning?

Then QRAlarm is perfect for you! 🫵

QRAlarm is not just another alarm clock app - the QR alarm mission functionality is there to help you always get up on time! Because a strong and innovative alarm clock does not only wake you up - it makes sure that you GET UP! 🙌

If you are seeking to:

- Fight with oversleeping,
- Break your snooze habits,
- Improve your morning productivity,
- Build your perfect morning routine,
- Stay healthy and happy,

Then you have found the perfect alarm clock application - this is your new life hack! 📈

QRAlarm runs on Android 7.0 and later. Object recognition uses a bundled model and works offline. 📱

QRAlarm has a simple, clean and intuitive user interface that makes the experience easy and seamless! You will feel at ease using QRAlarm as it does not overwhelm the user with bloated functions. QRAlarm is uncluttered, clear and beautiful! ✨

Download the app now, set up and configure your alarms and let QRAlarm work its magic! 🪄

<p align="center">
   <a href="https://play.google.com/store/apps/details?id=com.sweak.qralarm"><img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Get it on Google Play" height=80/></a>
   <a href="https://f-droid.org/packages/com.sweak.qralarm/"><img src="https://fdroid.gitlab.io/artwork/badge/get-it-on-en.svg" alt="Get it on F-Droid" height=80/></a>
   <a href="https://apt.izzysoft.de/fdroid/index/apk/com.sweak.qralarm/"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height=80/></a>
</p>

## Screenshots
<p>  
   <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/Promo 1.png" width="210" height="470"/>  
   <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/Promo 2.png" width="210" height="470"/>  
   <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/Promo 3.png" width="210" height="470"/>  
   <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/Promo 4.png" width="210" height="470"/>  
   <img src="./fastlane/metadata/android/en-US/images/phoneScreenshots/Promo 5.png" width="210" height="470"/>
</p>  

## Setup
* Download [qralarm-android-signed.apk](https://github.com/sweakpl/qralarm-android/releases),
* Put it e.g. in a `Downloads` folder in Your Android device,
* Go to the `Downloads` folder on the Android device,
* Tap the file and install - the app doesn't require any special permissions,

or get it on [Google Play](https://play.google.com/store/apps/details?id=com.sweak.qralarm), [F-Droid](https://f-droid.org/packages/com.sweak.qralarm/) or [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/com.sweak.qralarm/).

## APK Signature Verification

All official open-source releases of QRAlarm are signed with the following SHA-256 fingerprint:
`9D:88:90:E1:97:16:01:D1:E4:E5:A1:C0:0F:F1:E3:A1:54:38:10:D2:8E:1C:5B:E4:D2:B5:C0:89:64:B1:81:57`

You can verify an APK’s signature with:
`keytool -printcert -jarfile qralarm-android-signed.apk`


## Development with devenv

Install [devenv](https://devenv.sh/getting-started/), then run commands from the repository root.
The locked environment supplies JDK 21, Android SDK 37.0, build-tools 36, NDK 28.2, and adb.
A debug build does not need a release keystore. Release signing still uses your local,
ignored `keystore.properties` and keystore.

| Command | Purpose |
| --- | --- |
| `devenv shell -- compile` | Build `app/build/outputs/apk/debug/app-debug.apk` |
| `devenv shell -- lint` | Run Android lint |
| `devenv shell -- test` | Run JVM unit tests |
| `devenv shell -- test-device` | Run Room migration and offline model tests on connected devices |
| `devenv shell -- upload-app [serial]` | Build and install the debug APK using adb |
| `devenv shell -- fetch-model` | Verify or restore the pinned model and label assets |

Enable USB debugging and accept the computer's authorization prompt on a connected phone.
Use `devenv shell -- adb devices` to find its serial. `upload-app` requires a serial when
multiple devices are connected. Installation keeps application data and requires a compatible
signing key; it reports an adb error if an installed release uses a different key.

## Object recognition alarms

Choose **Recognize an object** in the alarm editor and select one category from the searchable
list. Tap **Stop** when the alarm rings, then show the selected object to the camera. The
recognition camera shows a bounding box and confirmation progress. Torch, temporary mute,
cancellation locks, snoozing, and the configured emergency task work as they do for code alarms.
Close and reopen the scanner to retry a camera or model error; an error never dismisses the alarm.

Recognition runs entirely on the phone using the bundled EfficientDet-Lite0 int8 model and
MediaPipe Tasks Vision. All 80 COCO categories are selectable. **Bottle** includes water bottles
and other bottles; the model does not classify rooms or identify a particular physical object.
The current confirmation policy requires three consecutive detections at confidence 0.60 or
higher, spanning at least 150 ms, with no gap longer than 500 ms. CameraX keeps the latest frame
when inference cannot keep pace. These values are centralized in `ObjectConfirmation`.

The model source is Google's [versioned EfficientDet-Lite0 asset](https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/1/efficientdet_lite0.tflite),
SHA-256 `0720bf247bd76e6594ea28fa9c6f7c5242be774818997dbbeffc4da460c723bb`.
The bundled [COCO label map](https://storage.googleapis.com/mediapipe-tasks/object_detector/labelmap.txt)
has SHA-256 `f8803ef7900160c629d570848dfda4175e21667bf7b71f73f8ece4938c9f2bf2`.
Model attribution and the Apache 2.0 license are included in the application assets.
Backups now use format 2; format 1 code/button backups remain readable. Older app versions
reject format 2 rather than silently losing an object's dismissal requirement.

### Physical-device validation

Run `compile`, `lint`, `test`, and `test-device` before device trials. For toilet, toothbrush,
sink, and several water bottles, try at least ten presentations each in bright and dim light,
with varied angles and clutter. Record successful confirmations, time from first clear view
to dismissal, and failures. Then record one-minute negative trials per category with the target
absent, including similar objects. Test portrait/landscape, torch, background/foreground,
closing/reopening the scanner, camera permission denial, and Android 13 frame rotation.

Verify one-shot and repeating dismissal, stopping while snoozed, pre-ring cancellation inside
the cancellation lock, emergency dismissal, copying, chains, and a backup/restore round trip.
For QR/barcode alarms, verify assigned and unassigned codes, wrong codes, and link opening.
Calibrate the shared confidence/window constants against these recorded trials before claiming
household accuracy or latency. Re-run the confirmation tests whenever thresholds change.
