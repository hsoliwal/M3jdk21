#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe CENSUS3_GAPS v1 (m3/ports/precompute): the RESTORE members of census pass 3 leaves P3-010 (M3EditDistancePlan; M3TextSignals is decision-only).

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
BASE_COMMIT = "08fa7d2e705205eea0a685cfb14f39fd0f4b448e"
DONOR_COMMIT = "78cf2ad4c7e56f7b0ade5aa206c491bc866178a6"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "donor_blob", "rule")
ROOT_UP = 4


class Refused(Exception):
    pass


SRC = "m3/ports/precompute/src/main/java/jdk/m3/precompute/"
PLAN_DONOR = "8302a1524f207adfc2a66bea4eb373064c3095e5"
A = "    //FLLDCIM: MRL - census3 P3-010, donor MIndexStringEditDistancePlan blob 8302a1524f20: "

# leaf, path, preimage blob, donor blob, rule note, edits [(old, new)]
RULES = (
    ("P3-010", SRC + "M3EditDistancePlan.java", "0ce6b26740c698480fbfbe81323c1babc0517e6e",
     "develop MIndexStringEditDistancePlan blob " + PLAN_DONOR,
     "RESTORE the legacy code-unit flag (compile(CharSequence, boolean), usesUtf16CodeUnits; both values keep UTF-16"
     " behaviour) and the exact incremental Frontier API (FrontierLimits, frontier(), Frontier with append/range"
     " append/minimumPossibleDistance/within, advanceRow, frontierCheck); ProgressMonitor -> M3Progress (null = no-op);"
     " appendDirect/FrontierBackend/FrontierNative/native kernels HOLD_DEP (indexstring JNI, 4d); the private"
     " constructor takes the flag (private, not API)", [
         ("import java.util.Arrays;\nimport java.util.Objects;\n",
          "import java.util.Arrays;\nimport java.util.Objects;\n"
          "import java.util.concurrent.CancellationException;\nimport java.util.function.IntUnaryOperator;\n"),
         (" * preceded by {@link M3TextSignals#editLowerBound(M3TextSignals)}.</p>\n */\n"
          "public final class M3EditDistancePlan implements CharSequence {\n",
          " * preceded by {@link M3TextSignals#editLowerBound(M3TextSignals)}.</p>\n"
          " *\n"
          " * <p>{@link #frontier()} admits an immutable incremental checkpoint (exact DP row over a consumed\n"
          " * prefix) that appends UTF-16 units under {@link FrontierLimits}. The legacy code-unit flag of\n"
          " * {@link #compile(CharSequence, boolean)} remains observable; both flag values retain UTF-16-unit\n"
          " * behaviour. Threshold answers are exact within the admitted workload.</p>\n */\n"
          "public final class M3EditDistancePlan implements CharSequence {\n"),
         ("    private final long myersDomainMask;\n\n"
          "    private M3EditDistancePlan(\n"
          "            char[] pattern,\n"
          "            char[] myersUnits,\n"
          "            long[] myersMasks,\n"
          "            long myersHighBit,\n"
          "            long myersDomainMask) {\n"
          "        this.pattern = pattern;\n"
          "        this.myersUnits = myersUnits;\n"
          "        this.myersMasks = myersMasks;\n"
          "        this.myersHighBit = myersHighBit;\n"
          "        this.myersDomainMask = myersDomainMask;\n"
          "    }\n",
          "    private final long myersDomainMask;\n"
          + A + "legacy code-unit flag lane\n"
          "    private final boolean useUnicodeCodeUnits;\n\n"
          "    private M3EditDistancePlan(\n"
          "            char[] pattern,\n"
          "            char[] myersUnits,\n"
          "            long[] myersMasks,\n"
          "            long myersHighBit,\n"
          "            long myersDomainMask,\n"
          "            boolean utf16CodeUnits) {\n"
          "        this.pattern = pattern;\n"
          "        this.myersUnits = myersUnits;\n"
          "        this.myersMasks = myersMasks;\n"
          "        this.myersHighBit = myersHighBit;\n"
          "        this.myersDomainMask = myersDomainMask;\n"
          "        this.useUnicodeCodeUnits = utf16CodeUnits;\n"
          "    }\n"),
         ("    public static M3EditDistancePlan compile(CharSequence pattern) {\n"
          "        Objects.requireNonNull(pattern, \"pattern\");\n",
          "    public static M3EditDistancePlan compile(CharSequence pattern) {\n"
          "        return compile(pattern, true);\n"
          "    }\n\n"
          + A + "compile with the legacy code-unit flag\n"
          "    /** Compiles {@code pattern}; the flag is retained and observable, both values use UTF-16 units. */\n"
          "    public static M3EditDistancePlan compile(CharSequence pattern, boolean utf16CodeUnits) {\n"
          "        Objects.requireNonNull(pattern, \"pattern\");\n"),
         ("            return new M3EditDistancePlan(snapshot, null, null, 0L, 0L);\n",
          "            return new M3EditDistancePlan(snapshot, null, null, 0L, 0L, utf16CodeUnits);\n"),
         ("        return new M3EditDistancePlan(snapshot, units, masks, highBit, domainMask);\n",
          "        return new M3EditDistancePlan(snapshot, units, masks, highBit, domainMask, utf16CodeUnits);\n"),
         ("    public boolean usesMyers() {\n        return myersUnits != null;\n    }\n",
          "    public boolean usesMyers() {\n        return myersUnits != null;\n    }\n\n"
          + A + "flag accessor\n"
          "    public boolean usesUtf16CodeUnits() {\n        return useUnicodeCodeUnits;\n    }\n"),
         ("    private int myersDistance(CharSequence value, M3Progress progress) {\n",
          A + "frontier admission\n"
          "    public Frontier frontier() {\n"
          "        return frontier(FrontierLimits.DEFAULT, null);\n"
          "    }\n\n"
          "    /**\n"
          "     * Admits an immutable empty-prefix checkpoint owned by this exact compiled plan. A null monitor\n"
          "     * is a no-op; thread interruption is honored. The caller owns begin/done and input lifetimes.\n"
          "     */\n"
          "    public Frontier frontier(FrontierLimits limits, M3Progress monitor) {\n"
          "        Objects.requireNonNull(limits, \"limits\");\n"
          "        frontierCheck(monitor);\n"
          "        long bytes = Integer.BYTES * ((long) pattern.length + 1L);\n"
          "        if (bytes > limits.maxAdditionalPrimitiveBytes()) {\n"
          "            throw new IllegalArgumentException(\"frontier workspace budget exceeded\");\n"
          "        }\n"
          "        int[] row = new int[pattern.length + 1];\n"
          "        for (int j = 0; j < row.length; j++) {\n"
          "            if ((j & 1023) == 0) frontierCheck(monitor);\n"
          "            row[j] = j;\n"
          "        }\n"
          "        frontierCheck(monitor);\n"
          "        return new Frontier(this, limits, row, 0);\n"
          "    }\n\n"
          "    private int myersDistance(CharSequence value, M3Progress progress) {\n"),
         ("    private void ensureWorkload(int sourceLength) {\n",
          A + "pure exact row atom of the frontier\n"
          "    private static void advanceRow(\n"
          "            char[] query, int[] previous, int[] current, char unit, int consumed, M3Progress monitor) {\n"
          "        current[0] = consumed;\n"
          "        int diagonal = previous[0];\n"
          "        int left = consumed;\n"
          "        for (int j = 1; j <= query.length; j++) {\n"
          "            if ((j & 1023) == 0) frontierCheck(monitor);\n"
          "            int above = previous[j];\n"
          "            int score = Math.min(Math.min(left + 1, above + 1), diagonal + (query[j - 1] == unit ? 0 : 1));\n"
          "            current[j] = score;\n"
          "            diagonal = above;\n"
          "            left = score;\n"
          "        }\n"
          "    }\n\n"
          + A + "cancellation point (interrupt or monitor)\n"
          "    private static void frontierCheck(M3Progress monitor) {\n"
          "        if (Thread.currentThread().isInterrupted()) {\n"
          "            throw new CancellationException(\"distance frontier interrupted\");\n"
          "        }\n"
          "        if (monitor != null) monitor.checkCanceled();\n"
          "        if (Thread.currentThread().isInterrupted()) {\n"
          "            throw new CancellationException(\"distance frontier interrupted\");\n"
          "        }\n"
          "    }\n\n"
          "    private void ensureWorkload(int sourceLength) {\n"),
         ("    public static final class Workspace {\n",
          A + "frontier limits\n"
          "    /** Per-branch logical work and per-append added Java primitive storage admission. */\n"
          "    public record FrontierLimits(int maxInputUnits, long maxCellUpdates, long maxAdditionalPrimitiveBytes) {\n"
          "        public static final FrontierLimits DEFAULT = new FrontierLimits(1_048_576, 100_000_000L, 64L << 20);\n\n"
          "        public FrontierLimits {\n"
          "            if (maxInputUnits < 0 || maxInputUnits == Integer.MAX_VALUE\n"
          "                    || maxCellUpdates < 0 || maxAdditionalPrimitiveBytes < 0) {\n"
          "                throw new IllegalArgumentException(\"invalid frontier limits\");\n"
          "            }\n"
          "        }\n"
          "    }\n\n"
          + A + "immutable incremental checkpoint (appendDirect/FrontierBackend held: indexstring JNI)\n"
          "    /**\n"
          "     * Exact immutable DP boundary: {@code row[j] = distance(pattern[0,j), consumed prefix)}.\n"
          "     *\n"
          "     * <p>The checkpoint retains only its plan, limits, unit count and numeric row. Appends may run\n"
          "     * concurrently from one checkpoint, with independent monitors and stable inputs. A failed append\n"
          "     * never mutates the checkpoint or publishes its partially computed successor. Keep or discard\n"
          "     * older checkpoints explicitly: no hidden or global cache is introduced.</p>\n"
          "     *\n"
          "     * <p>Each appended UTF-16 unit costs {@code max(1, patternLength)} logical cell updates. The work\n"
          "     * budget applies to the complete branch, so partitioning cannot evade it. Initialization and row\n"
          "     * copies are reported by storage accounting, not added to that recurrence count. Additional\n"
          "     * primitive storage is two int rows per nonempty append, excluding the plan, existing\n"
          "     * checkpoints and object headers. This is not a reusable text-to-text transfer matrix or a\n"
          "     * scalar-distance composition rule.</p>\n"
          "     */\n"
          "    public static final class Frontier {\n"
          "        private final M3EditDistancePlan owner;\n"
          "        private final FrontierLimits limits;\n"
          "        private final int[] row;\n"
          "        private final int inputUnits;\n\n"
          "        private Frontier(M3EditDistancePlan owner, FrontierLimits limits, int[] row, int inputUnits) {\n"
          "            this.owner = owner;\n"
          "            this.limits = limits;\n"
          "            this.row = row;\n"
          "            this.inputUnits = inputUnits;\n"
          "        }\n\n"
          "        public M3EditDistancePlan plan() {\n            return owner;\n        }\n\n"
          "        public FrontierLimits limits() {\n            return limits;\n        }\n\n"
          "        public int inputUnits() {\n            return inputUnits;\n        }\n\n"
          "        public long cellUpdates() {\n"
          "            return (long) inputUnits * Math.max(1, owner.pattern.length);\n        }\n\n"
          "        public long primitivePayloadBytes() {\n            return Integer.BYTES * (long) row.length;\n        }\n\n"
          "        public int distance() {\n            return row[owner.pattern.length];\n        }\n\n"
          "        public int[] rowCopy() {\n            return row.clone();\n        }\n\n"
          "        /**\n"
          "         * Exact minimum final distance over all possible suffixes of {@code remainingUnits}.\n"
          "         *\n"
          "         * <p>This is a lower bound for a particular unseen suffix, never proof that it matches. It\n"
          "         * performs one existing-row lookup (or length arithmetic), with no scan, allocation, source\n"
          "         * access or new cache. It does not admit append resources; append limits still apply. The\n"
          "         * long result also describes logical completions larger than this frontier can admit.</p>\n"
          "         *\n"
          "         * <p>Let m be the pattern length and r the suffix length. Every completion crosses this\n"
          "         * boundary at a pattern prefix j, costing at least {@code row[j] + abs(m-j-r)}. Adjacent exact\n"
          "         * row values differ by at most one, so the minimum is {@code row[m-r]} for r &lt;= m, otherwise\n"
          "         * {@code inputUnits+r-m}. Choosing the final r pattern units (or padding when r &gt; m) attains\n"
          "         * it. This rule is specific to the unit-cost UTF-16 insertion/deletion/substitution\n"
          "         * contract; it is not a weighted, transposition, grapheme or normalization bound.</p>\n"
          "         *\n"
          "         * @throws IllegalArgumentException if remainingUnits is negative\n"
          "         */\n"
          "        public long minimumPossibleDistance(int remainingUnits) {\n"
          "            if (remainingUnits < 0) throw new IllegalArgumentException(\"negative remaining units\");\n"
          "            int width = owner.pattern.length;\n"
          "            return remainingUnits <= width ? row[width - remainingUnits]\n"
          "                    : (long) inputUnits + remainingUnits - width;\n"
          "        }\n\n"
          "        public boolean within(int maximum) {\n"
          "            if (maximum < 0) throw new IllegalArgumentException(\"negative maximum distance\");\n"
          "            return distance() <= maximum;\n"
          "        }\n\n"
          "        public Frontier append(CharSequence value) {\n            return append(value, null);\n        }\n\n"
          "        /** Input must remain stable only for this synchronous call; it is never retained. */\n"
          "        public Frontier append(CharSequence value, M3Progress monitor) {\n"
          "            Objects.requireNonNull(value, \"value\");\n"
          "            frontierCheck(monitor);\n"
          "            return append(value, 0, value.length(), monitor);\n"
          "        }\n\n"
          "        /** Appends exactly [begin,end) without substring creation, normalization or case folding. */\n"
          "        public Frontier append(CharSequence value, int begin, int end, M3Progress monitor) {\n"
          "            Objects.requireNonNull(value, \"value\");\n"
          "            frontierCheck(monitor);\n"
          "            Objects.checkFromToIndex(begin, end, value.length());\n"
          "            frontierCheck(monitor);\n"
          "            return extend(end - begin, index -> value.charAt(begin + index), monitor);\n"
          "        }\n\n"
          "        private int admittedLength(int count) {\n"
          "            long total = (long) inputUnits + count;\n"
          "            if (total > limits.maxInputUnits()) {\n"
          "                throw new IllegalArgumentException(\"frontier input budget exceeded\");\n"
          "            }\n"
          "            long cells = total * Math.max(1, owner.pattern.length);\n"
          "            if (cells > limits.maxCellUpdates()) {\n"
          "                throw new IllegalArgumentException(\"frontier cell budget exceeded\");\n"
          "            }\n"
          "            long bytes = count == 0 ? 0L : 2L * Integer.BYTES * row.length;\n"
          "            if (bytes > limits.maxAdditionalPrimitiveBytes()) {\n"
          "                throw new IllegalArgumentException(\"frontier workspace budget exceeded\");\n"
          "            }\n"
          "            return (int) total;\n"
          "        }\n\n"
          "        private Frontier extend(int count, IntUnaryOperator unitAt, M3Progress monitor) {\n"
          "            int total = admittedLength(count);\n"
          "            frontierCheck(monitor);\n"
          "            if (count == 0) return this;\n"
          "            int[] previous = row.clone();\n"
          "            int[] current = new int[row.length];\n"
          "            int reported = 0;\n"
          "            for (int index = 0; index < count; index++) {\n"
          "                frontierCheck(monitor);\n"
          "                char unit = (char) unitAt.applyAsInt(index);\n"
          "                frontierCheck(monitor);\n"
          "                advanceRow(owner.pattern, previous, current, unit, inputUnits + index + 1, monitor);\n"
          "                int[] swap = previous;\n"
          "                previous = current;\n"
          "                current = swap;\n"
          "                if ((index & 1023) == 1023 || index == count - 1) {\n"
          "                    int delta = index + 1 - reported;\n"
          "                    if (monitor != null) monitor.worked(delta);\n"
          "                    reported = index + 1;\n"
          "                    frontierCheck(monitor);\n"
          "                }\n"
          "            }\n"
          "            frontierCheck(monitor);\n"
          "            return new Frontier(owner, limits, previous, total);\n"
          "        }\n"
          "    }\n\n"
          "    public static final class Workspace {\n"),
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
