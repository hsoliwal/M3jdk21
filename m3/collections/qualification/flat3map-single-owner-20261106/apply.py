#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe FLAT3MAP_SINGLE_OWNER v1 (census leaf L075): one Flat3 map implementation in m3/collections.

Owner decision (the user, relayed by the coordinating session 63, 2026-11-06): the M3-native jdk.m3.collections.M3Flat3Map
of the core module is the single implementation; jdk.m3.collections.apache.M3Flat3Map (implementations/
apache-flat3-map) stays only as a thin Commons-compatible facade that delegates to it, proven by a Commons
differential. README.md holds the decision, the hunk verdicts and the facade design; GATES.tsv the proof.

Rule kinds. VERIFY: the target must equal its pinned preimage and stays unchanged. EDIT: anchored substitutions, every
anchor exactly once. TEMPLATE: the body is replaced wholesale by a file under templates/. ADD: a new file (preimage
ABSENT) from templates/. TEMPLATE and ADD pin their postimage sha256 as well, so a drifted template is refused.
Crate identity: a rule may name the sha256 of the receiving crate's postimage (m3jdk21-flat3map-20261029, pinned in
README.md); the pinned preimage must reproduce it once the module package is spelled the crate's way again (the
2026-10-31 JDK naming rename is the only difference), so --check proves the receive mechanically.

Hash-pinned: a target must equal the preimage (the rule applies) or the postimage (already applied, no-op); anything
else is drift and the recipe refuses (exit 2) without writing. Once applied, the preimage is read back with git
cat-file, so --check recomputes every postimage from the pinned bytes. Payloads are written with LF and re-joined with
the preimage's line ending. Idempotent: a second run reports changed=0 and EDITS.tsv (file, kind, before blob, before
sha256, after sha256, crate sha256, rule) is a fixed point; the per-run state (CHANGED, ADDED, UNCHANGED) is printed.

usage: python -I apply.py [--root DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_DIR_REL = "m3/collections/qualification/flat3map-single-owner-20261106"
BASE_COMMIT = "e65f0f664fa334b4ad60b1ebcb2f1436380aace1"
OWNER = "jdk.m3.collections.M3Flat3Map"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "kind", "before_blob", "before_sha256", "after_sha256", "crate_sha256", "rule")
ABSENT = "ABSENT"
MODULE_PACKAGE = "jdk.m3.collections"
# the crate's package, spelled in parts: the naming recipe rewrites .py files and would turn a literal into the module's
CRATE_PACKAGE = ".".join(("com", "m3", "collections"))
CORE = "m3/collections/src/main/java/jdk/m3/collections/"
CORE_TEST = "m3/collections/src/test/java/jdk/m3/collections/"


class Refused(Exception):
    pass


# kind, path, preimage blob (ABSENT for a new file), crate sha256 or "-", note, payload, postimage sha256 or "-"
RULES = (
    # --- commit 1: the core and its crate tests already carry the crate postimage (receive abc8b3aea44, renamed by
    # fe35ea98841); the 11 hunks of the historical prepacked candidate are superseded (HUNKS.tsv, NEC.tsv) ----------
    ("VERIFY", CORE + "M3Flat3Map.java", "f2de0a79c1cf4c2413b8b61b2f3538812068eddf",
     "28fb1d0ac39c3e1836750694c729349d01dd4477d4d3d711c82242055263710e",
     "crate postimage (packed two-bit states), received by abc8b3aea44 and renamed by fe35ea98841; unchanged",
     None, "-"),
    ("VERIFY", CORE + "M3SlotArrays.java", "ab4663c2c76108dad0de96252ec294006e7f629c",
     "59da94e994bbbf3decc17fb1c217e42b126496361f28e8a4dcbedf1baedf54d5",
     "crate's unchanged hash/capacity kernel", None, "-"),
    ("VERIFY", CORE_TEST + "M3Flat3MapContract.java", "b5f0740db5ecc8ff0fc4ff17e69dfabf262e6506",
     "7378bc357e20e8e3b5fb53167248ddc9748c9c004d2eaa50c376ac36bb15e2a4",
     "crate oracle: 1,951,504 assertions", None, "-"),
    ("VERIFY", CORE_TEST + "M3Flat3MapTest.java", "0e67769c40ffda50b31178211cf6b6b9fd652122",
     "04f7b82e6a18c9870d87ab3370e5f2bd77d7191d36ff882d2b2ce9c79e12394a",
     "crate Jupiter wrapper of the oracle", None, "-"),
    ("VERIFY", "m3/collections/interop/src/test/java/jdk/m3/collections/interop/M3Flat3MapProviderTest.java",
     "254d85294724b1cdcaa26a2c526da0fa17d623be",
     "508f91d8852c460c27cd4b81004496c58af08f5bab14432228fff2ebe33970ba",
     "crate provider test (Commons and account providers)", None, "-"),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> bytes:
    return b"\r\n" if b"\r\n" in data else b"\n"


def with_eol(text: str, eol: bytes) -> str:
    return text.replace("\n", eol.decode())


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


def template(name: str, eol: bytes) -> bytes:
    lines = (RECIPE_DIR / "templates" / name).read_text(encoding="utf-8").splitlines()
    return (eol.decode().join(lines) + eol.decode()).encode("utf-8")


def postimage(kind: str, pre: bytes | None, payload, path: str) -> bytes:
    """The bytes a rule produces from its pinned preimage."""
    if kind == "VERIFY":
        return pre
    if kind == "ADD":
        return template(payload, b"\n")
    eol = eol_of(pre)
    if kind == "TEMPLATE":
        return template(payload, eol)
    text = pre.decode("utf-8")
    for edit in payload:
        text = apply_edit(text, edit, eol, path)
    return text.encode("utf-8")


def apply_edit(text: str, edit: tuple, eol: bytes, path: str) -> str:
    mode, old, new = edit
    if mode == "sub":
        old, new = with_eol(old, eol), with_eol(new, eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        return text.replace(old, new)
    text, count = re.subn(old, new, text, flags=re.MULTILINE)
    if count != 1:
        raise Refused("pattern matched %d times (need 1) in %s: %r" % (count, path, old[:60]))
    return text


def preimage(root: Path, rel: str, blob: str, on_disk: bytes | None) -> bytes | None:
    if blob == ABSENT:
        return None
    if on_disk is not None and blob_sha(on_disk) == blob:
        return on_disk
    return git_blob(root, blob)


def crate_check(rel: str, pre: bytes | None, crate: str) -> None:
    """The pinned preimage spelled with the crate's package must be the crate postimage, byte for byte."""
    if crate == "-":
        return
    spelled = pre.replace(MODULE_PACKAGE.encode(), CRATE_PACKAGE.encode())
    if sha256(spelled) != crate:
        raise Refused("crate identity: %s with the crate package is %s, crate postimage %s"
                      % (rel, sha256(spelled), crate))


def run_rule(root: Path, rule: tuple, check: bool) -> tuple:
    kind, rel, blob, crate, note, payload, pinned_post = rule
    target = root / rel
    on_disk = target.read_bytes() if target.exists() else None
    pre = preimage(root, rel, blob, on_disk)
    crate_check(rel, pre, crate)
    post = postimage(kind, pre, payload, rel)
    if pinned_post != "-" and sha256(post) != pinned_post:
        raise Refused("drifted template for %s: %s, pinned %s" % (rel, sha256(post), pinned_post))
    if on_disk is not None and on_disk != post and (pre is None or blob_sha(on_disk) != blob):
        raise Refused("drifted target %s: on disk blob %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
    if on_disk is None and pre is not None:
        raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
    state = "UNCHANGED"
    if on_disk != post:
        state = "ADDED" if on_disk is None else "CHANGED"
        if not check:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(post)
    print("%s %s %s" % (kind, state, rel))
    row = (rel, kind, blob, ABSENT if pre is None else sha256(pre), sha256(post), crate, note)
    return state, row


def run(root: Path, check: bool) -> int:
    rows, changed = [], 0
    for rule in RULES:
        state, row = run_rule(root, rule, check)
        changed += state != "UNCHANGED"
        rows.append(row)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    edits_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if edits_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    counts = {kind: sum(rule[0] == kind for rule in RULES) for kind in ("VERIFY", "EDIT", "TEMPLATE", "ADD")}
    print("FLAT3MAP_SINGLE_OWNER recipe=%s %s base=%s owner=%s rules=%d changed=%d verify=%d edits=%d templates=%d"
          " adds=%d edits_tsv=%s" % (RECIPE, VERSION, BASE_COMMIT[:12], OWNER, len(rows), changed, counts["VERIFY"],
                                     counts["EDIT"], counts["TEMPLATE"], counts["ADD"],
                                     "CHANGED" if edits_state else "UNCHANGED"))
    return 1 if check and (changed or edits_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
