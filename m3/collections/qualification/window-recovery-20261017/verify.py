#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Verify exact window-index JNI/Java successor without mutating historical manifests."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
RECEIPT = Path(__file__).with_name('manifest.tsv')
PIN = 'f0592e9a9f9b4207573100ebc9771c0dc3905144d44a677943e8880081bcd484'
PATHS = ('m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java', 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c', 'm3/collections/src/test/java/com/m3/collections/M3IndexMaskWindowRecoverySelfTest.java', 'm3/collections/src/test/java/com/m3/collections/M3IndexMaskWindowRecoveryTest.java')
PREVIOUS = {'m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java': 'e4d8fd83ec0acb9b94bdde5a138d797ccd539c0bb87fe60d419a52f910051fcb', 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c': 'd7786cd5308f4a1e6743680d35e51f5f11a66b106abdb8f170edcb92b901d1cb'}

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
