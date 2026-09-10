---
name: VPN consent recovery
description: How website enforcement should recover after the Android VPN approval flow.
---

When Android returns from the VPN consent activity, re-evaluate and retry the current active-zone website policy; do not only refresh the passive consent status.

**Why:** Consent can be requested after zone enforcement already tried to activate. Updating the Settings label alone leaves that pending policy inactive until another zone transition.

**How to apply:** Resolve passive readiness when the UI first opens. In the consent result callback, retry the active policy, falling back to a readiness refresh when no eligible zone policy exists.