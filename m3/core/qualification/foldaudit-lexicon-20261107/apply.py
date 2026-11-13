#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FOLDAUDIT_RESTORE v1: fold-close audit of the m3/core lexicon family (lane com-m3-0e, jdk.m3.text).

Engine: m3/core/qualification/mergeloss-2472-20261107/apply.py v1 (from mergeloss-67-69-20261107, itself from
make/m3/collections/qualification/census-small-batch-20261106): anchored "sub" edits, every anchor exactly once, LF payloads
re-joined with the preimage's line ending, a per-file leaf chain whose accepted states are the base (pinned by git blob
id) and each chain prefix. Anything else is drift and the recipe refuses (exit 2) before any write. Idempotent: a second
run reports files_changed=0 and EDITS.tsv is a fixed point. --check writes nothing and exits 1 when a run would change
a file.

The audit (S:/m3-scratch/eb-conv/foldaudit, LEAVES.tsv/HUNK_VERDICTS.tsv) found 15 leaves of closed-unmerged PRs
#546-#677 and #2424 whose hunks never reached master. DECISION.tsv decides each; verify.py checks the record-only
decisions. One leaf is restored here:

Leaf L0064 (m3/core/src/jdk/m3/text/M3PrecomputePayload.java): origin d58ff13e954 "perf(m3): precompile the immutable
lexicon payload field-name pattern" (2026-11-04, PR #2447, merged into the #2424 branch head 95440f4a6944, not into
master; Synexia recipe crate m3-payload-field-pattern-20261104, com.synexia#16308: before = blob 83f625d94819, after =
blob c6325cf501f9). #2424 was closed 2026-11-06T05:46Z as "fully contained in master (b65fef768d34, landed through
superset #2489)"; the containing merge b9445775f4a3 "merge(superset): retain #2424 head" changed no file against its
first parent, and the JDK-naming fold c9a1d41baaf kept master's String.matches line. Master's file is the #2447
preimage after the JDK naming rename (only the package and class names differ), so the two postimage hunks drop in:
one private static final Pattern and the matcher call in Parser.object(); public API, regex, flags and error messages
are unchanged.

usage: python -I apply.py [--root DIR] [--check] [--leaf L0064]
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
BASE_COMMIT = "db4bd0b1b5bdfad5319b7f9416585a1a8723231d"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin", "dropping_merge", "donor_blobs", "rule")
PAYLOAD = "m3/core/src/jdk/m3/text/M3PrecomputePayload.java"


class Refused(Exception):
    pass


def lines(*text):
    return "\n".join(text) + "\n"


FIELD_TYPES_OLD = lines(
    "    private static final Map<String, Expected> FIELD_TYPES = fieldTypes();")
FIELD_TYPES_NEW = lines(
    "    //FLLDCIM: MRL - fold-close audit L0064 (#2447 d58ff13e954 in #2424 head 95440f4a6944; retain b9445775f4a3"
    " kept master): one compiled field-name pattern, no per-field String.matches compile",
    "    private static final java.util.regex.Pattern FIELD_NAME_PATTERN =",
    "            java.util.regex.Pattern.compile(\"[a-z][a-z0-9_]*\");",
    "    private static final Map<String, Expected> FIELD_TYPES = fieldTypes();")
MATCHES_OLD = lines(
    "                if (!name.matches(\"[a-z][a-z0-9_]*\")) throw error(\"invalid payload field name\");")
MATCHES_NEW = lines(
    "                if (!FIELD_NAME_PATTERN.matcher(name).matches()) throw error(\"invalid payload field name\");")

# path, base blob (master db4bd0b1b5bd), chain of (leaf, origin, dropping merge, donor blobs, rule note, edits)
CHAINS = (
    (PAYLOAD, "9041b429eb141526ed4008867e7e33850d8ea476", (
        ("L0064", "d58ff13e95440190c4a78e69cc3f26b8444dea95", "b9445775f4a3",
         "post=c6325cf501f9 pre=83f625d94819 (M3PrecomputePayload.java at d58ff13e954, #2447)",
         "RESTORE FIELD_NAME_PATTERN: the #2447 postimage compiles the field-name expression once and validates each"
         " name with a Matcher; same regex, flags, message and API as master's String.matches line", [
             (FIELD_TYPES_OLD, FIELD_TYPES_NEW),
             (MATCHES_OLD, MATCHES_NEW),
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
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("base blob %s is not in this repository (%s)" % (blob, failure)) from failure


def step(pre: bytes, edits, path: str) -> bytes:
    eol = eol_of(pre)
    text = pre.decode("utf-8")
    for old, new in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rel, blob, chain in CHAINS:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        images = [on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)]
        for link in chain:
            images.append(step(images[-1], link[5], rel))
        if on_disk not in images:
            raise Refused("drifted preimage %s: on disk %s is no image of its chain (base blob %s)"
                          % (rel, blob_sha(on_disk), blob))
        at = images.index(on_disk)
        goal = len(chain) if leaf is None else next(
            (i + 1 for i, link in enumerate(chain) if link[0] == leaf), at)
        goal = max(goal, at)  # never undo a later leaf
        for i, (rleaf, origin, merge, donors, note, _) in enumerate(chain):
            state = "APPLIED" if i < at else ("CHANGED" if i < goal else "PENDING")
            applied += state != "PENDING"
            rows.append((rel, sha256(images[i]), sha256(images[i + 1]), rleaf, origin[:12], merge[:12], donors, note))
            print("EDIT %s %s %s" % (rleaf, state, rel))
        if goal > at:
            changed += 1
            writes.append((target, images[goal]))
    if not check:  # every chain verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("FOLDAUDIT_RESTORE recipe=%s %s base=%s leaves=%d applied=%d files_changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
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
