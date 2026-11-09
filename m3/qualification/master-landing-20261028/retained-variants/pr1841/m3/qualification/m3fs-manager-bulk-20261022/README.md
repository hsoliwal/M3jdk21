# Original M3JDK21 M3FS manager-routed masked bulk operations

2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

This is the original Java21/JNI M3JDK21 SDK, never a second M3+JDK
runtime. This draft is based on [M3FS range-indexed #1824]
(https://github.com/hsoliwal/M3jdk21/pull/1824) and receives
the existing [M3FS manager donor #1749]
(https://github.com/hsoliwal/M3jdk21/pull/1749) with its original
canonical [Synexia #12846]
(https://github.com/hsoliwal/com.synexia/pull/12846) recipe and
catalogue; no alternative provider engine is introduced.

## Use case

Batch path metadata, directory creation, deletion, recursive delete,
copy and move across the providers already registered with the
original `com.m3.fs.vfs.FileSystemManager`. The existing
`M3FsBulk.Batch` owns command columns and `Result` owns byte status,
int metadata and indexed failures; the original manager remains the
sole authority for each provider's FileObject and URI parsing.

A caller-owned `long[]` bitmap selects physical command indexes, and
a checked `[first,first+count)` window admits only selected work in
encounter order. The original **qualified** first-party donor uses
`Long.numberOfTrailingZeros` to jump over empty 64-bit mask words:
`O(n/64 + k)` selected-word work rather than O(n) per-slot bit tests
for sparse k. This is an algorithm complexity claim, **not a measured
CPU improvement**. The selection mask is cloned before provider calls
and no per-row Node/Entry object graph or second payload is stored.

Each provider command succeeds/fails independently. A requested
atomic multi-provider batch is refused **before any provider I/O**;
no distributed transaction/rollback is fabricated. Partial effects,
fail-fast, callback/progress and original failure identity are
preserved by the original M3FS manager implementation.

Only `FileSystemManager.java` is modified: its exact old Git
blob `d77ad058b73a61ed304312053574029fa33643a0` matched
the canonical donor preimage before serial source receiving.
The additive methods are defaults, retaining all existing abstract
manager API. `M3FsManagerBulk.java`, owning original Java
test and two qualification documents are added as original
donor blobs (five exact postimages).

## Qualification and outstanding source/jni gates

- PASS: five exact GitHub donor source postimage comparisons, strict
  receiving scope and prior manager source preimage check.
- HISTORICAL DONOR: #1749 reports 10/10 isolated Java21 tests with
  minimal API-compatible stand-ins, including 320 randomized sparse
  mask trials. No actual original M3FS native/repacked Maven test
  was run on this combined receiver.
- NOT RUN: canonical Maven/OpenRewrite source recipe fixed-point
  and source-drift-refusal execution; original M3FS Maven/JUnit and
  native SQLite JNI/C17, ZIP/JRT cross-provider and provider lifetime
  tests, full M3JDK21 HotSpot/JMOD/jtreg, supported platforms,
  and CPU/heap/native-memory measurements.

Native `M3FsStatBatch` from original donor #1740 is **held as a
separate following PR**, because its historical `M3FS.java`
postimage is older than the 49 KiB current original source.
It must forward only the additive native batch method into the
latest owner, not overwrite the newer SDK file.

Review `RECEIVED_DONORS.tsv` for exact source object IDs; the
complete donor algorithm comparisons remain in the original
`m3/fs/qualification/manager-bulk-20261021/DONORS.tsv`.
No LeetCode/HackerRank/GfG challenge code copied; original
licenses and attribution retained.

DRAFT only; no implicit merge or performance claim.
