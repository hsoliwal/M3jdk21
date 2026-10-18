# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Fail closed on canonical recipe mirror and target generated-source hashes."""
from pathlib import Path
import hashlib
import sys

CRATE = 'm3jdk21-five-provider-key-ranges-20261018'
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
PACK = HERE/'src/main/resources/com/synexia/rewrite/hash-pinned-java'/CRATE

def digest(data): return hashlib.sha256(data).hexdigest()
def main():
    names = [line.split('\t') for line in (PACK/'manifest.tsv').read_text().splitlines()
             if line.strip() and not line.startswith('#')]
    assert len(names) == 2
    paths=[]
    for path, before, after, template in names:
        assert before == 'ABSENT', (path, before)
        assert path.startswith('m3/collections/interop/src/'), path
        candidate=(PACK/template).read_bytes()
        assert digest(candidate) == after, ('template SHA drift', path)
        product=ROOT/path
        assert product.is_file() and not product.is_symlink(), ('missing postimage',path)
        assert product.read_bytes() == candidate, ('generated postimage drift',path)
        paths.append(path)
    assert len(paths) == len(set(paths)) and paths == sorted(paths)
    print('M3_FIVE_PROVIDER_KEY_RANGES_POSTIMAGES_PASS',len(paths))
if __name__ == '__main__':
    try: main()
    except Exception as err:
        print('RECEIVER_SOURCE_SEAL_FAILURE',repr(err),file=sys.stderr)
        raise
