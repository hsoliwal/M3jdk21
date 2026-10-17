#!/usr/bin/env python3
"""Preflight strict template and current source hashes; not a replacement for OpenRewrite."""
from __future__ import annotations
import hashlib
from pathlib import Path
import re

REPO = Path(__file__).resolve().parents[4]
CRATE = (REPO / 'm3/tooling/migration-recipes/src/main/resources/com/synexia/'
         'rewrite/hash-pinned-java/m3-index-path-rank-20261017')
MANIFEST = CRATE / 'manifest.tsv'
EXPECTED = {
    'm3/collections/src/main/java/com/m3/collections/M3IndexPathRank.java',
    'm3/collections/src/test/java/com/m3/collections/M3IndexPathRankNativeSelfTest.java',
    'm3/collections/src/test/java/com/m3/collections/M3IndexPathRankSelfTest.java',
    'm3/collections/src/test/java/com/m3/collections/M3IndexPathRankTest.java',
}

def main() -> None:
    paths: list[str] = []
    for line in MANIFEST.read_text(encoding='utf-8').splitlines():
        columns = line.split('\t')
        if len(columns) != 4:
            raise ValueError('invalid recipe manifest row')
        path, preimage, digest, template = columns
        if path not in EXPECTED or preimage != 'ABSENT' or not re.fullmatch(r'[0-9a-f]{64}', digest):
            raise ValueError('unadmitted recipe target')
        if '/' in template or '\\' in template or not template.endswith('.java.txt'):
            raise ValueError('unadmitted recipe template')
        for data in ((CRATE / template).read_bytes(), (REPO / path).read_bytes()):
            if hashlib.sha256(data).hexdigest() != digest:
                raise ValueError(f'candidate drift or template mismatch: {path}')
        paths.append(path)
    if paths != sorted(EXPECTED):
        raise ValueError('noncanonical duplicate/unsorted/missing recipe targets')
    print('PASS sealed four Java template/postimage hashes; OpenRewrite execution separate')

if __name__ == '__main__':
    main()
