#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Adapted 2026-10-21 from published core #12648 for this distinct NLP lock task.
"""Read back the finite receiving source set and unchanged canonical recipe owners."""
import argparse
import hashlib
import json
from pathlib import Path

from prepare_packet import CANONICAL, NAMED, SOURCE, TASK, safe

ENGINES = {
    'M3HashPinnedJavaSnapshotRecipe.java': '92cc90c46a1ca672a016a89c0d65e6909e090264ba1aa76a85b7fa333470e38b',
    'M3HashPinnedTextSnapshotRecipe.java': '613fd494de5eebdbc29d8982c599d44cd321754b370175f7d097720904a04853',
    'M3EditScope.java': '962d37d62d740c3e8055c365a048ae7ee0479c26a9f6bbfe741e540d6c1134ab',
    'M3ScopeInference.java': 'd2cd6bb4d800878b9f512bf436bf4e38cf73b37676235832e1f68840cfe895c0',
}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('repository', type=Path)
    args = parser.parse_args()
    root = args.repository.resolve(strict=True)
    crate = Path(__file__).resolve().parent
    seal_path = crate / 'SOURCE_SEALS.json'
    seals = json.loads(seal_path.read_text())
    assert seals['schema'] == 'M3_NLP_CURRENT_RECEIVER_LOCK_SOURCE_SEALS/1'
    assert seals['task'] == TASK and seals['recipe'] == NAMED
    assert seals['receiving_revision'] == SOURCE and seals['canonical_source_revision'] == CANONICAL
    seen = set()
    for row in seals['files']:
        relative = row['target_path']
        assert relative not in seen, 'duplicate source pin: ' + relative
        seen.add(relative)
        actual = sha(safe(root, relative))
        assert actual == row['after_sha256'], 'receiver postimage drift: ' + relative
    assert seen, 'empty receiving manifest'
    owner = crate.resolve().parent.parent / 'src/main/java/jdk/internal/m3/rewrite'
    for name, expected in ENGINES.items():
        assert sha(owner / name) == expected, 'canonical engine drift: ' + name
    print(json.dumps({'status': 'EXACT_RECIPE_POSTIMAGES', 'target_paths': len(seen),
                      'unchanged_engines': len(ENGINES), 'source_seals_sha256': sha(seal_path),
                      'scope': 'source readback only; runtime and source-built JDK gates are separate'}))


if __name__ == '__main__':
    main()
