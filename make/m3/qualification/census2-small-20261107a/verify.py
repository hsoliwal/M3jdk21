#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""CENSUS2_SMALL_20261107A verify v1: re-check every no-edit decision of DECISION.tsv against a revision.

PROBES.tsv rows: row, file, kind, literal.
  PRESENT / ABSENT  the literal must / must not occur in the file
  ABSENT_FILE       the path must not exist
  BLOB              the file's git blob id must equal the literal
  NO_BASENAME       no tracked path ends in /<file>, except retained variants of the master landing
Files come from --rev (`git show REV:path`, nothing checked out) or, by default, from the working tree with the
index as fallback for paths outside a sparse checkout. A FAIL means master moved under a decision; that row needs a
new decision. Exit 0 when every probe passes, 1 otherwise.

usage: python -I verify.py [--root DIR] [--rev REV]
"""
from __future__ import annotations

import argparse
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
RETAINED = "/retained-variants/"


def git(root: Path, *args: str):
    result = subprocess.run(["git", "-C", str(root), *args], capture_output=True)
    return result.stdout.decode("utf-8") if result.returncode == 0 else None


def spec(rev, rel: str) -> str:
    return "%s:%s" % (rev, rel) if rev else ":" + rel


def read(root: Path, rev, rel: str):
    path = root / rel
    if rev is None and path.is_file():
        return path.read_text(encoding="utf-8")
    return git(root, "show", spec(rev, rel))


def blob(root: Path, rev, rel: str):
    path = root / rel
    if rev is None and path.is_file():
        text = git(root, "hash-object", "--path=" + rel, str(path))
    else:
        text = git(root, "rev-parse", spec(rev, rel))
    return text.strip() if text else None


def paths(root: Path, rev):
    text = git(root, "ls-tree", "-r", "--name-only", rev) if rev else git(root, "ls-files")
    return [p for p in (text or "").splitlines() if RETAINED not in p]


def probe(root: Path, rev, rel: str, kind: str, literal: str, listing) -> bool:
    if kind == "NO_BASENAME":
        return not any(p == rel or p.endswith("/" + rel) for p in listing())
    if kind == "BLOB":
        return blob(root, rev, rel) == literal
    text = read(root, rev, rel)
    if kind == "ABSENT_FILE":
        return text is None
    return text is not None and (literal in text) == (kind == "PRESENT")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(HERE.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--rev", help="read files from this revision instead of the working tree")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    rows = [line.split("\t") for line in (HERE / "PROBES.tsv").read_text(encoding="utf-8").splitlines()[1:] if line]
    cached = []

    def listing():
        if not cached:
            cached.append(paths(root, args.rev))
        return cached[0]

    failures = 0
    for row, rel, kind, literal in rows:
        ok = probe(root, args.rev, rel, kind, literal, listing)
        failures += not ok
        print("%s %s %s %s %s" % ("PASS" if ok else "FAIL", row, kind, rel, literal[:80]))
    print("CENSUS2_SMALL_20261107A probes=%d failures=%d rev=%s" % (len(rows), failures, args.rev or "worktree"))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
