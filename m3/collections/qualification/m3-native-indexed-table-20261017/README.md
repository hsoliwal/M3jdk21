<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native M3 Indexed Table receiver — Java and existing JNI

**This is native M3 collection code, NOT a Guava, Eclipse, Apache, Spring,
or JDK adapter**. It belongs to the existing public M3JDK21
`com.m3.collections` product module. Source owner/recipe:
[Synexia PR #12185](https://github.com/hsoliwal/com.synexia/pull/12185),
exact recipe commit `ac6323c3186639136232b68ad9139f38af06cace`.
The public `recipe-mirror/` is an exact noncanonical copy for review and
offline Java21/Maven source replay; it is not a runtime dependency.

The original implementation is `M3IndexedTable<R,C,V>`, storing one
`Object[]` payload with parallel primitive `int[]` per-row, per-column
and global link/offset lanes, `byte[]` flags and free lists. Existing
`M3LinkedHashMap`, `M3LongLongHashMap`, `M3SlotArrays` and
`M3IndexMaskCompactor` remain the only key/hash/capacity/mask authorities.
No retained Cell/Entry/Node object, source-array duplication, arbitrary
Java object reference passed to JNI or new native symbol.

Functionality: null row/column/value support, exact coordinate presence,
stable global/row/column order, cell replacement/removal/GC payload clearing,
reused physical and row/column IDs, revision-rejected stale index views,
O(1) physical-index slices, reversed and stable value-sorted integer
permutations, 8-bit required/forbidden mask filtering. Java selection
allocates exact output; `NATIVE_REQUIRED` reuses the original
`m3_index_mask_compactor.c` C17 primitive-array JNI backend without
falling back. This class does **not** implement a vendor Table interface.

## Source-first gates

Recipe hash-pins three **ABSENT-before** product files: production Java,
dependency-free Java21 seeded/randomized self-test, JUnit adapter.
See `recipe-mirror/SOURCE_RECEIPT.tsv` and `verify.py`.
Fix transformations/templates in Synexia then rerun the recipe; never
patch generated receiver sources. A source is qualified only if all three
byte-match their templates and all required Java/JNI/SDK tests pass.

The actual code's **isolated** Java21 `javac --release 21 -proc:none
-Xlint:all -Werror` and standalone **62,986 assertions** passed for
6,000 randomized table ops and 8,000 axis/slot recycling cycles. That test
used API-shaped compile-only M3-owner stand-ins, not actual target owner code
or native C; **real product compilation and JNI are separate unexecuted
qualification gates**. The stock Java21 Maven generator self-test for
source SHA256, missing-target/drift refusal and fixed point passed without Maven.
Local Maven not installed. No speedup or full JDK superset is claimed.

Reviewer:
```sh
python3 m3/collections/qualification/m3-native-indexed-table-20261017/verify.py
mvn -B -ntp -f m3/collections/qualification/m3-native-indexed-table-20261017/recipe-mirror/pom.xml verify
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3IndexedTableTest test
```

The receiver workflow also strictly compiles against **real existing M3
source owners**, runs `--native` against the unchanged C17
`M3IndexMaskCompactor` JNI library with `-Xcheck:jni` and UBSan, and
repeats under `-Xint`. The source/test workflow is not a PASS until
it actually runs on the current GitHub PR head.

LeetCode #27/#283/#912, HackerRank BitSet and GfG sparse-matrix/search
families inform problem taxonomy only; no tutorial/submission code is
copied. Original first-party Apache-2.0 copyright, external donor
ownership and user-specified String→arrays→collections→AST order preserved.
**DRAFT/HOLD:** broader five-provider API superset, JDK installation,
GC/JIT/jtreg, Windows/ARM JNI, managed residency/spill and benchmarks
are not completed by this PR.
