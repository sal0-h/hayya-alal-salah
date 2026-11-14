# Security reporting

This is an unreleased application. Only the current development head is maintained;
there is no supported public release yet.

Do not put private prayer history, coordinates, imported audio, signing material or
unreviewed logs into public issues. For a vulnerability, use
[GitHub private vulnerability reporting](https://github.com/sal0-h/hayya-alal-salah/security/advisories/new).
This repository feature was verified enabled on 2026-11-13. Reports stay private
while the maintainer investigates. Do not publish exploit details in ordinary issues.

Include the commit/build ID, Android version, affected component and a minimal
synthetic reproduction. Ordinary app code has no runtime Internet permission or
telemetry. Keep imported-file validation, exported components, PendingIntent identity,
Direct Boot minimization, local deletion and Focus emergency escape in scope when
reviewing changes. Do not use production signing credentials in ordinary CI.
