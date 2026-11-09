# M3JDK21: original M3File one-read host inputs to completed build-state V2

<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

M3JDK21 is **one integrated Java21/HotSpot/JNI SDK**, not an M3 + JDK
layer. This original-owner product PR stacks on
[#1758](https://github.com/hsoliwal/M3jdk21/pull/1758) whose
`M3BuildStateStore` extends **current master V2** with independent
observed inventories and one successful-build head. It does not
replace source-built master V2 with older V1 checkpoints.

## Original data/control owners

- `M3File.observe(long)` retains the same NIO provider boundary,
  exact SHA-256/four big-endian digest words, input byte budget,
  file-key/size/mtime bracketing and error policy. The **new additive**
  `observe(long, M3StreamProgress, M3File.ByteObserver)` hands the
  same consumed chunk to a borrowed synchronous parser/analysis hook;
  no separate file read, parser, raw Java pointer, JNI export or
  persistent scratch buffer is added. Existing public/ledger entry
  points and their results stay unchanged.
- `M3BuildState` V2 receives a *package-private* host-row factory.
  It reuses its original immutable array/offset/mask columns and V2
  image/decoder; ABI and dependencies remain unknown/empty. The
  new small `M3HostFileInventory` coordinator is in the **existing
  com.m3.fs** package; it consumes a *caller-owned*
  `Stream<M3File>`. It does **not** implement a second directory
  traversal, a watcher, SQL parser, object graph, or native engine.
- The source supplies an entire input selection in one stream,
  which must be explicitly attested by the caller; an arbitrary
  truncated/filtered stream cannot prove complete project membership.
  Project root + stable selection identity form the inventory scope.
  The exact caller toolchain/build-policy fingerprint is separately
  bound into the resulting 32-byte context, used as the observed
  SQLite profile name. Incomparable profiles never invent removals.
- `Rows` holds growing String[], byte[], long[], SHA bytes and a
  **primitive int[] stable mergesort** order. Duplicate paths are
  refused. The compact final V2 inventory retains SHA256, length,
  role (including nested module POMs), last-modified-known bit mask,
  and exact added/modified/removed masks from the original
  `M3BuildState.InventoryDelta` merge join. No per-entry FileNode
  or Entry wrapper is retained.
- .git/.m3 control paths, symlinks, paths escaping the project root,
  invalid source kinds, count/byte/image overruns and cancellation
  fail before offering a completed fact image. Generic timestamps
  are hints, never evidence that unchanged size/time means equal
  content. A mutating filesystem can evade metadata bracketing;
  users requiring atomic revision must freeze inputs or rely on a
  provider-guaranteed generation.

## Original native SQLite transaction and build correctness

`M3HostFileInventory.stage` calls the existing profiled
`M3BuildStateStore.observe` implemented in #1758.
The original native M3FS SQLite/C/JNI WAL, optimistic
SHA commitment, writer lock and atomic rename remain untouched.
That operation **stages observation only**. Only a tool that
successfully runs its full compiler, annotation processors,
lifecycle plugins and output checks may call
`M3BuildStateStore.promoteObserved`, keeping one globally
authoritative successful head. A hash equality alone is not
permission to skip Maven/Gradle static analyzers or compilation.

`m3/fs/pom.xml` and `jdk.m3fs` module-info add a direct one-way
dependency on the original `com.m3.file` SDK module, not a
separate integration wrapper. The original M3 collections
(parent-owned dynamic arrays, index/offset/state masks),
filtered/sorted backed views, native collection JNI kernels,
M3AST/javac and other system contracts are preserved unchanged.

## Recipe, algorithm catalogue and tests

Canonical first-party source-sealed Maven/OpenRewrite recipes:
[Synexia #12877](https://github.com/hsoliwal/com.synexia/pull/12877).

- Java recipe `com.synexia.m3.HostStreamV2IngressJava20261021`
- Original Maven XML recipe `com.synexia.m3.HostStreamV2IngressPom20261021`

Each existing Java/module/POM target was matched **byte-for-byte**
against its pinned preimage before serial receiving. The new
files have an explicit ABSENT preimage. Hash-sealed postimages,
two-cycle fixed-point and source-drift-refusal tests,
category/donor evaluation, Apache-2.0 attribution and MOP
correction custody are all present in the Synexia source recipe.

The added M3FS JUnit `M3HostFileInventoryTest` checks:
cold/warm inventory, SHA despite same length/mtime, nested
module POM/build roles, added/removed masks, project/selection
scope changes, primitive hash equality, decoder round trip,
one-pass byte callback/progress parity, duplicate/escape/byte
budget and parse-failure refusal, and native SQLite observation
that remains distinct from successful-build promotion.

No LeetCode, HackerRank or GeeksforGeeks submissions were copied:
their problem categories were compared against existing first-
party hash, stable mergesort, array and JNI kernel donors.

## Gate state

Product/recipe exact GitHub pre/post source readback: **PASS**.
Definitions for the original Maven/OpenRewrite and owning JUnit
tests: **COMMITTED**. Full actual Maven/OpenRewrite recipe
execution, complete M3File/M3FS native SQLite/JNI/JUnit, JDK21
source-built HotSpot/JMOD/jtreg, Windows/Android/provider trials,
Maven/Gradle effective-model dependency graph reuse, five-library
filtered/sorted wrapper differential tests, and CPU/heap/native
memory benchmarks: **NOT RUN** in this receiving step.
No general speedup or complete all-PR merge is asserted.

**Keep this PR DRAFT.** The native module stack and original
compiler/task lifecycle must be qualified before merge into
the master SDK image.
