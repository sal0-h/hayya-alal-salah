#!/usr/bin/env python3
"""Reproduce committed derived assets from the documented, verified upstream inputs."""
import hashlib
import json
from pathlib import Path
import re
import sys
import zipfile
import unicodedata

archive, lookup = map(Path, sys.argv[1:])
assert hashlib.sha256(archive.read_bytes()).hexdigest() == "50ef2f0cf7a1dd6d67123aaa6cdd45a8cf48ff5d23b3677e896f009ccce768d7"
assert hashlib.sha256(lookup.read_bytes()).hexdigest() == "a08388e1ee9286a80f5d00614f8bdd2024e8399e114559d3d2611e2eee6499a0"
rows = zipfile.ZipFile(archive).read("cities15000.txt").decode().splitlines()
city_path = Path("app/src/main/assets/geography/cities.tsv")
city_path.parent.mkdir(parents=True, exist_ok=True)
def searchable(value):
    return "".join(c for c in unicodedata.normalize("NFD", value.lower()) if not unicodedata.category(c).startswith("M") and c != "ـ")
city_path.write_text("".join("\t".join([f[0],f[1],f[2],f[4],f[5],f[8],f[17],f[3],searchable(" ".join([f[1],f[2],f[3]]))]) + "\n" for row in rows if len(f := row.split("\t")) >= 18))
match = re.search(r'var X=("(?:\\.|[^"\\])*"),T=(\[.*?\]);if', lookup.read_text())
assert match
path = Path("core/src/main/resources/geography/timezone-tree.txt")
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.loads(match[1]) + "\n" + "\n".join(json.loads(match[2])) + "\n")
