---
name: Reliable Android blocking overlays
description: Why app-block enforcement must use a service-owned system overlay on modern Android.
---

Use a foreground-service-owned `TYPE_APPLICATION_OVERLAY` for the blocking screen. Do not rely on starting a normal Activity after detecting another foreground app.

**Why:** Modern Android restricts background Activity launches; denial can be silent, and Android 15 requires a visible overlay for the relevant exemption. Merely holding overlay permission does not make an Activity an overlay.

**How to apply:** Require “Display over other apps” access before reporting blocking as active, add/remove the overlay through `WindowManager`, and only mark it active after the window was added successfully.