#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe OBJECT_SIGNAL_NATIVE_OWNER_EDITS v1: the owner-file edits of leaf object-signal-native-20261107 (m3/collections).

Two rules (the kernel itself is relocated by ../object-signal-20261107/relocate.py v2):
  N1 LIBRARY: the JNI library registry of qualification/full-java-jni-20261018/verify.py (LIBRARIES; "no second native
     owner") gains m3_object_signal_jni.c (owner M3ObjectSignalOps) in the existing library m3signalorder, next to
     m3_signal_order_jni.c (owner M3SignalOrderNative). The registry is the module's only native build description:
     verify.py refuses a C file under src/main/native/collections that no library admits, compiles each file with
     -include of its owner's javac -h header and links one shared object per library.
  N2 FAMILY_ROW: the object-signal row of make/m3/collections/JDK_FAMILY_MAP.tsv (written by ../object-signal-20261107/apply.py)
     no longer says the JNI kernel is held.

Derived from ../object-signal-20261107/apply.py v1 (anchored "sub" edits, every anchor exactly once, LF payloads
re-joined with the preimage's line ending). Each rule names the git blob sha of its preimage at the base commit; a
target must equal the preimage (the rule is applied) or the postimage (already applied, no-op); anything else is a
drifted preimage and the recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and
EDITS.tsv (file, before/after sha256, leaf, rule) is a fixed point. The FAMILY_ROW preimage is the postimage of
../object-signal-20261107/apply.py, so that recipe's --check is meaningful at its own base (ee47f1325def) only.

usage: python -I apply.py [--root DIR] [--check]
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
BASE_COMMIT = "3654bf4265b2f0b2ccb8e7e1ba2ed52bc971255b"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "rule")
LEAF = "object-signal-native-20261107"
# leaf, path, preimage blob at BASE_COMMIT, rule note, edits [(old, new)]
RULES = (
    (LEAF, "make/m3/collections/qualification/full-java-jni-20261018/verify.py", "a26fa7bb0eae4a45acba5e15d645e5ecc435f544",
     "N1 LIBRARY: m3_object_signal_jni.c (M3ObjectSignalOps) joins the existing library m3signalorder", [
         ("    'm3signalorder': [('m3_signal_order_jni.c', 'M3SignalOrderNative')],\n",
          "    'm3signalorder': [('m3_signal_order_jni.c', 'M3SignalOrderNative'),\n"
          "                      ('m3_object_signal_jni.c', 'M3ObjectSignalOps')],\n"),
     ]),
    (LEAF, "make/m3/collections/JDK_FAMILY_MAP.tsv", "6253d33f804fa102292ecfe779b5a8d81828266a",
     "N2 FAMILY_ROW: the object-signal row names the JNI kernel in libm3signalorder", [
         ("Object.equals remain authority; Java kernel only, the donor JNI kernel is a HELD row",
          "Object.equals remain authority; Java kernel by default, the donor JNI kernel in the module library"
          " m3signalorder for 256 words and more when that library is on java.library.path (opt-out"
          " m3.objectsignal.disableNative; leaf object-signal-native-20261107)"),
     ]),
)


class Refused(Exception):
    pass


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


def run(root: Path, check: bool) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for leaf, rel, blob, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += 1
        rows.append((rel, sha256(pre), sha256(post), leaf, note))
        print("EDIT %s %s %s" % (leaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("OBJECT_SIGNAL_NATIVE_OWNER_EDITS recipe=%s %s base=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
