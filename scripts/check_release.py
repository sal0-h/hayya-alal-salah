#!/usr/bin/env python3
"""Validate actual optimized APK contents; never signs or publishes a build."""
from pathlib import Path
import hashlib
import json
import zipfile
from xml.etree import ElementTree as ET

apk = Path('app/build/outputs/apk/release/app-release-unsigned.apk')
assert apk.is_file(), 'Assemble release first'
assert apk.stat().st_size <= 35 * 1024 * 1024, 'Release APK exceeds the approved 35 MiB budget'
with zipfile.ZipFile(apk) as archive:
    names = set(archive.namelist())
    for license in Path('licenses').glob('*.txt'):
        assert archive.read('assets/' + license.name) == license.read_bytes(), license
    assert not any(n.startswith('lib/') for n in names), 'Review newly introduced native code/ABIs'
    assert not any(n.startswith('META-INF/') and n.endswith(('.RSA','.DSA','.EC')) for n in names), 'Ordinary builds must stay unsigned'
    assert 'classes.dex' in names
    # Generation must happen before committing profiles; never manufacture a profile.
    if Path('app/src/main/baseline-prof.txt').is_file():
        assert 'assets/dexopt/baseline.prof' in names
android = '{http://schemas.android.com/apk/res/android}'
manifests = list(Path('app/build/intermediates/merged_manifests/release').glob('**/AndroidManifest.xml'))
assert manifests
for manifest in manifests:
    root = ET.parse(manifest).getroot()
    assert root.get('package') == 'org.hayya.salah'
    app = root.find('application')
    assert app.get(android + 'debuggable', 'false') == 'false'
    assert app.find('profileable') is None, 'Shell profiling belongs only in isolated benchmark targets'
    for component in app:
        name = component.get(android + 'name', '')
        assert not any(word in name for word in ('Screenshot','CaptureHost','DebugAlarmProbe','BenchmarkSeed','FocusTarget','AudioFixture')), name
result = {'apk': apk.name, 'bytes': apk.stat().st_size, 'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
          'production_signed': False, 'physical_acceptance': False}
directory = Path('app/build/reports/release'); directory.mkdir(parents=True, exist_ok=True)
(directory / 'artifact.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result))
