#!/usr/bin/env python3
"""Catch a declared-but-uncompiled synthetic receiver before starting an emulator journey."""
from pathlib import Path
import sys, zipfile
for value in sys.argv[1:]:
    path=Path(value)
    with zipfile.ZipFile(path) as apk:
        assert any(b'Lorg/hayya/salah/fixtures/BenchmarkSeedReceiver;' in apk.read(name)
                   for name in apk.namelist() if name.endswith('.dex')), path
    print(f'Compiled synthetic seed receiver verified: {path}')
