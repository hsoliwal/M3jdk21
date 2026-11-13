#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS2_FV_HANDOFF v1: the HANDOFF lines of qualification/mergeloss2-filtered-views-20261107.

#2695 (qualification/mergeloss2-filtered-views-20261107) recorded 82 lost lines of rows 17, 19, 27 and 35 as
HANDOFF because their owners were held (m3_index_mask_compactor.c, M3IndexPathMasks, M3IndexedListSnapshot,
M3LongIndexPathMasks). DECISION.tsv decides every line; this recipe applies the one RESTORE:
  rows 35 + 27 (origins 22c3d19339c / 01952e935de, retain merges 09ff95dd6cf3 #1513 / 1633da209622 #1619):
  the single-mask compactor exports (m3_select_range: selectNative/selectRangeNative/selectWindowNative, and
  selectLongWindowNative) fetch only the enclosing admitted span [first, last] of each 256-state page instead of
  the whole page. Master already skipped rejected pages (05a37a87a10) and trims both ends in the two-mask
  exports (adab56cdc85); the origin harnesses m3_sparse_mask_reads.c (row 35) and m3_mask_compactor_spans.c
  (row 27) fail on master and pass here. Row 35's m3_selected_span is restored (it replaces m3_first_selected,
  whose front scan it contains); row 27's m3_admit_chunk is the same transfer contract by another route and is
  not copied.
plus the origin tests of those rows, CREATEd from the retained-variant blobs master carries.
Derived from make/m3/collections/qualification/mergeloss2-filtered-views-20261107/apply.py v1 (anchored "sub" edits,
every anchor exactly once, LF payloads re-joined with the preimage's line ending, rules on one path chain, CREATE
from pinned donor blobs). An EDIT target must equal its pinned preimage blob (the rule applies) or the final
postimage (no-op); anything else is drift and the recipe refuses (exit 2) without writing. Idempotent; --check
writes nothing and exits 1 when a run would change a file. Each rule records its hunk-scoped 3-way.

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
BASE_COMMIT = "9556aff30c01"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin", "dropping_merge", "three_way",
                 "donor_blobs", "rule")
NATIVE = "m3/collections/src/main/native/collections/"
TNATIVE = "m3/collections/src/test/native/"
TEST = "m3/collections/src/test/java/jdk/m3/collections/"
DIR = "qualification/mergeloss2-fv-handoff-20261107"
A35 = ("//FLLDCIM: MRL - mergeloss2 leaves 35+27: enclosing admitted span restored from origin 22c3d19339c (PR #1513),"
       " dropped by retain merge 09ff95dd6cf3 (row 27: 01952e935de, #1619, 1633da209622); recipe " + DIR + "\n")


def anchor(leaf, origin, pr, merge, what):
    return ("//FLLDCIM: MRL - mergeloss2 leaf %s: %s of origin %s (PR %s), dropped by retain merge %s; recipe %s\n"
            % (leaf, what, origin, pr, merge, DIR))


class Refused(Exception):
    pass


# leaf, kind, path, preimage blob (EDIT) / donor blob (CREATE), origin, merge, 3-way, donor blobs, note, edits
RULES = (
    ("35+27", "EDIT", NATIVE + "m3_index_mask_compactor.c", "7fc85f1cb85bc4fde0e74d744419443b99a00493",
     "22c3d19339c+01952e935de", "09ff95dd6cf3+1633da209622",
     "row 35: base=15024c40ba9b theirs=795a55d3b864 ours=7fc85f1cb85b conflicts=7; row 27: base=cb33ba476927"
     " theirs=eaeef1c565dd conflicts=7 (master grew from 426 to 992 lines)",
     "origin post=795a55d3b864 pre=15024c40ba9b; row 27 post=eaeef1c565dd pre=cb33ba476927",
     "ADD: m3_selected_span verbatim in place of m3_first_selected (its front scan is the same loop; the doc keeps"
     " 'zero-mask never reads marks'); m3_select_range and selectLongWindowNative fetch rows [begin, stop) and"
     " compact from row_ids[i - begin] (the origin's loop bodies, master variable names); rejected pages keep the"
     " second state scan and drift check (05a37a87a10); exports, validation and exceptions unchanged", [
         ("/* First retained ordinal in one bounded marks page. Zero-mask never reads marks. */\n"
          "static jsize m3_first_selected(const jbyte *marks, jsize size, jint mask) {\n"
          "    if (mask == 0) return 0;\n"
          "    jsize first = 0;\n"
          "    while (first < size) {\n"
          "        unsigned int flags = (unsigned int)(unsigned char)marks[first];\n"
          "        if ((flags & (unsigned int)mask) == (unsigned int)mask) break;\n"
          "        first++;\n"
          "    }\n"
          "    return first;\n"
          "}\n",
          A35 +
          "/* Reuse the state lane to bound each row fetch. Empty selection chunks never\n"
          " * cross JNI for row values. Bounds are local offsets; no heap scratch or new ABI.\n"
          " * Zero-mask never reads marks: the whole page is the span.\n"
          " */\n"
          "static jsize m3_selected_span(const jbyte *marks, jsize length, jint mask, jsize *end) {\n"
          "    jsize first = 0;\n"
          "    *end = length;\n"
          "    if (mask == 0) return first;\n"
          "    while (first < length && (((unsigned char) marks[first] & (unsigned int) mask) != (unsigned int) mask))"
          " first++;\n"
          "    if (first == length) { *end = first; return first; }\n"
          "    while (*end > first && (((unsigned char) marks[*end - 1] & (unsigned int) mask) != (unsigned int) mask))"
          " (*end)--;\n"
          "    return first;\n"
          "}\n"),
         ("        jsize begin = m3_first_selected(marks, chunk, required_mask);\n"
          "        if (begin == chunk) {\n"
          "            /* Keep the second state scan/drift check, but do not fetch rejected payload. */\n"
          "            offset += chunk;\n"
          "            continue;\n"
          "        }\n"
          "        (*env)->GetIntArrayRegion(env, rows, first + offset, chunk, row_ids);\n"
          "        if ((*env)->ExceptionCheck(env)) return NULL;\n"
          "        jsize accepted = chunk;\n"
          "        if (required_mask != 0) {\n"
          "            accepted = 0;\n"
          "            for (jsize i = begin; i < chunk; i++) {\n"
          "                unsigned int bits = (unsigned int)(unsigned char)marks[i];\n"
          "                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {\n"
          "                    row_ids[accepted++] = row_ids[i];\n",
          "        " + A35 +
          "        jsize stop;\n"
          "        jsize begin = m3_selected_span(marks, chunk, required_mask, &stop);\n"
          "        if (begin == stop) {\n"
          "            /* Keep the second state scan/drift check, but do not fetch rejected payload. */\n"
          "            offset += chunk;\n"
          "            continue;\n"
          "        }\n"
          "        /* Only the enclosing admitted span crosses JNI. */\n"
          "        (*env)->GetIntArrayRegion(env, rows, first + offset + begin, stop - begin, row_ids);\n"
          "        if ((*env)->ExceptionCheck(env)) return NULL;\n"
          "        jsize accepted = stop - begin;\n"
          "        if (required_mask != 0) {\n"
          "            accepted = 0;\n"
          "            for (jsize i = begin; i < stop; i++) {\n"
          "                unsigned int bits = (unsigned int)(unsigned char)marks[i];\n"
          "                if ((bits & (unsigned int)required_mask) == (unsigned int)required_mask) {\n"
          "                    row_ids[accepted++] = row_ids[i - begin];\n"),
         ("        jsize begin = m3_first_selected(marks, chunk, mask);\n"
          "        if (begin == chunk) {\n"
          "            position += chunk;\n"
          "            continue;\n"
          "        }\n"
          "        (*env)->GetLongArrayRegion(env, rows, row_offset + position, chunk, values);\n"
          "        if ((*env)->ExceptionCheck(env)) return NULL;\n"
          "        jsize batch = chunk;\n"
          "        if (mask != 0) {\n"
          "            /* Stable prefix compaction: batch <= i, so unread values stay intact. */\n"
          "            batch = 0;\n"
          "            /* Prefix compaction cannot overwrite an unread row: batch <= i. */\n"
          "            for (jsize i = begin; i < chunk; i++) {\n"
          "                unsigned int flags = (unsigned int)(unsigned char)marks[i];\n"
          "                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i];\n",
          "        " + A35 +
          "        jsize stop;\n"
          "        jsize begin = m3_selected_span(marks, chunk, mask, &stop);\n"
          "        if (begin == stop) { position += chunk; continue; }\n"
          "        (*env)->GetLongArrayRegion(env, rows, row_offset + position + begin, stop - begin, values);\n"
          "        if ((*env)->ExceptionCheck(env)) return NULL;\n"
          "        jsize batch = stop - begin;\n"
          "        if (mask != 0) {\n"
          "            /* Stable prefix compaction: batch <= i - begin, so unread values stay intact. */\n"
          "            batch = 0;\n"
          "            for (jsize i = begin; i < stop; i++) {\n"
          "                unsigned int flags = (unsigned int)(unsigned char)marks[i];\n"
          "                if ((flags & (unsigned int)mask) == (unsigned int)mask) values[batch++] = values[i - begin];\n"),
     ]),
    ("35", "CREATE", TNATIVE + "m3_sparse_mask_reads.c", "24de90805f54e363081bcfb9536a0164d7d363fa", "22c3d19339c",
     "09ff95dd6cf3", "-", "retained pr1513 24de90805f54 (origin 1888952af540)",
     "CREATE the origin's JNI function-table harness (absent on master): single-match row units; fails on master"
     " (row_units 256 for one admitted state), passes on the restored span", [
         ("#include <jni.h>\n",
          anchor("35", "22c3d19339c", "#1513", "09ff95dd6cf3", "origin harness") + "#include <jni.h>\n"),
     ]),
    ("27", "CREATE", TNATIVE + "m3_mask_compactor_spans.c", "bc7cc6ac917ba1b2adfe218a6311f9fd59b06217", "01952e935de",
     "1633da209622", "-", "retained pr1619 bc7cc6ac917b (origin f7649a87332e)",
     "CREATE the origin's JNI function-table harness (absent on master): only enclosing admitted spans transfer;"
     " fails on master (elements=256), passes on the restored span", [
         ("#include <jni.h>\n",
          anchor("27", "01952e935de", "#1619", "1633da209622", "origin harness") + "#include <jni.h>\n"),
     ]),
    ("27", "CREATE", TEST + "M3MaskCompactorSpanContract.java", "4d44386cfb68c602ece65114c3a4744e7795bb3e",
     "01952e935de", "1633da209622", "-", "retained pr1619 4d44386cfb68 (origin 29838ab703c9)",
     "CREATE the origin's scalar oracle (Java backend by default, 'native' under -Xcheck:jni)", [
         ("package jdk.m3.collections;\n",
          anchor("27", "01952e935de", "#1619", "1633da209622", "origin contract") + "package jdk.m3.collections;\n"),
     ]),
    ("27", "CREATE", TEST + "M3MaskCompactorSpanTest.java", "8f10a24e6fef3c2f210bfc6565ba80dfcc75e595",
     "01952e935de", "1633da209622", "-", "retained pr1619 8f10a24e6fef (origin 538b93f5e94c)",
     "CREATE the origin's JUnit wrapper (Java backend)", [
         ("package jdk.m3.collections;\n",
          anchor("27", "01952e935de", "#1619", "1633da209622", "origin test") + "package jdk.m3.collections;\n"),
     ]),
    ("35", "CREATE", TEST + "M3SparseMaskReadContract.java", "53d4fad7ce495d651b170dade030159bbc09cf37",
     "22c3d19339c", "09ff95dd6cf3", "-", "retained pr1513 53d4fad7ce49 (origin 57a23e8d744d)",
     "CREATE the origin's real-JNI parity contract (main; requires the native library, run by the native gate)", [
         ("package jdk.m3.collections;\n",
          anchor("35", "22c3d19339c", "#1513", "09ff95dd6cf3", "origin contract") + "package jdk.m3.collections;\n"),
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
        raise Refused("pinned blob %s is not in this repository (%s)" % (blob, failure)) from failure


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


def chain(root: Path):
    staged, plan = {}, []
    for leaf, kind, rel, blob, origin, merge, three, donors, note, edits in RULES:
        if rel in staged:
            pre = before = staged[rel]
        else:
            pre = git_blob(root, blob)
            if blob_sha(pre) != blob:
                raise Refused("pinned blob %s does not hash to itself" % blob)
            before = b"" if kind == "CREATE" else pre
        post = postimage(pre, edits, rel)
        staged[rel] = post
        plan.append((leaf, kind, rel, before, post, origin, merge, three, donors, note))
    return plan, staged


def run(root: Path, check: bool) -> int:
    plan, finals = chain(root)
    firsts = {}
    for leaf, kind, rel, before, *_ in plan:
        firsts.setdefault(rel, (kind, before))
    writes, changed = [], 0
    for rel, final in finals.items():
        kind, first = firsts[rel]
        target = root / rel
        on_disk = target.read_bytes() if target.exists() else None
        if kind == "CREATE":
            if on_disk not in (None, final):
                raise Refused("CREATE target %s exists with other content (blob %s)" % (rel, blob_sha(on_disk)))
        elif on_disk is None:
            raise Refused("missing preimage %s" % rel)
        elif on_disk != final and on_disk != first:
            raise Refused("drifted preimage %s: on disk blob %s" % (rel, blob_sha(on_disk)))
        state = "UNCHANGED" if on_disk == final else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append((target, final))
        print("FILE %s %s" % (state, rel))
    rows = [(rel, sha256(before) if before else "ABSENT", sha256(post), leaf, origin, merge, three, donors,
             kind + " " + note) for leaf, kind, rel, before, post, origin, merge, three, donors, note in plan]
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, data in writes:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS2_FV_HANDOFF recipe=%s %s base=%s rules=%d files=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, len(rows), len(finals), changed, "CHANGED" if table_state else "UNCHANGED"))
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
