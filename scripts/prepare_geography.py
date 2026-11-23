#!/usr/bin/env python3
"""Reproduce committed derived assets from the documented, verified upstream inputs."""
import hashlib
import json
from pathlib import Path
import re
import sys
import zipfile
import unicodedata

archive, admin1, lookup = map(Path, sys.argv[1:])
assert hashlib.sha256(archive.read_bytes()).hexdigest() == "69cf3265bbb28c90bb222e10a454116b8e895797353175eac8360bda1318b081"
assert hashlib.sha256(lookup.read_bytes()).hexdigest() == "a08388e1ee9286a80f5d00614f8bdd2024e8399e114559d3d2611e2eee6499a0"
assert hashlib.sha256(admin1.read_bytes()).hexdigest() == "2e4867f5397acb056ea26377601b57af37cb4c81399770225018e9e439e91ad3"
with zipfile.ZipFile(archive) as zipped:
    rows = [line.split("\t") for line in zipped.read("cities5000.txt").decode().splitlines()]
# Population orders equal-quality matches without passing it to the runtime index.
rows.sort(key=lambda row: (-int(row[14] or 0), int(row[0])))
regions = {f[0]: (f[1], f[2]) for line in admin1.read_text().splitlines() if len(f := line.split("\t")) >= 3}
city_path = Path("app/src/main/assets/geography/cities.tsv")
city_path.parent.mkdir(parents=True, exist_ok=True)
def searchable(value):
    return "".join(c for c in unicodedata.normalize("NFD", value.lower()) if not unicodedata.category(c).startswith("M") and c != "ـ")
with city_path.open("w", encoding="utf-8", newline="\n") as out:
    for row in rows:
        ident, name, ascii_name, aliases = row[:4]
        latitude, longitude, country, admin, zone = row[4], row[5], row[8], row[10], row[17]
        if row[6] != "P" or not zone:
            continue
        region, ascii_region = regions.get(f"{country}.{admin}", ("", ""))
        region_keys = searchable(",".join([region, ascii_region, admin]))
        names = ",".join(dict.fromkeys(searchable(v).strip() for v in (name, ascii_name, *aliases.split(",")) if v.strip()))
        out.write("\t".join((ident, name, latitude, longitude, country, zone, region, region_keys, names)) + "\n")
match = re.search(r'var X=("(?:\\.|[^"\\])*"),T=(\[.*?\]);if', lookup.read_text())
assert match
path = Path("core/src/main/resources/geography/timezone-tree.txt")
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.loads(match[1]) + "\n" + "\n".join(json.loads(match[2])) + "\n")
