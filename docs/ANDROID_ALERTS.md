# Android delivery and Focus implementation

The core compiles future targets. `AlarmCoordinator` installs a rolling seven-day
window using explicit immutable PendingIntents keyed by service date/prayer/kind.
Each changed payload gets a fresh opaque token; unchanged events keep their token.
Primary alerts use exact idle-capable alarms when access is granted, otherwise an
honest inexact fallback. Fajr uses `setAlarmClock` only with exact and notification
readiness. Before/after reminders are inexact and may be dropped under restrictions.
No FGS starts during boot: boot receivers only reinstall upcoming alarms.

A claim is durably persisted before notification/playback. Stale limits are five
minutes for primary alerts, two for extras and ten for Fajr. Credential-unlocked
delivery rechecks the current calculation, enabled rule, zone, pause and record.
Settings/records changes, foreground use, boot/unlock, package/clock/zone/exact-access
changes reconcile desired versus installed state. Inexact twelve-hour maintenance
only exists while future alert events exist. Force-stop and OEM restrictions are
not bypassed; reopening the app reconciles future events without replaying old ones.

Device-protected preferences contain upcoming alarm targets, instants, zone IDs,
kind, sound ID, challenge difficulty, generation token/claim and selected language.
They contain no coordinates, history records or pause dates. Both credential and
device-protected domains are excluded from backup/transfer. Imported audio stays in
credential-private files; pre-unlock/missing imports use bundled audio.

Media playback owns a foreground service only while active. Fajr takes priority;
ordinary Adhan respects ringer/DND changes. Playback uses alarm audio attributes,
explicit audio-focus handling and finite timeouts; Fajr does not change Android
volume or DND. A denied foreground start retains a usable notification. Full-screen
access is optional; the notification still opens the ringing screen. Solving the
challenge, emergency stop and playback completion never write a prayer record.
Notification ON_TIME is an explicit authenticated action with the original target.

Focus only receives package/window events. Its manifest requests no content, key,
gesture or screenshot capabilities. Selected launcher apps exclude the app itself,
home, dialer/telephony, settings, permission controllers and enabled input methods.
A nonfocusable accessibility overlay leaves system navigation available, with a
pinned emergency bypass and scrollable large-text content. Completion/excuse/pause,
bypass, preference changes and boundary expiry release it. There is no routine snooze.
A system service consent screen follows explicit in-app disclosure. Store policy
review and physical OEM/accessibility validation remain external release gates.

## Actual validation limits

JVM calculation/suppression/focus rules and local Android compilation/lint pass.
Instrumentation covers snapshot claim durability/stale tokens, bundled media
preparation and installed accessibility capabilities; check PROGRESS.md for actual
CI execution rather than assuming those tests have run. No physical alarm, Doze,
call interruption, pre-unlock playback, overlay, TalkBack or overnight test has run.
Android 17 background audio, permission boundaries and OEM behavior require the
emulator/device matrix in IMPLEMENTATION_PLAN.md. Compilation is not delivery proof.
