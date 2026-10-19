# New Opacha

A noncommercial mod of [Opacha-mda by yiotro](https://github.com/yiotro/Opacha-mda).
The original Java source and assets stay in `src/` and `assets/`.

## Install on a Pixel 8a

Download the APK from [Releases](https://github.com/2thestar/New-Opacha/releases) on the phone and open it to install **New Opacha**. Android may ask you to allow the browser to install apps. The app has its own package ID and saves, so it can be installed alongside the original game.

## Build the Android APK

On Windows, prepare the local JDK and Android SDK once:

```powershell
.\scripts\setup-java.ps1
.\scripts\setup-android.ps1
```

Then build the APK:

```powershell
.\opacha.ps1 :android:exportApk
```

The installable file is `build/downloads/New-Opacha-0.1.0.apk`. It contains ARM64 libraries for the Pixel 8a and x86-64 libraries for emulator testing. The minimum Android version is 8.0. This development build is signed with the local key in `.tools/signing/`; keep that directory to build updates that preserve your installed app and saves. Signing keys and machine-specific SDK paths are excluded from Git.

For USB installation with USB debugging enabled:

```powershell
.\opacha.ps1 :android:installDebug
```

The Android build uses SDK platform 36, target SDK 35, build-tools 35.0.0, and Android Gradle Plugin 8.13.2. The SDK setup helper pins command-line tools 19.0 and verifies the files required by the build after installation.

Android checks: APK signing and 16 KB ZIP/native-library alignment; lint with no errors; installation, menu navigation, a skirmish, Android Back-to-pause, and Home/launcher resume on an Android 15 emulator with the Pixel 8a's 1080 × 2400 display profile. The launcher keeps one game instance. A physical Pixel 8a has not been tested yet.

## Run on Windows

Use JDK 17. If Java is not installed, download a project-local, checksum-verified Temurin JDK:

```powershell
.\scripts\setup-java.ps1
```

Start the game:

```powershell
.\opacha.ps1
```

The helper uses `JAVA_HOME`, the project-local JDK, or Java on `PATH`. It also keeps the Gradle cache inside this project. No global Java installation or Gradle installation is required when using the project-local JDK.

## Run on Linux or macOS

Install JDK 17 and run:

```sh
sh ./gradlew lwjgl3:run
```

macOS receives the required `-XstartOnFirstThread` JVM option automatically.

## Build and verify

```powershell
.\opacha.ps1 :lwjgl3:build
.\opacha.ps1 :lwjgl3:smokeTest
```

With an installed JDK, the same tasks are available through `gradlew.bat` on Windows or `sh ./gradlew` on Linux/macOS.

- `:lwjgl3:build`: compile and package the desktop game. Archives appear in `lwjgl3/build/distributions/`.
- `:lwjgl3:installDist`: create an unpacked distribution in `lwjgl3/build/install/new-opacha/`.
- `:lwjgl3:smokeTest`: launch the packaged JARs at 480 × 800 and 390 × 844, click through the menus, start a skirmish, check that the simulation advances, and pause with Escape. Screenshots and separate test saves go in `lwjgl3/build/smoke/`. A display with OpenGL is required.

The Gradle wrapper pins Gradle 8.14.6 and verifies the distribution checksum. The game uses libGDX 1.14.2 with its LWJGL3 desktop backend and FreeType extension. Game source is compiled for Java 8; the build uses JDK 17.

iOS is not configured. The desktop window keeps the original portrait layout at a fixed size because the game creates its layout and framebuffers at startup. Desktop saves remain under `.opacha-upgraded` in the user's home directory so earlier development saves continue to work.

## Original author's notice

> This is a source code for iOS/Android game called 'Opacha-mda'.
>
> You can use this code to make a mod or to just see how I solved certain problems, maybe it will be interesting to someone. Please keep your mod as a non commercial project.
>
> How to get started:
> 1. Create new libGDX project.
> 2. Make sure that you can compile and run it.
> 3. Swap core and assets with ones provided here.
>
> Some essential files are missing from the repository (like android manifest or gradle files). You are supposed to already have some basic experience with libGDX then it should not be a problem.
>
> If you have any questions about this project (how to set it up/why it doesn't compile/why it crashes on startup) then email me at yiotro93@gmail.com, I'll help you.
