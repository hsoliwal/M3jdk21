#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Stage exact engine reuse and reviewed resources; Java constructs candidates, never Python edits."""
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys

base = Path(sys.argv[1]).resolve()
crate = Path(__file__).resolve().parent
candidate = Path(sys.argv[2]).resolve()
engines = {
    'M3VerbatimJavaPairRecipe.java': '3af361baeb3c04b81808750c1a0d1d8f5c5621a87a2065deec460c540c2cc2b9',
    'M3Jdk21HashPinnedTextSnapshotRecipe.java': '8133bcf7e9f750c714cdd42784bdf30b60f67dd957b2831cd54cb01c9a2cfc3a',
}
engine_path = Path('com/m3/rewrite/backport')
for name, sha in engines.items():
    source = base / 'm3/tooling/migration-recipes/src/main/java' / engine_path / name
    raw = source.read_bytes()
    if hashlib.sha256(raw).hexdigest() != sha:
        raise RuntimeError('existing engine drift: ' + name)
    target = crate / 'target/engine-sources' / engine_path / name
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(raw)
classes = crate / 'target/input-classes'
classes.mkdir(parents=True, exist_ok=True)
subprocess.run(['javac', '--release', '21', '-Xlint:all', '-Werror', '-d', str(classes),
                str(crate/'src/main/java/com/synexia/rewrite/RegionFoundationInputs.java')], check=True)
subprocess.run(['java', '-cp', str(classes), 'com.synexia.rewrite.RegionFoundationInputs', str(base), str(candidate)], check=True)
resource = crate/'src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-region-foundation-20261016'
resource.mkdir(parents=True, exist_ok=True)
paths = ['.github/workflows/m3-foundation.yml', 'm3/docs/name-mapping.json',
         'test/jdk/java/lang/String/M3RegionMatchesNonPositive.java']
rows = []
for index, path in enumerate(paths):
    if index == 2:
        if (base/path).exists():
            raise RuntimeError('absent-before target exists')
        after = (crate/'templates/M3RegionMatchesNonPositive.java').read_bytes()
        before = 'ABSENT'
    else:
        after = (candidate/path).read_bytes()
        before = hashlib.sha256((base/path).read_bytes()).hexdigest()
    name = f'postimage-{index}.txt'
    (resource/name).write_bytes(after)
    rows.append('\t'.join([path, before, hashlib.sha256(after).hexdigest(), name]))
(resource/'manifest.tsv').write_text('\n'.join(rows)+'\n', encoding='utf-8')
print('PREPARATION_ONLY; target sources unchanged')
