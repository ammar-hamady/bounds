---
name: Enforcement mutation synchronization
description: How to prevent active enforcement transitions from racing configuration writes.
---

Active enforcement state transitions and the final callback that commits a protected configuration mutation must use the same synchronization boundary.

**Why:** Checking enforcement before launching an asynchronous write leaves a narrow race where enforcement can start before persistence commits, allowing a stale editor or indirect mutation to weaken the new session.

**How to apply:** Keep UI checks for feedback only. Recheck under a shared guard at the repository's final mutation callback, and update the protected enforcement identity through that same guard.