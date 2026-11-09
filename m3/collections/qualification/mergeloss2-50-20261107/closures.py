#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Build-closure probe for merge-loss phase-2 leaf 50: which jobs compile a leaf-50 C owner, and can they reach
src/java.base/share/native/libjava/m3_jni_util.h through the origin's source-relative include?

For every .github/workflows job that names one of the four leaf-50 C files next to a compiler call, report its
checkout (FULL or the sparse path list), its -I list, whether the header path is inside the checkout, and whether
the job pins the current git blob (or template bytes) of a leaf file it compiles. Reads the workflows with
`git show <rev>:<path>` (no checkout). CPU-only, deterministic; TSV on stdout.

usage: python -I closures.py <git-dir> <rev>
"""
from __future__ import annotations

import fnmatch
import re
import subprocess
import sys

# FLLDCIM: MRL - mergeloss2 leaf 50: the four C owners the origin e47eeda7c05 moved onto m3_jni_util.h.
LEAF = ("m3_index_mask_window.c", "m3_long_index_path_masks_jni.c", "m3_state_mask_histogram.c",
        "segmented_bit_lane.c")
HEADER = "src/java.base/share/native/libjava/m3_jni_util.h"
SRC = "m3/collections/src/main/native/collections/"
COMPILER = re.compile(r"(^|[\s;(])(gcc|cc|clang|\$CC|\"\$CC\"|\$\{CC\}|\$\{CC:-cc\})\s")
PIN = re.compile(r"git hash-object|\bcmp\b|sha256sum")
COLUMNS = ("workflow", "job", "compiles", "checkout", "include_flags", "header_in_checkout", "pins_leaf_bytes")


def git(repo: str, *args: str) -> str:
    return subprocess.run(["git", "-C", repo, *args], capture_output=True, text=True, encoding="utf-8",
                          errors="replace", check=True).stdout


def jobs(text: str):
    """Split a workflow into (job id, job text) by the two-space job keys under `jobs:`."""
    body = text.split("\njobs:\n", 1)[-1]
    parts = re.split(r"(?m)^  ([A-Za-z0-9_-]+):\s*$", body)
    return list(zip(parts[1::2], parts[2::2]))


def sparse(job: str):
    found = []
    for match in re.finditer(r"(?m)^(\s*)sparse-checkout:\s*\|\s*\n", job):
        indent = len(match.group(1))
        for line in job[match.end():].splitlines():
            if not line.strip() or len(line) - len(line.lstrip()) <= indent:
                break
            found.append(line.strip())
    return found


def reaches(paths) -> bool:
    if not paths:
        return True
    return any(HEADER == p.rstrip("/") or HEADER.startswith(p.rstrip("/*") + "/") or fnmatch.fnmatch(HEADER, p)
               for p in paths)


def main() -> int:
    repo, rev = sys.argv[1], sys.argv[2]
    sys.stdout.reconfigure(newline="\n")  # LF rows on Windows too
    blobs = {leaf: git(repo, "rev-parse", "%s:%s%s" % (rev, SRC, leaf)).strip() for leaf in LEAF}
    names = git(repo, "grep", "-l", "-e", LEAF[0], "-e", LEAF[1], "-e", LEAF[2], "-e", LEAF[3], rev, "--",
                ".github/workflows").split()
    print("\t".join(COLUMNS))
    for name in sorted(n.split(":", 1)[1] for n in names):
        for job_id, job in jobs(git(repo, "show", "%s:%s" % (rev, name))):
            joined = re.sub(r"\\\n\s*", " ", job)
            compiled = sorted({leaf for leaf in LEAF if leaf in joined}) if COMPILER.search(joined) else []
            if not compiled:
                continue
            paths = sparse(job)
            flags = sorted(set(re.findall(r"-I\s*\"?([^\"\s]+)", joined)))
            pinned = sorted({leaf for leaf in compiled if blobs[leaf] in joined} | {
                leaf for leaf in compiled for line in joined.splitlines()
                if PIN.search(line) and (leaf in line or "$source" in line)})
            print("\t".join((name.rsplit("/", 1)[-1], job_id, "+".join(compiled), ",".join(paths) or "FULL",
                             " ".join(flags), "y" if reaches(paths) else "n", "+".join(pinned) or "-")))
    return 0


if __name__ == "__main__":
    sys.exit(main())
