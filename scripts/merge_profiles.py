#!/usr/bin/env python3
"""Merge actual Macrobenchmark output; never fabricate profile rules."""
from pathlib import Path
import sys

root=Path(sys.argv[1])
output=Path(sys.argv[2])
output.mkdir(parents=True,exist_ok=True)
for suffix in ('baseline-prof.txt','startup-prof.txt'):
    files=list(root.rglob('*-'+suffix))
    assert files, f'No generated {suffix} under {root}'
    rules={}
    for file in files:
        for line in file.read_text().splitlines():
            line=line.strip()
            if not line or line.startswith('#') or 'org/hayya/salah/fixtures/' in line: continue
            flags=line[:len(line)-len(line.lstrip('HSP'))]
            signature=line[len(flags):]
            assert signature.startswith('L'), (file,line)
            rules.setdefault(signature,set()).update(flags)
    assert rules
    (output/suffix).write_text(''.join(''.join(c for c in 'HSP' if c in rules[s])+s+'\n' for s in sorted(rules)))
    print(suffix,len(rules),'rules from',len(files),'generated files')
