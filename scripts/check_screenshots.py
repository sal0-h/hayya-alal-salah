#!/usr/bin/env python3
"""Fail capture CI unless AGP retained all 16 expected image files."""
from pathlib import Path
import struct

root = Path("app/build/outputs/connected_android_test_additional_output")
expected = {f"today-{language}-{scale}x-{theme}.png"
            for language in ("en", "ru", "az", "ar")
            for scale in (1, 2) for theme in ("light", "dark")}
images = list(root.rglob("today-*.png"))
actual = {image.name for image in images}
assert actual == expected, f"Missing captures: {sorted(expected - actual)}; unexpected: {sorted(actual - expected)}"
for image in images:
    data = image.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", image
    assert len(data) > 1000, f"Empty/truncated capture: {image}"
    width, height = struct.unpack(">II", data[16:24])
    assert width >= 320 and height >= 480, (image, width, height)
print(f"Verified {len(expected)} retained PNG captures across four languages, both scales and themes")
