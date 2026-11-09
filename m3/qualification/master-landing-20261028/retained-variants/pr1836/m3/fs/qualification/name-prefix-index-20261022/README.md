# M3JDK21: repeated name/prefix lookup on native directory physical indexes

Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0.

**One integrated Java21/JDK/HotSpot/JNI SDK**, never an M3 + JDK wrapper.
Original `M3DirectoryTable` remains the sole directory data owner.
This draft stacks on [M3FS range/indexed PR #1824](https://github.com/hsoliwal/M3jdk21/pull/1824)
and leaves all M3File SHA/capture/scratch, sequential ZIP/JRT streams, M3FS
native SQLite WAL/JNI, and original filtered/sorted collections intact.

## Why this use case and what was implemented

Maven/Gradle offline builds, Eclipse/JDT project explorations, source-root
path lookup and directory explorers frequently query a *single immutable
directory page* for many names and prefixes. Repeated
`DirectoryTable.entries()` + String sorting/filtering materializes
public objects on each query. The M3 alternative uses existing parent
`char[]` names, `int[]` offsets, stat/state mask columns and physical IDs.

**One new additive original-owner `nameIndex(allowedKinds)`:**
- Reuse the existing `selectedRows` kind-mask filter to build a detached
  physical `int[]` row page. No source entry/name/stat arrays are copied.
- Stable bottom-up primitive mergesort compares original UTF-16 code-unit
  slices, with physical original-row tie order even for duplicate names.
- The returned `NameIndex` retains only that sorted `int[]` and the
  immutable parent reference; `row(rank)`, `rows()`,
  `entries()`, `rowsWithPrefix(prefix)` and
  `entriesWithPrefix(prefix)` are read-only/detached as appropriate.
- Two monotone binary-search prefix bounds query the prepared index;
  suffix/prefix searches create detached `int[]` results or the
  existing on-demand immutable `DirEntry` projection. No retained
  String, DirEntry, tree/trie node or copied native payload per row.
- Strings follow **JDK `String.compareTo` and `startsWith` UTF-16
  code-unit semantics**, including supplementary characters and
  unusual unpaired surrogates, with no case-fold/Unicode normalization.
  This is a typed application index, not a redefinition of NIO
  provider-specific name-comparison/path semantics.

Preparation: O(n log n * compared name-code units) and one
temporary int[n] scratch plus one retained int[n] row array.
Repeated prefix query: O(log n * prefix-code-unit comparisons + k)
for k matching rows; callers should reuse the index for many queries.
This is a complexity argument, **not a measured speedup**.

## Source custody and algorithm catalogue

Canonical Maven/OpenRewrite [Synexia recipe PR (source branch)](https://github.com/hsoliwal/com.synexia/tree/chatgpt/m3jdk21-m3fs-name-prefix-recipe-20261022),
`com.synexia.m3.M3FsNamePrefixIndex20261022` reuses the
existing hash-pinned Java snapshot recipe, with exact SHA-256
before/after for the original `M3DirectoryTable.java` and
two ABSENT-preimage Java21/JUnit test owners. `RECEIVED_DONORS.tsv`
lists exact before/after Git blobs; `verify_received_blobs.py`
refuses anything else. The per-file postimage was read back from
GitHub and matched the canonical authoring template.

**Problem/category decisions** in Synexia: choose original M3FS
stable int-index mergesort and binary-search equal-range over
borrowed char slices; reject boxed public-object sorting and
per-character trie nodes for this immutable single-page workload.
LeetCode 208 (Trie), HackerRank Sparse Arrays, and
GeeksforGeeks binary search are reference/problem categories only,
not copied challenge source. Original first-party Apache-2.0 and
Commons VFS native donor notices remain unchanged.

## Actual proof versus admission holds

**Actually run locally:** Linux Java21
`javac --release 21 -Xlint:all -Werror` and `java -ea` on an
isolated algorithm-shaped `char[]/int[]/kind[]` representative:
`M3_DIRECTORY_NAME_INDEX_PASS checks=779223`.
That oracle compared sorted physical IDs and exact prefix results
against JDK String and stable comparator sorting with duplicate
names, all kind masks, surrogate UTF-16, random sources and detached
returned array behavior. **It did NOT build the actual original
M3DirectoryTable/M3FS source or JNI.**

**Product test source received:** original-package
`M3DirectoryNameIndexContract` and JUnit5 wrapper; exercises
actual M3DirectoryTable.fromEntries/decode, JDK oracle equivalence,
native-style UTF-8 name decoding, immutable projections, duplicates
and invalid input refusal.

**Unexecuted:** actual Synexia Maven/OpenRewrite `Recipe.run`,
product `m3/fs` Maven/JUnit/native SQLite JNI/CTest, source-built
M3JDK21/JMOD/HotSpot/jtreg/JCK/GC, ZIP/JRT/remote providers,
Windows/macOS/Android semantics, cross-library filtered/sorted
views and comparative CPU/heap/native memory measurements.

**DRAFT/HOLD** until full source-built SDK qualification. No merge
or universal performance claim is implied by source custody.
