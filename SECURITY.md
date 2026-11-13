# Security reporting

This is an unreleased application. Only the current development head is maintained;
there is no supported public release yet.

Do not put private prayer history, coordinates, imported audio, signing material or
unreviewed logs into public issues. For a vulnerability, first use the repository's
private vulnerability reporting feature if available. Availability must be checked;
this document does not claim that GitHub reporting is enabled. If unavailable, open
a public issue containing only a request for a private contact, with no exploit or
sensitive details. A verified private security contact is a public-release gate.

Include the commit/build ID, Android version, affected component and a minimal
synthetic reproduction. Ordinary app code has no runtime Internet permission or
telemetry. Keep imported-file validation, exported components, PendingIntent identity,
Direct Boot minimization, local deletion and Focus emergency escape in scope when
reviewing changes. Do not use production signing credentials in ordinary CI.
