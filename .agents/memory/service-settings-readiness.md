---
name: Service settings readiness
description: Preventing Android services from using temporary default preferences during process startup.
---

Android services that depend on persisted settings must wait until the first complete DataStore settings snapshot is loaded.

**Why:** Geofence and blocking services can run before the main activity, especially after reboot. Reading application defaults during that window makes persisted choices behave inconsistently.

**How to apply:** Maintain one application-level readiness signal for the complete runtime settings snapshot, await it in services after satisfying foreground-service timing requirements, and do not copy ViewModel initial defaults back into application state.