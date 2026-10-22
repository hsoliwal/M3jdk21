# M3JDK21 original M3FS manager-routed bulk operations — superset receiver

Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0.

M3JDK21 is **one integrated Java21/JDK/HotSpot/JNI SDK**, not a
wrapper on top of the JDK. This is a reviewable stacked receiver
above [#1824](https://github.com/hsoliwal/M3jdk21/pull/1824),
which itself retains native M3FS SQLite m3db provider support,
nondefault NIO ZIP/JRT providers, the one-read M3File
capture/shared scratch, selected source byte ranges, and the
parent-owned native row index/mask sorted directory views.

## One original VFS manager dispatch and array-mask owner

Receive exact first-party
[M3JDK21 donor #1749](https://github.com/hsoliwal/M3jdk21/pull/1749),
canonical [Synexia recipe #12846](https://github.com/hsoliwal/com.synexia/pull/12846),
rather than invent another VFS manager or execute JNI per Java callback.

- New original `com.m3.fs.M3FsManagerBulk`: routes existing
  M3FsBulk.Batch stat/mkdir/delete/deleteRecursive/copy/move
  commands through the existing Commons VFS FileSystemManager;
  each source/target retains its registered provider and its
  filesystem semantics. Results reuse original packed columns.
- Existing original `com.m3.fs.vfs.FileSystemManager`: new
  contract-compatible default methods expose bulk operations
  without removing an existing public/protected descriptor,
  a prior per-provider hook or a native SQLite source.
- Selected command ordinals are a copied `long[]` packed physical
  index mask, with signed half-open windows and stable physical
  order. Sparse windows jump by 64-bit words and
  `Long.numberOfTrailingZeros`; no per-command retained
  Entry/Node/payload object. Unselected positions remain
  SKIPPED in original result lanes.
- Nonatomic cross-provider operations preserve fail-fast,
  checked exception, partial result and progress contracts.
  An atomic multi-provider batch is rejected before performing
  provider I/O; no transactional rollback or synthetic global
  filesystem capability is claimed.
- No additional JNI symbol, native database or persistent
  source/build scanner is created. The existing native SQLite
  and native mask/kernel owners remain authoritative.

## Exact source receipt

The original target FileSystemManager.java Git blob
`d77ad058b73a61ed304312053574029fa33643a0`
was checked before its source-sealed receiving. The new
M3FsManagerBulk and owning JUnit source were absent; the
original category and proof documents were also absent.
All five original donor Java/test/qualification postimages
were checked against exact GitHub source blob IDs after
serial file-by-file publication.

`RECEIVED_DONORS.tsv` records exact source old/new Git
blobs and problem categories. The read-only
`verify_received_blobs.py` verifies five source paths and
does not modify a generated target.

## Donor qualification and remaining original SDK gates

Canonical Synexia recipe #12846 records an actual algorithm-
category decision between per-row linear scans and bounded
64-bit word-jump selection, alternative Kotlin/Java wrappers,
licence provenance, exact Java source transformations and
MOP learning after test-stand-in corrections. LeetCode,
HackerRank and GeeksforGeeks are reference-only categories;
no competitive source answers were imported.

The donor PR #1749 reports **10 isolated Java21 contract
tests with minimal VFS/JUnit stand-ins** and 320 deterministic
mask windows. Those executions are **historical donor results**,
not a run of this integrated branch, real ZIP/JRT/native
SQLite manager or source-built JVM.

**NOT RUN here:** actual Maven/OpenRewrite Recipe.run and
fixed-point, original m3/fs Maven/JUnit against real providers
and JNI/SQLite CTest, full M3JDK21/HotSpot/JMOD/JTReg,
Windows/macOS/Android/provider race matrix, and CPU/heap/
native-memory/multi-provider throughput benchmarks.

Do not merge on source seals alone or claim a universal
JNI speedup. Product and recipe donors stay reviewable.
