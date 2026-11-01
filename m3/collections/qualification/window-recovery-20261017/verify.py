#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Verify exact window-index JNI/Java successor without mutating historical manifests."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
RECEIPT = Path(__file__).with_name('manifest.tsv')
PIN = '5a31e842ac133187201a6a1fab8c08e3d057e4fd1fb60e43c19f5472921f2b09'
PATHS = ('m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java', 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c', 'm3/collections/src/test/java/jdk/m3/collections/M3IndexMaskWindowRecoverySelfTest.java', 'm3/collections/src/test/java/jdk/m3/collections/M3IndexMaskWindowRecoveryTest.java')
PREVIOUS = {'m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java': '5986cf6a903a0eddbf1191bfd0a8a80a9dbddb1c5d764b8227c54428532b5fee', 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c': 'ec1091ae700abede90db95bb04924ad2ea0eb53432e4ef91f5049227c3d25dcb'}

def verify() -> int:
    if RECEIPT.is_symlink() or not RECEIPT.is_file() or hashlib.sha256(RECEIPT.read_bytes()).hexdigest() != PIN:
        raise SystemExit('Refuse window successor manifest drift or alias')
    lines = [row.split('\t') for row in RECEIPT.read_text(encoding='utf-8').splitlines()]
    if len(lines) != len(PATHS):
        raise SystemExit('Refuse window successor source count drift')
    for row, expected_path in zip(lines, PATHS):
        if len(row) != 4:
            raise SystemExit('Refuse malformed window successor row')
        path, before, after, template = row
        if path != expected_path or before != PREVIOUS.get(path, 'ABSENT') or \
                len(after) != 64 or any(c not in '0123456789abcdef' for c in after) or \
                template != path.rsplit('/', 1)[1] + '.after':
            raise SystemExit('Refuse unapproved successor source ' + path)
        source = ROOT / path
        if source.is_symlink() or not source.is_file() or hashlib.sha256(source.read_bytes()).hexdigest() != after:
            raise SystemExit('Refuse mismatched or missing source ' + path)
    print('PASS M3 window successor SHA256 source custody: ' + str(len(lines)) + ' files')
    return len(lines)

if __name__ == '__main__':
    verify()
