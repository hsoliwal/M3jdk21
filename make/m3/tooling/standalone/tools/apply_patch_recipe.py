#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""PATCH_RECIPE v1: apply one unified-diff recipe to a Git work tree, re-runnable to a fixed point.

The recipe is a single structured patch (exact pre-images and post-images of in-file edits). States:
FIXED_POINT when the reverse patch applies cleanly (already applied), APPLIED when the forward patch applies
cleanly and was applied (or would be, with --check), DRIFT when neither applies (the owners moved: rebuild the
recipe from the current files). Exit 0 for FIXED_POINT/APPLIED, 2 for DRIFT.
CLI: python -I apply_patch_recipe.py --repo WORKTREE --patch RECIPE.patch [--check]
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

EXIT_OK = 0
EXIT_DRIFT = 2
STATE_FIXED_POINT = "FIXED_POINT"
STATE_APPLIED = "APPLIED"
STATE_DRIFT = "DRIFT"


def git_apply(repo: Path, patch: Path, *flags: str) -> bool:
    command = ["git", "-C", str(repo), "apply", "--whitespace=nowarn", *flags, str(patch.resolve())]
    return subprocess.run(command, capture_output=True).returncode == 0


def state(repo: Path, patch: Path, check: bool) -> str:
    if git_apply(repo, patch, "--check", "--reverse"):
        return STATE_FIXED_POINT
    if git_apply(repo, patch, "--check"):
        if not check and not git_apply(repo, patch):
            return STATE_DRIFT
        return STATE_APPLIED
    return STATE_DRIFT


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--repo", required=True, type=Path)
    parser.add_argument("--patch", required=True, type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    result = state(args.repo, args.patch, args.check)
    print(f"patch_recipe\t{result}\t{args.patch.name}")
    return EXIT_DRIFT if result == STATE_DRIFT else EXIT_OK


if __name__ == "__main__":
    sys.exit(main())
