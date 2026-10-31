#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""fdp-delegate gate: the repository's public-names check over this branch, as one JSON record.

  python public_names_gate.py --base <rev> --out <dir> [--checker <check_public_names.py>]

Resolves --base and HEAD to commit ids and runs m3/compatibility/check_public_names.py --base --head on them,
with GIT_NO_LAZY_FETCH=1 so a blobless clone fetches nothing. A sparse checkout without m3/compatibility passes a
copy with --checker; the copy must be HEAD's blob of that path (git blob ids). Writes <out>/public-names.json
(base, head, checker blob, exit code, changed files, violations, status) and exits with the checker's code.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

sys.dont_write_bytecode = True

REPO = Path(__file__).resolve().parents[4]
GATE = "public_names"
CHECKER_REL = "m3/compatibility/check_public_names.py"
OUTPUT = "public-names.json"
SUMMARY = re.compile(r"^PUBLIC_NAMES files=(?P<files>\d+) violations=(?P<violations>\d+)$", re.MULTILINE)
GIT_ENV = {"GIT_NO_LAZY_FETCH": "1"}
EXIT_PASS = 0
EXIT_VIOLATIONS = 1
EXIT_REFUSED = 2
STATUS = {EXIT_PASS: "PASS", EXIT_VIOLATIONS: "FAIL"}
STATUS_REFUSED = "REFUSED"


def git(*args: str) -> str:
    result = subprocess.run(["git", "-C", str(REPO), *args], capture_output=True, text=True, check=False,
                            env={**os.environ, **GIT_ENV})
    if result.returncode:
        raise SystemExit("PUBLIC_NAMES_GATE_REFUSED: git " + args[0] + ": " + result.stderr.strip())
    return result.stdout.strip()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--checker", type=Path, default=REPO / CHECKER_REL)
    args = parser.parse_args(argv)
    base = git("rev-parse", "--verify", args.base + "^{commit}")
    head = git("rev-parse", "--verify", "HEAD^{commit}")
    checker_blob = git("rev-parse", "--verify", "HEAD:" + CHECKER_REL)
    if git("hash-object", "--no-filters", str(args.checker)) != checker_blob:
        raise SystemExit(f"PUBLIC_NAMES_GATE_REFUSED: {args.checker} is not HEAD's {CHECKER_REL}")
    result = subprocess.run([sys.executable, "-I", str(args.checker), "--base", base, "--head", head,
                             "--repo", str(REPO)], capture_output=True, text=True, check=False,
                            env={**os.environ, **GIT_ENV})
    summary = SUMMARY.search(result.stdout)
    violations = [line.split("\t", 1) for line in result.stdout.splitlines() if "\t" in line]
    record = {"gate": GATE, "checker": CHECKER_REL, "checker_blob": checker_blob, "base": base, "head": head,
              "exit": result.returncode, "files": int(summary["files"]) if summary else None,
              "violation_count": int(summary["violations"]) if summary else None, "violations": violations,
              "refusal": result.stderr.strip(), "status": STATUS.get(result.returncode, STATUS_REFUSED)}
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / OUTPUT).write_text(json.dumps(record, indent=1, sort_keys=True) + "\n", encoding="utf-8",
                                   newline="\n")
    print(json.dumps({k: record[k] for k in ("gate", "head", "files", "violation_count", "status")}, sort_keys=True))
    return result.returncode if result.returncode in STATUS else EXIT_REFUSED


if __name__ == "__main__":
    raise SystemExit(main())
