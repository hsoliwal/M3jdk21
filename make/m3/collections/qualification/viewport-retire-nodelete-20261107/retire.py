#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe VIEWPORT_REGIONS_RETIRE v2 (no delete by default): status rows and caller edit now, file removal only on --delete.

Split of #2673 (VIEWPORT_REGIONS_RETIRE v1), which removed two tracked files; deleting a tracked file waits for the
user's explicit approval, so v2 makes no deletion unless --delete is given (default off). Derived from
make/m3/collections/qualification/mergeloss-s1-20261106/apply.py v2 (anchored "sub" edits, every anchor exactly once,
hash-pinned preimages, refusal on drift, idempotent, --check fixed point). Rule kinds:
  EDIT    the file must equal its pinned preimage blob (applied) or the postimage (no-op);
  RETIRE  the file must hold its pinned blob or be absent; without --delete the rule only reports PENDING_DELETE and
          keeps the file (not a change: --check stays changed=0); with --delete it removes the file. A retire is never
          silent: each RETIRE rule has a RETIRED_PENDING_DELETE status row (DECISION.tsv; for the held file also a row
          appended to the HELD.tsv that held it, by an EDIT rule of this recipe).
Every preimage is verified before the first write; a refusal (exit 2) leaves the tree untouched. EDITS.tsv (file,
before/after sha256, leaf, decision, cited commits, rule; after "-" = removed once --delete runs) is written by the
recipe and is a fixed point on a second run.

Leaves (DECISION.tsv):
  R1 m3/collections/src/test/native/m3_mask_viewport_regions.c (landed by 2f9a1a2a9d0e, #1497) RETIRED_PENDING_DELETE:
     retargeted to M3_VIEWPORT_CHUNK it compiles with gcc -Werror but its allocation/read/write-count assertions
     describe the d0cec9717801 project() that 7fb4de24b1e8 replaced (single-commit publication through O(selected)
     scratch); verify_mask_viewport.py, its only caller, drops the region steps now.
  R2 #2547's held M3MaskViewportNative.java (pr1766) RETIRED_PENDING_DELETE citing mergeloss-s3-20261106 (#2656): a
     second projectFramePage with master's exact descriptor; a status row is appended to #2547's HELD.tsv now.

usage: python -I retire.py [--root DIR] [--check] [--leaf R1|R2] [--delete]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "retire.py"
VERSION = "v2"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "8ad25365b6a"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "decision", "cited_commits", "rule")
REGIONS = "m3/collections/src/test/native/m3_mask_viewport_regions.c"
VERIFY = "make/m3/collections/verify_mask_viewport.py"
TWINS = "make/m3/qualification/retained-twins-core-lanes-20261106/"
HELD_FILE = TWINS + "held/m3/collections/src/main/java/jdk/m3/collections/M3MaskViewportNative.java"
R1_CITED = "7fb4de24b1e8;d0cec9717801;2f9a1a2a9d0e (#1497)"
R2_CITED = "mergeloss-s3-20261106 (#2656);487ed3c5b7aa (#1766);62a9d54fa777;facc2e13d0c6"
ANCHOR = ("# FLLDCIM: MRL - viewport-retire-nodelete-20261107 R1: region steps removed; src/test/native/"
          "m3_mask_viewport_regions.c is RETIRED_PENDING_DELETE (its counts describe d0cec9717801's project(), replaced"
          " by 7fb4de24b1e8)")


class Refused(Exception):
    pass


VERIFY_EDITS = [
    ("    native = root / 'src/main/c/m3maskviewport.c'\n"
     "    regions = root / 'src/test/native/m3_mask_viewport_regions.c'\n",
     "    native = root / 'src/main/c/m3maskviewport.c'\n"
     "    " + ANCHOR + "\n"),
    ("    run('regions-compile', [args.cc, *flags, regions, '-o', out / 'regions'])\n"
     "    region_output = run('regions', [out / 'regions'])\n"
     "    if 'PASS viewport JNI regions:' not in region_output:\n"
     "        raise SystemExit('missing region completion marker')\n",
     ""),
    ("for p in [*java, *tests, native, regions]},", "for p in [*java, *tests, native]},"),
    ("    print('PASS mandatory JNI, bounded regions, Java differential, and UBSan')\n",
     "    print('PASS mandatory JNI, Java differential, and UBSan')\n"),
]

HELD_ROW = ("m3/collections/src/main/java/jdk/m3/collections/M3MaskViewportNative.java\tpr1766\tRETIRED_PENDING_DELETE\t"
            "909dddcbe68f6ec47755062ca76d8b960fe0c2aeb7ddac571f596e00aac8610d\t"
            "retire decided 2026-11-07 by make/m3/collections/qualification/viewport-retire-nodelete-20261107 (R2), citing"
            " mergeloss-s3-20261106 (#2656: cases 10 and 14 SUPERSEDED by 62a9d54fa777/facc2e13d0c6 and 7fb4de24b1e8)."
            " Run evidence (#2656 proof.sh): javac of the held copy with master's kernel 'method projectFramePage(...) is"
            " already defined'; projectFramePage declarations held 2, master 1; master descriptor ([I[IIIIIII[II[II[II)I"
            " = the held one; master's single declaration is the one method set. The held/ copy is kept until the user"
            " approves the deletion (#2673 held; retire.py --delete)\n")

HELD_EDITS = [
    ("not resolved here); master's file compiles with the same command\n",  # last row at the pinned preimage
     "not resolved here); master's file compiles with the same command\n" + HELD_ROW),
]

R1_NOTE = ("RETIRE (pending: runs only with --delete): assertions on allocations/reads/writes describe d0cec9717801's streaming 1024-element regions;"
           " 7fb4de24b1e8's project() publishes once from O(selected) scratch (probe on master: 4097-row disjoint case"
           " allocations=1 reads=99 writes=1 vs asserted 0/20/5; overlap case allocations=1 vs asserted 0)")
R1V_NOTE = "EDIT: drop the retired region test's compile/run steps, its receipt source and its marker from the only caller"
R2_NOTE = ("RETIRE (pending: runs only with --delete): held twin of a declaration master holds with the same descriptor;"
           " status row appended to #2547's HELD.tsv")
R2H_NOTE = "EDIT: append the RETIRED_PENDING_DELETE status row for the held path (hold.py's 5 columns)"
PENDING = "RETIRED_PENDING_DELETE"

# leaf, kind, path, pinned blob, decision, cited, note, edits
RULES = (
    ("R1", "RETIRE", REGIONS, "22f8eaf891304f2fd35b6198003c5f57ab73e444", PENDING, R1_CITED, R1_NOTE, None),
    ("R1", "EDIT", VERIFY, "38453ef5012fa07bf0942bcb956b76414d15552d", PENDING, R1_CITED, R1V_NOTE, VERIFY_EDITS),
    ("R2", "RETIRE", HELD_FILE, "5c2a523e548466f6ec69d5a198be02cca9103c6c", PENDING, R2_CITED, R2_NOTE, None),
    ("R2", "EDIT", TWINS + "HELD.tsv", "9a199b5cbae098da30446bb585d2eea47499b0c7", PENDING, R2_CITED, R2H_NOTE,
     HELD_EDITS),
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
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(pre: bytes, edits, path: str) -> bytes:
    eol = eol_of(pre)
    text = pre.decode("utf-8")
    for old, new in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


def plan_edit(root: Path, rel: str, blob: str, edits):
    target = root / rel
    if not target.exists():
        raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
    on_disk = target.read_bytes()
    pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
    post = postimage(pre, edits, rel)
    if blob_sha(on_disk) != blob and on_disk != post:
        raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
    return sha256(pre), sha256(post), (None if on_disk == post else (target, post))


def plan_retire(root: Path, rel: str, blob: str):
    target = root / rel
    if not target.exists():
        pre = git_blob(root, blob)
        return sha256(pre), "-", None
    data = target.read_bytes()
    if blob_sha(data) != blob:
        raise Refused("drifted retire target %s: on disk %s, pinned blob %s" % (rel, blob_sha(data), blob))
    return sha256(data), "-", (target, None)


def run(root: Path, check: bool, leaf: str | None, delete: bool = False) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, kind, rel, blob, decision, cited, note, edits in RULES:
        before, after, work = plan_edit(root, rel, blob, edits) if kind == "EDIT" else plan_retire(root, rel, blob)
        if kind == "RETIRE" and work is not None and not delete:
            work = None  # no deletion without the explicit --delete flag: the file stays, its status row records it
            state = "PENDING_DELETE"
        else:
            state = "UNCHANGED" if work is None else "PENDING" if leaf not in (None, rleaf) else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append(work)
        applied += state != "PENDING"
        rows.append((rel, before, after, rleaf, decision, cited, kind + " " + note))
        print("%s %s %s %s" % (kind, rleaf, state, rel))
    if not check:  # every preimage verified before the first write
        for target, data in writes:
            if data is None:
                target.unlink()
            else:
                target.write_bytes(data)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("VIEWPORT_REGIONS_RETIRE recipe=%s %s base=%s delete=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, "on" if delete else "off", len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=sorted({rule[0] for rule in RULES}), help="apply one leaf's rules only")
    parser.add_argument("--delete", action="store_true",
                        help="also remove the RETIRE targets (default off: needs the user's explicit approval)")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf, args.delete)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
