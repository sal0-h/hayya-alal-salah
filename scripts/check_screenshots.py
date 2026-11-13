#!/usr/bin/env python3
"""Fail if the full-shell matrix is missing actual collected PNG files."""
from pathlib import Path
import struct
root = Path("app/build/outputs/connected_android_test_additional_output")
assert root.exists(), "AGP did not collect screenshots"
expected = set()
for width in ("phone", "tablet"):
    routes = ("today","history","settings","onboarding","location","calculation","alerts","fajr","focus","qibla","pause","appearance","privacy","credits","record","date","focus-consent","privacy-confirm","polar","focus-overlay","ringing","ringing-keyboard") if width == "phone" else ("today","history","settings")
    for route in routes:
        for language in ("en","ru","az","ar"):
            for scale in (1,2):
                for theme in ("light","dark"):
                    for position in (("top","bottom") if width == "phone" and route not in ("record","date","focus-consent","privacy-confirm","focus-overlay","ringing","ringing-keyboard") else ("top",)):
                        expected.add(f"{route}-{language}-{scale}x-{theme}-{width}-{position}.png")
for language in ("en","ru","az","ar"):
    for scale in (1,2):
        for theme in ("light","dark"):
            for position in ("top","bottom"):
                expected.add(f"widgets-{language}-{scale}x-{theme}-{position}.png")
files = {path.name: path for path in root.rglob("*.png")}
assert expected <= files.keys(), sorted(expected - files.keys())
for name in expected:
    data = files[name].read_bytes()
    assert len(data) > 1024 and data[:8] == b"\x89PNG\r\n\x1a\n" and data[-8:-4] == b"IEND", name
    w,h = struct.unpack(">II", data[16:24])
    assert w >= 800 and h >= 1900, (name,w,h)
print(f"Validated {len(expected)} full-shell screenshots")
