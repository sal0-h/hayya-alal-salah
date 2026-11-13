# Architecture decisions implemented so far

`app` owns Compose, platform lifecycle and DataStore setup. `core` owns date/coordinate
validation, profile routing, adapters and pure tracking/scheduling rules; it imports no
Android APIs. `praytimes` is a separate JVM library containing pinned upstream sources
and the documented unwrapped-output adaptation. `benchmark` reserves the planned
performance module; there is no performance harness yet.

Both adapters implement one `PrayerCalculator`. Only the selected profile engine runs.
The Adhan dependency is its JVM variant; using an upstream multiplatform dependency
does not introduce an application-level KMP build. No new astronomy is implemented.

A service date is the Gregorian date requested at a location/zone. Event instants
retain their actual day, including before/after midnight. Dhuhr must fall on the
requested civil date; when a time zone crosses the solar date line, the engine input
and UTC anchor are aligned using Dhuhr before accepting the schedule. PrayTimes uses
zero offset and unwrapped UTC hours; each instant is converted through zone rules,
rather than applying a single noon offset to every event on a DST transition date.
Minute rounding/corrections happen before validation and are shared by display and
the pure schedule compiler. Invalid/absent events become a structured unavailable
result. Polar estimation will be a separate explicit user-confirmed policy.

The unadjusted platform Umm al-Qura chronology determines Ramadan for the Umm al-Qura
120-minute Isha interval. Future Hijri display corrections must remain separate.
Turkey is labeled an approximation. Calculation output does not claim universal
agreement with published authority timetables; independent comparisons are pending.

`PrayerKey(serviceDate, prayer)` identifies records and alert actions. Missing records
are unrecorded; silence never creates a missed record. Excuses pause verified streaks;
unknown history interrupts verification and remains unconfirmed. The desired-event
compiler is pure and filters stale/completed/excused/paused targets. There is no Android
alarm reconciler or persisted prayer repository in this slice.

The app requests no network/location permissions. Setup storage is credential-protected
and excluded from backup/transfer. There is no Direct Boot snapshot yet because there
are no installed alarms. Future Direct Boot data must remain minimal and must not copy
history or coordinates. The countdown only runs in a visible started activity.
