---
name: Foreground app detection
description: Android usage-statistics behavior relevant to reliable blocked-app detection.
---

The foreground blocker should derive the current app from recent `UsageEvents` foreground transitions and explicitly exclude the Bounds package. `queryUsageStats()` returns usage aggregates, so selecting the package with the greatest `lastTimeUsed` can report the app used immediately before Bounds and launch the blocking overlay over Bounds itself.

**Why:** Starting the blocking service from the Bounds screen exposed that usage aggregates are not a reliable representation of the currently resumed activity.

**How to apply:** When changing foreground detection, preserve the recent-event lookup and self-exclusion; verify the manual lock flow on a physical device where Usage Access is enabled.