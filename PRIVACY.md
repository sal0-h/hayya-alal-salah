# Privacy policy

Draft for the unreleased Android application, updated 13 November 2026. The policy
must be published at a stable public URL and reviewed with the final release build.

Hayya ‘ala-s-Salah has no account, server, advertising, analytics, telemetry or app
Internet permission. The app does not transmit your location, prayer records,
installed-app choices, imported audio or usage information to the developer.

Prayer records, their original date/time zone, pause dates, settings and copied audio
are stored in the app's private storage. Cloud backup and device-transfer backup are
excluded using Android controls. Uninstalling, clearing app storage or losing the
device removes this data; v1 has no export, sync or recovery service.

Manual city/coordinate mode requests no device location. If you choose automatic
location or explicit refresh, Android's foreground location providers are used for
a bounded request while the app is visible. A recent cached result may be reused.
There is no background travel tracking. Android or the device vendor's own location
services are governed by their policies; choosing manual mode avoids these requests.

Prayer Focus is optional and requires a separate disclosure and affirmative consent.
It observes foreground package/window changes to show an overlay over apps you
select during your selected prayer windows. It does not retrieve screen contents,
read keystrokes, capture screenshots, perform gestures or send app-use data anywhere.
Your selected package names and temporary emergency bypass stay on the device.
Phone, launcher, settings, input and other essential functions are excluded. You can
use emergency bypass, disable Focus or revoke its accessibility permission.

Upcoming alert identifiers, times and minimal sound/challenge/language/appearance
configuration are kept in device-protected storage so enabled alarms can work before
first unlock after reboot. This cache contains no coordinates, prayer history or
pause dates. Imported audio cannot be opened before unlock; bundled audio is used.

Widgets intentionally display prayer times/place labels on the home screen. Android
also controls notifications and lock-screen visibility; choose those settings with
your device's privacy needs in mind. Qibla sensors run only while its screen is visible.

Deleting records restores an unrecorded state. Full reset deletes records, pauses,
settings and imported audio, cancels the app's alerts and clears its widget display.
You may separately revoke Android permissions. No data is automatically uploaded with
a bug report; review any screenshot/log you choose to share.

Questions and defects can be raised in the repository. Do not put private history,
coordinates, audio or sensitive logs into a public issue. Security reporting is
covered in SECURITY.md; a private contact must be established before public release.
