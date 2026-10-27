#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed original M3JDK21 mask-compactor source and successor admission.

Preserve four unchanged historical sources and admit exactly one qualified
original-owner successor without rewriting the 2026-10-18 historical SHA.
This validates source custody, not JCK or release performance.
"""
from __future__ import annotations

from hashlib import sha256
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[3]
SUCCESSIONS = 'm3/qualification/native-mask-source-succession/SOURCE_SUCCESSIONS.tsv'
COMPACTOR = 'src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java'
HISTORICAL_SHA256 = {
    'src/java.base/share/classes/java/util/ArrayList.java':
        'd05d5efd61e1ae8514c904d28cea7335eec824becddbf62dcb0b425ec7790c7b',
    'src/java.base/share/classes/java/util/Vector.java':
        '14ebbeeb90561ab7c24ba67f4bea90d55735d5c7212fd43c9799e5ba84afdace',
    COMPACTOR:
        '6d26934bab87d6bc68a0c38f0c8ceeec72dc5817e5fca12cfafbc276bafb133d',
    'test/jdk/java/util/ArrayList/M3MaskCompactionDifferential.java':
        '84f7a259016ddcaf68fdc2fabb8bf6beb567b2c9ca38144386a03ae4f31c7091',
    'test/jdk/java/util/ArrayList/M3JdkRemoveIfMaskContracts.java':
        'afb109a5f6a1543f17cd972a091c45ccfc6ad784a849761d8bec4441cfbdd18f',
}
APPROVED_SHA256 = '17a2c1b601d5552b3baeb6751c18700f7254ee6faa23afc0913cf3c45a3b4c05'
ORIGIN_PRODUCT = 'hsoliwal/M3jdk21#1908'
ORIGIN_RECIPE = 'hsoliwal/com.synexia#13301'
ORIGIN_COMMIT = '1dd3623a9bf800129a0a1c5f7fc3c971e0c48298'
LICENSE = 'GPL-2.0-only WITH Classpath-exception-2.0'
STATE = 'SOURCE_SUCCESSOR_REVIEWED_RELEASE_GATES_PENDING'
HEX = re.compile(r'[0-9a-f]{64}\Z')


class SourceCustodyError(ValueError):
    """A source image was missing, forged or evolved without admission."""


def checked_file(root: Path, relative: str) -> Path:
    path = root / relative
    if (path.is_symlink() or not path.is_file()
            or not path.resolve().is_relative_to(root)):
        raise SourceCustodyError('MISSING_OR_LINKED_SOURCE ' + relative)
    return path


def successor(root: Path) -> str:
    rows = [line for line in checked_file(root, SUCCESSIONS)
            .read_text(encoding='utf-8').splitlines()
            if line and not line.startswith('#')]
    if len(rows) != 1:
        raise SourceCustodyError('EXACTLY_ONE_SUCCESSOR_REQUIRED')
    cells = rows[0].split('\t')
    expected = [COMPACTOR, HISTORICAL_SHA256[COMPACTOR], APPROVED_SHA256,
                ORIGIN_PRODUCT, ORIGIN_RECIPE, ORIGIN_COMMIT, LICENSE, STATE]
    if cells != expected or not HEX.fullmatch(cells[1]) or not HEX.fullmatch(cells[2]):
        raise SourceCustodyError('UNREVIEWED_COMPACTOR_SUCCESSOR')
    return cells[2]


def verify(root: Path = ROOT) -> tuple[int, int]:
    root = root.resolve(strict=True)
    current = successor(root)
    for relative, historical in HISTORICAL_SHA256.items():
        file = checked_file(root, relative)
        digest = sha256(file.read_bytes()).hexdigest()
        expected = current if relative == COMPACTOR else historical
        if digest != expected:
            raise SourceCustodyError('M3JDK_NATIVE_MASK_RECEIVER_SOURCE_DRIFT '
                                     + relative + ': ' + digest)

    source = checked_file(root, 'src/java.base/share/classes/java/util/ArrayList.java').read_text()
    vector = checked_file(root, 'src/java.base/share/classes/java/util/Vector.java').read_text()
    helper = checked_file(root, COMPACTOR).read_text()
    for owner, text in [('ArrayList', source), ('Vector', vector)]:
        if text.count('import jdk.internal.mindex.M3CollectionMaskCompactor;') != 1:
            raise SourceCustodyError(owner + ' did not retain exactly one internal compactor import')
        if text.count('M3CollectionMaskCompactor.compact(es, beg, end, deathRow)') != 1:
            raise SourceCustodyError(owner + ' original deletion-mask boundary drifted')
    for phrase in ('System.arraycopy(values, from + nextSurvivor, values, write, span)',
                   'return write;', 'long[] deleted', 'removalRuns(deleted, length)',
                   'private static int compactWords(', 'Long.numberOfTrailingZeros('):
        if phrase not in helper:
            raise SourceCustodyError('MISSING_COMPACTION_INVARIANT ' + phrase)
    return len(HISTORICAL_SHA256) - 1, 1


if __name__ == '__main__':
    try:
        historical, evolved = verify()
    except (OSError, UnicodeError, SourceCustodyError) as error:
        print('M3JDK_NATIVE_MASK_RECEIVER_REFUSED', str(error))
        raise SystemExit(1)
    print('M3JDK_NATIVE_MASK_RECEIVER_SOURCE_PASS',
          f'historical={historical}', f'successors={evolved}',
          'runtime_release=NOT_QUALIFIED')
