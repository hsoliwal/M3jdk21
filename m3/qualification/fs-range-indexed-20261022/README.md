# M3JDK21 M3FS/M3File range, indexed directory and provider-use-case convergence

Copyright 2026 Hitesh Soliwal and contributors.
SPDX-License-Identifier: Apache-2.0.

**M3JDK21 is the original integrated Java21/JDK/HotSpot/JNI SDK.**
This reviewable branch is stacked on
[M3JDK21 #1823](https://github.com/hsoliwal/M3jdk21/pull/1823),
which already contains native M3FS m3db SQLite/VFS, provider-aware
NIO ZIP/JRT, original one-read M3File.captureBytes and the 64KiB
reused project scratch buffer. Synexia is **not a runtime layer**.

## Accepted existing owners and precise use cases

**Seekable host M3File / source classes / chunk store:** the existing
one-source original `M3File` adds bounded sorted/disjoint physical byte-range
SHA256, copied offset/length arrays, and on-demand records projected from
primitive long[] digest words. The source is exactly original PR #1815
forward-adapted to this branch's richer M3File, **not** a historical
whole-file replacement that would lose captureBytes or shared scratch.
A range digest is never a claim that unselected bytes have not changed.

**Stream-only M3FS provider / ZIP or remote archive:** the existing
`MFileStreams.transferRanges` from #1815 consumes gaps via original
read-through progress/shadows, writes only selected bytes to a borrowed
caller output, and retains only 8KiB scratch. It cannot seek and must not
claim that a partial destination write can be rolled back. No new JNI
export or secondary content store is introduced.

**Native M3FS SQLite directory result and file-browser index:** the original
`M3DirectoryTable` from #1793 keeps its parent-owned char[] name payload,
int[] physical offsets, state flags and scalar metadata. Filter and
stable size-sort operations return primitive physical row-ID arrays or
read-only indexed views; DirEntry records are produced on access, not
retained once per entry. Native M3FS row layout, JNI/C and SQLite engine
are unchanged. The original comparator, inclusive range, occurrence
and sorted tie order are preserved.

**Strict SDK host+linked-image verifier:** `m3/file/verify.py` retains
its exact Java source/test set gate, adding explicit markers for the
original M3File observation, shared scratch and resource lock contracts
plus the new range contract. A remote check of this product branch
confirmed **13 test source files and 13 verifier map entries**, with
zero missing or surplus names. This is a source-census result, **not**
execution of the host or jlink/image verifier.

## Canonical recipes, donor cases and licence custody

[Synexia #13012](https://github.com/hsoliwal/com.synexia/pull/13012)
holds the reusable class-backed `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe`, source SHA256 pre- and postimages
and new-file ABSENT receipts, Maven/OpenRewrite two-cycle fixed point
and drift-refusal **test definitions**, category-indexed donor
comparisons and correction-memory entries. Qualified donors
[M3JDK21 #1815](https://github.com/hsoliwal/M3jdk21/pull/1815)
and [#1793](https://github.com/hsoliwal/M3jdk21/pull/1793)
plus their Synexia #12967/#12887 sources are reused.
No LeetCode/HackerRank/GeeksforGeeks challenge answer bodies are copied.
Original licence notices and source/JNI authority remain unchanged.

`RECEIVED_DONORS.tsv` lists seven exact product Git blob preimages
and generated postimages, original donor and canonical recipe IDs
and problem categories. The read-only `verify_received_blobs.py`
checks **all seven source files** and fails on path or content drift;
it performs no source mutation, testing or native compilation.

## Actual receipt versus remaining gates

**PASS:** Source preimage checks before serial per-file receiving;
seven canonical Java/test/Python postimages read back byte-for-byte.
Strict M3File test census 13/13 exact names.

**HISTORICAL SCOPED donor results, NOT rerun here:** #1815 reports an
isolated equivalent Java21 range test (1,200 trials/22,168 assertions)
and compressed ZIP sample; #1793 reports isolated directory sorter
(520 trials/3,134 assertions) and C11-Java row codec sample.
Neither proves full original M3FS JNI/SQLite/JUnit/HotSpot packaging.

**NOT RUN:** canonical Maven/OpenRewrite Recipe.run/fixed point,
original `m3/file/verify.py` on host plus linked SDK image,
M3FS native SQLite/C/JNI/JUnit on actual library and provider
lifetimes, HotSpot/JMOD/jtreg/JCK, Windows/macOS/Android and
network/ZIP/JRT provider matrix, full Maven/Gradle/IDE dependent
task impact and CPU/heap/native-memory benchmarks. No speedup,
snapshot atomicity or full SDK admission is claimed.

**DRAFT; do not merge** on source identity alone.
