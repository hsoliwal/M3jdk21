<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — source-sealed primitive long sorted masked-window receiver

**Product:** M3JDK21 is one integrated Java 21/HotSpot/JNI SDK. This change
extends the original M3LongSortedSnapshot owner, not an M3-over-JDK layer,
generic wrapper library, or duplicate collection/recipe compiler.

## Original source and algorithm category

Receiving class: `m3/collections/src/main/java/com/m3/collections/M3LongSortedSnapshot.java`.
Exact preimage Git blob: `138b09eee908f5b952658fbbe835c83f908e56fb`.
Exact generated postimage Git blob: `25ed2be76be528acb7ee0e7353b72823a8f622a6`.
Preimage SHA-256: `d075b50975cb8656eb6a6fec3ed95c8f9393d3f76e424c6e92b7991b3c11bb3c`.
Postimage SHA-256: `d49a6dedff6fdddbca9a13f33e627ffb7e7a17d57abe5fed97715d4a0c841f85`.

Problem categories: BOUNDED_SORTED_ARRAY_VALIDATION, INDEX_MASK_COMPACTION,
IMMUTABLE_RANK_SELECT. An input may contain unrelated/unsorted values before or
after its selected sorted window; only the inspected [rowOffset, rowOffset+length)
must be strictly increasing/unique. The independent byte-state offset is checked
before selection. Each accepted occurrence is gathered into exactly one new
primitive long[] by the original M3IndexMaskCompactor two-pass algorithm.

The existing M3LongSortedSnapshot `rank`, `select`, `indexOf`,
`subSet`, `headSet`, `tailSet`, and immutable/defensive-copy behavior
remain unmodified. The new two public overloads are additive, not replacements
for existing sorted snapshot factories. Original backed views and their epoch
and comparator contracts remain separate and unchanged.

**JNI** is not rewritten: the receiving call uses the pre-existing required
AND excluded mask `selectLongWindow` JNI ABI merged through M3JDK21 #1695.
Java is the default. Explicit NATIVE_REQUIRED fails if its library/ABI is
unavailable; the original no-fallback semantics are retained.

## Transformation recipe saved before source change

Named OpenRewrite recipe:
`com.synexia.m3.LongSortedMaskedWindow20261021` at
`m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-long-sorted-masked-window-20261021.yml`.
Uses existing Synexia `M3HashPinnedJavaSnapshotRecipe`, not a parallel
OpenRewrite compiler or patcher. Exact before/after source seals for three
files are in `m3-long-sorted-masked-window-20261021/manifest.tsv`.
All three materialized product/test Git blobs matched their templates
byte-for-byte on GitHub readback. Fix the template and rerun the recipe
instead of editing generated output directly.

## Verification and scope

Run `bash m3/collections/qualification/long-sorted-masked-window-20261021/verify.sh`.
The test script compiles the original Java 21 sorted/array/mask owners with
`javac --release 21 -Xlint:all -Werror`; runs 2,048 seeded scalar oracle
cases over independent physical offsets, required/excluded high-bit masks,
duplicates outside the window, signed ranges, rank/select, source isolation
and rejection behavior; builds the existing C11 JNI library using checked
Region operations; repeats the same tests with `-Xcheck:jni`.
Maven JUnit adapter runs Java cases and optional native verification with
`-Dm3.test.native=true`. GitHub Actions CI runs both Java and native lanes.

**Submission gate status:** GitHub source/template readback VERIFIED; local
full original source compile, true JNI execution, Maven module/JUnit,
OpenRewrite fixed point, full JDK image/HotSpot/jtreg/JCK,
Windows/Android JNI and benchmark comparison remain **NOT_RUN/HOLD**
until independent results exist. The script and CI are implementation,
not themselves evidence that the tests passed. No speculative performance
claim or other PR merge is made by this change.

## Source/donor catalogue

See `m3/collections/algorithm-catalog/long-sorted-mask-window-20261021.tsv`.
M3LongSortedSnapshot, M3LaneSearch and M3IndexMaskCompactor remain the
accepted first-party Apache-2.0 code owners. LeetCode binary search and
GeeksforGeeks lower/upper-bound are pattern references only; no solution
source is copied. HackerRank Ice Cream Parlor is rejected as a problem mismatch.

## Next contract-by-contract closure

The rest of the M3JDK21 filtered and sorted backed wrappers, parent-owned
dynamic array masks, JNI range APIs, M3File/M3AST build state and Chrome AI
hunk corrections have independent canonical owners. They must be qualified
file-by-file using recipe preimages, behavioral atoms and source-preserving
Maven/Java/JNI gates. This immutable snapshot pass does not change live views.
