# Website blocking device validation

Run this checklist on a physical Android device with Chrome (or another
browser) and a second VPN available. The local filter intentionally does not
promise to defeat browser-encrypted DNS or direct IP access.

- [ ] In Settings, tap **Website blocklist**, approve Bounds in the Android VPN
      consent flow, return to Bounds, and confirm the state reads **Ready**.
- [ ] Cancel the consent flow and confirm Settings reads **Approval needed** with
      a message that approval was cancelled; approve on the next attempt.
- [ ] Approve while a zone with domain rules is already active and outside its
      grace period. Confirm the state advances through **Starting** to **Active**
      without leaving and re-entering the zone.
- [ ] Create a zone with `example.com` and a second selected app. Confirm the
      domain is normalized, duplicate entries are rejected, and malformed
      entries show an understandable error.
- [ ] Simulate or physically enter the zone with a non-zero grace period.
      Confirm the VPN does not report **Active** until the grace period ends.
- [ ] After activation, verify the configured domain and a subdomain fail to
      resolve in Chrome while an unrelated domain still loads.
- [ ] When a configured domain is blocked, confirm a brief **Website blocked by
      Bounds** banner identifies the matched domain. Without overlay permission,
      confirm the ongoing VPN notification identifies Bounds and the domain.
- [ ] Reload the blocked domain repeatedly and confirm feedback is rate-limited
      rather than stacking or continuously covering the browser.
- [ ] Leave the zone and confirm the VPN status returns to **Ready** and
      unrelated app blocking/manual locking still works.
- [ ] Enter two zones in succession. Confirm the newest zone's domains win and
      an exit event for the older zone does not clear the newer policy.
- [ ] Reboot while inside a zone and confirm geofences re-register, grace
      timing is applied again, and website enforcement becomes truthful.
- [ ] Change networks (Wi-Fi to cellular and back). Confirm a lost network is
      reported as unavailable and recovery can be retried from Settings.
- [ ] Revoke Bounds VPN access in Android Settings. Confirm the UI reports
      unavailable and offers consent again.
- [ ] Force-stop Bounds and reopen it. Confirm it does not claim website
      blocking until the service has re-established its tunnel.
- [ ] Start another VPN while Bounds is active. Confirm Bounds reports
      **Another VPN** / displaced rather than claiming domains are blocked.
- [ ] From **Another VPN** or **Needs attention**, disable the conflicting VPN or
      restore the network, tap **Try again**, and confirm the pending zone policy
      starts without restarting Bounds.
- [ ] Record that cached browser lookups, Chrome DoH/DoT, and IP-literal
      navigation are unsupported bypass cases rather than regressions.
