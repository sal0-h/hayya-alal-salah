#!/usr/bin/env python3
"""Inspect AGP merged manifests, including permissions introduced by dependencies."""
from pathlib import Path
from xml.etree import ElementTree as ET

manifests = list(Path("app/build/intermediates/merged_manifests").glob("**/AndroidManifest.xml"))
assert manifests, "Build the app before running the privacy check"
android = "{http://schemas.android.com/apk/res/android}"
# Location is foreground opt-in. No runtime network or background travel tracking.
for manifest in manifests:
    root = ET.parse(manifest).getroot()
    permissions = {p.get(android + "name") for p in root.findall("uses-permission")}
    assert "android.permission.INTERNET" not in permissions, (manifest, permissions)
    assert "android.permission.ACCESS_BACKGROUND_LOCATION" not in permissions, (manifest, permissions)
    assert "android.permission.QUERY_ALL_PACKAGES" not in permissions, (manifest, permissions)
    app = root.find("application")
    assert app.get(android + "allowBackup") == "false", manifest
    assert app.get(android + "dataExtractionRules"), manifest
    assert app.get(android + "fullBackupContent"), manifest
print(f"Privacy checks passed for {len(manifests)} merged manifests")
