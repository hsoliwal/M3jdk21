#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""MERGELOSS2_0E_RECORDS verify v1: re-check the evidence of every no-edit decision in DECISION.tsv.

PROBES.tsv rows: leaf, file, kind, literal. PRESENT means the literal must occur in the file, ABSENT means it must
not, and ABSENT_FILE means the path must not exist. Files are read from the working tree, or from a revision with
--rev (`git show REV:path`, nothing checked out). A FAIL means master moved under a decision and the leaf needs a
new decision. Exit 0 when every probe passes, 1 otherwise.

usage: python -I verify.py [--root DIR] [--rev REV]
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent


def read(root: Path, rev: str | None, rel: str):
    if rev is None:
        path = root / rel
        return path.read_text(encoding="utf-8") if path.is_file() else None
    result = subprocess.run(["git", "-C", str(root), "show", "%s:%s" % (rev, rel)], capture_output=True)
    return result.stdout.decode("utf-8") if result.returncode == 0 else None


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(HERE.parents[3]), help="repository root (default: three up)")
    parser.add_argument("--rev", help="read files from this revision instead of the working tree")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    rows = [line.split("\t") for line in (HERE / "PROBES.tsv").read_text(encoding="utf-8").splitlines()[1:] if line]
    cache, failures = {}, 0
    for leaf, rel, kind, literal in rows:
        if rel not in cache:
            cache[rel] = read(root, args.rev, rel)
        text = cache[rel]
        if kind == "ABSENT_FILE":
            ok = text is None
        elif text is None:
            ok = False
        else:
            ok = (literal in text) == (kind == "PRESENT")
        failures += not ok
        print("%s leaf=%s %s %s %s" % ("PASS" if ok else "FAIL", leaf, kind, rel, literal[:80]))
    print("MERGELOSS2_0E_RECORDS probes=%d failures=%d rev=%s" % (len(rows), failures, args.rev or "worktree"))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
