#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""fdp-delegate provenance: pin the M3 java.base sources one gate run compiled to an M3JDK commit.

  python m3src_pin.py --run <run_gates.py output dir> --commit <rev> --label <pull request> [--repo <checkout>]

Reads the sources run_gates.py staged under <run>/m3-src (the bytes javac compiled with --patch-module), checks
their SHA-256 against the run's m3_patch_compile record, and compares their git blob ids
(git hash-object --no-filters, which writes nothing) with the commit's tree (git ls-tree) for run_gates.py's
own file patterns. Both commands read tree objects only and run with GIT_NO_LAZY_FETCH=1, so a blobless clone
fetches nothing. Writes <run>/m3src-pin.json; exits 1 unless every compiled file is the commit's blob and the
commit holds no further file matching the patterns.
"""
from __future__ import annotations

import argparse
import fnmatch
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys

sys.dont_write_bytecode = True

TOOLS = Path(__file__).resolve().parent
REPO = TOOLS.parents[3]
GATE = "m3_src_pin"
SOURCE_RECORD = "m3_patch_compile"
OUTPUT = "m3src-pin.json"
STAGE_ROOT = "m3-src"
CLASSES_ROOT = "/classes/"
BLOB = "blob"
GIT_ENV = {"GIT_NO_LAZY_FETCH": "1"}
STATUS_PASS = "PASS"
STATUS_FAIL = "FAIL"
EXIT_UNPINNED = 1


def load_run_gates():
    spec = importlib.util.spec_from_file_location("fdp_delegate_run_gates", TOOLS / "run_gates.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def git(repo: Path, *args: str) -> str:
    result = subprocess.run(["git", "-C", str(repo), *args], capture_output=True, text=True, check=False,
                            env={**os.environ, **GIT_ENV})
    if result.returncode:
        raise SystemExit("M3SRC_PIN_REFUSED: git " + args[0] + ": " + result.stderr.strip())
    return result.stdout


def commit_blobs(repo: Path, commit: str, directory: str, patterns: tuple[str, ...]) -> dict[str, str]:
    """name -> blob id of every file directly under <directory> in <commit> matching one of <patterns>."""
    blobs = {}
    for line in git(repo, "ls-tree", commit, directory + "/").splitlines():
        meta, path = line.split("\t", 1)
        _, kind, blob = meta.split()
        name = path.rsplit("/", 1)[-1]
        if kind == BLOB and any(fnmatch.fnmatchcase(name, pattern) for pattern in patterns):
            blobs[name] = blob
    return blobs


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--label", required=True, help="the pull request the commit is the head of, e.g. #2215")
    parser.add_argument("--repo", type=Path, default=REPO, help="a checkout of the clone that holds the commit")
    args = parser.parse_args(argv)
    gates = load_run_gates()
    records = [json.loads(line) for line in (args.run / "RESULT.jsonl").read_text("utf-8").splitlines()]
    compiled = [r for r in records if r["gate"] == SOURCE_RECORD]
    if len(compiled) != 1:
        raise SystemExit(f"M3SRC_PIN_REFUSED: {len(compiled)} {SOURCE_RECORD} records in {args.run}")
    recorded = compiled[0]["files"]
    stage = args.run / STAGE_ROOT / gates.M3_MATH.split(CLASSES_ROOT, 1)[1]
    commit = git(args.repo, "rev-parse", "--verify", args.commit + "^{commit}").strip()
    in_commit = commit_blobs(args.repo, commit, gates.M3_MATH, gates.M3_SOURCES)
    names = sorted(recorded)
    staged = [stage / name for name in names]
    staged_blobs = git(args.repo, "hash-object", "--no-filters", *map(str, staged)).split()
    files = {name: {"commit_blob": in_commit.get(name), "staged_blob": blob,
                    "staged_sha256": hashlib.sha256(path.read_bytes()).hexdigest(), "record_sha256": recorded[name]}
             for name, path, blob in zip(names, staged, staged_blobs)}
    pinned = sum(1 for f in files.values()
                 if f["commit_blob"] == f["staged_blob"] and f["staged_sha256"] == f["record_sha256"])
    commit_only = sorted(set(in_commit) - set(recorded))
    passed = bool(files) and pinned == len(files) and not commit_only
    record = {"gate": GATE, "label": args.label, "commit": commit, "path": gates.M3_MATH,
              "patterns": list(gates.M3_SOURCES), "files": files, "pinned": pinned, "count": len(files),
              "commit_only": commit_only, "status": STATUS_PASS if passed else STATUS_FAIL}
    (args.run / OUTPUT).write_text(json.dumps(record, indent=1, sort_keys=True) + "\n", encoding="utf-8",
                                   newline="\n")
    print(json.dumps({k: record[k] for k in ("gate", "label", "commit", "pinned", "count", "commit_only", "status")},
                     sort_keys=True))
    return 0 if passed else EXIT_UNPINNED


if __name__ == "__main__":
    raise SystemExit(main())
