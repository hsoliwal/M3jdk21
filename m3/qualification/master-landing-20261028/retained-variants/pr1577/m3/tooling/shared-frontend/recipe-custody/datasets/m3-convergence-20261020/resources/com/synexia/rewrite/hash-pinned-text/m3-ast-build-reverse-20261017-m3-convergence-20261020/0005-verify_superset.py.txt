#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed Git-tree custody for the M3JDK21 integrated Java/JNI superset.

Source code is never rewritten here. The original Maven/OpenRewrite recipes remain
responsible for each parent's source, contract and byte-image qualification.
"""
from __future__ import annotations

import argparse
import json
import subprocess
from pathlib import Path

BASE = "d6d76b563050fc13c4fa79bc494edf29f1614b37"
PARENTS = (
    ("collections-parser-fs-jni-1539", "8a3540d43c25ce92dafe2dd6b3f3e61ee7783dda"),
    ("mindexast-scopes-1535", "6b0557f3fd47fd63ef0d336c46fec04d670e4dfb"),
    ("native-state-masks-1506", "f4f942ddeedf24d2a7bc1a2e9da2ff8ab1c70084"),
    ("real-jdt-1516", "ebb83cbb0775df3b9b91ad0b18376653077c9dce"),
)
EXPECTED_SOURCE_PATHS = 161
EXTRA_PREFIXES = (
    "m3/runtime-integration/convergence-20261020/",
    "m3/tooling/shared-frontend/recipe-custody/datasets/m3-convergence-20261020/",
)
EXTRA_FILES = frozenset({".github/workflows/m3-convergence-superset-20261020.yml"})


def command(repo: Path, *args: str) -> bytes:
    finished = subprocess.run(
        ("git", "-C", str(repo), *args),
        check=False, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    if finished.returncode:
        details = finished.stderr.decode("utf-8", "replace").strip()
        raise RuntimeError(f"git {' '.join(args)} refused ({finished.returncode}): {details[:700]}")
    return finished.stdout


def diff_paths(repo: Path, left: str, right: str) -> dict[str, str]:
    """Parse -z filename framing exactly, preserving spaces and tabs."""
    raw = command(repo, "diff", "--no-renames", "--name-status", "-z", left, right)
    tokens = raw.split(b"\0")
    if tokens.pop() != b"":
        raise ValueError("invalid Git -z termination")
    if len(tokens) % 2:
        raise ValueError("invalid Git -z name-status pairing")
    changes: dict[str, str] = {}
    for i in range(0, len(tokens), 2):
        mode = tokens[i].decode("ascii", "strict")
        path = tokens[i + 1].decode("utf-8", "surrogateescape")
        if mode not in {"A", "M", "D", "T"} or not path or path in changes:
            raise ValueError(f"unexpected or duplicate Git diff row: {mode}: {path!r}")
        changes[path] = mode
    return changes


def verify(repo: Path, merged: str, base: str = BASE,
           parents: tuple[tuple[str, str], ...] = PARENTS,
           expected_paths: int = EXPECTED_SOURCE_PATHS) -> dict:
    repo = repo.resolve(strict=True)
    actual_parents = command(repo, "rev-list", "--parents", "-n1", merged).decode().split()
    expected_parents = [sha for _, sha in parents]
    if len(actual_parents) != len(expected_parents) + 2 \
            or actual_parents[2:] != expected_parents:
        raise ValueError("merge parent identity/order differs from approved donor heads")
    recipe_parent = actual_parents[1]
    # The first parent is the independently reviewable source-recipe admission
    # commit. It may only add this task's qualification files, never source.
    recipe_changes = diff_paths(repo, base, recipe_parent)
    invalid_recipe = [p for p in recipe_changes if p not in EXTRA_FILES
                      and not any(p.startswith(prefix) for prefix in EXTRA_PREFIXES)]
    if invalid_recipe:
        raise ValueError(f"recipe parent touched product source: {invalid_recipe[:8]}")
    owned: dict[str, str] = {}
    counts: dict[str, int] = {}
    for name, sha in parents:
        original = diff_paths(repo, base, sha)
        if not original:
            raise ValueError(f"empty selected donor: {name}")
        conflicting = sorted(set(original).intersection(owned))
        if conflicting:
            raise ValueError(f"overlapping selected donor changes: {name}: {conflicting[:8]}")
        overwritten = sorted(set(original).intersection(diff_paths(repo, sha, merged)))
        if overwritten:
            raise ValueError(f"donor content/mode lost from {name}: {overwritten[:8]}")
        owned.update({path: name for path in original})
        counts[name] = len(original)
    if len(owned) != expected_paths:
        raise ValueError(f"source path count changed: expected {expected_paths}, got {len(owned)}")
    actual = diff_paths(repo, base, merged)
    missing = sorted(set(owned).difference(actual))
    if missing:
        raise ValueError(f"donor changes absent from merged delta: {missing[:8]}")
    unowned = sorted(set(actual).difference(owned))
    invalid_extra = [x for x in unowned if x not in EXTRA_FILES
                     and not any(x.startswith(prefix) for prefix in EXTRA_PREFIXES)]
    if invalid_extra:
        raise ValueError(f"unreviewed extra product changes: {invalid_extra[:8]}")
    # A full tree diff is used: mode, content and deletion status are preserved.
    return {
        "schema": "M3JDK21-SUPERSET-GIT-TREE-V1",
        "base": base,
        "recipe_parent": recipe_parent,
        "merged": merged,
        "donors": [{"name": name, "commit": sha, "changed_paths": counts[name]}
                   for name, sha in parents],
        "preserved_source_paths": len(owned),
        "qualification_paths": len(unowned),
        "conflicting_source_paths": 0,
        "source_mutation_performed": False,
        "proof": "EXACT_GIT_TREE_AND_MODE_EQUISOURCE",
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path.cwd())
    parser.add_argument("--merged", default="HEAD")
    args = parser.parse_args()
    result = verify(args.repository, args.merged)
    print(json.dumps(result, sort_keys=True, indent=2))


if __name__ == "__main__":
    main()
