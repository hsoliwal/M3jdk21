#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v1: merge-loss audit leaf 71 (m3/ast M3SyntaxDelta and its contract).

Engine: m3/core/qualification/mergeloss-67-69-20261107/apply.py v1 (itself derived from
m3/collections/qualification/census-small-batch-20261106/apply.py v1): anchored "sub" edits, every anchor exactly once,
LF payloads re-joined with the preimage's line ending, a per-file leaf chain whose accepted states are the base (pinned
by git blob id) and each chain prefix. Anything else is drift and the recipe refuses (exit 2) before any write.
Idempotent: a second run reports files_changed=0 and EDITS.tsv is a fixed point. --check writes nothing and exits 1
when a run would change a file.

Leaf 71 (com.synexia merge-loss audit LEAVES.tsv, 2026-11-06): origin fb3a33474a27 "fix(m3ast): source-sealed unique
bidirectional recovery-node matching" (2026-10-26) and its companion test commit a94236ef04c9 (same subject), PR #1975,
dropped by the retain merge 615c10fe7e9c (RETAIN_MERGE_WHOLE_PR: "promotion to live code is a reviewed follow-up"). Both
origin preimages equal master's files after the JDK naming package rename (fe35ea98841), and the retained copies under
m3/qualification/master-landing-20261028/retained-variants/pr1975 (blobs 3ebf8ec44da0, 198b79c43b1f) equal master plus
exactly these hunks, so the recipe replays the origin hunks.

usage: python -I apply.py [--root DIR] [--check] [--leaf 71]
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
BASE_COMMIT = "454408c2a5b061009347e7f2d55471b141ffe93a"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin", "dropping_merge", "donor_blobs", "rule")
DELTA = "m3/ast/src/main/java/jdk/m3/ast/M3SyntaxDelta.java"
CONTRACT = "m3/ast/src/test/java/jdk/m3/ast/M3SyntaxDeltaContract.java"


class Refused(Exception):
    pass


def lines(*text):
    return "\n".join(text) + "\n"


DELTA_0_OLD = lines(
    '        Arrays.fill(newMap, -1);',
    '        byte[] oldBytes = prior.sourceBytes(), newBytes = next.sourceBytes();',
)
DELTA_0_NEW = lines(
    '        Arrays.fill(newMap, -1);',
    '        //FLLDCIM: MRL - merge-loss leaf 71 (origin fb3a33474a27, retain 615c10fe7e9c): unique matches',
    '        // A unique new occurrence can still have multiple indistinguishable',
    '        // OLD occurrences (for example, duplicate zero-width recovery tokens).',
    '        // Invalidate the first match when a second old occurrence is found.',
    '        boolean[] ambiguousNew = new boolean[n];',
    '        byte[] oldBytes = prior.sourceBytes(), newBytes = next.sourceBytes();',
)
DELTA_1_OLD = lines(
    '            }',
    '            if (found >= 0 && newMap[found] < 0) {',
    '                oldMap[node] = found;',
    '                newMap[found] = node;',
    '                count++;',
    '            }',
)
DELTA_1_NEW = lines(
    '            }',
    '            //FLLDCIM: MRL - merge-loss leaf 71 (origin fb3a33474a27, retain 615c10fe7e9c): unique matches',
    '            if (found >= 0) {',
    '                if (ambiguousNew[found]) continue;',
    '                int previous = newMap[found];',
    '                if (previous >= 0) {',
    '                    // Do not guess which old occurrence survived a reparse.',
    '                    // Both sides remain externally unmatched (-1).',
    '                    oldMap[previous] = -1;',
    '                    newMap[found] = -1;',
    '                    ambiguousNew[found] = true;',
    '                    count--;',
    '                } else {',
    '                    oldMap[node] = found;',
    '                    newMap[found] = node;',
    '                    count++;',
    '                }',
    '            }',
)
CONTRACT_0_OLD = lines(
    '                && noGuess.newNodeForOld(2) == -1, "ambiguous zero-width nodes never arbitrarily matched");',
    '        var longBuilder = M3SyntaxTree.builder("large/js", new byte[1200]);',
)
CONTRACT_0_NEW = lines(
    '                && noGuess.newNodeForOld(2) == -1, "ambiguous zero-width nodes never arbitrarily matched");',
    '        //FLLDCIM: MRL - merge-loss leaf 71 (origin a94236ef04c9, retain 615c10fe7e9c): duplicate recovery tokens',
    '        // Old source may contain TWO identical zero-width recovery occurrences,',
    '        // while the independently reparsed source contains only ONE. A single',
    '        // available new node is still ambiguous and must not steal the first old.',
    '        var oneBuilder = M3SyntaxTree.builder("partial/error", new byte[] {\'a\'});',
    '        oneBuilder.open("file", "", 0, M3SyntaxTree.NAMED);',
    '        oneBuilder.leaf("missing", "expected", 1, 1, M3SyntaxTree.TOKEN | M3SyntaxTree.MISSING);',
    '        oneBuilder.close(1);',
    '        var one = oneBuilder.build();',
    '        var oldDuplicate = M3SyntaxDelta.match(new M3SyntaxIndex(doubled),',
    '                new M3SyntaxIndex(one), unchanged(doubled, one));',
    '        check(oldDuplicate.candidates() == 0',
    '                && oldDuplicate.newNodeForOld(1) == -1',
    '                && oldDuplicate.newNodeForOld(2) == -1',
    '                && oldDuplicate.oldNodeForNew(1) == -1,',
    '                "two equivalent old occurrences cannot claim one new recovery token");',
    '        var newDuplicate = M3SyntaxDelta.match(new M3SyntaxIndex(one),',
    '                new M3SyntaxIndex(doubled), unchanged(one, doubled));',
    '        check(newDuplicate.candidates() == 0',
    '                && newDuplicate.newNodeForOld(1) == -1,',
    '                "one old recovery token cannot claim two equivalent new occurrences");',
    '        var unique = M3SyntaxDelta.match(new M3SyntaxIndex(one),',
    '                new M3SyntaxIndex(one), unchanged(one, one));',
    '        check(unique.candidates() == one.size() && unique.newNodeForOld(1) == 1,',
    '                "unique recovery token still matches bidirectionally");',
    '        var tripleBuilder = M3SyntaxTree.builder("partial/error", new byte[] {\'a\'});',
    '        tripleBuilder.open("file", "", 0, M3SyntaxTree.NAMED);',
    '        for (int i = 0; i < 3; i++)',
    '            tripleBuilder.leaf("missing", "expected", 1, 1, M3SyntaxTree.TOKEN | M3SyntaxTree.MISSING);',
    '        tripleBuilder.close(1);',
    '        var triple = tripleBuilder.build();',
    '        var threeToOne = M3SyntaxDelta.match(new M3SyntaxIndex(triple),',
    '                new M3SyntaxIndex(one), unchanged(triple, one));',
    '        check(threeToOne.candidates() == 0',
    '                && threeToOne.oldNodeForNew(1) == -1',
    '                && Arrays.equals(threeToOne.oldUnmatched(), new int[] {0, 1, 2, 3}),',
    '                "third indistinguishable old token cannot reinstate an invalidated match");',
    '        var longBuilder = M3SyntaxTree.builder("large/js", new byte[1200]);',
)

# path, base blob (master), chain of (leaf, origin, dropping merge, donor blobs, rule note, edits [(old, new)])
CHAINS = (
    (DELTA, "5987c630ee8c236289c3e073477c9e34a9003f8b", (
        ("71", "fb3a33474a2718a07d76e9c9a0a064b04c644bcd", "615c10fe7e9c3ef8bbbe7ebddae753ac5b775980",
         "post=f0567949683 pre=a318d5bb2a0 retained=3ebf8ec44da0",
         "RESTORE the origin hunk: a second equivalent old occurrence invalidates the first match of a unique new"
         " node; both stay unmatched (origin preimage = master after JDK naming)", [
             (DELTA_0_OLD, DELTA_0_NEW),
             (DELTA_1_OLD, DELTA_1_NEW),
         ]),
    )),
    (CONTRACT, "0a9de685491586c973e1f6d80826148f32a9856a", (
        ("71", "a94236ef04c90f939ed53c95f961591b4c1e8c7d", "615c10fe7e9c3ef8bbbe7ebddae753ac5b775980",
         "post=505da150370 pre=383129fc8e9 retained=198b79c43b1f",
         "RESTORE the companion test hunk (two/three equivalent old recovery tokens vs one new; one old vs two new;"
         " unique still matches); origin preimage = master after JDK naming", [
             (CONTRACT_0_OLD, CONTRACT_0_NEW),
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
    print("MERGELOSS_RESTORE recipe=%s %s base=%s leaves=%d applied=%d files_changed=%d edits=%s" % (
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
