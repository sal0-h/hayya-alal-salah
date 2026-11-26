# Published calculation reference fixtures

Captured from AlAdhan on 2026-11-13. These are published provider calculations,
not a mosque timetable, religious ruling or independent astronomy certification.
No runtime app/network dependency is added. Tests read only committed facts.

ISNA uses 15°/15°, Karachi 18°/18°; both fixtures use Hanafi Asr. Ja‘fari uses
16° Fajr, 14° Isha, 4° Maghrib and standard Asr. All use explicit coordinates
and named IANA zones. January and July catch seasonal and DST conversion.

A two-minute comparison budget permits the engines’ documented rounding and
safety adjustments (including Adhan Dhuhr adjustment and sunrise rounding).
The test writes actual per-event deltas; a larger difference must be investigated,
not hidden by changing the captured reference. Shared calculation ancestry limits
independence. Final local authority comparisons remain a separate release review.

Method definitions: https://aladhan.com/calculation-methods

- Karachi 2026-01-15: [https://api.aladhan.com/v1/timings/15-01-2026?latitude=24.8607&longitude=67.0011&method=1&school=1&timezonestring=Asia%2FKarachi](https://api.aladhan.com/v1/timings/15-01-2026?latitude=24.8607&longitude=67.0011&method=1&school=1&timezonestring=Asia%2FKarachi); raw response SHA-256 `910b13879851293f8bd207f26408349ded1287c03e783ba20b1223c837e553a7`.

- Karachi 2026-07-15: [https://api.aladhan.com/v1/timings/15-07-2026?latitude=24.8607&longitude=67.0011&method=1&school=1&timezonestring=Asia%2FKarachi](https://api.aladhan.com/v1/timings/15-07-2026?latitude=24.8607&longitude=67.0011&method=1&school=1&timezonestring=Asia%2FKarachi); raw response SHA-256 `cf9bd2cc515220abd41ca8da6a9eb68b903896a16497230536b25a8f703db25b`.

- Raleigh 2026-01-15: [https://api.aladhan.com/v1/timings/15-01-2026?latitude=35.775&longitude=-78.6336&method=2&school=1&timezonestring=America%2FNew_York](https://api.aladhan.com/v1/timings/15-01-2026?latitude=35.775&longitude=-78.6336&method=2&school=1&timezonestring=America%2FNew_York); raw response SHA-256 `7bff14661602ace477837564711f1010e56ec8ce8e50efbbd60ae890576e8642`.

- Raleigh 2026-07-15: [https://api.aladhan.com/v1/timings/15-07-2026?latitude=35.775&longitude=-78.6336&method=2&school=1&timezonestring=America%2FNew_York](https://api.aladhan.com/v1/timings/15-07-2026?latitude=35.775&longitude=-78.6336&method=2&school=1&timezonestring=America%2FNew_York); raw response SHA-256 `788d8ca0dd5fefcae1cc9cd943338a16562ee8e989fca7101f5d16e87548a34b`.

- Tehran 2026-01-15: [https://api.aladhan.com/v1/timings/15-01-2026?latitude=35.6892&longitude=51.389&method=0&school=0&timezonestring=Asia%2FTehran](https://api.aladhan.com/v1/timings/15-01-2026?latitude=35.6892&longitude=51.389&method=0&school=0&timezonestring=Asia%2FTehran); raw response SHA-256 `4f8478f7c381d1de5fb1361a426e45cbf41862ee8508d561fd75376c37caedd4`.

- Tehran 2026-07-15: [https://api.aladhan.com/v1/timings/15-07-2026?latitude=35.6892&longitude=51.389&method=0&school=0&timezonestring=Asia%2FTehran](https://api.aladhan.com/v1/timings/15-07-2026?latitude=35.6892&longitude=51.389&method=0&school=0&timezonestring=Asia%2FTehran); raw response SHA-256 `4427bf8ca243b48dc8837e92de595ea227ff51292f386fac745a789da702777e`.

Observed local execution: all 36 comparisons were within one minute. Adhan’s four
Dhuhr comparisons were +1 minute; Raleigh January Maghrib was -1 minute; Tehran
January Asr was +1 minute. Other captured comparisons were equal to the minute.
The test retains a two-minute regression budget, not an authority-conformance claim.
