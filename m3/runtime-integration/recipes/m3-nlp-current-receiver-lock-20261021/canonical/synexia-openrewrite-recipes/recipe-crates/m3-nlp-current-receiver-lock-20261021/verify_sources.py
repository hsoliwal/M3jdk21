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
    'M3HashPinnedJavaSnapshotRecipe.java': '083f8b1fd956fe2ed81168bc580d1e0f39a815401c50571d397ddba40150cf63',
    'M3HashPinnedTextSnapshotRecipe.java': '5a64bd502cb8aa7d51b78813b736e37a0a7f420233f405c4d2df08ee2eefe8bc',
    'M3EditScope.java': '962d37d62d740c3e8055c365a048ae7ee0479c26a9f6bbfe741e540d6c1134ab',
    'M3ScopeInference.java': '9a6c54f42564c0ef82de3f40a2de17e6598569916c15255653bf79fbe3353579',
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
    owner = crate.resolve().parent.parent / 'src/main/java/com/synexia/rewrite'
    for name, expected in ENGINES.items():
        assert sha(owner / name) == expected, 'canonical engine drift: ' + name
    print(json.dumps({'status': 'EXACT_RECIPE_POSTIMAGES', 'target_paths': len(seen),
                      'unchanged_engines': len(ENGINES), 'source_seals_sha256': sha(seal_path),
                      'scope': 'source readback only; runtime and source-built JDK gates are separate'}))


if __name__ == '__main__':
    main()
