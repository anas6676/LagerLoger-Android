# LagerLoger Handheld for Zebra TC27

This is a standalone Android wrapper. It does not modify or copy the LagerLoger web app. It opens the existing internal web server at `192.168.1.129:3000`, starts in Pick-Hub, and provides native Pick-Hub and Search tabs. Search result detail pages remain available. The web app's own mobile bottom navigation is hidden in the wrapper.

The device must be on the internal Wi-Fi and the LagerLoger server must be running. This app needs local network access; it does not need public internet access. Barcode scans are expected to arrive as keyboard input from the Zebra DataWedge service.

## Build on a computer

1. Install Android Studio and accept its Android SDK setup.
2. In Android Studio, choose **Open** and select this `LagerLoger-Android` folder.
3. Let Android Studio complete Gradle sync. If prompted to install Android SDK Platform 36 or Android Gradle Plugin dependencies, accept.
4. Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Android Studio will show a link to the generated debug APK. The file is normally `app/build/outputs/apk/debug/app-debug.apk`.

## Install by USB

1. On the TC27, open **Settings > About phone** and tap **Build number** seven times to enable Developer options (if your organization has not disabled this setting).
2. Open **Settings > System > Developer options**, enable **USB debugging**, and approve the prompt after connecting the Zebra to the computer with a data-capable USB-C cable.
3. In Android Studio, use **Run** with the TC27 selected, or copy `app-debug.apk` to the Zebra and open it to install.
4. If Android asks, allow the app used to open the APK to **Install unknown apps**. This is a one-time setting for that installer.
5. Launch **LagerLoger**, connect the device to the warehouse Wi-Fi, and confirm Pick-Hub/Search load.

For a durable release/update path, create a signed release APK in Android Studio and keep its signing key in a safe place. Android accepts updates only when signed with the same key. Do not distribute the debug APK as the long-term production build.

## Scanner setup

DataWedge is provided on Zebra devices. Set up a DataWedge profile associated with package `com.lagerloger.handheld`, enable the built-in barcode scanner and **Keystroke output**, and enable sending an **Enter** key after each scan. Open Search and scan a known device barcode. The `/m/search` page focuses its scan field and submits on Enter.

## Network address

The server address is set in `app/src/main/java/com/lagerloger/handheld/MainActivity.java` as `SERVER`. If the server gets a different IP or port, update it there and rebuild the APK.

## Current limitations

- The app loads the web screens from the server; it does not work offline.
- The server address uses HTTP on the private network, matching the confirmed LagerLoger URL.
- Android Studio/Android SDK are required to produce the APK. This build computer currently has no Java, Gradle, or Android SDK tools installed.
