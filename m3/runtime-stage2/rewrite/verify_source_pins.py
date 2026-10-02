#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Fail closed when the stage-2 OpenJDK source set drifts from pinned blobs."""
from __future__ import annotations

import json
from pathlib import Path
import subprocess
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
PINS = json.loads((HERE / "source-pins.json").read_text(encoding="utf-8"))

def blob(path: Path) -> str:
    result = subprocess.run(
        ["git", "-C", str(ROOT), "hash-object", str(path.relative_to(ROOT))],
        check=True, capture_output=True, text=True
    )
    return result.stdout.strip()

def main() -> int:
    states: set[str] = set()
    checked = 0
    for relative, expected in PINS["files"].items():
        path = ROOT / relative
        if not path.is_file() or path.is_symlink():
            raise SystemExit(f"source pin missing/non-regular: {relative}")
        actual = blob(path)
        if actual == expected["before_git_blob"]:
            states.add("before")
        elif actual == expected["after_git_blob"]:
            states.add("after")
        else:
            raise SystemExit(
                f"source drift: {relative}: {actual} is neither "
                f"{expected['before_git_blob']} nor {expected['after_git_blob']}"
            )
        checked += 1

    for relative, expected in PINS.get("created_files", {}).items():
        path = ROOT / relative
        if not path.exists():
            states.add("before")
        elif path.is_symlink() or not path.is_file():
            raise SystemExit(f"created source non-regular: {relative}")
        else:
            actual = blob(path)
            if actual != expected["after_git_blob"]:
                raise SystemExit(
                    f"created source drift: {relative}: {actual} != "
                    f"{expected['after_git_blob']}"
                )
            states.add("after")
        checked += 1

    if len(states) != 1:
        raise SystemExit(f"mixed stage-2 source state rejected: {sorted(states)}")
    state = next(iter(states))
    print(f"M3_STAGE2_SOURCE_PINS_PASS state={state} files={checked}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
