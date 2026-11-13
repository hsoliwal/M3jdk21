#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Recipe JBCOLL_MASK_COMPACTOR_LINEAGE v1: census leaf L003 (jdk/internal/mindex/M3CollectionMaskCompactor.java).

Classification only; never writes product files. The census row (crate m3jdk21-java-base-arraylist-inline-20261025,
PARTIAL 48/52, no preimage, LOWCONF_NOPRE) carries a postimage with no preimage. This recipe places that postimage
among the owner's blobs:
  - the four retained variants of JDK_TREE_3WAY_20261106.tsv (pr1754, pr1802, pr1873, pr1908; base=ff0b94fc2a3b);
  - the owner's own history on master (git log -- <owner>);
  - the live owner.
For every pinned blob it records the sha256, its line delta to the postimage and to the live owner, and the master
commits whose owner blob it equals (with ancestry to the base commit). It then runs `git merge-file -p --diff3
ours=live base=<base> theirs=<variant>` for every candidate pair and records CLEAN or CONFLICT=<n>, plus whether the
merge result equals ours or theirs.

Output: LINEAGE.tsv and MERGES.tsv in this directory, regenerated and compared (fixed point). --check writes nothing
and exits 1 when either table would change. Any blob that is missing, or whose content differs from its pin, is
refused (exit 2).

usage: python -I lineage.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import difflib
import hashlib
import os
import subprocess
import sys
import tempfile
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "fa8215c8a2df98ccaf717c584688171954a88956"
OWNER = "src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java"
# label, git blob, sha256 prefix (16), provenance
BLOBS = (
    ("postimage", "51ea3a72bb191afc250a58be964363bd5680467c", "1c3f3d2cf129b0cc",
     "crate m3jdk21-java-base-arraylist-inline-20261025 M3CollectionMaskCompactor.java.txt (manifest: path + one sha256,"
     " a pinned dependency of the ArrayList postimage, not an edit)"),
    ("pr1754", "f85e7359010bd25e0a909a145e283007cff25d7c", "221bc2531f34ec8f",
     "m3/qualification/master-landing-20261028/retained-variants/pr1754/" + OWNER),
    ("pr1802", "f85e7359010bd25e0a909a145e283007cff25d7c", "221bc2531f34ec8f",
     "m3/qualification/master-landing-20261028/retained-variants/pr1802/" + OWNER),
    ("pr1873", "e6ca59d8ae7fba700765e670dd5ec7d8dd84bc8d", "812bbae89c04a8b1",
     "m3/qualification/master-landing-20261028/retained-variants/pr1873/" + OWNER),
    ("pr1908", "3f88d207e1f6d68da96477a0d184cf8d1caaa6d7", "17a2c1b601d5552b",
     "m3/qualification/master-landing-20261028/retained-variants/pr1908/" + OWNER),
    ("live", "9f8b4c965b527ed097243bb5bc4600953f85f51d", "9e9c11e3a04cbc29",
     "master " + BASE_COMMIT[:12] + ":" + OWNER),
)
# ours, base, theirs (labels): the merges that would restore or apply a candidate onto live
MERGES = (
    ("live", "postimage", "postimage"),
    ("live", "postimage", "pr1754"),
    ("live", "postimage", "pr1873"),
    ("live", "postimage", "pr1908"),
    ("live", "pr1873", "pr1754"),
    ("live", "pr1873", "pr1908"),
)


class Refused(Exception):
    pass


def git(root: Path, *args: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), *args], check=True, capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("git %s failed (%s)" % (" ".join(args), failure)) from failure


def changed_lines(a: bytes, b: bytes) -> int:
    diff = difflib.unified_diff(a.decode("utf-8").splitlines(), b.decode("utf-8").splitlines(), lineterm="", n=0)
    return sum(1 for line in diff if line[:1] in "+-" and not line.startswith(("+++", "---")))


def history(root: Path) -> list:
    """(commit, date, blob) of every master commit that changed the owner, newest first."""
    log = git(root, "log", "--format=%H %ad", "--date=short", BASE_COMMIT, "--", OWNER).decode().split("\n")
    rows = []
    for line in filter(None, log):
        commit, date = line.split()
        blob = git(root, "rev-parse", commit + ":" + OWNER).decode().strip()
        rows.append((commit, date, blob))
    return rows


def run(root: Path, check: bool) -> int:
    data = {}
    for label, blob, sha, _note in BLOBS:
        content = git(root, "cat-file", "blob", blob)
        if hashlib.sha256(content).hexdigest()[:16] != sha:
            raise Refused("%s blob %s has sha256 %s, pinned %s" % (label, blob, hashlib.sha256(content).hexdigest(), sha))
        data[label] = content
    if git(root, "rev-parse", BASE_COMMIT + ":" + OWNER).decode().strip() != BLOBS[-1][1]:
        raise Refused("live owner blob at %s is not the pinned blob" % BASE_COMMIT[:12])
    commits = history(root)
    lineage = ["label\tblob\tsha256_16\tlines_vs_postimage\tlines_vs_live\tmaster_commits_with_this_blob\tprovenance"]
    for label, blob, sha, note in BLOBS:
        same = ["%s(%s)" % (c[:12], d) for c, d, b in commits if b == blob]
        lineage.append("\t".join((label, blob, sha, str(changed_lines(data["postimage"], data[label])),
                                  str(changed_lines(data["live"], data[label])), ",".join(same) or "-", note)))
    lineage.append("history\t-\t-\t-\t-\t%s\tgit log %s -- %s (newest first; every commit is an ancestor of the base)" % (
        ",".join("%s(%s)=%s" % (c[:12], d, b[:12]) for c, d, b in commits), BASE_COMMIT[:12], OWNER))
    merges = ["ours\tbase\ttheirs\tresult\tequals"]
    with tempfile.TemporaryDirectory(dir=os.environ.get("TMP")) as tmp:
        for ours, base, theirs in MERGES:
            paths = []
            for name, label in (("o", ours), ("b", base), ("t", theirs)):
                path = Path(tmp) / name
                path.write_bytes(data[label])
                paths.append(str(path))
            result = subprocess.run(["git", "merge-file", "-p", "--diff3", *paths], capture_output=True)
            kind = "CLEAN" if result.returncode == 0 else "CONFLICT=%d" % result.returncode
            equals = "ours" if result.stdout == data[ours] else "theirs" if result.stdout == data[theirs] else "-"
            merges.append("\t".join((ours, base, theirs, kind, equals)))
    changed = 0
    for name, rows in (("LINEAGE.tsv", lineage), ("MERGES.tsv", merges)):
        text = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n# generated by lineage.py (JBCOLL_MASK_COMPACTOR_LINEAGE v1)"
                " over master %s\n" % BASE_COMMIT[:12] + "\n".join(rows) + "\n").encode("utf-8")
        path = RECIPE_DIR / name
        if not path.exists() or path.read_bytes() != text:
            changed += 1
            if not check:
                path.write_bytes(text)
    for row in merges[1:]:
        print("MERGE " + row.replace("\t", " "))
    print("LINEAGE recipe=lineage.py v1 base=%s blobs=%d history=%d merges=%d clean=%d tables=%s" % (
        BASE_COMMIT[:12], len(BLOBS), len(commits), len(MERGES), sum("\tCLEAN\t" in m for m in merges),
        "CHANGED" if changed else "UNCHANGED"))
    return 1 if check and changed else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a table would change")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
