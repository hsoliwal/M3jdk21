#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1: merge-loss audit leaf 74 (m3/eclipse/framework M3EclipseIndexedSnapshot).

Engine: m3/core/qualification/mergeloss-67-69-20261107/apply.py v1 (itself derived from
make/m3/collections/qualification/census-small-batch-20261106/apply.py v1): anchored "sub" edits, every anchor exactly once,
LF payloads re-joined with the preimage's line ending, a per-file leaf chain whose accepted states are the base (pinned
by git blob id, or ABSENT) and each chain prefix. Anything else is drift and the recipe refuses (exit 2) before any
write. Extension for ABSENT bases: a create edit (old None) takes its bytes from a pinned donor git blob, checks the
blob id, then applies the remaining anchored edits. Idempotent: a second run reports files_changed=0 and EDITS.tsv is
a fixed point. --check writes nothing and exits 1 when a run would change a file.

Leaf 74 (com.synexia merge-loss audit LEAVES.tsv, 2026-11-06): origin 33236c95de67 "Select Eclipse snapshot payloads
directly through M3 state-mask slots" (2026-10-21, canonical crate com.synexia#12639 m3-eclipse-structures-20261021),
PR #1631, dropped by the retain merge 776933fb645a (RETAIN_MERGE_WHOLE_PR: "promotion to live code is a reviewed
follow-up"). The origin preimage of M3EclipseIndexedSnapshot equals master's file after the JDK naming package rename
(fe35ea98841); the retained copies under m3/qualification/master-landing-20261028/retained-variants/pr1631 are master
plus the origin hunk (blob 09b466e5b494) and the origin's independent selection test (blob 0df8bb3f8420, the origin
blob c2c3de78faf renamed). The recipe replays the hunk and creates the test from the retained blob.

usage: python -I apply.py [--root DIR] [--check] [--leaf 74]
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
BASE_COMMIT = "76489d93964"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin", "dropping_merge", "donor_blobs", "rule")
PKG = "m3/eclipse/framework/src/%s/java/jdk/m3/eclipse/framework/"
SNAPSHOT = PKG % "main" + "M3EclipseIndexedSnapshot.java"
SELECTION_TEST = PKG % "test" + "M3EclipseSelectionTest.java"
ABSENT = "ABSENT"
ORIGIN = "33236c95de676178e12337b24eeeecdbfe181a5c"
RETAIN = "776933fb645ae1c3de04a074e670ff81f363d86a"


class Refused(Exception):
    pass


def lines(*text):
    return "\n".join(text) + "\n"


SELECTED_OLD = lines(
    "    List<T> selected(int requiredMask, int excludedMask) {",
    "        long[] source = handles.toArray();",
    "        long[] result = states.snapshotHandles(source, 0, source.length,",
    "                requiredMask, excludedMask);",
    "        List<T> selected = M3Collections.arrayList();",
    "        for (long handle : result) {",
    "            selected.add(payloads.get(M3SlotAllocator28.slot(handle)));",
    "        }",
    "        return List.copyOf(selected);",
    "    }")
SELECTED_NEW = lines(
    "    //FLLDCIM: MRL - merge-loss leaf 74 (origin 33236c95de67, retain 776933fb645a): state-mask slot selection",
    "    List<T> selected(int requiredMask, int excludedMask) {",
    "        // Snapshot slots are dense, append-only ordinals; no handle materialization is needed.",
    "        int slot = states.nextMatchingSlot(0, requiredMask, excludedMask);",
    "        if (slot < 0) return List.of();",
    "        if ((requiredMask | excludedMask) == 0) return all();",
    "        List<T> selected = M3Collections.arrayList();",
    "        do {",
    "            selected.add(payloads.get(slot));",
    "            slot = states.nextMatchingSlot(slot + 1, requiredMask, excludedMask);",
    "        } while (slot >= 0);",
    "        return List.copyOf(selected);",
    "    }")
TEST_HEADER_OLD = lines(
    "// SPDX-License-Identifier: Apache-2.0",
    "package jdk.m3.eclipse.framework;")
TEST_HEADER_NEW = lines(
    "// SPDX-License-Identifier: Apache-2.0",
    "//FLLDCIM: MRL - merge-loss leaf 74 (origin 33236c95de67, retain 776933fb645a): independent selection oracle",
    "package jdk.m3.eclipse.framework;")
SELECTION_TEST_BLOB = "0df8bb3f84205488ce24a09b3fdf63811d16bd40"

# path, base blob (master) or ABSENT, chain of (leaf, origin, dropping merge, donor blobs, rule note, edits)
CHAINS = (
    (SNAPSHOT, "525ee05d02a7733dc5fa83efd36a18da4208d254", (
        ("74", ORIGIN, RETAIN, "post=c6576562d84 pre=9397d649d87 retained=09b466e5b494",
         "RESTORE the origin hunk: selected() walks M3ObjectStateSlots28.nextMatchingSlot over the dense append-only"
         " slots (slot = payload ordinal) instead of copying every handle; empty and unfiltered results reuse List.of()"
         " and all()", [
             (SELECTED_OLD, SELECTED_NEW),
         ]),
    )),
    (SELECTION_TEST, ABSENT, (
        ("74", ORIGIN, RETAIN, "post=c2c3de78faf retained=0df8bb3f8420",
         "RESTORE the origin's independent scalar-mask oracle (all 65,536 byte-mask pairs, segment boundaries,"
         " detached immutable results, mask validation on empty and overlapping selections); created from the"
         " retained blob", [
             (None, SELECTION_TEST_BLOB),
             (TEST_HEADER_OLD, TEST_HEADER_NEW),
         ]),
    )),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


def git_blob(root: Path, blob: str) -> bytes:
    try:
        data = subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("blob %s is not in this repository (%s)" % (blob, failure)) from failure
    if blob_sha(data) != blob:
        raise Refused("blob %s read back as %s" % (blob, blob_sha(data)))
    return data


def step(root: Path, pre: bytes, edits, path: str) -> bytes:
    for old, new in edits:
        if old is None:  # create: the file must be absent before this step
            if pre:
                raise Refused("create step for %s found existing bytes" % path)
            pre = git_blob(root, new)
            continue
        eol = eol_of(pre)
        text = pre.decode("utf-8")
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        pre = text.replace(old, new).encode("utf-8")
    return pre


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rel, blob, chain in CHAINS:
        target = root / rel
        if blob != ABSENT and not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes() if target.exists() else b""
        if blob == ABSENT:
            base = b""
        else:
            base = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        images = [base]
        for link in chain:
            images.append(step(root, images[-1], link[5], rel))
        if on_disk not in images:
            raise Refused("drifted preimage %s: on disk %s is no image of its chain (base %s)"
                          % (rel, blob_sha(on_disk) if target.exists() else ABSENT, blob))
        at = images.index(on_disk)
        goal = len(chain) if leaf is None else next(
            (i + 1 for i, link in enumerate(chain) if link[0] == leaf), at)
        goal = max(goal, at)  # never undo a later leaf
        for i, (rleaf, origin, merge, donors, note, _) in enumerate(chain):
            state = "APPLIED" if i < at else ("CHANGED" if i < goal else "PENDING")
            applied += state != "PENDING"
            before = ABSENT if i == 0 and blob == ABSENT else sha256(images[i])
            rows.append((rel, before, sha256(images[i + 1]), rleaf, origin[:12], merge[:12], donors, note))
            print("EDIT %s %s %s" % (rleaf, state, rel))
        if goal > at:
            changed += 1
            writes.append((target, images[goal]))
    if not check:  # every chain verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS_RESTORE recipe=%s %s base=%s leaves=%d applied=%d files_changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: five up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({link[0] for chain in CHAINS for link in chain[2]}),
                        help="apply the chain up to this leaf only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
