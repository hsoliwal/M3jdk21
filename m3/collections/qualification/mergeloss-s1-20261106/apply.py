#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS_RESTORE v2 (S1): restore the M3IndexPathMasks long-row members and JNI entry dropped by merge e913ab890566.

Derived from m3/collections/qualification/census-small-batch-20261106/apply.py v1: anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending. An EDIT rule names the git blob sha of its preimage
at the base commit; a target must equal the preimage (the rule is applied) or the postimage (already applied, no-op);
anything else is a drifted preimage and the recipe refuses (exit 2) without writing. A MOVE rule moves one file byte for
byte: the source must hold the pinned blob and the destination be absent (applied), or the source be absent and the
destination hold the pinned blob (no-op); anything else refuses. Every preimage is verified before the first write.
Idempotent: a second run reports changed=0 and EDITS.tsv (file, before/after sha256, leaf, decision, cited commits,
rule) is a fixed point. --leaf applies one leaf's rules only (one commit per leaf); --check writes nothing and exits 1
when a run would change a file.

Case 05 of the M3jdk21 merge-loss audit (DECISION.tsv): origin ebbfcbdf93aa (2026-10-17, 'receive hash-sealed
long-index path-mask Java/JNI extension') added selectLong/selectLongJava/selectLongNative/validateGeometry and the
shared int/long JNI core m3_select + Java_jdk_m3_collections_M3IndexPathMasks_selectLongNative; ancestry merge
e913ab890566 (parent 1 lost) dropped them. Master has since evolved both owners (validateShape split by 221e3d4edfa /
548642eae65 / cf0afb7659d; the cached int JNI path), so every hunk is MAPPED by member name: the long members reuse
master's validateShape through a row-size overload (the split the retained #1294/#1368 twin carries, blob
4738bd290e62), and m3_select is master's uncached selectNative body generalised over int[]/long[] rows with the cached
int fast path kept int-only. L2 un-holds the origin's two tests from #2517's held/ directory (HELD.tsv rows 3-4).

v2 adds L3, merge-loss leaf 20 (held HOLD_CASE05 by that leaf's session): origin 0e68366337be (2026-10-21, 'refine
lexical reuse, primitive indices and JNI result writes') batched m3_select_cached's output into 128-element Regions;
the retain merge fb9b504bc047 (#1788) dropped it. Several EDIT rules on one file chain: rule k+1 pins the git blob
of rule k's postimage, the file must equal one stage of the chain, and the rules before that stage count as applied.

usage: python -I apply.py [--root DIR] [--check] [--leaf L1|L2|L3]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v2"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "c33ef9458dcc"
ORIGIN = "ebbfcbdf93aa30f13179fd8a9f2bb26de80f410c"
DROPPING_MERGE = "e913ab89056657eca4009678cfe9a657c020e79b"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "decision", "cited_commits", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
NATIVE = "m3/collections/src/main/native/collections/"
TEST = "m3/collections/src/test/java/jdk/m3/collections/"
HELD = "m3/qualification/loss-guard-restore-20261106/held/"
CITED = "origin=" + ORIGIN[:12] + ";dropped_by=" + DROPPING_MERGE[:12]
ANCHOR = ("//FLLDCIM: MRL - mergeloss-s1-20261106 L1: restored from origin ebbfcbdf93aa, dropped by ancestry merge"
          " e913ab890566 (case 05)")


class Refused(Exception):
    pass


SELECT_LONG = (
    "    " + ANCHOR + "\n"
    "    /**\n"
    "     * Select signed 64-bit row IDs through the existing logical-to-physical index\n"
    "     * path and full 32-bit required/forbidden stage masks. No 32-bit truncation:\n"
    "     * row IDs may include Long.MIN_VALUE and Long.MAX_VALUE. Duplicates and\n"
    "     * encounter order are retained; the returned long[] is detached. JAVA is\n"
    "     * the default reference; NATIVE_REQUIRED preserves explicit load failure.\n"
    "     * All caller-owned input arrays must remain stable during the operation.\n"
    "     */\n"
    "    public static long[] selectLong(long[] orderedRows, int[] physicalOffsets,\n"
    "            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,\n"
    "            int first, int count, int stride, Backend backend) {\n"
    "        Objects.requireNonNull(backend, \"backend\");\n"
    "        Objects.requireNonNull(orderedRows, \"orderedRows\");\n"
    "        validateGeometry(orderedRows.length, physicalOffsets, statePlanes,\n"
    "                requiredMasks, forbiddenMasks, first, count, stride);\n"
    "        if (backend == Backend.NATIVE_REQUIRED) {\n"
    "            if (!NativeLibrary.AVAILABLE) {\n"
    "                throw new UnsatisfiedLinkError(\"m3_collections_pathmask JNI library unavailable\");\n"
    "            }\n"
    "            return selectLongNative(orderedRows, physicalOffsets, statePlanes,\n"
    "                    requiredMasks, forbiddenMasks, first, count, stride);\n"
    "        }\n"
    "        return selectLongJava(orderedRows, physicalOffsets, statePlanes,\n"
    "                requiredMasks, forbiddenMasks, first, count, stride);\n"
    "    }\n"
    "\n")

SELECT_LONG_JAVA = (
    "    " + ANCHOR + "\n"
    "    /** Long-row reference kernel: two scans, one exact output, no row objects. */\n"
    "    static long[] selectLongJava(long[] rows, int[] offsets, int[][] planes,\n"
    "            int[] required, int[] forbidden, int first, int count, int stride) {\n"
    "        int kept = 0;\n"
    "        long at = first;\n"
    "        for (int n = 0; n < count; n++, at += stride) {\n"
    "            if (matches(planes, required, forbidden, offsets[(int) at])) kept++;\n"
    "        }\n"
    "        long[] selected = new long[kept];\n"
    "        int written = 0;\n"
    "        at = first;\n"
    "        for (int n = 0; n < count; n++, at += stride) {\n"
    "            int index = (int) at;\n"
    "            if (matches(planes, required, forbidden, offsets[index])) {\n"
    "                if (written == kept) {\n"
    "                    throw new ConcurrentModificationException(\"mask stages changed during selection\");\n"
    "                }\n"
    "                selected[written++] = rows[index];\n"
    "            }\n"
    "        }\n"
    "        if (written != kept) {\n"
    "            throw new ConcurrentModificationException(\"mask stages changed during selection\");\n"
    "        }\n"
    "        return selected;\n"
    "    }\n"
    "\n")

JAVA_EDITS = [
    (" * Modified 2026: refuse output aliasing mask vectors before selection or JNI dispatch.\n",
     " * Modified 2026: refuse output aliasing mask vectors before selection or JNI dispatch.\n"
     " * Modified 2026-11-06: restore the long-row selection dropped by merge e913ab890566.\n"),
    ("                requiredMasks, forbiddenMasks, first, count, stride);\n    }\n\n    /**\n"
     "     * First matching logical encounter index, or -1.",
     "                requiredMasks, forbiddenMasks, first, count, stride);\n    }\n\n" + SELECT_LONG + "    /**\n"
     "     * First matching logical encounter index, or -1."),
    ("            int first, int count, int stride) {\n"
     "        int physicalCapacity = validateShape(rows, positions, planes, required,\n"
     "                forbidden, first, count, stride);\n",
     "            int first, int count, int stride) {\n"
     "        Objects.requireNonNull(rows, \"orderedRows\");\n"
     "        validateGeometry(rows.length, positions, planes, required, forbidden,\n"
     "                first, count, stride);\n"
     "    }\n\n"
     "    " + ANCHOR + "\n"
     "    private static void validateGeometry(int rowSize, int[] positions,\n"
     "            int[][] planes, int[] required, int[] forbidden,\n"
     "            int first, int count, int stride) {\n"
     "        int physicalCapacity = validateShape(rowSize, positions, planes, required,\n"
     "                forbidden, first, count, stride);\n"),
    ("        Objects.requireNonNull(rows, \"orderedRows\");\n"
     "        Objects.requireNonNull(positions, \"physicalOffsets\");\n",
     "        Objects.requireNonNull(rows, \"orderedRows\");\n"
     "        return validateShape(rows.length, positions, planes, required, forbidden,\n"
     "                first, count, stride);\n"
     "    }\n\n"
     "    " + ANCHOR + " (row-size overload of the shared preflight)\n"
     "    private static int validateShape(int rowSize, int[] positions,\n"
     "            int[][] planes, int[] required, int[] forbidden,\n"
     "            int first, int count, int stride) {\n"
     "        Objects.requireNonNull(positions, \"physicalOffsets\");\n"),
    ("        if (rows.length != positions.length) {\n", "        if (rowSize != positions.length) {\n"),
    ("first < 0 || first > rows.length) {\n", "first < 0 || first > rowSize) {\n"),
    ("            if (first == rows.length || last < 0 || last >= rows.length) {\n",
     "            if (first == rowSize || last < 0 || last >= rowSize) {\n"),
    ("    private static final class NativeLibrary {\n",
     SELECT_LONG_JAVA + "    private static final class NativeLibrary {\n"),
    ("            int first, int count, int stride);\n}\n",
     "            int first, int count, int stride);\n"
     "    " + ANCHOR + "\n"
     "    private static native long[] selectLongNative(long[] orderedRows, int[] physicalOffsets,\n"
     "            int[][] statePlanes, int[] requiredMasks, int[] forbiddenMasks,\n"
     "            int first, int count, int stride);\n}\n"),
]

C_ANCHOR = ("//FLLDCIM: MRL - mergeloss-s1-20261106 L1: m3_select + selectLongNative restored from origin ebbfcbdf93aa,"
            " dropped by ancestry merge e913ab890566 (case 05)")

C_EDITS = [
    ("JNIEXPORT jintArray JNICALL Java_jdk_m3_collections_M3IndexPathMasks_selectNative(\n"
     "        JNIEnv *env, jclass owner, jintArray rows, jintArray offsets,\n"
     "        jobjectArray planes, jintArray required, jintArray forbidden,\n"
     "        jint first, jint count, jint stride) {\n"
     "    (void)owner;\n"
     "    if (rows == NULL",
     C_ANCHOR + "\n"
     "/* Shared exact two-pass selection core for int[] and long[] row IDs; the cached\n"
     " * stage path above stays int-only, long rows take the reference loop below. */\n"
     "static jarray m3_select(JNIEnv *env, jarray rows, jintArray offsets,\n"
     "        jobjectArray planes, jintArray required, jintArray forbidden,\n"
     "        jint first, jint count, jint stride, int long_rows) {\n"
     "    if (rows == NULL"),
    ("    if (stages <= M3_CACHED_STAGE_LIMIT && count >= 8) {\n"
     "        return m3_select_cached(env, rows, offsets, planes, required, forbidden,\n",
     "    if (!long_rows && stages <= M3_CACHED_STAGE_LIMIT && count >= 8) {\n"
     "        return (jarray)m3_select_cached(env, (jintArray)rows, offsets, planes, required, forbidden,\n"),
    ("        /* Output allocation is deferred until after exact counting. */\n"
     "        jintArray result = NULL;\n"
     "        if (pass == 1) {\n"
     "            result = (*env)->NewIntArray(env, matching);\n"
     "            if (result == NULL) return NULL;\n",
     "        /* Output allocation is deferred until after exact counting. */\n"
     "        jarray output = NULL;\n"
     "        if (pass == 1) {\n"
     "            output = long_rows ? (jarray)(*env)->NewLongArray(env, matching)\n"
     "                               : (jarray)(*env)->NewIntArray(env, matching);\n"
     "            if (output == NULL) return NULL;\n"),
    ("                jint row = 0;\n"
     "                (*env)->GetIntArrayRegion(env, rows, (jsize)at, 1, &row);\n"
     "                if ((*env)->ExceptionCheck(env)) return NULL;\n"
     "                (*env)->SetIntArrayRegion(env, result, written++, 1, &row);\n"
     "                if ((*env)->ExceptionCheck(env)) return NULL;\n",
     "                if (long_rows) {\n"
     "                    jlong row = 0;\n"
     "                    (*env)->GetLongArrayRegion(env, (jlongArray)rows, (jsize)at, 1, &row);\n"
     "                    if ((*env)->ExceptionCheck(env)) return NULL;\n"
     "                    (*env)->SetLongArrayRegion(env, (jlongArray)output, written, 1, &row);\n"
     "                } else {\n"
     "                    jint row = 0;\n"
     "                    (*env)->GetIntArrayRegion(env, (jintArray)rows, (jsize)at, 1, &row);\n"
     "                    if ((*env)->ExceptionCheck(env)) return NULL;\n"
     "                    (*env)->SetIntArrayRegion(env, (jintArray)output, written, 1, &row);\n"
     "                }\n"
     "                if ((*env)->ExceptionCheck(env)) return NULL;\n"
     "                ++written;\n"),
    ("            return result;\n        }\n    }\n    return NULL;\n}\n",
     "            return output;\n        }\n    }\n    return NULL;\n}\n"
     "\n"
     "JNIEXPORT jintArray JNICALL Java_jdk_m3_collections_M3IndexPathMasks_selectNative(\n"
     "        JNIEnv *env, jclass owner, jintArray rows, jintArray offsets,\n"
     "        jobjectArray planes, jintArray required, jintArray forbidden,\n"
     "        jint first, jint count, jint stride) {\n"
     "    (void)owner;\n"
     "    return (jintArray)m3_select(env, (jarray)rows, offsets, planes, required,\n"
     "                                  forbidden, first, count, stride, 0);\n"
     "}\n"
     "\n"
     + C_ANCHOR + "\n"
     "JNIEXPORT jlongArray JNICALL Java_jdk_m3_collections_M3IndexPathMasks_selectLongNative(\n"
     "        JNIEnv *env, jclass owner, jlongArray rows, jintArray offsets,\n"
     "        jobjectArray planes, jintArray required, jintArray forbidden,\n"
     "        jint first, jint count, jint stride) {\n"
     "    (void)owner;\n"
     "    return (jlongArray)m3_select(env, (jarray)rows, offsets, planes, required,\n"
     "                                   forbidden, first, count, stride, 1);\n"
     "}\n"),
]

L20_ORIGIN = "0e68366337be549b91ecbde593194e3ea97afd7f"
L20_MERGE = "fb9b504bc047"
L20_CITED = "origin=" + L20_ORIGIN[:12] + ";dropped_by=" + L20_MERGE + " (RETAIN #1788);merge-loss leaf 20"
L20_ANCHOR = ("//FLLDCIM: MRL - mergeloss-s1-20261106 L3: merge-loss leaf 20, chunked cached-lane output writes restored from"
              " origin 0e68366337be, dropped by retain merge fb9b504bc047 (#1788)")

C_L3_EDITS = [
    (" * Optional JNI parity backend for precomputed integer mask stages.\n",
     " * Optional JNI parity backend for precomputed integer mask stages.\n"
     " * Modified 2026: batch only detached output writes in the existing cached lane.\n"),
    ("#define M3_CACHED_STAGE_LIMIT 8\n",
     "#define M3_CACHED_STAGE_LIMIT 8\n"
     + L20_ANCHOR + "\n"
     "/* Reuse the bounded pending-output Region pattern from m3_index_mask_window.c.\n"
     " * Only the private result is buffered; every source read retains its order.\n"
     " */\n"
     "#define M3_PATH_OUTPUT_CHUNK 128\n"),
    ("    jintArray result = NULL;\n    jsize matching = 0;\n",
     "    jintArray result = NULL;\n    jsize matching = 0;\n    jint selected[M3_PATH_OUTPUT_CHUNK];\n"),
    ("        jsize written = 0;\n        jlong at = first;\n",
     "        jsize written = 0;\n        jsize pending = 0;\n        jlong at = first;\n"),
    ("                (*env)->SetIntArrayRegion(env, result, written++, 1, &row);\n"
     "                if ((*env)->ExceptionCheck(env)) goto failure;\n",
     "                selected[pending++] = row;\n"
     "                ++written;\n"
     "                if (pending == M3_PATH_OUTPUT_CHUNK) {\n"
     "                    (*env)->SetIntArrayRegion(env, result, written - pending,\n"
     "                            pending, selected);\n"
     "                    if ((*env)->ExceptionCheck(env)) goto failure;\n"
     "                    pending = 0;\n"
     "                }\n"),
    ("            m3_release_stages(env, &cache);\n            return result;\n",
     "            if (pending != 0) {\n"
     "                (*env)->SetIntArrayRegion(env, result, written - pending,\n"
     "                        pending, selected);\n"
     "                if ((*env)->ExceptionCheck(env)) goto failure;\n"
     "            }\n"
     "            m3_release_stages(env, &cache);\n            return result;\n"),
]

L3_NOTE = ("APPLICABLE (merge-loss leaf 20, held HOLD_CASE05 by the leaf-20 session): the origin's m3_index_path_masks.c"
           " hunk (bounded 128-element output Regions in m3_select_cached) applies verbatim on the L1 postimage, whose"
           " cached int lane L1 left unchanged; chained after L1 (pinned blob = L1 postimage)")

L1_NOTE = ("MAPPED by member name: selectLong, selectLongJava, selectLongNative, validateGeometry (origin text) over"
           " master's validateShape via a row-size overload; JNI m3_select = master's uncached selectNative body"
           " generalised over int[]/long[] rows (cached int path unchanged and int-only) + selectLongNative export")
L2_NOTE = ("UNHOLD: #2517 held the origin's own tests (pkg-normalised equal to ebbfcbdf93aa's) for 11 x 'cannot find"
           " symbol selectLong'; moved back byte for byte to the HELD.tsv path")

# leaf, kind, path (EDIT) or (src, dst) (MOVE), pinned blob, decision, cited commits, rule note, edits (EDIT)
RULES = (
    ("L1", "EDIT", SRC + "M3IndexPathMasks.java", "1adc99a3825acb8c60c31769199b3bd01ef0c25a", "RESTORE",
     CITED, L1_NOTE, JAVA_EDITS),
    ("L1", "EDIT", NATIVE + "m3_index_path_masks.c", "1bf825b0b55d898578092b0250196d95dbb93a90", "RESTORE",
     CITED, L1_NOTE, C_EDITS),
    ("L2", "MOVE", (HELD + TEST + "M3IndexPathMasksLongSelfTest.java", TEST + "M3IndexPathMasksLongSelfTest.java"),
     "aa090bc23aa0dc3298e879b3377eceaea40dee92", "UNHOLD", CITED + ";held_by=#2517", L2_NOTE, None),
    ("L2", "MOVE", (HELD + TEST + "M3IndexPathMasksLongTest.java", TEST + "M3IndexPathMasksLongTest.java"),
     "85308c0dc6601f3dbda6f048ae0131f790d8fba1", "UNHOLD", CITED + ";held_by=#2517", L2_NOTE, None),
    ("L3", "EDIT", NATIVE + "m3_index_path_masks.c", "fe98290ee524a7730ea81bfcb47662d39cb16afe", "RESTORE",
     L20_CITED, L3_NOTE, C_L3_EDITS),
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


def plan_chain(root: Path, rel: str, rules):
    """Stages of one file's EDIT rules applied in order: [pinned preimage, after rule 1, after rule 2, ...].

    Rule k+1's pinned blob must be the git blob of rule k's postimage. The file on disk must equal one stage; the rules
    before that stage are applied (UNCHANGED), the rest are CHANGED. Anything else is drift (refusal)."""
    target = root / rel
    if not target.exists():
        raise Refused("missing preimage %s (pinned blob %s)" % (rel, rules[0][3]))
    on_disk = target.read_bytes()
    first_blob = rules[0][3]
    stages = [on_disk if blob_sha(on_disk) == first_blob else git_blob(root, first_blob)]
    for rule in rules:
        if blob_sha(stages[-1]) != rule[3]:
            raise Refused("chained rule %s on %s pins %s, previous stage is %s" % (
                rule[0], rel, rule[3], blob_sha(stages[-1])))
        stages.append(postimage(stages[-1], rule[7], rel))
    if on_disk not in stages:
        raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), first_blob))
    return target, stages, stages.index(on_disk)


def plan_move(root: Path, pair, blob: str):
    src, dst = root / pair[0], root / pair[1]
    if src.exists() and not dst.exists() and blob_sha(src.read_bytes()) == blob:
        data = src.read_bytes()
        return pair[0] + " -> " + pair[1], sha256(data), sha256(data), (dst, data, src)
    if not src.exists() and dst.exists() and blob_sha(dst.read_bytes()) == blob:
        data = dst.read_bytes()
        return pair[0] + " -> " + pair[1], sha256(data), sha256(data), None
    raise Refused("move %s -> %s: source/destination state does not match pinned blob %s" % (pair[0], pair[1], blob))


def run(root: Path, check: bool, leaf: str | None) -> int:
    rows, changed, applied, writes = [], 0, 0, []
    chains = {}
    for rule in RULES:
        if rule[1] == "EDIT":
            chains.setdefault(rule[2], []).append(rule)
    planned = {rel: plan_chain(root, rel, chain) for rel, chain in chains.items()}
    reached = {rel: plan[2] for rel, plan in planned.items()}  # stage index the run leaves the file at
    for rleaf, kind, where, blob, decision, cited, note, edits in RULES:
        if kind == "EDIT":
            target, stages, at = planned[where]
            k = chains[where].index((rleaf, kind, where, blob, decision, cited, note, edits))
            name, before, after = where, sha256(stages[k]), sha256(stages[k + 1])
            if k < at:
                state = "UNCHANGED"
            elif leaf not in (None, rleaf) or reached[where] != k:
                state = "PENDING"  # not selected, or an earlier rule of this file is still pending
            else:
                state = "CHANGED"
                reached[where] = k + 1
        else:
            name, before, after, work = plan_move(root, where, blob)
            state = "UNCHANGED" if work is None else "PENDING" if leaf not in (None, rleaf) else "CHANGED"
            if state == "CHANGED":
                writes.append(work)
        changed += state == "CHANGED"
        applied += state != "PENDING"
        rows.append((name, before, after, rleaf, decision, cited, kind + " " + note))
        print("%s %s %s %s" % (kind, rleaf, state, name))
    for rel, (target, stages, at) in planned.items():
        if reached[rel] != at:
            writes.append((target, stages[reached[rel]], None))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, data, remove in writes:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            if remove is not None:
                remove.unlink()
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS_RESTORE S1 recipe=%s %s base=%s origin=%s merge=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, ORIGIN[:12], DROPPING_MERGE[:12], len(rows), applied, changed,
        "CHANGED" if table_state else "UNCHANGED"))
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
