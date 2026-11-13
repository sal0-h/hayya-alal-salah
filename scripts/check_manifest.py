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
    banned = {"android.permission.QUERY_ALL_PACKAGES", "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.SYSTEM_ALERT_WINDOW", "android.permission.READ_PHONE_STATE",
        "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_AUDIO", "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.RECORD_AUDIO"}
    assert not permissions & banned, (manifest,permissions & banned)
    app = root.find("application")
    assert app.get(android + "allowBackup") == "false", manifest
    assert app.get(android + "dataExtractionRules"), manifest
    assert app.get(android + "fullBackupContent"), manifest
    for component in app:
        if component.tag == "service" and component.get(android + "name", "").endswith("PrayerFocusService"):
            assert component.get(android + "permission") == "android.permission.BIND_ACCESSIBILITY_SERVICE", manifest
        if component.get(android + "exported") == "true" and component.tag in ("service","receiver","provider"):
            assert component.get(android + "permission"), (manifest,component.attrib)
# Verify both source backup formats exclude credential/device-protected user data.
required_domains = {"root","file","database","sharedpref","external","device_root","device_file","device_database","device_sharedpref"}
backup = ET.parse("app/src/main/res/xml/backup_rules.xml").getroot()
excludes = {node.get("domain") for node in backup.findall("exclude") if node.get("path") == "."}
assert required_domains <= excludes
extraction = ET.parse("app/src/main/res/xml/data_extraction_rules.xml").getroot()
for kind in ("cloud-backup","device-transfer"):
    excludes = {node.get("domain") for node in extraction.find(kind).findall("exclude") if node.get("path") == "."}
    assert required_domains <= excludes, kind
print(f"Privacy checks passed for {len(manifests)} merged manifests")
