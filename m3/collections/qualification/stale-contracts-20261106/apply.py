#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe STALE_CONTRACTS v1: bring two stale m3/collections test contracts back to their owners and run them.

Derived from m3/collections/qualification/census2-c2-20261106/apply.py v1 (#2609): anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending. An EDIT rule names the git blob sha of its preimage
at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already applied, no-op);
anything else is a drifted preimage and the recipe refuses (exit 2) without writing. A NEW rule creates a test file that
must be absent or already equal to its postimage. Idempotent: a second run reports changed=0 and EDITS.tsv (file,
before/after sha256, leaf, decision, cited commits, rule) is a fixed point. --leaf applies one leaf's rules only;
--check writes nothing and exits 1 when a run would change a file.

Leaves (DECISION.tsv): L1 M3LinkedClearContract bound-split check STALE_CHECK (4650910eff1 made the linked spliterator
fail fast; the fail-fast form of 2aeaffe6424/3538a25a194 was dropped by the retain merges 11848337f18 #1479 and
6d0e9f5461e #1522) plus a JUnit wrapper; W1 M3WrapperConvergenceContract comparator-failure STALE_CHECK (79a8df95080
and ef0beec8bfb route bounded floor through the owner's near-entry primitive, which never calls compare(5, 2)) plus
the mode wired into M3WrapperConvergenceTest. readonly-linear and readonly-failfast get no rule: PENDING_WITNESS,
never passed on any owner version since the contract was written (5fde7416ab1).

usage: python -I apply.py [--root DIR] [--check] [--leaf L1|W1]
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
BASE_COMMIT = "2d8273c6ad55757ec519afe8c45a5db75e61335f"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "decision", "cited_commits", "rule")
TEST = "m3/collections/src/test/java/jdk/m3/collections/"
ABSENT = "-"

HEADER = (
    "/* *****************************************************************************\n"
    " * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.\n"
    " *\n"
    " * This program and the accompanying materials are made available under the\n"
    " * terms of the Apache License, Version 2.0, or (at your option) the Eclipse\n"
    " * Public License 2.0, which are available at\n"
    " * https://www.apache.org/licenses/LICENSE-2.0 and\n"
    " * https://www.eclipse.org/legal/epl-2.0/\n"
    " *\n"
    " * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n"
    " *\n"
    " * Contributors:\n"
    " *     Hitesh Soliwal (Synexia) - initial API and implementation\n"
    " *******************************************************************************/\n")

L1_ANCHOR = ("//FLLDCIM: MRL - stale-contracts-20261106 L1: 4650910eff1 made the linked spliterator fail fast"
             " (LinkedIndexSplit binds modCount); fail-fast form of 2aeaffe6424/3538a25a194, dropped by retain merges"
             " 11848337f18 (#1479) and 6d0e9f5461e (#1522)")
W1_ANCHOR = ("//FLLDCIM: MRL - stale-contracts-20261106 W1: 79a8df95080 (M3FilteredViews) and ef0beec8bfb"
             " (M3MutableFilteredViews) acquire bounded floor through the owner's near-entry primitive, which never"
             " calls compare(5, 2); fail on the first armed call")

# leaf, kind, path, preimage blob (or ABSENT), decision, cited commits, rule note, payload
#   EDIT payload: [(old, new)] anchored substitutions; NEW payload: the whole LF postimage
RULES = (
    ("L1", "EDIT", TEST + "M3LinkedClearContract.java", "b17894235688a95713ae79d2ac6cd1ba32d77c0f", "STALE_CHECK",
     "4650910eff1;2aeaffe6424;3538a25a194;11848337f18;6d0e9f5461e",
     "bound spliterator check asserts the fail-fast LinkedIndexSplit: CME after a nonempty clear", [
         ("        // The existing iterator spliterator checks hasNext before next; an emptied\n"
          "        // owner reports exhaustion here. Fail-fast is not guaranteed for this path.\n"
          "        check(!bound.tryAdvance(v -> { throw new AssertionError(\"stale payload\"); }),\n"
          "                \"bound spliterator cannot expose a cleared payload\");\n",
          "        " + L1_ANCHOR + "\n"
          "        // The current index-backed split binds its structural version above.\n"
          "        // A nonempty clear invalidates that version even when no slots remain.\n"
          "        boolean staleSplit = false;\n"
          "        try { bound.tryAdvance(v -> { throw new AssertionError(\"stale payload\"); }); }\n"
          "        catch (ConcurrentModificationException expected) { staleSplit = true; }\n"
          "        check(staleSplit, \"bound index split must be invalidated by nonempty clear\");\n"),
     ]),
    ("L1", "NEW", TEST + "M3LinkedClearTest.java", ABSENT, "JUNIT_WRAPPER", "4650910eff1",
     "surefire runs M3LinkedClearContract (no JUnit ran it on master); with m3.arrays.native.path the whole contract,"
     " without it the M3LinkedList half, then the map/set half's JNI refusal aborts the test",
     HEADER +
     "package jdk.m3.collections;\n"
     "\n"
     "import java.util.Arrays;\n"
     "import org.junit.jupiter.api.Assertions;\n"
     "import org.junit.jupiter.api.Assumptions;\n"
     "import org.junit.jupiter.api.Test;\n"
     "\n"
     "/** Runs the real-owner clear/reuse and escaped-view contract in Maven. */\n"
     "final class M3LinkedClearTest {\n"
     "    //FLLDCIM: MRL - stale-contracts-20261106 L1: JUnit wrapper; bound-split check follows 4650910eff1\n"
     "    @Test\n"
     "    void clearRetainsCapacityAndInvalidatesEscapedViews() throws Exception {\n"
     "        String library = System.getProperty(\"m3.arrays.native.path\");\n"
     "        if (library != null && !library.isBlank()) {\n"
     "            M3LinkedClearContract.main(new String[0]);\n"
     "            return;\n"
     "        }\n"
     "        // Every M3LinkedList check runs first; M3LinkedHashMap/Set tables need the m3_arrays JNI library,\n"
     "        // which the default build does not load (qualification/jni-test-gating-20261106).\n"
     "        UnsatisfiedLinkError missing = Assertions.assertThrows(UnsatisfiedLinkError.class,\n"
     "                () -> M3LinkedClearContract.main(new String[0]));\n"
     "        Assertions.assertTrue(Arrays.stream(missing.getStackTrace())\n"
     "                .anyMatch(frame -> \"mapAndSet\".equals(frame.getMethodName())), \"only map/set may need JNI\");\n"
     "        Assumptions.abort(\"M3LinkedList half passed; map/set half needs -Pm3-collections-native\");\n"
     "    }\n"
     "}\n"),
    ("W1", "EDIT", TEST + "M3WrapperConvergenceContract.java", "ab8483d19b40f2b79ef743d906dec0147f14e777",
     "STALE_CHECK", "79a8df95080;ef0beec8bfb",
     "comparator-failure throws on the first armed comparator call and pins a one-call tape, not the old headMap pair", [
         ("                if (left == 5 && right == 2) throw marker;\n",
          "                " + W1_ANCHOR + "\n"
          "                if (tape.size() == 1) throw marker;\n"),
         ("        equal(List.of(\"5:2\"), tape, \"map comparator callback tape stops at original failure\");\n",
          "        equal(1, tape.size(), \"map comparator callback tape stops at original failure\");\n"),
         ("        equal(List.of(\"5:2\"), tape, \"set comparator callback tape stops at original failure\");\n",
          "        equal(1, tape.size(), \"set comparator callback tape stops at original failure\");\n"),
     ]),
    ("W1", "EDIT", TEST + "M3WrapperConvergenceTest.java", "98c2f1b84592bd563f969e577ad0ef3d2b9f8dd0",
     "JUNIT_WIRING", "79a8df95080;ef0beec8bfb",
     "surefire runs the comparator-failure mode next to readonly-policy", [
         ("        M3WrapperConvergenceContract.run(\"readonly-policy\");\n    }\n",
          "        M3WrapperConvergenceContract.run(\"readonly-policy\");\n    }\n"
          "\n"
          "    //FLLDCIM: MRL - stale-contracts-20261106 W1: comparator-failure mode wired; check follows"
          " 79a8df95080/ef0beec8bfb\n"
          "    @Test\n"
          "    void comparatorOwnedFailurePropagatesThroughBoundedNavigation() {\n"
          "        M3WrapperConvergenceContract.run(\"comparator-failure\");\n"
          "    }\n"),
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


def resolve(root: Path, kind: str, rel: str, blob: str, payload) -> tuple:
    """(on-disk bytes or None, preimage bytes or None, postimage bytes); refuses a drifted target."""
    target = root / rel
    on_disk = target.read_bytes() if target.exists() else None
    if kind == "NEW":
        post = payload.encode("utf-8")
        if on_disk is not None and on_disk != post:
            raise Refused("NEW target %s exists with other content (%s)" % (rel, blob_sha(on_disk)))
        return on_disk, None, post
    if on_disk is None:
        raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
    pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
    post = postimage(pre, payload, rel)
    if blob_sha(on_disk) != blob and on_disk != post:
        raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
    return on_disk, pre, post


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, kind, rel, blob, decision, cited, note, payload in RULES:
        on_disk, pre, post = resolve(root, kind, rel, blob, payload)
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((root / rel, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, ABSENT if pre is None else sha256(pre), sha256(post), rleaf, decision, cited,
                     kind + ": " + note))
        print("%s %s %s %s" % (kind, rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("STALE_CONTRACTS recipe=%s %s base=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), applied, changed, "CHANGED" if table_state else "UNCHANGED"))
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
