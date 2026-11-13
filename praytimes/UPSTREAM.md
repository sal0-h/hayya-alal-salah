# Kotlin PrayTimes provenance

Vendored from https://github.com/persian-calendar/praytimes at
`1d1b4b5df73fbe6186c93d203c3ad0715fb989f6` (LGPL-3.0).
`LICENSE` contains the upstream license; the repository GPL license supplies GPL terms.
Source lives in its original package, compiled as a separate JVM library. Upstream tests
are retained. The Android application is GPL-3.0-or-later; recipients receive source and
can rebuild/relink a modified library. No upstream build plugins are imported.

Adaptations, if any, are recorded here before integration. Astronomy is upstream code.

2026-11-13: Added immutable `unwrappedHours` before final `fixHour` calls. Wrapped public
fields and all astronomical calculations remain unchanged. The app requests offset 0,
anchors the unwrapped hours to a UTC date and converts each event to the IANA zone.
This avoids applying a single noon UTC offset across DST transitions and preserves
Isha/midnight on the next date. Civil/solar date alignment is checked using Dhuhr.

Added optional validated fajr/isha/maghrib angle and fixed-Isha-interval constructor
parameters. They substitute upstream `MinuteOrAngleDouble` inputs; all existing formulas,
profile defaults, and no-override wrapped output remain unchanged.
