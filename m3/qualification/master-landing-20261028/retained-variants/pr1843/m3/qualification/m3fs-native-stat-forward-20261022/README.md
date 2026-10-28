# M3JDK21 forward-owned JNI SQLite stat batching

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

**M3JDK21 is one integrated Java 21 / HotSpot / JNI SDK.** This is
a stacked draft above [original M3FS manager bulk #1841]
(https://github.com/hsoliwal/M3jdk21/pull/1841). It adds
an existing first-party [native metadata donor #1740]
(https://github.com/hsoliwal/M3jdk21/pull/1740) with a
new exact-source-forward [canonical Synexia recipe #13073]
(https://github.com/hsoliwal/com.synexia/pull/13073).

## Why forward instead of copy?

The historical donor M3FS.java was 40.2 KiB. The current M3JDK21
original M3FS.java has reached 49.6 KiB through other SDK additions.
Receiving the older full source would delete newer native files,
module and project APIs. This recipe preserves the current original
49.6 KiB preimage and inserts ONLY the qualified
`statBatch(String...)` method after the unchanged `stat(String)`
atom. Original `M3FsNative.java` and `m3fs_jni.c`
preimages match the qualified donor exactly, so those two
complete Java/C postimages are safely reusable. Two new files
retain the original donor implementation and JUnit tests.

## Original M3FS Java/JNI data ownership

`M3FS.statBatch(String...)` checks a maximum of 4,096
caller-provided paths, clones their String[] before entering native,
and uses one additive M3FsNative JNI call through the existing
M3FS/SQLite handle. The original C handler leases that single
existing handle, probes paths individually and writes up to 32
fixed-stack rows at a time into the returned long[9*n].
Status, inode, size, generation, mtime, type, mode, links and block
size are retained as primitive long[] columns, with parallel
int[] row-state masks in `M3FsStatBatch`.

No per-file retained `M3FS.Stat` objects are created until
explicit public `stat(row)` access. File/dir/symlink/missing/error
states remain distinct, and selected/sorted results manipulate
physical int[] row indexes, preserving stable ties. No new
native SQL schema, JPEG/DTO, file wrapper, or JVM object state
manager is added. Original JNI namespace, handle lifetimes,
SQLite WAL and native cancellation/progress integration remain
the authoritative product owners.

A non-snapshot live SQLite handle does **not** magically provide
a single atomic multi-file observation. Each path receives its
own native status; callers requiring repeatability must use the
existing read-only pinned M3FS snapshot capability. Older native
libraries lacking this new JNI symbol refuse it explicitly,
rather than returning fabricated metadata.

## Recipe-first and proof boundaries

- Exact Java and C pre/post SHA-256 and ABSENT new paths: canonical
  `com.synexia.m3.M3FsNativeStatForward20261022`.
- Original Maven/OpenRewrite
  `M3HashPinnedJavaSnapshotRecipe` and
  `M3HashPinnedTextSnapshotRecipe` unchanged. Fixed-point,
  source drift and native text refusal test definitions retained.
- Source received serially: first original M3FS.java, then
  original M3FsNative.java, new immutable M3FsStatBatch,
  its original JUnit test and existing m3fs_jni.c native entry.
- See `RECEIVED_DONORS.tsv` for all five prior/next Git blobs.
  The selected/rejected categories and licensing decisions live
  in Synexia #13073; LeetCode/HackerRank/GeeksforGeeks supply
  algorithm taxonomy only and no challenge/editorial code is copied.

**Verified on this receiving branch:** source preimages match the
canonical recipe (new paths were absent); all five candidate
postimages match exact GitHub blobs. Historical donor #1740
had 318 synthetic Java assertions, C11 syntax and a mock JNI
`-Xcheck:jni` pass, not complete native SQLite/product tests.

**NOT RUN:** actual Maven/OpenRewrite Recipe.run/fixed point;
full original m3/fs Maven/JUnit and CMake/CTest/native SQLite JNI,
actual JNI generated-header/symbol/ABI validation on the
source-built JDK image, full M3JDK21 HotSpot/JMOD/jtreg/JCK,
cross-platform/provider stability, CPU, heap and native memory
comparison. A reduction in JNI crossings is not itself a
measured speedup. Keep DRAFT; no merge.
