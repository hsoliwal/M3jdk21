# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Refuse missing, drifted and foreign generated Java source postimages."""
from pathlib import Path
import hashlib
import sys

CRATE = 'm3jdk21-five-provider-key-groups-20261018'
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
PACK = HERE/'src/main/resources/com/synexia/rewrite/hash-pinned-java'/CRATE

def sha(data): return hashlib.sha256(data).hexdigest()

def verify():
    entries = [line.split('\t') for line in (PACK/'manifest.tsv').read_text().splitlines()
               if line and not line.startswith('#')]
    if len(entries) != 2 or any(len(row) != 4 for row in entries):
        raise ValueError('unexpected generated-source manifest shape')
    seen=set()
    for name, before, after, template in entries:
        if before != 'ABSENT' or not name.startswith('m3/collections/interop/src/') or name in seen:
            raise ValueError('bad path, preimage or duplicate: '+name)
        seen.add(name)
        source=PACK/template
        if not source.is_file() or source.is_symlink() or sha(source.read_bytes())!=after:
            raise ValueError('canonical source template changed: '+name)
        target=ROOT/name
        if not target.is_file() or target.is_symlink() or target.read_bytes()!=source.read_bytes():
            raise ValueError('missing or drifting generated Java: '+name)
    print('M3_FIVE_PROVIDER_GROUPS_SEALED_OUTPUTS_PASS',len(seen))

if __name__ == '__main__':
    try: verify()
    except Exception as exc:
        print('M3_FIVE_PROVIDER_GROUPS_SEAL_FAIL',repr(exc),file=sys.stderr)
        raise
