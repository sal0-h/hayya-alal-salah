# Offline geographic data

GeoNames cities15000.zip, downloaded 2026-11-13 from
https://download.geonames.org/export/dump/cities15000.zip
Source archive SHA-256: `50ef2f0cf7a1dd6d67123aaa6cdd45a8cf48ff5d23b3677e896f009ccce768d7`.
Bundled derived TSV: ID, name, ASCII name, latitude, longitude, country, IANA zone,
and all published alternate names (needed for native Azerbaijani/Arabic/Cyrillic search).
No private locations.
34215 populated-place records. GeoNames CC BY 4.0; attribution in
licenses/GeoNames-NOTICE.txt, license text in licenses/CC-BY-4.0.txt.

The app does not query a remote geocoder. Time-zone data is a separate pinned
local lookup; boundary suggestions are approximate and are shown for confirmation
when the selected zone changes. Users can always choose an IANA zone manually.

## Time-zone suggestions

PhotoStructure/darksky `tz-lookup`, pinned revision
`a09cbf7a1874937d81f42ccb1bdad4ac88d0400e`:
https://github.com/photostructure/tz-lookup/blob/a09cbf7a1874937d81f42ccb1bdad4ac88d0400e/tz.js
Upstream tz.js SHA-256: `a08388e1ee9286a80f5d00614f8bdd2024e8399e114559d3d2611e2eee6499a0`.
Regenerate both assets with `python3 scripts/prepare_geography.py cities15000.zip tz.js`.
The compact traversal is ported under CC0 with the same arithmetic/constants. The
tree and zone list are extracted without boundary changes into
`core/src/main/resources/geography/timezone-tree.txt` (derived database, ODbL-1.0).
Upstream dataset uses timezone-boundary-builder/OpenStreetMap; their database rights
are acknowledged separately from the CC0 code license. Texts/attribution are bundled.

This is lossy boundary estimation, not an authoritative time-zone polygon lookup.
The upstream README documents substantial errors near boundaries and in uninhabited
areas. Results only prefill an editable zone; automatic-mode refreshes require
confirmation before changing the selected zone. Actual offset/DST rules come from
the installed Android IANA database; the suggestion does not replace those rules.
Unsupported IDs require manual selection, rather than substituting a fixed offset.
