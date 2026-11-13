#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""CENSUS2_ONE_PASS_20261107A verify v1: re-check every no-edit decision of DECISION.tsv against a revision.

PROBES.tsv rows: row, file, kind, literal; kinds BLOB / PRESENT / ABSENT / ABSENT_FILE / NO_BASENAME.
The probe atom is reused unchanged from ../census2-small-20261107a/verify.py (same contract, same --rev and
worktree semantics); this file only binds it to this directory's PROBES.tsv. A FAIL means master moved under a
decision; that row needs a new decision. Exit 0 when every probe passes, 1 otherwise, 2 if the reused atom is absent.

usage: python -I verify.py [--root DIR] [--rev REV]
"""
from __future__ import annotations

import argparse
import importlib.util
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ATOM = HERE.parent / "census2-small-20261107a" / "verify.py"


def load_atom():
    if not ATOM.is_file():
        return None
    spec = importlib.util.spec_from_file_location("census2_small_verify", ATOM)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(HERE.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--rev", help="read files from this revision instead of the working tree")
    args = parser.parse_args(argv)
    atom = load_atom()
    if atom is None:
        print("CENSUS2_ONE_PASS_20261107A reused probe atom absent: %s" % ATOM)
        return 2
    root = Path(args.root).resolve()
    rows = [line.split("\t") for line in (HERE / "PROBES.tsv").read_text(encoding="utf-8").splitlines()[1:] if line]
    cached = []

    def listing():
        if not cached:
            cached.append(atom.paths(root, args.rev))
        return cached[0]

    failures = 0
    for row, rel, kind, literal in rows:
        ok = atom.probe(root, args.rev, rel, kind, literal, listing)
        failures += not ok
        print("%s %s %s %s %s" % ("PASS" if ok else "FAIL", row, kind, rel, literal[:80]))
    print("CENSUS2_ONE_PASS_20261107A probes=%d failures=%d rev=%s" % (len(rows), failures, args.rev or "worktree"))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
