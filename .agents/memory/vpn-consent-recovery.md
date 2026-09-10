---
name: VPN consent recovery
description: How website enforcement should recover after the Android VPN approval flow.
---

When Android returns from the VPN consent activity, re-evaluate and retry the current active-zone website policy; do not only refresh the passive consent status.

Passive resume checks must not overwrite a consent result, a startup-in-progress state, or a specific runtime failure. Startup timeouts must also be scoped to a unique attempt so an older timeout cannot fail a same-zone retry.

**Why:** Consent can be requested after zone enforcement already tried to activate, and Android may deliver the activity result before or after the returning activity resumes. Unscoped delayed checks can also outlive the attempt that created them.

**How to apply:** Resolve passive readiness when the UI first opens. In the consent result callback, retry the active policy, falling back to readiness when no eligible policy exists. Suppress the consent flow's resume refresh and identify every delayed startup check by attempt.