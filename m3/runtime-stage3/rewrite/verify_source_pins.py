#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Fail closed on a mixed stage-2/stage-3 MIndexString VM layout."""
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

def matches(relative: str, expected: str | None) -> bool:
    path = ROOT / relative
    if expected is None:
        return not path.exists()
    return path.is_file() and not path.is_symlink() and blob(path) == expected

def main() -> int:
    states: set[str] = set()
    for relative, expected in PINS["files"].items():
        before = expected["before_git_blob"]
        after = expected["after_git_blob"]
        if matches(relative, before):
            states.add("before")
        elif matches(relative, after):
            states.add("after")
        else:
            path = ROOT / relative
            actual = blob(path) if path.is_file() and not path.is_symlink() else "ABSENT"
            raise SystemExit(
                f"source drift: {relative}: {actual} is neither before={before} nor after={after}"
            )
    if len(states) != 1:
        raise SystemExit(f"mixed stage-3 source state rejected: {sorted(states)}")
    state = next(iter(states))
    print(f"M3_STAGE3_SOURCE_PINS_PASS state={state} files={len(PINS['files'])}")
    return 0

if __name__ == "__main__":
    sys.exit(main())
