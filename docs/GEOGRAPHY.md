# Offline geographic data

GeoNames files retrieved 2026-11-23:

| Input | SHA-256 |
| --- | --- |
| [cities5000.zip](https://download.geonames.org/export/dump/cities5000.zip) | `69cf3265bbb28c90bb222e10a454116b8e895797353175eac8360bda1318b081` |
| [admin1CodesASCII.txt](https://download.geonames.org/export/dump/admin1CodesASCII.txt) | `2e4867f5397acb056ea26377601b57af37cb4c81399770225018e9e439e91ad3` |

The bundled index has 70,101 populated places, up from 34,215. It preserves GeoNames
coordinates and IANA zones. It includes primary regions, country codes, and normalized
published aliases, including native Azerbaijani, Arabic, and Cyrillic names. Exact
names and aliases rank above prefixes and substrings, using source population to
break ties. Country names display in the active language. A query can include region
or country after commas, for example `Springfield, IL, US`. Results with unsupported
IANA zones are omitted. Searches stream the index on demand. The app caps displayed
results and keeps the gazetteer out of startup state.

GeoNames is credited under CC BY 4.0 in `licenses/GeoNames-NOTICE.txt`.
The full license is in `licenses/CC-BY-4.0.txt`. The data contains public places.
The app does not query a remote geocoder. Time-zone data is a separate pinned
local lookup; boundary suggestions are approximate and are shown for confirmation
when the selected zone changes. Users can always choose an IANA zone manually.

## Time-zone suggestions

PhotoStructure/darksky `tz-lookup`, pinned revision
`a09cbf7a1874937d81f42ccb1bdad4ac88d0400e`:
https://github.com/photostructure/tz-lookup/blob/a09cbf7a1874937d81f42ccb1bdad4ac88d0400e/tz.js
Upstream tz.js SHA-256: `a08388e1ee9286a80f5d00614f8bdd2024e8399e114559d3d2611e2eee6499a0`.
Regenerate both assets with `python3 scripts/prepare_geography.py cities5000.zip admin1CodesASCII.txt tz.js`.
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
