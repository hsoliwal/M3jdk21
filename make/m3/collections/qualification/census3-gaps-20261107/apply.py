#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS3_GAPS v1 (m3/collections): the RESTORE members of census pass 3 leaves P3-006 (M3MonotoneLongDeque) and P3-009 (M3PersistentLongSumTree).

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending). Every rule names the git blob sha of its
preimage at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already
applied, no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing (every
preimage is verified before the first write). Idempotent: a second run reports changed=0 and EDITS.tsv (file,
before/after sha256, leaf, donor blob, rule) is a fixed point. --leaf applies one leaf's rules only; --check writes
nothing and exits 1 when a run would change a file. Donor: com.synexia develop 78cf2ad4c7e5 (blobs named per rule);
per-member decisions in DECISION.tsv (only RESTORE rows have edits here).

usage: python -I apply.py [--root DIR] [--check] [--leaf LEAF]
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
BASE_COMMIT = "9540290ac55e087a760aed39ef2688636ad63d32"
DONOR_COMMIT = "78cf2ad4c7e56f7b0ade5aa206c491bc866178a6"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "donor_blob", "rule")
ROOT_UP = 3


class Refused(Exception):
    pass


SRC = "m3/collections/src/main/java/jdk/m3/collections/"
DEQUE_DONOR = "4ec10eec0a0717695c8475bc57d3e3b551e6c656"
TREE_DONOR = "cc78be1a0a82ed1d04ef96e2082639c33185fc2b"

# leaf, path, preimage blob, donor blob, rule note, edits [(old, new)]
RULES = (
    ("P3-006", SRC + "M3MonotoneLongDeque.java", "b73c9f9b9402c40d442e050fea38562d43437040",
     "develop MIndexMonotoneLongDeque blob " + DEQUE_DONOR,
     "RESTORE lastIndex()/lastValue()/requireNonEmpty(): the tail is the latest offer under both equal-value"
     " policies, so the donor accessors keep their meaning on the owner's long index lane (int -> long like"
     " firstIndex()); evictBefore/offer/firstIndex stay NEC, windowMaximum HOLD_DEP (DECISION.tsv)", [
         ("    public long firstIndex() {\n"
          "        if (size == 0) throw new NoSuchElementException();\n"
          "        return indexes[head];\n"
          "    }\n",
          "    public long firstIndex() {\n"
          "        if (size == 0) throw new NoSuchElementException();\n"
          "        return indexes[head];\n"
          "    }\n"
          "\n"
          "    //FLLDCIM: MRL - census3 P3-006, donor MIndexMonotoneLongDeque blob 4ec10eec0a07: tail accessors"
          " (the tail is the latest offer under either equal-value policy)\n"
          "    public long lastIndex() {\n"
          "        requireNonEmpty();\n"
          "        return indexes[physical(size - 1)];\n"
          "    }\n"
          "\n"
          "    public long lastValue() {\n"
          "        requireNonEmpty();\n"
          "        return values[physical(size - 1)];\n"
          "    }\n"),
         ("    private boolean dominated(long retained, long incoming) {\n",
          "    //FLLDCIM: MRL - census3 P3-006, donor MIndexMonotoneLongDeque blob 4ec10eec0a07: empty guard of the"
          " restored tail accessors\n"
          "    private void requireNonEmpty() {\n"
          "        if (size == 0) {\n"
          "            throw new NoSuchElementException(\"empty monotone deque\");\n"
          "        }\n"
          "    }\n"
          "\n"
          "    private boolean dominated(long retained, long incoming) {\n"),
     ]),
    ("P3-009", SRC + "M3PersistentLongSumTree.java", "1de1ca65fabd0949d3dd1e17174b9eeb65a3b765",
     "develop MIndexPersistentLongSumTree blob " + TREE_DONOR,
     "RESTORE length(), the presizing ctor (int,int), forkAdd (overflow validated along the whole path before any"
     " state change; delta 0 shares the base root), rootNode, pathNodeCount, the scratch lanes and PackedCapacity"
     " (MAX bound); node 0 stays the owner's sentinel, so created node ids equal the donor's; grow and the length"
     " field are PRESENT_EVOLVED (DECISION.tsv)", [
         ("import java.util.Arrays;\n",
          "import java.util.Arrays;\nimport java.util.Objects;\n"),
         ("    private int nodeCount = 1; // node 0 is the implicit all-zero sentinel.\n",
          "    private int nodeCount = 1; // node 0 is the implicit all-zero sentinel.\n"
          "\n"
          "    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: forkAdd path"
          " lanes, presized by the restored constructor and allocated by forkAdd otherwise\n"
          "    private int[] scratchNodes;\n"
          "    private long[] scratchSums;\n"
          "    private boolean[] scratchRight;\n"),
         ("        sums = new long[16];\n    }\n",
          "        sums = new long[16];\n    }\n"
          "\n"
          "    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: presizing"
          " constructor (positive length, expected additional versions)\n"
          "    public M3PersistentLongSumTree(int length, int expectedAdditionalVersions) {\n"
          "        if (length <= 0) {\n"
          "            throw new IllegalArgumentException(\"length must be positive\");\n"
          "        }\n"
          "        if (expectedAdditionalVersions < 0) {\n"
          "            throw new IllegalArgumentException(\"expectedAdditionalVersions\");\n"
          "        }\n"
          "        this.size = length;\n"
          "\n"
          "        int pathNodes = pathNodeCount(length);\n"
          "        scratchNodes = new int[pathNodes];\n"
          "        scratchSums = new long[pathNodes];\n"
          "        scratchRight = new boolean[pathNodes];\n"
          "\n"
          "        roots = new int[Math.max(1, Math.addExact(expectedAdditionalVersions, 1))];\n"
          "\n"
          "        long estimatedNodes = 1L\n"
          "                + (long) Math.max(1, expectedAdditionalVersions) * pathNodes;\n"
          "        int initialNodes = (int) Math.min(\n"
          "                PackedCapacity.MAX,\n"
          "                Math.max(8L, estimatedNodes));\n"
          "        left = new int[initialNodes];\n"
          "        right = new int[initialNodes];\n"
          "        sums = new long[initialNodes];\n"
          "    }\n"),
         ("    public int size() { return size; }\n",
          "    public int size() { return size; }\n"
          "    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: donor name of size()\n"
          "    public int length() { return size; }\n"),
         ("    public long total(int version) {\n"
          "        checkVersion(version);\n"
          "        return sums[roots[version]];\n"
          "    }\n",
          "    public long total(int version) {\n"
          "        checkVersion(version);\n"
          "        return sums[roots[version]];\n"
          "    }\n"
          "\n"
          "    /**\n"
          "     * Forks {@code baseVersion} and adds {@code delta} to one position.\n"
          "     *\n"
          "     * <p>Arithmetic is validated along the complete path before any version/node state changes.\n"
          "     * A zero delta shares the base root.\n"
          "     */\n"
          "    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: checked fork-add\n"
          "    public int forkAdd(int baseVersion, int index, long delta) {\n"
          "        checkVersion(baseVersion);\n"
          "        Objects.checkIndex(index, size);\n"
          "\n"
          "        ensureRootCapacity(Math.addExact(versionCount, 1));\n"
          "        if (delta == 0L) {\n"
          "            roots[versionCount] = roots[baseVersion];\n"
          "            return versionCount++;\n"
          "        }\n"
          "        if (scratchNodes == null) {\n"
          "            int pathNodes = pathNodeCount(size);\n"
          "            scratchNodes = new int[pathNodes];\n"
          "            scratchSums = new long[pathNodes];\n"
          "            scratchRight = new boolean[pathNodes];\n"
          "        }\n"
          "\n"
          "        int oldNode = roots[baseVersion];\n"
          "        int from = 0;\n"
          "        int to = size;\n"
          "        int depth = 0;\n"
          "\n"
          "        while (true) {\n"
          "            scratchNodes[depth] = oldNode;\n"
          "            scratchSums[depth] = Math.addExact(sums[oldNode], delta);\n"
          "            if (to - from == 1) {\n"
          "                break;\n"
          "            }\n"
          "            int middle = (from + to) >>> 1;\n"
          "            boolean goRight = index >= middle;\n"
          "            scratchRight[depth] = goRight;\n"
          "            if (goRight) {\n"
          "                oldNode = right[oldNode];\n"
          "                from = middle;\n"
          "            } else {\n"
          "                oldNode = left[oldNode];\n"
          "                to = middle;\n"
          "            }\n"
          "            depth++;\n"
          "        }\n"
          "\n"
          "        ensureNodeCapacity(Math.addExact(nodeCount, depth + 1));\n"
          "\n"
          "        int newChild = 0;\n"
          "        for (int level = depth; level >= 0; level--) {\n"
          "            int source = scratchNodes[level];\n"
          "            int created = nodeCount++;\n"
          "            left[created] = left[source];\n"
          "            right[created] = right[source];\n"
          "            sums[created] = scratchSums[level];\n"
          "\n"
          "            if (level < depth) {\n"
          "                if (scratchRight[level]) {\n"
          "                    right[created] = newChild;\n"
          "                } else {\n"
          "                    left[created] = newChild;\n"
          "                }\n"
          "            }\n"
          "            newChild = created;\n"
          "        }\n"
          "\n"
          "        roots[versionCount] = newChild;\n"
          "        return versionCount++;\n"
          "    }\n"
          "\n"
          "    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: root node id of a"
          " version\n"
          "    public int rootNode(int version) {\n"
          "        checkVersion(version);\n"
          "        return roots[version];\n"
          "    }\n"),
         ("            throw new IndexOutOfBoundsException(fromInclusive + \"..\" + toExclusive);\n"
          "        }\n"
          "    }\n"
          "}\n",
          "            throw new IndexOutOfBoundsException(fromInclusive + \"..\" + toExclusive);\n"
          "        }\n"
          "    }\n"
          "\n"
          "    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: root-to-leaf path"
          " length\n"
          "    private static int pathNodeCount(int length) {\n"
          "        int nodes = 1;\n"
          "        long span = 1L;\n"
          "        while (span < length) {\n"
          "            span <<= 1;\n"
          "            nodes++;\n"
          "        }\n"
          "        return nodes;\n"
          "    }\n"
          "\n"
          "    /** Local array-capacity bound of the presizing constructor, keeping this kernel JDK-only. */\n"
          "    //FLLDCIM: MRL - census3 P3-009, donor MIndexPersistentLongSumTree blob cc78be1a0a82: PackedCapacity"
          " (grow is PRESENT_EVOLVED as ensureNodeCapacity/ensureRootCapacity)\n"
          "    private static final class PackedCapacity {\n"
          "        private static final int MAX = Integer.MAX_VALUE - 8;\n"
          "\n"
          "        private PackedCapacity() {}\n"
          "    }\n"
          "}\n"),
     ]),
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


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    for rleaf, rel, blob, donor, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED" and leaf not in (None, rleaf):
            state = "PENDING"
        elif state == "CHANGED":
            changed += 1
            writes.append((target, post))
        applied += on_disk == post or state == "CHANGED"
        rows.append((rel, sha256(pre), sha256(post), rleaf, donor, note))
        print("EDIT %s %s %s" % (rleaf, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("CENSUS3_GAPS recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], DONOR_COMMIT[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[ROOT_UP]), help="repository root")
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
