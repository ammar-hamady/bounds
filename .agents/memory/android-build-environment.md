---
name: Android build environment
description: Environment constraint affecting local Android Gradle validation.
---

Android Gradle tasks cannot run until a Java runtime is available and `ANDROID_HOME` or `local.properties` points to a valid Android SDK. This workspace currently lacks both prerequisites.

**Why:** The project is an Android application, but this workspace currently has no Java executable and no Android SDK directory or configured SDK path, so Gradle stops before compiling code or tests.

**How to apply:** Before claiming Android tests or an APK build passed, check the SDK path first. If it is absent, report the exact Gradle configuration failure and rely on static review until a device/SDK validation task is available.