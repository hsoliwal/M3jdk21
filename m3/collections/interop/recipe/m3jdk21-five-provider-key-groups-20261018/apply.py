# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Local exact-source materializer; real OpenRewrite execution is the Maven/JUnit gate."""
from pathlib import Path
import hashlib
import sys

CRATE='m3jdk21-five-provider-key-groups-20261018'
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[4]
PACK=HERE/'src/main/resources/com/synexia/rewrite/hash-pinned-java'/CRATE

def main():
    items=[]
    for line in (PACK/'manifest.tsv').read_text().splitlines():
        if not line or line.startswith('#'): continue
        name,before,after,resource=line.split('\t')
        if before != 'ABSENT' or not name.startswith('m3/collections/interop/src/'):
            raise ValueError('invalid original preimage')
        data=(PACK/resource).read_bytes()
        if hashlib.sha256(data).hexdigest()!=after: raise ValueError('template drift '+name)
        target=ROOT/name
        if target.exists() and (target.is_symlink() or target.read_bytes()!=data):
            raise ValueError('occupied source drift '+name)
        items.append((target,data))
    for target,data in items:
        if not target.exists():
            target.parent.mkdir(parents=True,exist_ok=True)
            target.write_bytes(data)
    print('EXACT_SOURCE_MATERIALIZED',len(items))

if __name__=='__main__':
    try: main()
    except Exception as exc:
        print('SOURCE_REFUSAL',repr(exc),file=sys.stderr);raise
