#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe PMEM_ACCELERATOR_MERGE v1: the MERGE rows of recipe/pmem-accelerator-20261107/RELOCATION_MAP.tsv (the three
existing m3/optane files the relocated classes need: the m3pmemaccel CMake target, the surefire filter and native
profile property of pom.xml, one README section).

Derived from m3/qualification/census2-small-batch-20261106/apply.py v1 (#2611): every rule names the git blob sha of
its preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already
applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing. Every preimage is
verified before the first write. The anchored edits live in hunks/<leaf>.edits (@@@ FILE/HUNK/OLD/NEW/END, every OLD
block exactly once in the preimage). Idempotent: a second run reports changed=0, and EDITS.tsv (file, before/after
sha256, leaf, crate, donor sha chain, rule) is a fixed point. --leaf applies one leaf's rules only; --check writes
nothing and exits 1 when a run would change a file.

usage: python -I apply.py [--root DIR] [--check] [--leaf L008]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "f6210762e04f089faf654236b7734b53b39143f2"
DONOR_COMMIT = "56cbdc6312594f06e21ef56f0ce33b605383270b"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "crate", "donor_chain", "rule")
MARK = "@@@ "


class Refused(Exception):
    pass


# leaf, path, preimage blob (master f6210762e04), crate, donor chain, rule note
RULES = (
    ("L008", "m3/optane/src/main/native/CMakeLists.txt", "c39e930d8b836208ec01f6e822d78f437444ddb5",
     "m3-pmem-ordered-avx512-20261030 (live owner: the donor jmod native directory, FILE row 3 of RELOCATION_MAP.tsv)",
     "donor build.sh 9a4d9d8ba54c (javac -h, gcc -std=c11 -O2 -ffp-contract=off -fno-fast-math -Wall -Wextra -Werror"
     " -fPIC -shared, 8 JNI symbols) -> CMake target m3pmemaccel of this module",
     "MERGE: one shared library target m3pmemaccel (javac -h of jdk.internal.m3.optane.M3PmemAccelerator,"
     " m3pmemaccel.c, C17, -Wall -Wextra -Wpedantic -Werror -ffp-contract=off -fno-fast-math); m3optane and"
     " m3pmemsdk unchanged"),
    ("L008", "m3/optane/pom.xml", "4792c4b0f2c282165aae76c48e16c39f00b7a097",
     "m3-pmem-ordered-avx512-20261030 (live owner: the donor accelerator jmod, FILE rows 1 and 2 of RELOCATION_MAP.tsv)",
     "donor build.sh runs PmemAcceleratorSelfTest under -Xcheck:jni with the library on java.library.path -> the"
     " profile m3-optane-native of this module",
     "MERGE: the default surefire filter excludes M3PmemAcceleratorJUnitTest (libm3pmemaccel.so absent on the Java"
     " path); the profile m3-optane-native passes -Dpmem.accelerator.native=true (its filter *Test already includes"
     " the test)"),
    ("L008", "m3/optane/README.md", "98b4fd6c4cf5e57f1333a6c182ec4ad40cc0c75a",
     "m3-pmem-ordered-avx512-20261030 + m3-pmem-16source-numa-convergence-20261031",
     "census L008 + L047 (C:/m3-superset/conv/record/census/LEAVES_PROPOSED.tsv) -> recipe/pmem-accelerator-20261107",
     "MERGE: one section after the pmem-region-core relocation table naming the accelerator, the CPU-list parser,"
     " the recipe and the hardware HOLD"),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def load_hunks(leaf: str, path: str) -> list[tuple[str, str]]:
    text = (RECIPE_DIR / "hunks" / (leaf + ".edits")).read_text(encoding="utf-8")
    hunks, current, mode, old, new = [], None, None, [], []
    for line in text.split("\n")[:-1]:
        if line.startswith(MARK):
            word, _, rest = line[len(MARK):].partition(" ")
            if word == "FILE":
                current = rest.strip()
            elif word == "HUNK":
                mode, old, new = None, [], []
            elif word in ("OLD", "NEW"):
                mode = word
            elif word == "END":
                if mode != "NEW":
                    raise Refused("malformed hunk in %s for %s" % (leaf, path))
                if current == path:
                    hunks.append(("\n".join(old) + "\n", "\n".join(new) + "\n"))
                mode = None
            else:
                raise Refused("unknown marker in %s: %s" % (leaf, line))
        elif mode == "OLD":
            old.append(line)
        elif mode == "NEW":
            new.append(line)
    if not hunks:
        raise Refused("no hunks for %s %s" % (leaf, path))
    return hunks


def postimage(pre: bytes, hunks, path: str) -> bytes:
    if b"\r\n" in pre:
        raise Refused("CRLF preimage %s (receivers are LF in git)" % path)
    text = pre.decode("utf-8")
    for old, new in hunks:
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, crate, chain, note in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, load_hunks(rleaf, rel), rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, sha256(pre), sha256(post), rleaf, crate, chain, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("PMEM_ACCELERATOR_MERGE recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({rule[0] for rule in RULES}), help="apply one leaf's rules only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
