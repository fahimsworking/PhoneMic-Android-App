# Phone Mic

One-button Android app: tap the mic to speak through your connected Bluetooth speaker, tap again to stop.

## Get the APK — easiest way (no software to install)
1. Create a free account at github.com and make a new repository (e.g. `PhoneMic`).
2. Click **"uploading an existing file"** and drag in **all** the files and folders from this zip
   (including the hidden `.github` folder — on Windows, enable "Show hidden items").
3. Open the **Actions** tab. The "Build APK" job starts by itself (about 3–4 minutes).
4. When it shows a green tick, open the run and download **PhoneMic-apk** at the bottom.
   Unzip it to get `app-debug.apk`.

## Or build with Android Studio
Open this folder in Android Studio → wait for Gradle sync → **Build ▸ Build APK(s)**.

## Install on the phone
Copy `app-debug.apk` to the phone, tap it, and allow "Install unknown apps" when asked.

## Use
1. Pair and connect the Bluetooth speaker in Android settings.
2. Open **Phone Mic**, tap the grey mic (allow microphone access the first time).
   It turns red = live. Tap again = off.

Tips: keep the phone a few metres from the speaker (or turn the speaker down) to avoid a squeal.
Bluetooth always adds a small delay (roughly 0.1–0.3 s depending on the speaker); that is normal.
Keep the app open while speaking — the screen stays on automatically while the mic is live.
