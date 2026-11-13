# Polar calculation policy

Search strategy adapted from Adhan JS AqrabBalad (MIT),
revision `e779a32a9bb9c7849c7612b0bdbdf8cbad60b080`, `src/PolarCircleResolution.ts`. License included in
`licenses/adhan-js-MIT.txt`. Like upstream, keep longitude and search toward the
equator in 0.5-degree steps, checking today and tomorrow. We validate complete
ordered schedules separately through the selected Sunni or Ja‘fari engine, rather
than just sunrise/sunset. The bounded search can continue to the equator when
the selected profile still cannot produce valid twilight boundaries. No new solar
formula or nearest-day algorithm is introduced.

ASK returns only a proposed reference latitude, never schedulable times. NEAREST
and REFERENCE are persisted only after explicit user consent. DISABLED retains
structured unavailability. User corrections that reorder events are rejected rather
than hidden by estimation. Provenance retains actual and effective coordinates;
Qibla uses actual coordinates. Profile/place changes reset prior consent.
